package com.OPEN.OU.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.OPEN.OU.R
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.UserRepository
import com.OPEN.OU.ui.components.Base64Image
import com.OPEN.OU.ui.components.CommentsSheet
import com.OPEN.OU.ui.screens.*
import com.OPEN.OU.util.TwoFactorGate

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val CREATE_POST = "create_post"
    const val PROFILE = "profile/{uid}"
    const val EDIT_PROFILE = "edit_profile/{uid}"
    const val SETTINGS = "settings"
    const val ACCOUNT_SETTINGS = "account_settings"
    const val PRIVACY_SETTINGS = "privacy_settings"
    const val BLOCKED_USERS = "blocked_users"
    const val THREAD = "thread/{threadId}"
    fun profile(uid: String) = "profile/$uid"
    fun editProfile(uid: String) = "edit_profile/$uid"
    fun thread(threadId: String) = "thread/$threadId"
}

/** تبويبات شريط التنقّل السفلي: الرئيسية / التيكرز / الحساب. تبويب "الحساب" يعرض
 *  [ProfileScreen] الخاصة بالمستخدم الحالي (uid الخاص به)، بأيقونة صورته الرمزية
 *  في الشريط السفلي، مع إمكانية الوصول لشاشة الإعدادات من داخل تلك الصفحة (زر
 *  الترس أعلى الشاشة، عبر onOpenSettings). *ملاحظة*: هذا التبويب كان قد أُزيل
 *  سابقًا بسبب انهيار متكرر لم يُحدَّد سببه؛ أُعيد الآن بناءً على طلب صريح — إن
 *  تكرّر الانهيار عند فتحه يُستحسن مراجعة السبب الأصلي قبل الاعتماد عليه. */
private enum class MainTab { HOME, TEKERS, ACCOUNT }

/**
 * مصنع ViewModel بسيط وصريح: يُنشئ النسخة عبر lambda عادية بدل الاعتماد على
 * إنشاء ViewModelProvider الافتراضي، الذي يتطلّب باطن Reflection باحثًا عن
 * "مُنشئ بلا معاملات" (no-arg constructor). فئات مثل [ProfileViewModel] لديها
 * معاملات لها قيم افتراضية فقط (بدون @JvmOverloads)، وهذا لا يُنتج فعليًا
 * مُنشئًا بلا معاملات في الـ bytecode — ما قد يجعل الإنشاء عبر الانعكاس هشًّا
 * حسب إصدار مكتبة lifecycle والمسار الذي تسلكه داخليًا. هذا المصنع يتجاوز
 * الانعكاس تمامًا ويضمن نفس نتيجة استدعاء المُنشئ في كود Kotlin العادي.
 */
private class SimpleViewModelFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}

@Composable
fun OpouNavGraph(
    /** اختصار التطبيق الذي فُتح منه التطبيق (إن وُجد) — راجع [com.OPEN.OU.EXTRA_SHORTCUT_ACTION]. */
    pendingShortcutAction: String? = null,
    /** يُستدعى بعد معالجة اختصار التطبيق لمنع إعادة معالجته عند إعادة التركيب (recomposition). */
    onShortcutConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authRepo = remember { AuthRepository() }
    val userRepo = remember { UserRepository() }

    // ── حارس التحقق بخطوتين ───────────────────────────────────────────────
    // لا يكفي الاعتماد على وجود جلسة Firebase Auth سارية (authRepo.currentUserId) لتقرير
    // فتح الشاشة الرئيسية مباشرة: Firebase Auth يُبقي المستخدم "مسجَّل دخول" فعليًا فور
    // نجاح كلمة المرور، أي قبل إدخال رمز PIN بخطوة كاملة. بدون هذا الفحص، كان يكفي من
    // يملك كلمة المرور فقط إغلاق التطبيق وإعادة فتحه ليتجاوز شاشة PIN كليًا. لذا ننتظر هنا
    // (شاشة تحميل بسيطة) حتى نتحقق فعليًا: هل هذا المستخدم يتطلّب PIN ولم يُتمّه بعد لهذه
    // الجلسة (TwoFactorGate)؟ إن كان كذلك نوجّهه لخطوة PIN بدل الشاشة الرئيسية مباشرة.
    var authGateChecked by remember { mutableStateOf(false) }
    var needsPinRecheck by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = authRepo.currentUserId
        if (uid != null) {
            val user = runCatching { userRepo.getUser(uid) }.getOrNull()
            needsPinRecheck = user?.twoFactorEnabled == true && !TwoFactorGate.isVerified(uid)
        }
        authGateChecked = true
    }

    if (!authGateChecked) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (authRepo.currentUserId != null && !needsPinRecheck) Routes.MAIN else Routes.LOGIN

    // بيانات المستخدم الحالي الحقيقية، تُقرأ فوريًا (Realtime) من /users/{uid}
    var currentUsername by remember { mutableStateOf("") }
    var currentAvatar by remember { mutableStateOf("") }
    var currentAvatarBase64 by remember { mutableStateOf("") }

    LaunchedEffect(authRepo.currentUserId) {
        val uid = authRepo.currentUserId ?: return@LaunchedEffect
        userRepo.observeUser(uid).collect { user ->
            if (user != null) {
                currentUsername = user.username
                currentAvatar = user.avatarUrl
                currentAvatarBase64 = user.avatarBase64
            }
        }
    }

    var activeCommentsPost by remember { mutableStateOf<Post?>(null) }
    /** الفقرة قيد التعديل حاليًا (إن وُجدت) — تُمرَّر لشاشة CreatePostScreen في وضع "تعديل". */
    var editingPost by remember { mutableStateOf<Post?>(null) }
    /** الفقرة التي يتابع المستخدم سلسلته انطلاقًا منها (إن وُجدت). */
    var continuingFromPost by remember { mutableStateOf<Post?>(null) }
    // تبويب الشاشة الرئيسية مرفوع هنا (بدل داخل composable(MAIN)) بحيث يمكن لاختصارات
    // التطبيق ("تيكرز"/"حساب") تغييره من الخارج دون الحاجة لإعادة إنشاء الشاشة الرئيسية.
    var mainTab by rememberSaveable { mutableStateOf(MainTab.HOME) }

    // يعالج اختصار التطبيق الذي فُتح منه التطبيق (إن وُجد) بعد تسجيل الدخول فقط،
    // ثم يُبلّغ MainActivity باستهلاكه لمنع إعادة تنفيذه عند أي إعادة تركيب لاحقة.
    LaunchedEffect(pendingShortcutAction, authRepo.currentUserId) {
        val action = pendingShortcutAction ?: return@LaunchedEffect
        if (authRepo.currentUserId == null) return@LaunchedEffect
        when (action) {
            "new_post" -> navController.navigate(Routes.CREATE_POST)
            "tekers" -> {
                mainTab = MainTab.TEKERS
                navController.navigate(Routes.MAIN) { launchSingleTop = true; popUpTo(Routes.MAIN) }
            }
            "account" -> {
                mainTab = MainTab.ACCOUNT
                navController.navigate(Routes.MAIN) { launchSingleTop = true; popUpTo(Routes.MAIN) }
            }
        }
        onShortcutConsumed()
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val vm: AuthViewModel = viewModel()
            // إن وصلنا هنا بسبب جلسة سارية تتطلّب إعادة التحقق من PIN (وليس تسجيل دخول
            // جديد كليًا)، ننتقل مباشرة لخطوة PIN دون طلب البريد/كلمة المرور مجددًا.
            LaunchedEffect(needsPinRecheck) {
                if (needsPinRecheck) {
                    authRepo.currentUserId?.let { vm.beginPinRecheck(it) }
                }
            }
            LoginScreen(
                viewModel = vm,
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            val vm: AuthViewModel = viewModel()
            RegisterScreen(
                viewModel = vm,
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.MAIN) {
            Scaffold(
                bottomBar = {
                    OpouBottomBar(
                        selected = mainTab,
                        onSelect = { mainTab = it },
                        currentAvatar = currentAvatar,
                        currentAvatarBase64 = currentAvatarBase64
                    )
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (mainTab) {
                        MainTab.HOME -> {
                            val vm: FeedViewModel = viewModel()
                            FeedScreen(
                                viewModel = vm,
                                currentUsername = currentUsername,
                                currentAvatar = currentAvatar,
                                currentAvatarBase64 = currentAvatarBase64,
                                onOpenProfile = { uid -> navController.navigate(Routes.profile(uid)) },
                                onOpenComments = { post -> activeCommentsPost = post },
                                onCreatePost = { navController.navigate(Routes.CREATE_POST) },
                                onEditPost = { post -> editingPost = post; navController.navigate(Routes.CREATE_POST) },
                                onOpenThread = { threadId -> navController.navigate(Routes.thread(threadId)) },
                                onContinueThread = { post -> continuingFromPost = post; navController.navigate(Routes.CREATE_POST) }
                            )
                        }

                        MainTab.TEKERS -> {
                            val vm: TekersViewModel = viewModel()
                            TekersScreen(
                                viewModel = vm,
                                onOpenProfile = { uid -> navController.navigate(Routes.profile(uid)) }
                            )
                        }

                        MainTab.ACCOUNT -> {
                            val myUid = authRepo.currentUserId
                            if (myUid != null) {
                                val vm: ProfileViewModel = viewModel(
                                    key = "profile_$myUid",
                                    factory = SimpleViewModelFactory { ProfileViewModel() }
                                )
                                ProfileScreen(
                                    uid = myUid,
                                    viewModel = vm,
                                    onBack = {},
                                    showBackButton = false,
                                    onEditRoom = { navController.navigate(Routes.editProfile(myUid)) },
                                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                                )
                            }
                        }
                    }
                }
            }
        }

        composable(Routes.CREATE_POST) {
            val vm: FeedViewModel = viewModel()
            CreatePostScreen(
                viewModel = vm,
                currentUsername = currentUsername,
                currentAvatar = currentAvatar,
                currentAvatarBase64 = currentAvatarBase64,
                onDone = { editingPost = null; continuingFromPost = null; navController.popBackStack() },
                onBack = { editingPost = null; continuingFromPost = null; navController.popBackStack() },
                editingPost = editingPost,
                continuingFromPost = continuingFromPost
            )
        }

        composable(
            route = Routes.THREAD,
            arguments = listOf(navArgument("threadId") { type = NavType.StringType })
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId").orEmpty()
            val vm: ThreadViewModel = viewModel()
            ThreadScreen(
                threadId = threadId,
                viewModel = vm,
                currentUsername = currentUsername,
                currentAvatar = currentAvatar,
                currentAvatarBase64 = currentAvatarBase64,
                onBack = { navController.popBackStack() },
                onOpenProfile = { uid -> navController.navigate(Routes.profile(uid)) },
                onOpenComments = { post -> activeCommentsPost = post },
                onEditPost = { post -> editingPost = post; navController.navigate(Routes.CREATE_POST) },
                onContinueThread = { post -> continuingFromPost = post; navController.navigate(Routes.CREATE_POST) }
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenAccountSettings = { navController.navigate(Routes.ACCOUNT_SETTINGS) },
                onOpenPrivacySettings = { navController.navigate(Routes.PRIVACY_SETTINGS) }
            )
        }

        composable(Routes.ACCOUNT_SETTINGS) {
            AccountSettingsScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PRIVACY_SETTINGS) {
            PrivacySettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenBlockedUsers = { navController.navigate(Routes.BLOCKED_USERS) }
            )
        }

        composable(Routes.BLOCKED_USERS) {
            BlockedUsersScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.PROFILE,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid").orEmpty()
            val vm: ProfileViewModel = viewModel(
                key = "profile_$uid",
                factory = SimpleViewModelFactory { ProfileViewModel() }
            )
            ProfileScreen(
                uid = uid,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEditRoom = { navController.navigate(Routes.editProfile(uid)) }
            )
        }

        composable(
            route = Routes.EDIT_PROFILE,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid").orEmpty()
            val vm: ProfileViewModel = viewModel(
                key = "edit_profile_$uid",
                factory = SimpleViewModelFactory { ProfileViewModel() }
            )
            EditRoomScreen(
                uid = uid,
                viewModel = vm,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }

    // تُعرض فوق NavHost (وليس داخل تبويب واحد) بحيث يمكن فتح تعليقات فقرة من أي شاشة
    // (التغذية، سلسلة فقرات، غرفة مستخدم...) لا من الشاشة الرئيسية فقط.
    activeCommentsPost?.let { post ->
        val commentsVm: CommentsViewModel = viewModel()
        CommentsSheet(
            postId = post.postId,
            currentUsername = currentUsername,
            currentAvatar = currentAvatar,
            currentAvatarBase64 = currentAvatarBase64,
            viewModel = commentsVm,
            postAuthorId = post.authorId,
            onDismiss = { activeCommentsPost = null }
        )
    }
}

@Composable
private fun OpouBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    currentAvatar: String,
    currentAvatarBase64: String
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
        NavigationBarItem(
            selected = selected == MainTab.HOME,
            onClick = { onSelect(MainTab.HOME) },
            icon = {
                Icon(
                    if (selected == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = stringResource(R.string.nav_home)
                )
            },
            label = { Text(stringResource(R.string.nav_home)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        )
        NavigationBarItem(
            selected = selected == MainTab.TEKERS,
            onClick = { onSelect(MainTab.TEKERS) },
            icon = {
                Icon(
                    if (selected == MainTab.TEKERS) Icons.Filled.Groups else Icons.Outlined.Groups,
                    contentDescription = stringResource(R.string.nav_tekers)
                )
            },
            label = { Text(stringResource(R.string.nav_tekers)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        )
        NavigationBarItem(
            selected = selected == MainTab.ACCOUNT,
            onClick = { onSelect(MainTab.ACCOUNT) },
            icon = {
                AccountNavIcon(
                    avatarUrl = currentAvatar,
                    avatarBase64 = currentAvatarBase64,
                    selected = selected == MainTab.ACCOUNT
                )
            },
            label = { Text(stringResource(R.string.nav_account)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        )
    }
}

/** أيقونة تبويب "الحساب": تعرض صورة المستخدم الرمزية الحقيقية إن وُجدت (Base64 أو رابط)،
 *  وإلا أيقونة شخص افتراضية — بدل أيقونة ترس عامة، لتمييز التبويب كملف شخصي حقيقي. */
@Composable
private fun AccountNavIcon(avatarUrl: String, avatarBase64: String, selected: Boolean) {
    val hasImage = avatarBase64.isNotBlank() || avatarUrl.isNotBlank()
    if (hasImage) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
        ) {
            if (avatarBase64.isNotBlank()) {
                Base64Image(
                    base64 = avatarBase64,
                    modifier = Modifier.size(24.dp),
                    cornerRadiusDp = 24
                )
            } else {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = stringResource(R.string.nav_account),
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
            }
        }
    } else {
        Icon(
            if (selected) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
            contentDescription = stringResource(R.string.nav_account)
        )
    }
}
