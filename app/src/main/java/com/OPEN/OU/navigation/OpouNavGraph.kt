package com.OPEN.OU.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.UserRepository
import com.OPEN.OU.ui.components.CommentsSheet
import com.OPEN.OU.ui.screens.*

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FEED = "feed"
    const val CREATE_POST = "create_post"
    const val PROFILE = "profile/{uid}"
    const val EDIT_PROFILE = "edit_profile/{uid}"
    fun profile(uid: String) = "profile/$uid"
    fun editProfile(uid: String) = "edit_profile/$uid"
}

@Composable
fun OpouNavGraph() {
    val navController = rememberNavController()
    val authRepo = remember { AuthRepository() }
    val userRepo = remember { UserRepository() }
    val startDestination = if (authRepo.currentUserId != null) Routes.FEED else Routes.LOGIN

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

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val vm: AuthViewModel = viewModel()
            LoginScreen(
                viewModel = vm,
                onLoginSuccess = {
                    navController.navigate(Routes.FEED) {
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
                    navController.navigate(Routes.FEED) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.FEED) {
            val vm: FeedViewModel = viewModel()
            FeedScreen(
                viewModel = vm,
                currentUsername = currentUsername,
                currentAvatar = currentAvatar,
                currentAvatarBase64 = currentAvatarBase64,
                onOpenProfile = { uid -> navController.navigate(Routes.profile(uid)) },
                onOpenComments = { post -> activeCommentsPost = post },
                onCreatePost = { navController.navigate(Routes.CREATE_POST) }
            )

            activeCommentsPost?.let { post ->
                val commentsVm: CommentsViewModel = viewModel()
                CommentsSheet(
                    postId = post.postId,
                    currentUsername = currentUsername,
                    currentAvatar = currentAvatar,
                    viewModel = commentsVm,
                    onDismiss = { activeCommentsPost = null }
                )
            }
        }

        composable(Routes.CREATE_POST) {
            val vm: FeedViewModel = viewModel()
            CreatePostScreen(
                viewModel = vm,
                currentUsername = currentUsername,
                currentAvatar = currentAvatar,
                currentAvatarBase64 = currentAvatarBase64,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
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
