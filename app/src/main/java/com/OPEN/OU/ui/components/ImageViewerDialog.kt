package com.OPEN.OU.ui.components

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.OPEN.OU.util.ImageDownloader
import com.OPEN.OU.util.NativeBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * عارض صورة بملء الشاشة: يفتح فوق أي محتوى (منشور، صورة رمزية، بانر) عند
 * الضغط عليه، ويدعم التكبير بإصبعين (Pinch-to-zoom)، والتكبير/الإرجاع
 * بضغطة مزدوجة، والسحب أثناء التكبير، بالإضافة لزر تنزيل يحفظ الصورة في
 * معرض الجهاز. يدعم كِلا مصدري الصور المستخدَمين في التطبيق: Base64
 * (المخزّن مباشرة في قاعدة البيانات) أو رابط URL خارجي — يُعطى الأولوية
 * لـ Base64 عند توفّره.
 */
@Composable
fun ImageViewerDialog(
    base64: String? = null,
    imageUrl: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val bitmap = remember(base64) {
        base64?.takeIf { it.isNotBlank() }?.let {
            runCatching {
                val bytes = NativeBridge.decodeBase64(it)
                if (bytes.isEmpty()) null else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }

    fun startDownload() {
        if (isDownloading) return
        isDownloading = true
        scope.launch {
            val saved = ImageDownloader.saveToGallery(context, base64, imageUrl)
            isDownloading = false
            statusMessage = if (saved) "تم حفظ الصورة في المعرض" else "تعذّر حفظ الصورة"
        }
    }

    // على أندرويد 9 فما دون نحتاج إذن التخزين صراحة قبل الكتابة لمجلد الصور العام.
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startDownload() else statusMessage = "يلزم إذن التخزين لحفظ الصورة"
    }

    fun onDownloadClick() {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        if (needsPermission) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            startDownload()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            if (newScale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 3f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val zoomModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = zoomModifier
                    )
                } else if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = zoomModifier
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = Color.White)
            }

            IconButton(
                onClick = { onDownloadClick() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .size(40.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Download, contentDescription = "تنزيل الصورة", tint = Color.White)
                }
            }

            statusMessage?.let { message ->
                LaunchedEffect(message) {
                    delay(2200)
                    statusMessage = null
                }
                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
