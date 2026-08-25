package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.OPEN.OU.R
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.ui.components.Base64Image
import com.OPEN.OU.ui.components.GradientText
import com.OPEN.OU.ui.components.ImagePickerButton
import com.OPEN.OU.ui.components.ImageViewerDialog
import com.OPEN.OU.ui.components.PostCard
import com.OPEN.OU.ui.components.ProfileHeaderSkeleton
import com.OPEN.OU.ui.components.PostListSkeleton
import com.OPEN.OU.ui.components.ResponsiveContent
import com.OPEN.OU.ui.theme.OpouAccentGreen
import com.OPEN.OU.ui.theme.OpouBrandGradient
import com.OPEN.OU.util.ImageCodec

/**
 * شاشة "الغرفة" (الملف الشخصي): بانر + صورة رمزية متراكبة (بنفس أسلوب EditRoomScreen)،
 * بطاقة إحصائيات، وقسمان منظّمان بعنوانين واضحين: "لمحة" (تصنيف الغرفة) و"السيرة
 * الذاتية". لصاحب الغرفة نفسه: زر تعديل بارز أعلى الشاشة (بجانب الإعدادات) وزر
 * تعديل كامل أسفل البطاقة، مع دعوة صريحة لإضافة سيرة ذاتية إن كانت فارغة.
 */
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
    val isBlocked by viewModel.isBlocked.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val myUid = remember { AuthRepository().currentUserId }
    val isOwnProfile = myUid != null && myUid == uid
    var avatarError by remember { mutableStateOf<String?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showAvatarViewer by remember { mutableStateOf(false) }
    var showBannerViewer by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(uid) { viewModel.load(uid) }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        // عند العرض كتبويب "الحساب" (showBackButton = false) تكون الشاشة
        // متداخلة داخل الـ Scaffold الخارجي في OpouNavGraph الذي يحجز مساحة
        // الشريط السفلي فعليًا، فلا داعي لأن يحجز هذا الـ Scaffold الداخلي
        // مساحة إضافية لشريط التنقّل السفلي للنظام (نفس مشكلة الشريط العلوي
        // المضاعفة، لكن بالأسفل هذه المرة). عند العرض كشاشة مستقلة (فتح ملف
        // شخص آخر) نُبقي السلوك الافتراضي لأنها غير متداخلة.
        contentWindowInsets = if (!showBackButton) {
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
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
                    if (isOwnProfile) {
                        IconButton(onClick = onEditRoom) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.profile_edit_profile)
                            )
                        }
                    }
                    if (onOpenSettings != null) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_open))
                        }
                    }
                    if (!isOwnProfile && myUid != null) {
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "خيارات")
                            }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (isBlocked) "إلغاء حظر المستخدم" else "حظر المستخدم") },
                                    leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        if (isBlocked) viewModel.toggleBlock(uid) else showBlockConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        val user = room
        if (user == null) {
            ResponsiveContent(modifier = Modifier.padding(padding)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    ProfileHeaderSkeleton()
                    PostListSkeleton(count = 2)
                }
            }
            return@Scaffold
        }
        val isLocked = user.isPrivateRoom && !isOwnProfile && !isTeking

        ResponsiveContent(modifier = Modifier.padding(padding)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── البانر ─────────────────────────────────────────────────────
            val hasBannerImage = user.bannerBase64.isNotBlank() || user.bannerUrl.isNotBlank()
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .then(
                        if (hasBannerImage) Modifier.clickable { showBannerViewer = true }
                        else Modifier
                    )
            ) {
                when {
                    user.bannerBase64.isNotBlank() -> Base64Image(
                        base64 = user.bannerBase64,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadiusDp = 0
                    )
                    user.bannerUrl.isNotBlank() -> AsyncImage(
                        model = user.bannerUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Box(Modifier.fillMaxSize().background(OpouBrandGradient))
                }
                // تظليل خفيف أسفل البانر حتى تبرز الصورة الرمزية والاسم فوقه بوضوح
                Box(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f))
                            )
                        )
                )
            }

            // ── الصورة الرمزية + الاسم + التصنيف ─────────────────────────────
            Column(Modifier.padding(horizontal = 16.dp)) {
                Box(Modifier.offset(y = (-32).dp)) {
                    val hasAvatarImage = user.avatarBase64.isNotBlank() || user.avatarUrl.isNotBlank()
                    val avatarModifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .then(
                            if (hasAvatarImage) Modifier.clickable { showAvatarViewer = true }
                            else Modifier
                        )

                    if (user.avatarBase64.isNotBlank()) {
                        Base64Image(base64 = user.avatarBase64, modifier = avatarModifier, cornerRadiusDp = 20)
                    } else {
                        AsyncImage(
                            model = user.avatarUrl.ifBlank { null },
                            contentDescription = null,
                            modifier = avatarModifier.background(Color(0xFF0B7A4A))
                        )
                    }

                    if (isOwnProfile) {
                        ImagePickerButton(
                            profile = ImageCodec.ImageProfile.AVATAR,
                            onImageReady = { encoded -> viewModel.updateAvatar(uid, encoded.base64) },
                            onError = { avatarError = it },
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }

                avatarError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                }

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
                    leadingIcon = { Icon(Icons.Filled.Sell, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(user.communityName.ifBlank { stringResource(R.string.official_member_title) }) }
                )

                if (user.categories.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        user.categories.take(4).forEach { category ->
                            AssistChip(onClick = {}, label = { Text(category, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ── بطاقة الإحصائيات ─────────────────────────────────────
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatColumn(user.paragraphsCount, stringResource(R.string.profile_paragraphs))
                        VerticalDivider()
                        StatColumn(user.tekersCount, stringResource(R.string.profile_followers))
                        VerticalDivider()
                        StatColumn(user.tekingCount, stringResource(R.string.profile_following))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── قسم "لمحة" ───────────────────────────────────────────
                ProfileSectionCard(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.profile_overview_title)
                ) {
                    Text(
                        "${stringResource(R.string.profile_joined_as)} ${
                            user.communityName.ifBlank { stringResource(R.string.official_member_title) }
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (isLocked) {
                    ProfileSectionCard(icon = Icons.Filled.Lock, title = "غرفة خاصة") {
                        Text(
                            "هذه الغرفة خاصة — تابع (تيك) صاحبها لرؤية فقراته وسيرته الذاتية",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // ── قسم "السيرة الذاتية" ───────────────────────────────────
                    ProfileSectionCard(
                        icon = Icons.Filled.Notes,
                        title = stringResource(R.string.profile_bio_title),
                        onClick = if (isOwnProfile && user.bio.isBlank()) onEditRoom else null
                    ) {
                        if (user.bio.isNotBlank()) {
                            Text(
                                user.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                stringResource(
                                    if (isOwnProfile) R.string.profile_bio_empty_self
                                    else R.string.profile_bio_empty_other
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (user.socialLinks.values.any { it.isNotBlank() }) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            user.socialLinks.filterValues { it.isNotBlank() }.forEach { (platform, url) ->
                                AssistChip(
                                    onClick = {
                                        val normalized = if (url.startsWith("http")) url else "https://$url"
                                        runCatching { uriHandler.openUri(normalized) }
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = OpouAccentGreen, modifier = Modifier.size(14.dp)) },
                                    label = { Text(platform.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    if (user.customButtons.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            user.customButtons.filter { it.label.isNotBlank() && it.url.isNotBlank() }.forEach { button ->
                                OutlinedButton(
                                    onClick = {
                                        val normalized = if (button.url.startsWith("http")) button.url else "https://${button.url}"
                                        runCatching { uriHandler.openUri(normalized) }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(button.label) }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // ── زر الإجراء الرئيسي ──────────────────────────────────
                if (!isOwnProfile && myUid != null) {
                    if (isTeking) {
                        OutlinedButton(
                            onClick = { viewModel.toggleTek(uid) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PersonRemove, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("إلغاء التيك")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.toggleTek(uid) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_tek))
                        }
                    }
                } else if (isOwnProfile) {
                    Button(
                        onClick = onEditRoom,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.profile_edit_profile))
                    }
                }

                if (!isLocked && posts.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    Text(
                        "الفقرات",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
                    )
                }

                Spacer(Modifier.height(if (isLocked || posts.isEmpty()) 24.dp else 0.dp))
            }

            if (!isLocked) {
                posts.forEach { post ->
                    PostCard(
                        post = post,
                        currentReaction = ReactionType.NONE,
                        onReact = {},
                        onComment = {},
                        onTek = {},
                        onOpenProfile = {},
                        isOwnPost = isOwnProfile,
                        onTogglePin = if (isOwnProfile) { { viewModel.togglePin(post) } } else null,
                        onBlockAuthor = if (!isOwnProfile) { { showBlockConfirm = true } } else null
                    )
                }
            }
        }
        }
    }

    if (showAvatarViewer) {
        ImageViewerDialog(
            base64 = room?.avatarBase64,
            imageUrl = room?.avatarUrl,
            onDismiss = { showAvatarViewer = false }
        )
    }

    if (showBannerViewer) {
        ImageViewerDialog(
            base64 = room?.bannerBase64,
            imageUrl = room?.bannerUrl,
            onDismiss = { showBannerViewer = false }
        )
    }

    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = { Text("حظر ${room?.username.orEmpty()}", fontWeight = FontWeight.Bold) },
            text = { Text("لن يتمكن هذا المستخدم من رؤية فقراتك أو التفاعل معك، والعكس صحيح. يمكنك إلغاء الحظر لاحقًا.") },
            confirmButton = {
                TextButton(onClick = {
                    showBlockConfirm = false
                    viewModel.toggleBlock(uid)
                }) { Text("حظر", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showBlockConfirm = false }) { Text("إلغاء") } }
        )
    }
}

