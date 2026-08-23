package com.OPEN.OU.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.OPEN.OU.util.NativeBridge

/**
 * يعرض صورة مخزّنة كـ Base64 (من فقرة أو غرفة) بأداء جيد:
 * يُفكّ الترميز مرة واحدة فقط عبر `remember` المرتبط بمحتوى النص نفسه،
 * بحيث لا يُعاد فك الترميز عند كل إعادة تركيب (recomposition) للواجهة.
 */
@Composable
fun Base64Image(
    base64: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    cornerRadiusDp: Int = 12
) {
    if (base64.isBlank()) return

    val bitmap = remember(base64) {
        runCatching {
            val bytes = NativeBridge.decodeBase64(base64)
            if (bytes.isEmpty()) null else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    bitmap?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier.clip(RoundedCornerShape(cornerRadiusDp.dp))
        )
    }
}
