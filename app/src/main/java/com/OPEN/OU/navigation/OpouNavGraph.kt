package com.OPEN.OU.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.OPEN.OU.R
import com.OPEN.OU.data.model.Comment
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.UserRepository
import com.OPEN.OU.ui.components.CommentsSheet
import com.OPEN.OU.ui.screens.*

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
    fun profile(uid: String) = "profile/$uid"
    fun editProfile(uid: String) = "edit_profile/$uid"
}

/** تبويبات شريط التنقّل السفلي: الرئيسية / التيكرز / الحساب */
private enum class MainTab { HOME, TEKERS, ACCOUNT }

@Composable
fun OpouNavGraph(
    pendingShortcutAction: String? = null,
    onShortcutConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authRepo = remember { AuthRepository() }
    val userRepo = remember { UserRepository() }
    val startDestination = if (authRepo.currentUserId != null) Routes.MAIN else Routes.LOGIN

    // التبويب الأولي لشاشة MAIN — تُغيَّر قيمته عند تفعيل اختصار "التيكرز" أو
    // "غرفتي" (راجع LaunchedEffect أدناه)، وتُستخدم كمفتاح تصفير لحالة `tab`
    // المحفوظة (rememberSaveable) داخل composable(Routes.MAIN) لإجبارها على
    // اعتماد التبويب المطلوب فورًا.
    var initialMainTab by remember { mutableStateOf(MainTab.HOME) }

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

    // معالجة اختصارات التطبيق الثابتة (راجع res/xml/shortcuts.xml وMainActivity):
    // تُطبَّق فقط إن كان المستخدم مسجّلاً دخوله بالفعل (المطلوب لإتاحة أي من
    // الشاشات الثلاث). تُستهلك مرة واحدة عبر onShortcutConsumed حتى لا تُعاد
    // معالجتها عند أي إعادة تركيب لاحقة لا علاقة لها بضغطة اختصار جديدة.
    LaunchedEffect(pendingShortcutAction, authRepo.currentUserId) {
        val uid = authRepo.currentUserId
        val action = pendingShortcutAction
        if (uid == null || action == null) return@LaunchedEffect
        when (action) {
            "new_post" -> navController.navigate(Routes.CREATE_POST)
            "tekers" -> {
                initialMainTab = MainTab.TEKERS
                navController.popBackStack(Routes.MAIN, inclusive = false)
            }
            "account" -> {
                initialMainTab = MainTab.ACCOUNT
                navController.popBackStack(Routes.MAIN, inclusive = false)
            }
        }
        onShortcutConsumed()
    }

    var activeCommentsPost by remember { mutableStateOf<Post?>(null) }
    // فقرة قيد التعديل حاليًا (تُفتح CREATE_POST بوضع تعديل عوضًا عن إنشاء) أو تعليق مُقتبَس للرد عليه بفقرة جديدة
    var editingPost by remember { mutableStateOf<Post?>(null) }
    var quotingComment by remember { mutableStateOf<Comment?>(null) }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val vm: AuthViewModel = viewModel()
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
            val myUid = authRepo.currentUserId
            var tab by rememberSaveable(initialMainTab) { mutableStateOf(initialMainTab) }

            Scaffold(
                bottomBar = {
                    OpouBottomBar(selected = tab, onSelect = { tab = it })
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (tab) {
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
                                onEditPost = { post ->
                                    editingPost = post
                                    navController.navigate(Routes.CREATE_POST)
                                }
                            )

                            activeCommentsPost?.let { post ->
                                val commentsVm: CommentsViewModel = viewModel()
                                CommentsSheet(
                                    postId = post.postId,
                                    currentUsername = currentUsername,
                                    currentAvatar = currentAvatar,
                                    currentAvatarBase64 = currentAvatarBase64,
                                    viewModel = commentsVm,
                                    postAuthorId = post.authorId,
                                    onDismiss = { activeCommentsPost = null },
                                    onOpenProfile = { uid ->
                                        activeCommentsPost = null
                                        navController.navigate(Routes.profile(uid))
                                    },
                                    onQuoteAsParagraph = { comment ->
                                        quotingComment = comment
                                        activeCommentsPost = null
                                        navController.navigate(Routes.CREATE_POST)
                                    }
                                )
                            }
                        }

                        MainTab.TEKERS -> {
                            val vm: TekersViewModel = viewModel()
                            TekersScreen(
                                viewModel = vm,
                                onOpenProfile = { uid -> navController.navigate(Routes.profile(uid)) }
                            )
                        }

                        MainTab.ACCOUNT -> {
                            if (myUid != null) {
                                val vm: ProfileViewModel = viewModel()
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
                editingPost = editingPost,
                quotedComment = quotingComment,
                onDone = {
                    editingPost = null
                    quotingComment = null
                    navController.popBackStack()
                },
                onBack = {
                    editingPost = null
                    quotingComment = null
                    navController.popBackStack()
                }
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
            val vm: ProfileViewModel = viewModel()
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
            val vm: ProfileViewModel = viewModel()
            EditRoomScreen(
                uid = uid,
                viewModel = vm,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun OpouBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
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
                Icon(
                    if (selected == MainTab.ACCOUNT) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                    contentDescription = stringResource(R.string.nav_account)
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
