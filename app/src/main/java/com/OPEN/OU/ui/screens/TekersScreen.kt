package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
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
import com.OPEN.OU.data.model.User
import com.OPEN.OU.ui.components.Base64Image
import com.OPEN.OU.ui.components.GradientText
import com.OPEN.OU.ui.theme.OpouBrandGradient

/**
 * شاشة "التيكرز" — تعرض في تبويبين: من يتابعك (متابعوك) ومن تتابعهم (تتابعهم).
 * تصميم مرتّب على شكل قائمة بطاقات صغيرة أنيقة بصورة رمزية واسم واسم مجتمع.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TekersScreen(
    viewModel: TekersViewModel,
    onOpenProfile: (String) -> Unit
) {
    val myUid = viewModel.myUid
    val tekers by viewModel.tekers.collectAsState()
    val teking by viewModel.teking.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var tab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { GradientText(text = stringResource(R.string.tekers_title), style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (myUid == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.tekers_login_required),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = {
                        Text(
                            "${stringResource(R.string.tekers_tab_tekers)} (${tekers.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = {
                        Text(
                            "${stringResource(R.string.tekers_tab_teking)} (${teking.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            val list = if (tab == 0) tekers else teking

            when {
                isLoading && list.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                list.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.tekers_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list, key = { it.uid }) { user ->
                            TekerRow(user = user, onClick = { onOpenProfile(user.uid) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TekerRow(user: User, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarModifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(2.dp, OpouBrandGradient, CircleShape)
                .padding(2.dp)
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

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.username, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    if (user.verified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (user.communityName.isNotBlank()) {
                    Text(
                        user.communityName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
