package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.OPEN.OU.R
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.data.model.User
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
 * شاشة "الغرفة" (الملف الشخصي) — إعادة كتابة كاملة بتصميم مختلف عن السابق مع الحفاظ على
 * كل الميزات: بانر بحواف سفلية دائرية + صورة رمزية مركزية متراكبة (بدل التوضّع من جهة
 * البداية)، بطاقة إحصائيات على هيئة ثلاث "حبّات" (pills) بدل الصف المُقسَّم بفواصل، ثم
 * تبويبان ("نبذة" و"الفقرات") بدل عرض كل شيء في تدفّق رأسي واحد متلاحق. قسم "نبذة" بأسلوب
 * قائمة (أيقونة + عنوان + فاصل رفيع) بدل بطاقات مرتفعة منفصلة لكل قسم.
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
    var selectedTab by remember { mutableStateOf(0) }
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
        // راجع تفسير هذا الشرط في النسخة السابقة من هذه الشاشة: عند العرض كتبويب
        // "الحساب" (showBackButton = false) يكون الـ Scaffold الخارجي في OpouNavGraph
        // هو من يحجز مساحة الشريط السفلي فعليًا، فلا داعي لحجزها مرتين هنا.
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
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.profile_edit_profile))
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
                RoomHeaderCard(
                    user = user,
                    isOwnProfile = isOwnProfile,
                    avatarError = avatarError,
                    onAvatarErrorChange = { avatarError = it },
                    onAvatarClick = { showAvatarViewer = true },
                    onBannerClick = { showBannerViewer = true },
                    onAvatarPicked = { encodedBase64 -> viewModel.updateAvatar(uid, encodedBase64) }
                )

                Spacer(Modifier.height(22.dp))

                if (isLocked) {
                    RoomLockedNotice()
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        RoomActionButton(
                            isOwnProfile = false,
                            canShowTek = myUid != null,
                            isTeking = isTeking,
                            onToggleTek = { viewModel.toggleTek(uid) },
                            onEditRoom = onEditRoom
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                } else {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("نبذة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    "${stringResource(R.string.profile_paragraphs)} (${user.paragraphsCount})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        )
                    }

                    when (selectedTab) {
                        0 -> {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                                RoomInfoRow(
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

                                RoomInfoRow(
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
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        user.socialLinks.filterValues { it.isNotBlank() }.forEach { (platform, url) ->
                                            AssistChip(
                                                onClick = {
                                                    val normalized = if (url.startsWith("http")) url else "https://$url"
                                                    runCatching { uriHandler.openUri(normalized) }
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Filled.Link,
                                                        contentDescription = null,
                                                        tint = OpouAccentGreen,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                },
                                                label = { Text(platform.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }

                                if (user.customButtons.isNotEmpty()) {
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
                                    Spacer(Modifier.height(18.dp))
                                }

                                RoomActionButton(
                                    isOwnProfile = isOwnProfile,
                                    canShowTek = !isOwnProfile && myUid != null,
                                    isTeking = isTeking,
                                    onToggleTek = { viewModel.toggleTek(uid) },
                                    onEditRoom = onEditRoom
                                )
                            }
                        }

                        else -> {
                            Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                if (posts.isEmpty()) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 56.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "لا توجد فقرات بعد",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
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

                    Spacer(Modifier.height(24.dp))
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

/**
 * رأس صفحة الغرفة: بانر بحواف سفلية دائرية، وصورة رمزية مركزية متراكبة عليه (بدل
 * توضّعها من جهة البداية كما في التصميم السابق)، يليها الاسم وشارة التوثيق، شارة
 * اسم المجتمع، تصنيفات الغرفة، ثم صف من ثلاث "حبّات" إحصائية.
 */
@Composable
private fun RoomHeaderCard(
    user: User,
    isOwnProfile: Boolean,
    avatarError: String?,
    onAvatarErrorChange: (String?) -> Unit,
    onAvatarClick: () -> Unit,
    onBannerClick: () -> Unit,
    onAvatarPicked: (String) -> Unit
) {
    val hasBannerImage = user.bannerBase64.isNotBlank() || user.bannerUrl.isNotBlank()
    Box(
        Modifier
            .fillMaxWidth()
            .height(128.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .then(if (hasBannerImage) Modifier.clickable(onClick = onBannerClick) else Modifier)
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
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            else -> Box(Modifier.fillMaxSize().background(OpouBrandGradient))
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.30f)))
                )
        )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.offset(y = (-42).dp)) {
            val hasAvatarImage = user.avatarBase64.isNotBlank() || user.avatarUrl.isNotBlank()
            val avatarModifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .then(if (hasAvatarImage) Modifier.clickable(onClick = onAvatarClick) else Modifier)

            if (user.avatarBase64.isNotBlank()) {
                Base64Image(base64 = user.avatarBase64, modifier = avatarModifier, cornerRadiusDp = 28)
            } else {
                AsyncImage(
                    model = user.avatarUrl.ifBlank { null },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = avatarModifier.background(Color(0xFF0B7A4A))
                )
            }

            if (isOwnProfile) {
                ImagePickerButton(
                    profile = ImageCodec.ImageProfile.AVATAR,
                    onImageReady = { encoded -> onAvatarPicked(encoded.base64) },
                    onError = { onAvatarErrorChange(it) },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }

        avatarError?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
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

        Spacer(Modifier.height(8.dp))

        AssistChip(
            onClick = {},
            leadingIcon = { Icon(Icons.Filled.Sell, contentDescription = null, modifier = Modifier.size(14.dp)) },
            label = {
                Text(
                    user.communityName.ifBlank { stringResource(R.string.official_member_title) },
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )

        if (user.categories.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                user.categories.take(4).forEach { category ->
                    AssistChip(onClick = {}, label = { Text(category, style = MaterialTheme.typography.labelSmall) })
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoomStatPill(
                icon = Icons.Filled.Notes,
                count = user.paragraphsCount,
                label = stringResource(R.string.profile_paragraphs),
                modifier = Modifier.weight(1f)
            )
            RoomStatPill(
                icon = Icons.Filled.Groups,
                count = user.tekersCount,
                label = stringResource(R.string.profile_followers),
                modifier = Modifier.weight(1f)
            )
            RoomStatPill(
                icon = Icons.Filled.PersonAdd,
                count = user.tekingCount,
                label = stringResource(R.string.profile_following),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** "حبّة" إحصائية واحدة: أيقونة صغيرة فوق رقم بارز فوق تسمية — بديل بطاقة الإحصائيات
 *  المُقسَّمة بفواصل عمودية في التصميم السابق. */
@Composable
private fun RoomStatPill(icon: ImageVector, count: Int, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(4.dp))
            Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** صفّ معلومة على هيئة عنصر قائمة: شارة أيقونة دائرية + عنوان بارز + محتوى حر، يتبعه
 *  فاصل رفيع — بديل بطاقات "لمحة"/"السيرة الذاتية" المرتفعة المنفصلة في التصميم السابق. */
@Composable
private fun RoomInfoRow(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val base = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)
    val rowModifier = if (onClick != null) base.clickable(onClick = onClick) else base

    Row(rowModifier, verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
    RoomDividerLine()
}

/** فاصل رفيع بلون خافت بين عناصر قائمة "نبذة". */
@Composable
private fun RoomDividerLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    )
}

/** إشعار "غرفة خاصة" المركزي — يستبدل تبويبَي "نبذة"/"الفقرات" بالكامل عندما تكون
 *  الغرفة مقفلة على الزائر (خاصة ولم يتابعها بعد). */
@Composable
private fun RoomLockedNotice() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text("غرفة خاصة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "هذه الغرفة خاصة — تابع (تيك) صاحبها لرؤية فقراته وسيرته الذاتية",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** زر الإجراء الرئيسي: تيك/إلغاء تيك لغرفة الآخرين، أو تعديل الغرفة لصاحبها. */
@Composable
private fun RoomActionButton(
    isOwnProfile: Boolean,
    canShowTek: Boolean,
    isTeking: Boolean,
    onToggleTek: () -> Unit,
    onEditRoom: () -> Unit
) {
    if (canShowTek) {
        if (isTeking) {
            OutlinedButton(onClick = onToggleTek, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PersonRemove, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("إلغاء التيك")
            }
        } else {
            Button(
                onClick = onToggleTek,
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
}
