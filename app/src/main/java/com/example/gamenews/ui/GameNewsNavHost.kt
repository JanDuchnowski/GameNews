package com.example.gamenews.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.gamenews.ui.detail.GameDetailRoute
import com.example.gamenews.ui.list.GameListRoute
import kotlinx.serialization.Serializable

/** Type-safe destinations; the compiler checks the argument types, not a string route. */
@Serializable
data object GameListDestination

@Serializable
data class GameDetailDestination(val gameId: Int)

@Composable
fun GameNewsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = GameListDestination,
        modifier = modifier,
    ) {
        composable<GameListDestination> {
            GameListRoute(
                onGameClick = { gameId -> navController.navigate(GameDetailDestination(gameId)) },
            )
        }
        composable<GameDetailDestination> { backStackEntry ->
            // Declared so the destination's argument type stays visible at the call site.
            backStackEntry.toRoute<GameDetailDestination>()
            GameDetailRoute(
                onBackClick = navController::popBackStack,
            )
        }
    }
}
