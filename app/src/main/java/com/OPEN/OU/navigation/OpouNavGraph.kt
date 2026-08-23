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
import com.OPEN.OU.ui.components.CommentsSheet
import com.OPEN.OU.ui.screens.*

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FEED = "feed"
    const val CREATE_POST = "create_post"
    const val PROFILE = "profile/{uid}"
    fun profile(uid: String) = "profile/$uid"
}

@Composable
fun OpouNavGraph() {
    val navController = rememberNavController()
    val authRepo = remember { AuthRepository() }
    val startDestination = if (authRepo.currentUserId != null) Routes.FEED else Routes.LOGIN

    // بيانات جلسة مبسّطة للمستخدم الحالي (تُستبدل لاحقًا بمصدر حقيقي من /users/{uid})
    var currentUsername by remember { mutableStateOf("مستخدم_أوبو") }
    var currentAvatar by remember { mutableStateOf("") }

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
            ProfileScreen(uid = uid, viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}
