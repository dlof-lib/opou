package com.OPEN.OU.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.ui.components.PostCard
import com.OPEN.OU.ui.components.ProfileHeaderSkeleton
import com.OPEN.OU.ui.components.ResponsiveContent

/**
 * شاشة "الغرفة" (الملف الشخصي). مقسّمة عبر عدة ملفات لسهولة الصيانة:
 * - ProfileTopBar.kt — الشريط العلوي.
 * - ProfileHeaderSection.kt — البانر + الصورة الرمزية + الاسم + بطاقة الإحصائيات.
 * - ProfileInfoSection.kt — لمحة/السيرة/الروابط + زر الإجراء الرئيسي.
 * - ProfileDialogs.kt — معاينة الصور وتأكيد الحظر.
 * - ProfileCommonUi.kt — عناصر مشتركة صغيرة (ProfileSectionCard/StatColumn/VerticalDivider).
 * هذا الملف هو المنسّق فقط: يحمل الحالة، ويستدعي الأقسام أعلاه بالترتيب.
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

    // مدة انتظار قصيرة قبل عرض حالة "تعذّر التحميل" بدل بقاء الهيكل العظمي (Skeleton)
    // يدور إلى الأبد بصمت إن فشل الاتصال أو كانت البيانات غير موجودة أصلًا.
    var showLoadTimeout by remember(uid) { mutableStateOf(false) }

    LaunchedEffect(uid) {
        showLoadTimeout = false
        // load() ذاتها لا تطلق استثناءً (كل عملياتها الداخلية محمية بـ runCatching)،
        // لكن runCatching هنا يبقى خط دفاع أخير رخيصًا يمنع أي مفاجأة مستقبلية من
        // إسقاط الشاشة بالكامل عند فتحها.
        runCatching { viewModel.load(uid) }
    }
    LaunchedEffect(uid, room) {
        if (room == null) {
            kotlinx.coroutines.delay(8000)
            showLoadTimeout = true
        }
    }
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
        // مساحة إضافية لشريط التنقّل السفلي للنظام. عند العرض كشاشة مستقلة
        // (فتح ملف شخص آخر) نُبقي السلوك الافتراضي لأنها غير متداخلة.
        contentWindowInsets = if (!showBackButton) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProfileTopBar(
                showBackButton = showBackButton,
                onBack = onBack,
                isOwnProfile = isOwnProfile,
                onEditRoom = onEditRoom,
                onOpenSettings = onOpenSettings,
                showMoreMenu = showMoreMenu,
                onShowMoreMenuChange = { showMoreMenu = it },
                canShowMoreMenu = !isOwnProfile && myUid != null,
                isBlocked = isBlocked,
                onToggleBlockMenuItem = {
                    showMoreMenu = false
                    if (isBlocked) viewModel.toggleBlock(uid) else showBlockConfirm = true
                }
            )
        }
    ) { padding ->
        val user = room
        if (user == null) {
            ResponsiveContent(modifier = Modifier.padding(padding)) {
                if (showLoadTimeout) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "تعذّر تحميل هذه الغرفة. تحقّق من اتصالك بالإنترنت ثم أعد المحاولة.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            showLoadTimeout = false
                            runCatching { viewModel.load(uid, force = true) }
                        }) { Text("إعادة المحاولة") }
                    }
                } else {
                    ProfileHeaderSkeleton()
                }
            }
            return@Scaffold
        }
        val isLocked = user.isPrivateRoom && !isOwnProfile && !isTeking

        ResponsiveContent(modifier = Modifier.padding(padding)) {
            // LazyColumn مهم جدًا هنا: الملف الشخصي قد يحتوي عشرات الفقرات،
            // وكل فقرة قد تحتوي صورة Base64 كبيرة. استخدام Column+verticalScroll
            // كان يكوّن كل البطاقات والصور دفعة واحدة، ما قد يؤدي إلى OOM وإغلاق
            // التطبيق فور فتح الحساب على الأجهزة ذات الذاكرة المحدودة.
            val profileListState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = profileListState,
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "profile-header") {
                    ProfileHeaderSection(
                        user = user,
                        isOwnProfile = isOwnProfile,
                        avatarError = avatarError,
                        onAvatarErrorChange = { avatarError = it },
                        onAvatarClick = { showAvatarViewer = true },
                        onBannerClick = { showBannerViewer = true },
                        onAvatarPicked = { base64 -> viewModel.updateAvatar(uid, base64) }
                    )

                    Column(Modifier.padding(horizontal = 16.dp)) {
                        ProfileInfoSection(
                            user = user,
                            isOwnProfile = isOwnProfile,
                            isLocked = isLocked,
                            uriHandler = uriHandler,
                            onEditRoom = onEditRoom
                        )

                        Spacer(Modifier.height(22.dp))

                        ProfileActionButton(
                            isOwnProfile = isOwnProfile,
                            canShowTekButton = !isOwnProfile && myUid != null,
                            isTeking = isTeking,
                            onToggleTek = { viewModel.toggleTek(uid) },
                            onEditRoom = onEditRoom
                        )

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
                }

                if (!isLocked && posts.isNotEmpty()) {
                    // لا نستخدم مفتاحًا مخصصًا هنا؛ بعض البيانات القديمة قد تحتوي postId فارغًا
                    // أو متكررًا، ومفتاح LazyColumn مكرر يسبب IllegalArgumentException.
                    items(posts) { post ->
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

    ProfileDialogs(
        room = room,
        showAvatarViewer = showAvatarViewer,
        onDismissAvatarViewer = { showAvatarViewer = false },
        showBannerViewer = showBannerViewer,
        onDismissBannerViewer = { showBannerViewer = false },
        showBlockConfirm = showBlockConfirm,
        onDismissBlockConfirm = { showBlockConfirm = false },
        onConfirmBlock = {
            showBlockConfirm = false
            viewModel.toggleBlock(uid)
        }
    )
}
