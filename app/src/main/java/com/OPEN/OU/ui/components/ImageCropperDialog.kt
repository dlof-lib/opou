package com.OPEN.OU.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.OPEN.OU.util.ImageCodec
import kotlin.math.max
import kotlin.math.min

/**
 * أداة قص صورة داخل التطبيق (بدون مكتبة خارجية): تعرض الصورة الأصلية كاملة، ويمكن
 * للمستخدم تحريكها وتكبيرها/تصغيرها بإصبعين حتى تملأ إطار القص الثابت (دائري
 * للصورة الرمزية، مستطيل للبانر وصورة الفقرة) بنسبة العرض/الارتفاع المُلزمة في
 * [profile]. عند الضغط على "تم"، تُقصّ الصورة فعليًا بنفس ما يظهر داخل الإطار
 * وتُعاد عبر [onCropped] كـ Bitmap جاهزة للترميز (راجع ImageCodec.encodeBitmap).
 *
 * أسلوب القص هو "cover": الصورة تُكبَّر تلقائيًا أولًا بحيث تغطي الإطار بالكامل
 * (لا فراغات)، ولا يمكن تصغيرها أكثر من ذلك — فقط تكبير إضافي وتحريك.
 */
@Composable
fun ImageCropperDialog(
    source: Bitmap,
    profile: ImageCodec.ImageProfile,
    onCropped: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    // مقياس ونقطة إزاحة إضافيان يتحكم بهما المستخدم فوق مقياس "cover" الأساسي.
    var userScale by remember { mutableStateOf(1f) }
    var userOffset by remember { mutableStateOf(Offset.Zero) }
    var frameSizePx by remember { mutableStateOf(Size.Zero) }

    val isCircle = profile == ImageCodec.ImageProfile.AVATAR

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // شريط علوي: إلغاء / عنوان / تم
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "إلغاء", tint = Color.White)
                    }
                    Text(
                        cropTitle(profile),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    IconButton(onClick = {
                        val result = cropToBitmap(
                            source = source,
                            frameSizePx = frameSizePx,
                            userScale = userScale,
                            userOffset = userOffset,
                            profile = profile
                        )
                        onCropped(result)
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = "تم", tint = Color.White)
                    }
                }

                // منطقة القص: الصورة قابلة للسحب والتكبير، مع قناع مُعتِم حول الإطار
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val maxWpx = constraints.maxWidth.toFloat()
                        val maxHpx = constraints.maxHeight.toFloat()
                        val frameW = min(maxWpx, maxHpx * profile.aspectRatio)
                        val frameH = frameW / profile.aspectRatio

                        LaunchedEffect(frameW, frameH) {
                            frameSizePx = Size(frameW, frameH)
                        }

                        if (frameSizePx.width > 0f) {
                            val baseScale = max(
                                frameSizePx.width / source.width,
                                frameSizePx.height / source.height
                            )

                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .pointerInput(frameSizePx, source) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val newUserScale = (userScale * zoom).coerceIn(1f, 5f)
                                            val displayedW = source.width * baseScale * newUserScale
                                            val displayedH = source.height * baseScale * newUserScale
                                            val maxOffsetX = max(0f, (displayedW - frameSizePx.width) / 2f)
                                            val maxOffsetY = max(0f, (displayedH - frameSizePx.height) / 2f)
                                            val newOffset = Offset(
                                                x = (userOffset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                y = (userOffset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                            )
                                            userScale = newUserScale
                                            userOffset = newOffset
                                        }
                                    }
                            ) {
                                CropCanvas(
                                    source = source,
                                    frameSize = frameSizePx,
                                    baseScale = baseScale,
                                    userScale = userScale,
                                    userOffset = userOffset,
                                    isCircle = isCircle
                                )
                            }
                        }
                    }
                }

                Text(
                    "اسحب للتحريك وقرّب إصبعيك للتكبير — سيُقصّ ما يظهر داخل الإطار فقط",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun cropTitle(profile: ImageCodec.ImageProfile): String = when (profile) {
    ImageCodec.ImageProfile.AVATAR -> "قص الصورة الرمزية"
    ImageCodec.ImageProfile.BANNER -> "قص صورة البانر"
    ImageCodec.ImageProfile.POST_IMAGE -> "قص صورة الفقرة"
}

/** يرسم الصورة مُحوَّلة (Canvas مباشر بدل graphicsLayer لضمان تطابق دقيق مع منطق القص)، فوق قناع معتم حول الإطار. */
@Composable
private fun CropCanvas(
    source: Bitmap,
    frameSize: Size,
    baseScale: Float,
    userScale: Float,
    userOffset: Offset,
    isCircle: Boolean
) {
    val imageBitmap = remember(source) { source.asImageBitmap() }
    val scale = baseScale * userScale
    val displayedW = source.width * scale
    val displayedH = source.height * scale

    Canvas(Modifier.fillMaxSize().onSizeChanged { }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val frameTopLeft = Offset(center.x - frameSize.width / 2f, center.y - frameSize.height / 2f)
        val imageTopLeft = Offset(
            center.x - displayedW / 2f + userOffset.x,
            center.y - displayedH / 2f + userOffset.y
        )

        // الصورة كاملة (مكبّرة/محرَّكة) مرسومة أولًا
        drawImage(
            image = imageBitmap,
            dstOffset = androidx.compose.ui.unit.IntOffset(imageTopLeft.x.toInt(), imageTopLeft.y.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(displayedW.toInt(), displayedH.toInt())
        )

        // قناع معتم خارج الإطار
        val maskColor = Color.Black.copy(alpha = 0.6f)
        clipRect {
            // أعلى الإطار
            drawRect(maskColor, topLeft = Offset(0f, 0f), size = Size(size.width, frameTopLeft.y))
            // أسفل الإطار
            drawRect(
                maskColor,
                topLeft = Offset(0f, frameTopLeft.y + frameSize.height),
                size = Size(size.width, size.height - (frameTopLeft.y + frameSize.height))
            )
            // يسار الإطار
            drawRect(
                maskColor,
                topLeft = Offset(0f, frameTopLeft.y),
                size = Size(frameTopLeft.x, frameSize.height)
            )
            // يمين الإطار
            drawRect(
                maskColor,
                topLeft = Offset(frameTopLeft.x + frameSize.width, frameTopLeft.y),
                size = Size(size.width - (frameTopLeft.x + frameSize.width), frameSize.height)
            )
        }

        // حدود الإطار نفسه (دائري أو مستطيل حسب النمط)
        val strokeWidth = 2.dp.toPx()
        if (isCircle) {
            drawOval(
                color = Color.White,
                topLeft = frameTopLeft,
                size = frameSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
        } else {
            drawRect(
                color = Color.White,
                topLeft = frameTopLeft,
                size = frameSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            // خطوط شبكة إرشادية خفيفة (ثلث/ثلث) تساعد على التوسيط
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            for (i in 1..2) {
                val x = frameTopLeft.x + frameSize.width * i / 3f
                drawLine(
                    Color.White.copy(alpha = 0.5f),
                    Offset(x, frameTopLeft.y),
                    Offset(x, frameTopLeft.y + frameSize.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash
                )
                val y = frameTopLeft.y + frameSize.height * i / 3f
                drawLine(
                    Color.White.copy(alpha = 0.5f),
                    Offset(frameTopLeft.x, y),
                    Offset(frameTopLeft.x + frameSize.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash
                )
            }
        }
    }
}

/**
 * يحسب مستطيل القص داخل بكسلات الصورة الأصلية اعتمادًا على حالة العرض الحالية
 * (baseScale الضمني + userScale + userOffset)، ثم يُنتج Bitmap مقصوصة بنسبة
 * [ImageCodec.ImageProfile.aspectRatio] تمامًا — جاهزة لـ [ImageCodec.encodeBitmap].
 */
private fun cropToBitmap(
    source: Bitmap,
    frameSizePx: Size,
    userScale: Float,
    userOffset: Offset,
    profile: ImageCodec.ImageProfile
): Bitmap {
    if (frameSizePx.width <= 0f || frameSizePx.height <= 0f) return source

    val baseScale = max(frameSizePx.width / source.width, frameSizePx.height / source.height)
    val scale = baseScale * userScale
    val displayedW = source.width * scale
    val displayedH = source.height * scale

    // إزاحة الصورة عن مركز الإطار (نفس ما يُرسم في CropCanvas)
    val imageOriginOffsetX = (frameSizePx.width - displayedW) / 2f + userOffset.x
    val imageOriginOffsetY = (frameSizePx.height - displayedH) / 2f + userOffset.y

    // إحداثيات إطار القص داخل بكسلات الصورة الأصلية
    var cropX = (-imageOriginOffsetX / scale)
    var cropY = (-imageOriginOffsetY / scale)
    var cropW = frameSizePx.width / scale
    var cropH = frameSizePx.height / scale

    // حماية من تجاوز حدود الصورة بسبب تقريب عشري بسيط
    cropX = cropX.coerceIn(0f, max(0f, source.width - 1f))
    cropY = cropY.coerceIn(0f, max(0f, source.height - 1f))
    cropW = min(cropW, source.width - cropX)
    cropH = min(cropH, source.height - cropY)
    if (cropW <= 1f || cropH <= 1f) return source

    return Bitmap.createBitmap(
        source,
        cropX.toInt(),
        cropY.toInt(),
        cropW.toInt(),
        cropH.toInt()
    )
}
