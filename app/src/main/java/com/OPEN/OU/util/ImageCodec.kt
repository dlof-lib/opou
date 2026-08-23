package com.OPEN.OU.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * محوّل صور احترافي إلى Base64 لتخزينها مباشرة داخل Firebase Realtime Database
 * (بدون الحاجة لـ Firebase Storage). يقوم بما يلي بشكل تلقائي وقوي:
 *
 * 1) قراءة الصورة بأمان (Stream) بدون تحميلها كاملة إلى الذاكرة أولًا (inJustDecodeBounds).
 * 2) تصغيرها (Downsampling) بذكاء حسب أبعادها الأصلية لتفادي OutOfMemoryError.
 * 3) تصحيح الدوران تلقائيًا حسب بيانات EXIF (مشكلة شائعة جدًا في صور الكاميرا).
 * 4) ضغط تكيّفي (Adaptive Quality Compression): يخفّض الجودة تدريجيًا حتى يصل
 *    الحجم النهائي إلى الحد المستهدف (لأن عقد Realtime Database لها حد أقصى للحجم).
 * 5) ترميز Base64 عبر الطبقة الأصلية (C++) لأعلى أداء ممكن.
 *
 * النتيجة: سلسلة Base64 جاهزة للتخزين مباشرة في حقل مثل Post.imageBase64
 * أو User.avatarBase64، وأيضًا segments بيانات Data URI جاهزة للعرض الفوري.
 */
object ImageCodec {

    /** أنماط الاستخدام المختلفة، لكل منها حد أقصى للأبعاد وحجم الملف المستهدف. */
    enum class ImageProfile(val maxDimension: Int, val targetBytes: Int) {
        AVATAR(512, 180_000),      // صورة رمزية: مربعة صغيرة، جودة عالية
        BANNER(1280, 350_000),     // بانر الغرفة: عريض
        // كانت القيمة سابقًا 700_000 بايت، والتي تتحوّل بعد ترميز Base64 (تضخّم ~4/3)
        // إلى ~933,000 حرف — أي أكبر بالفعل من الحد الآمن المفروض في PostRepository
        // (900,000 حرف)، ما كان يجعل رفع أي صورة فقرة كبيرة يفشل دائمًا تقريبًا.
        // خُفِّضت إلى 600_000 بايت (~800,000 حرف بعد الترميز) لتبقى دومًا ضمن الحد الآمن.
        POST_IMAGE(1600, 600_000)  // صورة داخل فقرة: أكبر مساحة مسموحة
    }

    /**
     * الحد الأقصى الآمن لطول نص Base64 النهائي الذي يُعيده [encode] — يجب أن يبقى
     * أقل من الحد المفروض في PostRepository.MAX_SAFE_FIELD_BYTES (900,000 حرف) بهامش
     * أمان مريح. يُستخدم كشبكة أمان أخيرة للصور المعقّدة جدًا التي لا تضغط جيدًا حتى
     * عند أدنى جودة مسموحة، بدل إرجاع نتيجة سيُرفضها الخادم لاحقًا دون أن يدري المستخدم لماذا.
     */
    private const val SAFE_BASE64_CHAR_LIMIT = 850_000

    /** أصغر أبعاد مسموح بالنزول إليها أثناء إعادة المحاولة، حتى تبقى الصورة مقروءة وواضحة. */
    private const val MIN_SHRINK_DIMENSION = 320

    data class EncodedImage(
        val base64: String,
        val mimeType: String,
        val width: Int,
        val height: Int,
        val byteSize: Int
    ) {
        /** جاهزة للعرض الفوري في Compose (Coil) أو WebView عبر Data URI. */
        val dataUri: String get() = "data:$mimeType;base64,$base64"
    }

    /**
     * يحوّل Uri لصورة (من منتقي الصور) إلى EncodedImage مضغوطة ومُحسّنة.
     * يرمي IllegalArgumentException إن تعذّرت قراءة أو فك ترميز الصورة (رسالة عربية واضحة).
     */
    fun encode(context: Context, uri: Uri, profile: ImageProfile): EncodedImage {
        val bounds = readBounds(context, uri)
            ?: throw IllegalArgumentException("تعذّر قراءة أبعاد الصورة")

        val sampleSize = calculateSampleSize(bounds.first, bounds.second, profile.maxDimension)

        val rawBitmap = decodeSampled(context, uri, sampleSize)
            ?: throw IllegalArgumentException("تعذّر فك ترميز الصورة، قد يكون التنسيق غير مدعوم")

        val rotated = correctOrientation(context, uri, rawBitmap)
        var working = capDimensions(rotated, profile.maxDimension)
        if (working !== rotated) rotated.recycle()
        if (rotated !== rawBitmap) rawBitmap.recycle()

        var compressed = adaptiveCompress(working, profile.targetBytes)
        var base64 = NativeBridge.encodeBase64(compressed.bytes)

        // شبكة أمان: بعض الصور (تفاصيل/ضوضاء كثيفة) لا تصل إلى الحجم المستهدف حتى
        // بأدنى جودة JPEG مسموحة. بدل إرجاع نص Base64 قد يتجاوز الحد الآمن ويُرفض
        // لاحقًا عند محاولة النشر، نُصغّر الأبعاد تدريجيًا ونُعيد الضغط والترميز
        // حتى نستقر ضمن الحد الآمن أو نصل لأصغر أبعاد مقبولة.
        var attempts = 0
        while (base64.length > SAFE_BASE64_CHAR_LIMIT &&
            attempts < 4 &&
            max(working.width, working.height) > MIN_SHRINK_DIMENSION
        ) {
            val nextDimension = max(
                MIN_SHRINK_DIMENSION,
                (max(working.width, working.height) * 0.75f).toInt()
            )
            val shrunk = capDimensions(working, nextDimension)
            if (shrunk === working) break // لم يعد بالإمكان التصغير أكثر
            working.recycle()
            working = shrunk

            compressed = adaptiveCompress(working, profile.targetBytes)
            base64 = NativeBridge.encodeBase64(compressed.bytes)
            attempts++
        }

        working.recycle()

        return EncodedImage(
            base64 = base64,
            mimeType = "image/jpeg",
            width = compressed.width,
            height = compressed.height,
            byteSize = compressed.bytes.size
        )
    }

    /** يفك ترميز Base64 إلى Bitmap جاهز للعرض (يُستخدم عند عدم استخدام Coil مباشرة). */
    fun decode(base64: String): Bitmap? {
        val bytes = NativeBridge.decodeBase64(base64)
        if (bytes.isEmpty()) return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    // -----------------------------------------------------------------
    // داخلي: القراءة، التصغير الذكي، تصحيح الدوران، والضغط التكيّفي
    // -----------------------------------------------------------------

    private fun readBounds(context: Context, uri: Uri): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // ملاحظة (إصلاح خلل "تعذّر قراءة أبعاد الصورة"): مع inJustDecodeBounds = true
        // فإن BitmapFactory.decodeStream يُعيد null دائمًا بتصميمه (هذا هو المتوقع
        // والنجاح، لا الفشل) — أبعاد الصورة تُكتب داخل `options` وليس في القيمة
        // المُعادة. الكود السابق كان يستخدم بالخطأ ناتج decodeStream نفسه (وهو null
        // دومًا هنا) كمؤشر فشل عبر `?: return null`، مما كان يجعل كل صورة تُرفض فورًا
        // بهذه الرسالة بغضّ النظر عن صحتها. الإصلاح: نتحقق فقط من فتح الـ Stream،
        // ثم نعتمد حصريًا على `options.outWidth/outHeight` بعد تنفيذ القراءة.
        val stream = openStream(context, uri) ?: return null
        stream.use { input -> BitmapFactory.decodeStream(input, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        return options.outWidth to options.outHeight
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxDimension || h / 2 >= maxDimension) {
            w /= 2; h /= 2; sample *= 2
        }
        return sample
    }

    private fun decodeSampled(context: Context, uri: Uri, sampleSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return openStream(context, uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun openStream(context: Context, uri: Uri): InputStream? =
        context.contentResolver.openInputStream(uri)

    /** يصحح دوران الصورة تلقائيًا اعتمادًا على بيانات EXIF (مشكلة شائعة في صور الكاميرا). */
    private fun correctOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = openStream(context, uri)?.use { input ->
            runCatching { ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            ) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun capDimensions(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = max(bitmap.width, bitmap.height)
        if (largestSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largestSide
        val newWidth = max(1, (bitmap.width * scale).toInt())
        val newHeight = max(1, (bitmap.height * scale).toInt())
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private data class CompressedResult(val bytes: ByteArray, val width: Int, val height: Int)

    /**
     * ضغط تكيّفي بالبحث الثنائي التقريبي على الجودة (95..40) حتى تحقيق الحجم المستهدف،
     * مع الحفاظ على أفضل جودة ممكنة ضمن الحد المسموح (توازن قوة/دقة/حجم).
     */
    private fun adaptiveCompress(bitmap: Bitmap, targetBytes: Int): CompressedResult {
        var quality = 92
        var bytes = compressToJpeg(bitmap, quality)

        var attempts = 0
        while (bytes.size > targetBytes && quality > 35 && attempts < 8) {
            val overshoot = bytes.size.toFloat() / targetBytes
            val step = min(20, max(5, (10 * overshoot).toInt()))
            quality = max(35, quality - step)
            bytes = compressToJpeg(bitmap, quality)
            attempts++
        }

        return CompressedResult(bytes, bitmap.width, bitmap.height)
    }

    private fun compressToJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return output.toByteArray()
    }
}
