package com.OPEN.OU.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * يعرض صورة Base64 بأمان.
 *
 * مهم: فك Base64 وفك ضغط Bitmap لا يحدثان على خيط واجهة Android.
 * شاشة الحساب قد تحتوي على بانر/صورة كبيرة، وكان فك الصورة أثناء Compose
 * قادرًا على تجميد التطبيق أو إسقاطه بسبب ضغط الذاكرة.
 *
 * C++ يبقى طبقة مساعدة لباقي العمليات الأصلية، أما عرض الصور هنا فيستخدم
 * Android Base64 كمسار آمن ومستقل عن JNI.
 */
@Composable
fun Base64Image(
    base64: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    cornerRadiusDp: Int = 12
) {
    if (base64.isBlank()) return

    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = base64) {
        value = withContext(Dispatchers.Default) {
            decodeBitmapSafely(base64)
        }
    }

    bitmapState.value?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier.clip(RoundedCornerShape(cornerRadiusDp.dp))
        )
    }
}

private fun decodeBitmapSafely(value: String): Bitmap? {
    return runCatching {
        // بعض قواعد البيانات قد تخزن data:image/...;base64, قبل المحتوى.
        val payload = value.substringAfter("base64,", value).trim()
        if (payload.isEmpty()) return@runCatching null

        // حد أمان إضافي: الصور التي تُنشأ بواسطة ImageCodec أصغر من ذلك،
        // لذلك أي قيمة ضخمة/تالفة من قاعدة البيانات لا ينبغي أن تحجز ذاكرة هائلة.
        if (payload.length > 900_000) return@runCatching null

        val bytes = Base64.decode(payload, Base64.DEFAULT)
        if (bytes.isEmpty()) return@runCatching null

        // نقرأ الأبعاد أولًا لتجنب تخصيص Bitmap ضخم عند فتح الحساب.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        // 1024 كحد أقصى للعرض على الشاشة. هذا يخفض ذاكرة الصورة إلى ربع
        // السقف السابق تقريبًا، ويمنع عدة صور ملف شخصي من استنزاف الذاكرة.
        val maxDimension = 1024
        var sample = 1
        while (bounds.outWidth / sample > maxDimension ||
            bounds.outHeight / sample > maxDimension) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            // صور ImageCodec هي JPEG، لذلك RGB_565 يقلل استهلاك الذاكرة
            // بشكل واضح. وإذا فشل التنسيق، نعيد المحاولة بالمسار الافتراضي.
            inPreferredConfig = Bitmap.Config.RGB_565
            inDither = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: run {
                val fallback = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, fallback)
            }
    }.getOrNull()
}
