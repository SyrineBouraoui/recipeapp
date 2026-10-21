package com.example.ap


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Error : UiState<Nothing>
}

class MainViewModel : ViewModel() {

    private fun <T> request(block: suspend () -> T): Flow<UiState<T>> =
        flow<UiState<T>> { emit(UiState.Success(block())) }
            .catch { emit(UiState.Error) }

    //home
    val categories: StateFlow<UiState<List<Category>>> =
        request { Api.categories() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _mealOfTheDay = MutableStateFlow<UiState<Meal>>(UiState.Loading)
    val mealOfTheDay: StateFlow<UiState<Meal>> = _mealOfTheDay.asStateFlow()

    init {
        rollMeal()
    }

    fun rollMeal() {
        viewModelScope.launch {
            _mealOfTheDay.value = UiState.Loading
            request { Api.randomMeal() ?: error("No meal") }
                .collect { _mealOfTheDay.value = it }
        }
    }



    private val listCache = mutableMapOf<String, StateFlow<UiState<List<MealSummary>>>>()
    private val mealCache = mutableMapOf<String, StateFlow<UiState<Meal>>>()

    private fun <T> cached(
        cache: MutableMap<String, StateFlow<UiState<T>>>,
        key: String,
        block: suspend () -> T,
    ): StateFlow<UiState<T>> =
        cache.getOrPut(key) {
            request(block)
                .onEach { if (it is UiState.Error) cache.remove(key) }
                .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)
        }

    fun mealsByCategory(category: String) =
        cached(listCache, "c:$category") { Api.mealsByCategory(category) }

    fun search(query: String) =
        cached(listCache, "s:${query.lowercase()}") { Api.search(query) }

    fun meal(id: String) =
        cached(mealCache, id) { Api.meal(id) ?: error("Meal not found") }

    // save data in favorite
    private val _favorites = MutableStateFlow<List<MealSummary>>(emptyList())
    val favorites: StateFlow<List<MealSummary>> = _favorites.asStateFlow()

    fun toggleFavorite(meal: MealSummary) {
        _favorites.update { list ->
            if (list.any { it.idMeal == meal.idMeal }) {
                list.filter { it.idMeal != meal.idMeal }
            } else {
                list + meal
            }
        }
    }
}