package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.OPEN.OU.R
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.ui.components.Base64Image
import com.OPEN.OU.ui.components.ImagePickerButton
import com.OPEN.OU.util.ImageCodec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uid: String,
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onEditRoom: () -> Unit = {}
) {
    val room by viewModel.room.collectAsState()
    val isTeking by viewModel.isTeking.collectAsState()
    val myUid = remember { AuthRepository().currentUserId }
    var avatarError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) { viewModel.load(uid) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_room)) },
                navigationIcon = { TextButton(onClick = onBack) { Text("رجوع") } }
            )
        }
    ) { padding ->
        val user = room
        if (user == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                if (user.avatarBase64.isNotBlank()) {
                    Base64Image(
                        base64 = user.avatarBase64,
                        modifier = Modifier.size(90.dp).clip(CircleShape)
                    )
                } else {
                    AsyncImage(
                        model = user.avatarUrl.ifBlank { null },
                        contentDescription = null,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B7A4A))
                    )
                }

                if (myUid == uid) {
                    ImagePickerButton(
                        profile = ImageCodec.ImageProfile.AVATAR,
                        onImageReady = { encoded ->
                            viewModel.updateAvatar(uid, encoded.base64)
                        },
                        onError = { avatarError = it },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            avatarError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(12.dp))
            Text(user.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (user.communityName.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                AssistChip(onClick = {}, label = { Text(user.communityName) })
            }

            if (user.bio.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(user.bio, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                StatColumn(user.paragraphsCount, stringResource(R.string.profile_paragraphs))
                StatColumn(user.tekersCount, stringResource(R.string.profile_followers))
                StatColumn(user.tekingCount, stringResource(R.string.profile_following))
            }

            Spacer(Modifier.height(24.dp))

            if (myUid != null && myUid != uid) {
                Button(onClick = { viewModel.toggleTek(uid) }) {
                    Text(if (isTeking) "إلغاء التيك" else stringResource(R.string.action_tek))
                }
            } else if (myUid == uid) {
                OutlinedButton(onClick = onEditRoom) {
                    Text(stringResource(R.string.profile_edit))
                }
            }
        }
    }
}

@Composable
private fun StatColumn(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
