package com.OPEN.OU.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.OPEN.OU.R
import com.OPEN.OU.ui.components.Base64Image
import com.OPEN.OU.ui.components.ImagePickerButton
import com.OPEN.OU.util.ImageCodec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: FeedViewModel,
    currentUsername: String,
    currentAvatar: String,
    currentAvatarBase64: String = "",
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var attachedImage by remember { mutableStateOf<ImageCodec.EncodedImage?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feed_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("إلغاء") }
                },
                actions = {
                    ImagePickerButton(
                        profile = ImageCodec.ImageProfile.POST_IMAGE,
                        onImageReady = { attachedImage = it; imageError = null },
                        onError = { imageError = it }
                    )
                    TextButton(
                        onClick = {
                            viewModel.publish(
                                content = text,
                                authorUsername = currentUsername,
                                authorAvatar = currentAvatar,
                                authorAvatarBase64 = currentAvatarBase64,
                                imageBase64 = attachedImage?.base64.orEmpty(),
                                onDone = onDone
                            )
                        },
                        enabled = (text.isNotBlank() || attachedImage != null) && !viewModel.isPosting
                    ) {
                        Text(stringResource(R.string.post_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.new_paragraph_hint)) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 6
            )

            attachedImage?.let { image ->
                Spacer(Modifier.height(12.dp))
                Box {
                    Base64Image(
                        base64 = image.base64,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                    TextButton(
                        onClick = { attachedImage = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) { Text("إزالة") }
                }
                Text(
                    text = "حجم الصورة بعد الضغط: ${image.byteSize / 1024} كيلوبايت",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            imageError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }

            Text(
                text = "${text.length} حرف",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
