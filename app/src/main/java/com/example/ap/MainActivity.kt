package com.example.ap

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFFD84315),
                    onPrimary = Color.White,
                    background = Color(0xFFFFF8F2),
                    surface = Color(0xFFFFF8F2),
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "home") {

        composable("home") {
            HomeScreen(
                vm,
                onSearch = { q -> nav.navigate("search/${Uri.encode(q)}") },
                onCategory = { name -> nav.navigate("category/${Uri.encode(name)}") },
                onMeal = { id -> nav.navigate("meal/$id") },
                onFavorites = { nav.navigate("favorites") },
            )
        }

        composable("category/{name}") { entry ->
            val name = entry.arguments?.getString("name").orEmpty()
            MealListScreen(
                title = name,
                source = vm.mealsByCategory(name),
                onBack = { nav.popBackStack() },
                onMeal = { id -> nav.navigate("meal/$id") },
            )
        }

        composable("search/{query}") { entry ->
            val query = entry.arguments?.getString("query").orEmpty()
            MealListScreen(
                title = "Results for \"$query\"",
                source = vm.search(query),
                onBack = { nav.popBackStack() },
                onMeal = { id -> nav.navigate("meal/$id") },
            )
        }

        composable("meal/{id}") { entry ->
            DetailScreen(
                vm,
                id = entry.arguments?.getString("id").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }

        composable("favorites") {
            FavoritesScreen(
                vm,
                onBack = { nav.popBackStack() },
                onMeal = { id -> nav.navigate("meal/$id") },
            )
        }
    }
}