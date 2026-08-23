<?php
// server/php/notify.php
// نقطة نهاية بسيطة يستدعيها التطبيق (أو Cloud Function) لإرسال إشعار FCM
// عند حدوث: تعليق جديد، تيك جديد (متابع)، أو تفاعل على فقرة.

require_once __DIR__ . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(['error' => 'الطريقة غير مسموحة'], 405);
}

require_bearer_token();

$input = json_decode(file_get_contents('php://input'), true);
$targetToken = $input['targetToken'] ?? null;
$title = $input['title'] ?? 'OPOU';
$body = $input['body'] ?? '';

if (!$targetToken || FCM_SERVER_KEY === '') {
    json_response(['error' => 'بيانات ناقصة أو مفتاح FCM غير مهيأ'], 400);
}

$payload = json_encode([
    'to' => $targetToken,
    'notification' => [
        'title' => $title,
        'body' => $body,
        'sound' => 'default',
    ],
]);

$ch = curl_init('https://fcm.googleapis.com/fcm/send');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_HTTPHEADER => [
        'Content-Type: application/json',
        'Authorization: key=' . FCM_SERVER_KEY,
    ],
    CURLOPT_POSTFIELDS => $payload,
]);
$result = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

json_response(['success' => $httpCode === 200, 'fcmResponse' => json_decode($result, true)]);
