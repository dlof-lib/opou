<?php
// server/php/base64_image.php
// نقطة نهاية احترافية وقوية لمعالجة صور Base64 القادمة من التطبيق:
// تتحقق من الحمولة، تفكّ ترميزها بأمان، تتحقق من كونها صورة حقيقية (magic bytes
// وليس فقط امتداد الملف)، تضغطها بذكاء (تكيّفي) لأصغر حجم ممكن بجودة مقبولة،
// وتعيد Base64 نظيفة جاهزة للتخزين المباشر في Firebase Realtime Database.
//
// هذا يكمّل — وليس يستبدل — التحويل الذي يتم أصلًا داخل التطبيق عبر
// ImageCodec.kt (Kotlin) + base64.cpp (C++/JNI)، ويُستخدم كطبقة تحقق ثانية
// من جهة الخادم (Defense in Depth) عند الحاجة لمسار خادمي.

require_once __DIR__ . '/config.php';

const MAX_BASE64_INPUT_BYTES = 12 * 1024 * 1024; // 12MB قبل فك الترميز (حد أمان صارم)
const TARGET_OUTPUT_BYTES    = 700 * 1024;        // الحجم المستهدف بعد الضغط
const MAX_DIMENSION           = 1600;              // أقصى بُعد (عرض/ارتفاع) بالبكسل

function fail(string $message, int $status = 400): void {
    json_response(['success' => false, 'error' => $message], $status);
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    fail('الطريقة غير مسموحة، استخدم POST', 405);
}

$uid = require_bearer_token(); // uid موثّق فعليًا من توقيع Firebase (وليس مجرد وجود الرمز)

$raw = file_get_contents('php://input');
if ($raw === false || strlen($raw) === 0) {
    fail('لم يتم إرسال أي بيانات');
}
if (strlen($raw) > MAX_BASE64_INPUT_BYTES) {
    fail('حجم البيانات المرسلة أكبر من الحد المسموح (12MB)');
}

$payload = json_decode($raw, true);
if (!is_array($payload) || empty($payload['base64'])) {
    fail('الحقل base64 مطلوب داخل جسم JSON');
}

// إزالة بادئة Data URI إن وُجدت (مثال: data:image/jpeg;base64,....)
$base64 = $payload['base64'];
if (preg_match('#^data:image/[a-zA-Z0-9.+-]+;base64,#', $base64)) {
    $base64 = preg_replace('#^data:image/[a-zA-Z0-9.+-]+;base64,#', '', $base64);
}

// تنظيف صارم: Base64 صحيح يحتوي فقط على هذه الرموز
$base64 = preg_replace('/[^A-Za-z0-9+\/=]/', '', $base64);

$decoded = base64_decode($base64, true);
if ($decoded === false || strlen($decoded) === 0) {
    fail('فشل فك ترميز Base64 — البيانات غير صالحة');
}

// التحقق الحقيقي من كون البيانات صورة فعلية (وليس مجرد الوثوق بالامتداد/النوع المُرسَل)
$imageInfo = @getimagesizefromstring($decoded);
if ($imageInfo === false) {
    fail('البيانات المفكوكة ليست صورة صالحة (فشل التحقق من البصمة الحقيقية)');
}

[$width, $height, $type] = $imageInfo;
$supportedTypes = [IMAGETYPE_JPEG, IMAGETYPE_PNG, IMAGETYPE_WEBP];
if (!in_array($type, $supportedTypes, true)) {
    fail('نوع الصورة غير مدعوم — المسموح: JPEG, PNG, WEBP');
}

$source = @imagecreatefromstring($decoded);
if ($source === false) {
    fail('تعذّر إنشاء الصورة من البيانات المفكوكة');
}

// تصغير الأبعاد إذا تجاوزت الحد الأقصى، مع الحفاظ على النسبة
$scale = min(1.0, MAX_DIMENSION / max($width, $height));
$newWidth = max(1, (int) round($width * $scale));
$newHeight = max(1, (int) round($height * $scale));

$resized = imagecreatetruecolor($newWidth, $newHeight);
imagealphablending($resized, false);
imagesavealpha($resized, true);
imagecopyresampled($resized, $source, 0, 0, 0, 0, $newWidth, $newHeight, $width, $height);
imagedestroy($source);

// ضغط تكيّفي: يخفّض الجودة تدريجيًا حتى الوصول للحجم المستهدف أو حد أدنى للجودة
$quality = 90;
$outputBytes = '';
for ($attempt = 0; $attempt < 8; $attempt++) {
    ob_start();
    imagejpeg($resized, null, $quality);
    $outputBytes = ob_get_clean();

    if (strlen($outputBytes) <= TARGET_OUTPUT_BYTES || $quality <= 35) {
        break;
    }
    $overshoot = strlen($outputBytes) / TARGET_OUTPUT_BYTES;
    $step = (int) min(20, max(5, 10 * $overshoot));
    $quality = max(35, $quality - $step);
}
imagedestroy($resized);

$cleanBase64 = base64_encode($outputBytes);

json_response([
    'success' => true,
    'uid' => $uid,
    'base64' => $cleanBase64,
    'mimeType' => 'image/jpeg',
    'width' => $newWidth,
    'height' => $newHeight,
    'byteSize' => strlen($outputBytes),
    'quality' => $quality,
]);
