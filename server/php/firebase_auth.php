<?php
// server/php/firebase_auth.php
// تحقق حقيقي وكامل من Firebase ID Token دون الحاجة لـ Firebase Admin SDK
// (لا يعتمد على أي مكتبة Composer خارجية — فقط curl + openssl المدمجتين في PHP).
//
// يتحقق من:
//   1) صحة توقيع JWT (RS256) عبر الشهادات العامة الرسمية من Google.
//   2) صلاحية الـ issuer (iss) ومطابقته لمشروع Firebase الصحيح.
//   3) صلاحية الـ audience (aud) ومطابقته لمعرّف المشروع (openou).
//   4) عدم انتهاء الصلاحية (exp) وأن وقت الإصدار منطقي (iat, auth_time).
//   5) وجود uid صالح (الحقل sub).
//
// المرجع الرسمي لهذه الخوارزمية:
// https://firebase.google.com/docs/auth/admin/verify-id-tokens#verify_id_tokens_using_a_third-party_jwt_library

declare(strict_types=1);

const FIREBASE_CERTS_URL = 'https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com';
const FIREBASE_CERTS_CACHE_FILE = __DIR__ . '/cache/firebase_certs.json';
const FIREBASE_CERTS_CACHE_TTL = 3600; // ساعة واحدة (نفس مدة صلاحية الشهادات تقريبًا)

class FirebaseAuthException extends \RuntimeException {}

/**
 * يتحقق من Firebase ID Token بالكامل ويعيد الحمولة (payload) عند النجاح،
 * أو يرمي FirebaseAuthException برسالة عربية واضحة عند الفشل.
 *
 * @return array{uid:string, email:?string, claims:array}
 */
function verifyFirebaseIdToken(string $idToken): array {
    $parts = explode('.', $idToken);
    if (count($parts) !== 3) {
        throw new FirebaseAuthException('رمز التفويض ليس بصيغة JWT صالحة');
    }

    [$headerB64, $payloadB64, $signatureB64] = $parts;

    $header = json_decode(base64UrlDecode($headerB64), true);
    $payload = json_decode(base64UrlDecode($payloadB64), true);
    $signature = base64UrlDecode($signatureB64);

    if (!is_array($header) || !is_array($payload)) {
        throw new FirebaseAuthException('تعذّر فك ترميز رأس أو حمولة الرمز');
    }

    if (($header['alg'] ?? '') !== 'RS256') {
        throw new FirebaseAuthException('خوارزمية التوقيع غير مدعومة، المتوقع RS256');
    }

    $kid = $header['kid'] ?? null;
    if (!$kid) {
        throw new FirebaseAuthException('رمز التفويض لا يحتوي على معرّف مفتاح (kid)');
    }

    // 1) التحقق من التوقيع عبر الشهادة العامة المطابقة لـ kid
    $certs = fetchGoogleCerts();
    $pem = $certs[$kid] ?? null;
    if (!$pem) {
        throw new FirebaseAuthException('لم يتم العثور على شهادة مطابقة لهذا الرمز (قد تكون الشهادات قديمة)');
    }

    $publicKey = openssl_pkey_get_public($pem);
    if ($publicKey === false) {
        throw new FirebaseAuthException('فشل تحميل المفتاح العام من الشهادة');
    }

    $signedData = $headerB64 . '.' . $payloadB64;
    $verified = openssl_verify($signedData, $signature, $publicKey, OPENSSL_ALGO_SHA256);
    if ($verified !== 1) {
        throw new FirebaseAuthException('توقيع الرمز غير صالح — قد يكون الرمز مزوّرًا أو تالفًا');
    }

    // 2) التحقق من المطالبات (claims)
    $now = time();
    $projectId = FIREBASE_PROJECT_ID;

    $exp = (int) ($payload['exp'] ?? 0);
    $iat = (int) ($payload['iat'] ?? 0);
    $authTime = (int) ($payload['auth_time'] ?? $iat);
    $aud = $payload['aud'] ?? '';
    $iss = $payload['iss'] ?? '';
    $sub = $payload['sub'] ?? '';

    if ($exp <= $now) {
        throw new FirebaseAuthException('انتهت صلاحية رمز التفويض، الرجاء تسجيل الدخول من جديد');
    }
    if ($iat > $now + 60) { // هامش تسامح 60 ثانية لفروق الساعة
        throw new FirebaseAuthException('وقت إصدار الرمز غير منطقي (في المستقبل)');
    }
    if ($authTime > $now + 60) {
        throw new FirebaseAuthException('وقت مصادقة المستخدم غير منطقي (في المستقبل)');
    }
    if ($aud !== $projectId) {
        throw new FirebaseAuthException('الرمز صادر لمشروع Firebase مختلف (aud غير مطابق)');
    }
    if ($iss !== "https://securetoken.google.com/{$projectId}") {
        throw new FirebaseAuthException('مُصدر الرمز (iss) غير مطابق لمشروع أوبو');
    }
    if ($sub === '' || strlen($sub) > 128) {
        throw new FirebaseAuthException('معرّف المستخدم (sub) داخل الرمز غير صالح');
    }

    return [
        'uid' => $sub,
        'email' => $payload['email'] ?? null,
        'claims' => $payload,
    ];
}

/** يجلب شهادات Google العامة مع تخزين مؤقت محلي لتفادي طلب شبكة عند كل تحقق. */
function fetchGoogleCerts(): array {
    if (is_file(FIREBASE_CERTS_CACHE_FILE) &&
        (time() - filemtime(FIREBASE_CERTS_CACHE_FILE)) < FIREBASE_CERTS_CACHE_TTL) {
        $cached = json_decode((string) file_get_contents(FIREBASE_CERTS_CACHE_FILE), true);
        if (is_array($cached)) return $cached;
    }

    $ch = curl_init(FIREBASE_CERTS_URL);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 8,
        CURLOPT_FAILONERROR => true,
    ]);
    $response = curl_exec($ch);
    $error = curl_error($ch);
    curl_close($ch);

    if ($response === false) {
        // في حال فشل الشبكة، استخدم النسخة المخزّنة مؤقتًا حتى لو انتهت صلاحيتها (أفضل من رفض كل الطلبات)
        if (is_file(FIREBASE_CERTS_CACHE_FILE)) {
            $cached = json_decode((string) file_get_contents(FIREBASE_CERTS_CACHE_FILE), true);
            if (is_array($cached)) return $cached;
        }
        throw new FirebaseAuthException('تعذّر جلب شهادات Google للتحقق من الرمز: ' . $error);
    }

    $certs = json_decode($response, true);
    if (!is_array($certs)) {
        throw new FirebaseAuthException('استجابة شهادات Google غير صالحة');
    }

    $cacheDir = dirname(FIREBASE_CERTS_CACHE_FILE);
    if (!is_dir($cacheDir)) mkdir($cacheDir, 0755, true);
    file_put_contents(FIREBASE_CERTS_CACHE_FILE, json_encode($certs));

    return $certs;
}

function base64UrlDecode(string $data): string {
    $padded = str_pad(strtr($data, '-_', '+/'), strlen($data) % 4 === 0 ? strlen($data) : strlen($data) + (4 - strlen($data) % 4), '=');
    $decoded = base64_decode($padded, true);
    if ($decoded === false) {
        throw new FirebaseAuthException('فشل فك ترميز Base64Url');
    }
    return $decoded;
}
