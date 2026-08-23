<?php
// إعدادات مشتركة لخادم أوبو المساعد (OPOU PHP Helper Backend)
// هذا الخادم مساعد اختياري بجانب Firebase — يُستخدم لمهام لا تُنفَّذ جيدًا
// مباشرة من التطبيق مثل: توليد صور مصغّرة، فحص محتوى، أو Webhook للإشعارات.

declare(strict_types=1);
header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');

// عدّل هذه القيم حسب بيئة الإنتاج لديك
const FIREBASE_PROJECT_ID   = 'openou';
const FIREBASE_DB_URL       = 'https://openou-default-rtdb.firebaseio.com';
const FCM_SERVER_KEY        = ''; // ضع مفتاح FCM من إعدادات مشروع Firebase (سيرفر)
const MAX_UPLOAD_BYTES      = 5 * 1024 * 1024; // 5MB
const ALLOWED_IMAGE_TYPES   = ['image/jpeg', 'image/png', 'image/webp'];

function json_response(array $data, int $status = 200): void {
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function require_bearer_token(): string {
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (!preg_match('/^Bearer\s+(.+)$/i', $header, $m)) {
        json_response(['error' => 'مفقود رمز التفويض (Authorization Bearer)'], 401);
    }
    return $m[1];
}
