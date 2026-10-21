package com.example.ap

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class Category(
    val idCategory: String,
    val strCategory: String,
    val strCategoryThumb: String,
)

@Serializable
data class CategoriesResponse(val categories: List<Category> = emptyList())

@Serializable
data class MealSummary(
    val idMeal: String,
    val strMeal: String,
    val strMealThumb: String,
)

@Serializable
data class MealsResponse(val meals: List<MealSummary>? = null)


data class Meal(
    val id: String,
    val name: String,
    val thumb: String,
    val category: String,
    val area: String,
    val instructions: String,
    val youtube: String,
    val ingredients: List<Pair<String, String>>, //2 string so it can pair by name or quantity
)

fun Meal.toSummary() = MealSummary(id, name, thumb)

object Api {
    private const val BASE = "https://www.themealdb.com/api/json/v1/1"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun categories(): List<Category> =
        client.get("$BASE/categories.php").body<CategoriesResponse>().categories

    suspend fun mealsByCategory(category: String): List<MealSummary> =
        client.get("$BASE/filter.php") { parameter("c", category) }
            .body<MealsResponse>().meals.orEmpty()

    suspend fun search(query: String): List<MealSummary> =
        client.get("$BASE/search.php") { parameter("s", query) }
            .body<MealsResponse>().meals.orEmpty()

    suspend fun meal(id: String): Meal? =
        parseMeal(client.get("$BASE/lookup.php") { parameter("i", id) }.body())

    suspend fun randomMeal(): Meal? =
        parseMeal(client.get("$BASE/random.php").body())



    private fun parseMeal(root: JsonObject): Meal? { //pour transformer la réponse JSON de l’API en objet Kotlin
        val obj = (root["meals"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return null

        fun text(key: String): String =
            (obj[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

        val ingredients = (1..20).mapNotNull { i ->
            val name = text("strIngredient$i")
            if (name.isBlank()) null else name to text("strMeasure$i")
        }

        return Meal(
            id = text("idMeal"),
            name = text("strMeal"),
            thumb = text("strMealThumb"),
            category = text("strCategory"),
            area = text("strArea"),
            instructions = text("strInstructions"),
            youtube = text("strYoutube"),
            ingredients = ingredients,
        )
    }
}