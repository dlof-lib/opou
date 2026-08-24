<?php
// server/php/notify.php
// نقطة نهاية بسيطة يستدعيها التطبيق (أو Cloud Function) لإرسال إشعار FCM
// عند حدوث: تعليق جديد، تيك جديد (متابع)، تفاعل على فقرة، أو بث فقرة عامة جديدة لكل المستخدمين.
//
// وجهة الإرسال واحدة من اثنتين (targetToken أو topic):
// - targetToken: جهاز مستخدم واحد بعينه (تعليق/تيك/تفاعل — إشعار شخصي).
// - topic      : كل الأجهزة المشتركة في موضوع بث معيّن (مثال: opou_new_paragraphs)،
//                يصل لكل مستخدمي التطبيق دفعة واحدة دون الحاجة لتخزين توكنات فردية.

require_once __DIR__ . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(['error' => 'الطريقة غير مسموحة'], 405);
}

require_bearer_token();

// قائمة مواضيع البث المسموح بها فقط — بدون هذا القيد كان أي مستخدم موثّق (وليس فقط
// الخادم/الإدارة) يقدر يبعث إشعار بث لكل مستخدمي التطبيق دفعة واحدة (سبام/تصيّد)
// عبر تمرير أي اسم "topic" يختاره. الإشعارات الشخصية (targetToken) غير مقيّدة بهذه
// القائمة لأنها تصل جهازًا واحدًا فقط (تعليق/تيك/تفاعل عادي).
const ALLOWED_BROADCAST_TOPICS = ['opou_new_paragraphs'];

$input = json_decode(file_get_contents('php://input'), true);
$targetToken = $input['targetToken'] ?? null;
$topic = $input['topic'] ?? null;

if ($topic !== null && !in_array($topic, ALLOWED_BROADCAST_TOPICS, true)) {
    json_response(['error' => 'موضوع بث غير مسموح به'], 403);
}

// حدّ أقصى معقول لطول العنوان/النص يمنع حمولات ضخمة أو محاولات إساءة استخدام الإشعارات
$title = mb_substr((string) ($input['title'] ?? 'OPOU'), 0, 100);
$body = mb_substr((string) ($input['body'] ?? ''), 0, 500);

// بيانات إضافية (data payload) تُستخدم من جهة التطبيق لبناء إشعار غني
// (نوع الإشعار، معرّف الفقرة، اسم الناشر، مقتطف من المحتوى...).
$dataPayload = array_filter([
    'type' => $input['type'] ?? null,
    'postId' => $input['postId'] ?? null,
    'authorUsername' => $input['authorUsername'] ?? null,
    'preview' => $input['preview'] ?? null,
], fn($value) => $value !== null);

if ((!$targetToken && !$topic) || FCM_SERVER_KEY === '') {
    json_response(['error' => 'بيانات ناقصة (targetToken أو topic) أو مفتاح FCM غير مهيأ'], 400);
}

// الوجهة: جهاز واحد بتوكنه مباشرة، أو موضوع بث بصيغة "/topics/الاسم"
$destination = $targetToken ?: ('/topics/' . $topic);

$payload = json_encode([
    'to' => $destination,
    'notification' => [
        'title' => $title,
        'body' => $body,
        'sound' => 'default',
    ],
    'data' => $dataPayload,
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
