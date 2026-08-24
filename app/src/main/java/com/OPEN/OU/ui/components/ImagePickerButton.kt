package com.OPEN.OU.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.OPEN.OU.util.ImageCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * زر اختيار صورة من المعرض. تدفّق العمل الآن:
 * 1) اختيار صورة من المعرض.
 * 2) تحميل نسخة عاملة منها (بخيط خلفي) وفتح أداة قص [ImageCropperDialog] بإطار
 *    ثابت النسبة خاص بنوع الاستخدام (دائري للصورة الرمزية 1:1، مستطيل عريض
 *    للبانر 3:1، مستطيل عمودي لصورة الفقرة 4:5 — راجع ImageCodec.ImageProfile).
 * 3) بعد تأكيد القص، تُضغط الصورة المقصوصة وتُرمّز Base64 بخيط خلفي أيضًا
 *    (Dispatchers.Default) دون تجميد الواجهة، إلى المقاس الدقيق الإلزامي لهذا
 *    النمط، ثم تُعاد النتيجة الجاهزة للتخزين والعرض عبر [onImageReady].
 */
@Composable
fun ImagePickerButton(
    profile: ImageCodec.ImageProfile,
    onImageReady: (ImageCodec.EncodedImage) -> Unit,
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var cropSource by remember { mutableStateOf<Bitmap?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isProcessing = true
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching { ImageCodec.loadWorkingBitmap(context, uri) }
            }
            isProcessing = false
            result.onSuccess { cropSource = it }
                .onFailure { onError(it.message ?: "تعذّرت قراءة الصورة") }
        }
    }

    // خلفية دائرية داكنة نصف شفافة حتى تبقى الأيقونة واضحة فوق أي صورة أو بانر خلفها
    IconButton(
        onClick = {
            launcher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        },
        enabled = !isProcessing,
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
    ) {
        if (isProcessing) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Icon(
                Icons.Filled.AddPhotoAlternate,
                contentDescription = "إضافة صورة",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    cropSource?.let { source ->
        ImageCropperDialog(
            source = source,
            profile = profile,
            onCancel = {
                source.recycle()
                cropSource = null
            },
            onCropped = { cropped ->
                cropSource = null
                isProcessing = true
                scope.launch {
                    val result = withContext(Dispatchers.Default) {
                        runCatching { ImageCodec.encodeBitmap(cropped, profile) }
                    }
                    if (cropped !== source) cropped.recycle()
                    source.recycle()
                    isProcessing = false
                    result.onSuccess(onImageReady)
                        .onFailure { onError(it.message ?: "تعذّرت معالجة الصورة") }
                }
            }
        )
    }
}
