package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.OPEN.OU.ui.components.Base64Image
import com.OPEN.OU.ui.components.ImagePickerButton
import com.OPEN.OU.ui.components.ResponsiveContent
import com.OPEN.OU.ui.theme.OpouBrandGradient
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
    val errorMessage by viewModel.errorMessage.collectAsState()

    var communityName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var pendingAvatar by remember { mutableStateOf<ImageCodec.EncodedImage?>(null) }
    var pendingBanner by remember { mutableStateOf<ImageCodec.EncodedImage?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var initialized by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        ResponsiveContent(modifier = Modifier.padding(padding)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            // البانر — بارتفاع أكبر مساحة، مع زر تغيير واضح في الزاوية
            Box(Modifier.fillMaxWidth().height(130.dp)) {
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
                            .background(OpouBrandGradient)
                    )
                }
                ImagePickerButton(
                    profile = ImageCodec.ImageProfile.BANNER,
                    onImageReady = { pendingBanner = it; imageError = null },
                    onError = { imageError = it },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                )
            }

            // الصورة الرمزية (تتراكب فوق حافة البانر مثل يوتيوب/إنستغرام)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Box(Modifier.offset(y = (-32).dp)) {
                    val user = room
                    val avatarModifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)

                    when {
                        pendingAvatar != null -> Base64Image(
                            base64 = pendingAvatar!!.base64,
                            modifier = avatarModifier,
                            cornerRadiusDp = 22
                        )
                        user != null && user.avatarBase64.isNotBlank() -> Base64Image(
                            base64 = user.avatarBase64,
                            modifier = avatarModifier,
                            cornerRadiusDp = 22
                        )
                        else -> Box(
                            avatarModifier.background(Color(0xFF0B7A4A))
                        )
                    }
                    ImagePickerButton(
                        profile = ImageCodec.ImageProfile.AVATAR,
                        onImageReady = { pendingAvatar = it; imageError = null },
                        onError = { imageError = it },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            Column(Modifier.padding(horizontal = 20.dp).padding(top = 0.dp, bottom = 20.dp).offset(y = (-24).dp)) {
                imageError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = communityName,
                    onValueChange = { communityName = it },
                    label = { Text("اسم المجتمع (مثل: يوتيوبر)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("السيرة الذاتية") },
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        }
    }
}
