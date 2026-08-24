package com.OPEN.OU.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * يحفظ صور التطبيق (صورة فقرة، صورة رمزية، بانر) في معرض الجهاز داخل ألبوم
 * "OPOU" الخاص، سواء كان مصدر الصورة نصّ Base64 مخزّنًا في قاعدة البيانات
 * أو رابط URL خارجي. يعتمد على MediaStore (بدون أي إذن مطلوب) على أندرويد 10
 * فما فوق، وعلى كتابة مباشرة لمجلد الصور العام (يتطلب إذن التخزين على
 * الإصدارات الأقدم) على أندرويد 9 فما دون.
 */
object ImageDownloader {

    /**
     * يحمّل الصورة (فك ترميز Base64 أو تنزيل من الرابط) ثم يحفظها في المعرض.
     * يُعيد true عند النجاح. يجب استدعاؤها من داخل coroutine (تعمل على Dispatchers.IO).
     */
    suspend fun saveToGallery(context: Context, base64: String?, imageUrl: String?): Boolean =
        withContext(Dispatchers.IO) {
            val bitmap = loadBitmap(context, base64, imageUrl) ?: return@withContext false
            runCatching { writeToGallery(context, bitmap) }.getOrDefault(false)
        }

    private suspend fun loadBitmap(context: Context, base64: String?, imageUrl: String?): Bitmap? {
        if (!base64.isNullOrBlank()) {
            return runCatching {
                val bytes = NativeBridge.decodeBase64(base64)
                if (bytes.isEmpty() || bytes.size > 8 * 1024 * 1024 || !NativeBridge.validateImageBytes(bytes)) {
                    null
                } else {
                    // Native fingerprint avoids reprocessing identical payloads in future cache layers.
                    NativeBridge.fastByteHash(bytes)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }.getOrNull()
        }
        if (!imageUrl.isNullOrBlank()) {
            return runCatching {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                    .allowHardware(false) // نحتاج Bitmap قابلة للقراءة مباشرة للحفظ
                    .build()
                val result = context.imageLoader.execute(request)
                (result.drawable as? BitmapDrawable)?.bitmap
            }.getOrNull()
        }
        return null
    }

    private fun writeToGallery(context: Context, bitmap: Bitmap): Boolean {
        val fileName = "OPOU_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OPOU")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            val written = resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            } ?: false
            if (written) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                resolver.delete(uri, null, null)
            }
            written
        } else {
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val opouDir = File(picturesDir, "OPOU")
            if (!opouDir.exists() && !opouDir.mkdirs()) return false
            val file = File(opouDir, fileName)
            val written = FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (written) {
                // إشعار الماسح الضوئي للوسائط حتى تظهر الصورة فورًا في تطبيقات المعرض الأخرى
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
            }
            written
        }
    }
}
