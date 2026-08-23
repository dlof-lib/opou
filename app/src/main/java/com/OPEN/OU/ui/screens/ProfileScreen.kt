package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ArrowBackIosNew
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
import com.OPEN.OU.ui.components.GradientText
import com.OPEN.OU.ui.components.ImagePickerButton
import com.OPEN.OU.ui.theme.OpouBrandGradient
import com.OPEN.OU.util.ImageCodec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uid: String,
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onEditRoom: () -> Unit = {},
    showBackButton: Boolean = true,
    onOpenSettings: (() -> Unit)? = null
) {
    val room by viewModel.room.collectAsState()
    val isTeking by viewModel.isTeking.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val myUid = remember { AuthRepository().currentUserId }
    var avatarError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uid) { viewModel.load(uid) }
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
                title = { Text(stringResource(R.string.profile_room), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    if (onOpenSettings != null) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_open))
                        }
                    }
                }
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
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val avatarModifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(3.dp, OpouBrandGradient, CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)

                if (user.avatarBase64.isNotBlank()) {
                    Base64Image(base64 = user.avatarBase64, modifier = avatarModifier, cornerRadiusDp = 24)
                } else {
                    AsyncImage(
                        model = user.avatarUrl.ifBlank { null },
                        contentDescription = null,
                        modifier = avatarModifier.background(Color(0xFF0B7A4A))
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
                Spacer(Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientText(text = user.username, style = MaterialTheme.typography.titleLarge)
                if (user.verified) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "موثّق",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            AssistChip(
                onClick = {},
                label = { Text(user.communityName.ifBlank { stringResource(R.string.official_member_title) }) }
            )

            if (user.bio.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    user.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                StatColumn(user.paragraphsCount, stringResource(R.string.profile_paragraphs))
                StatColumn(user.tekersCount, stringResource(R.string.profile_followers))
                StatColumn(user.tekingCount, stringResource(R.string.profile_following))
            }

            Spacer(Modifier.height(24.dp))

            if (myUid != null && myUid != uid) {
                if (isTeking) {
                    OutlinedButton(onClick = { viewModel.toggleTek(uid) }) {
                        Icon(Icons.Filled.PersonRemove, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إلغاء التيك")
                    }
                } else {
                    Button(
                        onClick = { viewModel.toggleTek(uid) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_tek))
                    }
                }
            } else if (myUid == uid) {
                OutlinedButton(onClick = onEditRoom) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
