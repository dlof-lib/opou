<?php
// server/php/upload_avatar.php
// يستقبل صورة الأفتار/البانر، يتحقق منها، يُصغّرها، ويعيد اسم الملف النهائي
// ليقوم التطبيق برفعه إلى Firebase Storage أو تخزينه على هذا الخادم.

require_once __DIR__ . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(['error' => 'الطريقة غير مسموحة'], 405);
}

require_bearer_token(); // التحقق الفعلي من توقيع Firebase ID Token (راجع firebase_auth.php)

if (!isset($_FILES['image'])) {
    json_response(['error' => 'لم يتم إرسال صورة'], 400);
}

$file = $_FILES['image'];

if ($file['error'] !== UPLOAD_ERR_OK) {
    json_response(['error' => 'فشل رفع الملف'], 400);
}

if ($file['size'] > MAX_UPLOAD_BYTES) {
    json_response(['error' => 'حجم الصورة يتجاوز الحد المسموح (5MB)'], 400);
}

$mime = mime_content_type($file['tmp_name']);
if (!in_array($mime, ALLOWED_IMAGE_TYPES, true)) {
    json_response(['error' => 'نوع الملف غير مدعوم'], 400);
}

$uploadDir = __DIR__ . '/uploads/avatars/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

$ext = $mime === 'image/png' ? 'png' : ($mime === 'image/webp' ? 'webp' : 'jpg');
$filename = bin2hex(random_bytes(16)) . '.' . $ext;
$destination = $uploadDir . $filename;

// تصغير الصورة إلى حد أقصى 512x512 مع الحفاظ على النسبة
[$width, $height] = getimagesize($file['tmp_name']);
$maxDim = 512;
$scale = min(1, $maxDim / max($width, $height));
$newWidth = (int) round($width * $scale);
$newHeight = (int) round($height * $scale);

$srcImage = match ($mime) {
    'image/png' => imagecreatefrompng($file['tmp_name']),
    'image/webp' => imagecreatefromwebp($file['tmp_name']),
    default => imagecreatefromjpeg($file['tmp_name']),
};

$dstImage = imagecreatetruecolor($newWidth, $newHeight);
if ($mime === 'image/png') {
    imagealphablending($dstImage, false);
    imagesavealpha($dstImage, true);
}
imagecopyresampled($dstImage, $srcImage, 0, 0, 0, 0, $newWidth, $newHeight, $width, $height);

match ($ext) {
    'png' => imagepng($dstImage, $destination),
    'webp' => imagewebp($dstImage, $destination),
    default => imagejpeg($dstImage, $destination, 85),
};

imagedestroy($srcImage);
imagedestroy($dstImage);

json_response([
    'success' => true,
    'filename' => $filename,
    'url' => '/server/php/uploads/avatars/' . $filename,
]);
