package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.OPEN.OU.ui.components.Base64Image
import com.OPEN.OU.ui.components.ImagePickerButton
import com.OPEN.OU.util.ImageCodec

/**
 * شاشة تعديل الغرفة الكاملة: السيرة الذاتية، اسم المجتمع، الصورة الرمزية، وصورة البانر.
 * كل حقل يُحفظ فوريًا (Realtime) بمجرد الضغط على "حفظ"، والصور تُضغط وتُرمّز محليًا
 * قبل الحفظ عبر ImageCodec (راجع util/ImageCodec.kt).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoomScreen(
    uid: String,
    viewModel: ProfileViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val room by viewModel.room.collectAsState()

    var communityName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var pendingAvatar by remember { mutableStateOf<ImageCodec.EncodedImage?>(null) }
    var pendingBanner by remember { mutableStateOf<ImageCodec.EncodedImage?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(uid) { viewModel.load(uid) }

    // تهيئة الحقول من بيانات الغرفة الحالية عند وصولها لأول مرة فقط
    LaunchedEffect(room) {
        val user = room
        if (user != null && !initialized) {
            communityName = user.communityName
            bio = user.bio
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تعديل الغرفة") },
                navigationIcon = { TextButton(onClick = onBack) { Text("إلغاء") } },
                actions = {
                    TextButton(onClick = {
                        pendingAvatar?.let { viewModel.updateAvatar(uid, it.base64) }
                        pendingBanner?.let { viewModel.updateBanner(uid, it.base64) }
                        viewModel.updateRoomInfo(uid, communityName = communityName, bio = bio)
                        onDone()
                    }) { Text("حفظ") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            // البانر
            Box(Modifier.fillMaxWidth().height(140.dp)) {
                val user = room
                when {
                    pendingBanner != null -> Base64Image(
                        base64 = pendingBanner!!.base64,
                        modifier = Modifier.fillMaxSize()
                    )
                    user != null && user.bannerBase64.isNotBlank() -> Base64Image(
                        base64 = user.bannerBase64,
                        modifier = Modifier.fillMaxSize()
                    )
                    user != null && user.bannerUrl.isNotBlank() -> AsyncImage(
                        model = user.bannerUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0B7A4A))
                    )
                }
                ImagePickerButton(
                    profile = ImageCodec.ImageProfile.BANNER,
                    onImageReady = { pendingBanner = it; imageError = null },
                    onError = { imageError = it },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                )
            }

            // الصورة الرمزية (تتراكب فوق حافة البانر)
            Box(
                Modifier
                    .padding(start = 20.dp)
                    .offset(y = (-36).dp)
            ) {
                val user = room
                when {
                    pendingAvatar != null -> Base64Image(
                        base64 = pendingAvatar!!.base64,
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                    )
                    user != null && user.avatarBase64.isNotBlank() -> Base64Image(
                        base64 = user.avatarBase64,
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                    )
                    else -> Box(
                        Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B7A4A))
                    )
                }
                ImagePickerButton(
                    profile = ImageCodec.ImageProfile.AVATAR,
                    onImageReady = { pendingAvatar = it; imageError = null },
                    onError = { imageError = it },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            Column(Modifier.padding(horizontal = 20.dp).padding(top = 0.dp, bottom = 20.dp)) {
                imageError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = communityName,
                    onValueChange = { communityName = it },
                    label = { Text("اسم المجتمع (مثل: يوتيوبر)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("السيرة الذاتية") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
