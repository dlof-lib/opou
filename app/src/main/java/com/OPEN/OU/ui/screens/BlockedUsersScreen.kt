package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.OPEN.OU.data.model.User
import com.OPEN.OU.ui.components.Base64Image
import com.OPEN.OU.ui.components.TekersSkeletonList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onBack: () -> Unit,
    viewModel: BlockedUsersViewModel = viewModel()
) {
    val blockedUsers by viewModel.blockedUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("قائمة المحظورين", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                isLoading -> TekersSkeletonList(count = 5)
                blockedUsers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("لا يوجد مستخدمون محظورون", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
                    items(blockedUsers, key = { it.uid }) { user ->
                        BlockedUserRow(user = user, onUnblock = { viewModel.unblock(user.uid) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedUserRow(user: User, onUnblock: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarModifier = Modifier.size(44.dp).clip(CircleShape)
        if (user.avatarBase64.isNotBlank()) {
            Base64Image(base64 = user.avatarBase64, modifier = avatarModifier, cornerRadiusDp = 22)
        } else {
            AsyncImage(
                model = user.avatarUrl.ifBlank { null },
                contentDescription = null,
                modifier = avatarModifier.background(Color(0xFF0B7A4A))
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.username, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            if (user.communityName.isNotBlank()) {
                Text(user.communityName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedButton(onClick = onUnblock) { Text("إلغاء الحظر") }
    }
}
