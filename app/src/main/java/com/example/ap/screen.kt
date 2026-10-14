package com.example.ap
import androidx.compose.foundation.background

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.StateFlow

// ====================== Shared pieces ======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = actions,
            )
        },
        content = content,
    )
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            "Could not load the data.\nCheck your internet connection.",
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun <T> StateView(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        UiState.Loading -> Loading(modifier)
        UiState.Error -> ErrorBox(modifier)
        is UiState.Success -> content(state.data)
    }
}

@Composable
fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        content = content,
    )
}

@Composable
fun MealCard(meal: MealSummary, onClick: () -> Unit) {
    AppCard(onClick) {
        AsyncImage(
            model = meal.strMealThumb,
            contentDescription = meal.strMeal,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Text(
            meal.strMeal,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MealGrid(meals: List<MealSummary>, modifier: Modifier, onMeal: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(meals, key = { it.idMeal }) { meal ->
            MealCard(meal) { onMeal(meal.idMeal) }
        }
    }
}

private val FullWidth: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

// ====================== Screen 1: Home ======================

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onSearch: (String) -> Unit,
    onCategory: (String) -> Unit,
    onMeal: (String) -> Unit,
    onFavorites: () -> Unit,
) {
    val categories by vm.categories.collectAsState()
    val mealOfTheDay by vm.mealOfTheDay.collectAsState()
    val favorites by vm.favorites.collectAsState()
    var query by remember { mutableStateOf("") }

    Scaffold { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            item(span = FullWidth) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "What's for dinner?",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Find a recipe, cook it, save it.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onFavorites) {
                        BadgedBox(
                            badge = {
                                if (favorites.isNotEmpty()) Badge { Text("${favorites.size}") }
                            },
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Favorites",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Search
            item(span = FullWidth) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search a recipe (e.g. pasta)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { if (query.isNotBlank()) onSearch(query.trim()) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Meal of the day
            item(span = FullWidth) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Meal of the day",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(onClick = { vm.rollMeal() }) { Text("🎲 Another one") }
                    }
                    StateView(mealOfTheDay, Modifier.fillMaxWidth().height(200.dp)) { meal ->
                        FeaturedMeal(meal) { onMeal(meal.id) }
                    }
                }
            }

            // Categories
            item(span = FullWidth) {
                Text(
                    "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            when (val c = categories) {
                UiState.Loading -> item(span = FullWidth) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                UiState.Error -> item(span = FullWidth) {
                    Text("Could not load the categories. Check your internet connection.")
                }
                is UiState.Success -> items(c.data, key = { it.idCategory }) { cat ->
                    AppCard(onClick = { onCategory(cat.strCategory) }) {
                        AsyncImage(
                            model = cat.strCategoryThumb,
                            contentDescription = cat.strCategory,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.4f).padding(8.dp),
                        )
                        Text(
                            cat.strCategory,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedMeal(meal: Meal, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = meal.thumb,
                contentDescription = meal.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .let { it }
                    .then(Modifier),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .then(
                        Modifier.padding(0.dp)
                    ),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .let { base ->
                            base
                        },
                )
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .let { it }
                    .padding(16.dp),
            ) {
                Text(
                    meal.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${meal.category} · ${meal.area}", color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

// ====================== Screen 2: Recipe list ======================

@Composable
fun MealListScreen(
    title: String,
    source: StateFlow<UiState<List<MealSummary>>>,
    onBack: () -> Unit,
    onMeal: (String) -> Unit,
) {
    val state by source.collectAsState()

    AppScaffold(title, onBack) { padding ->
        StateView(state, Modifier.padding(padding)) { meals ->
            if (meals.isEmpty()) {
                Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recipes found 🍽️")
                }
            } else {
                MealGrid(meals, Modifier.padding(padding), onMeal)
            }
        }
    }
}

// ====================== Screen 3: Recipe details ======================

@Composable
fun DetailScreen(vm: MainViewModel, id: String, onBack: () -> Unit) {
    val state by vm.meal(id).collectAsState()
    val favorites by vm.favorites.collectAsState()
    val context = LocalContext.current

    val meal = (state as? UiState.Success)?.data
    val isFavorite = favorites.any { it.idMeal == id }

    AppScaffold(
        title = meal?.name ?: "Recipe",
        onBack = onBack,
        actions = {
            if (meal != null) {
                IconButton(onClick = { vm.toggleFavorite(meal.toSummary()) }) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    ) { padding ->
        StateView(state, Modifier.padding(padding)) { m ->
            RecipeContent(
                meal = m,
                modifier = Modifier.padding(padding),
                onWatch = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(m.youtube)))
                },
            )
        }
    }
}

@Composable
fun RecipeContent(meal: Meal, modifier: Modifier, onWatch: () -> Unit) {
    val steps = meal.instructions.split("\n").map { it.trim() }.filter { it.isNotBlank() }

    Column(modifier.verticalScroll(rememberScrollState())) {
        AsyncImage(
            model = meal.thumb,
            contentDescription = meal.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.3f),
        )
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(meal.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "${meal.category} · ${meal.area}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )

            if (meal.youtube.isNotBlank()) {
                Button(onClick = onWatch) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Watch on YouTube")
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Ingredients", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            meal.ingredients.forEach { (name, measure) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, modifier = Modifier.weight(1f))
                    Text(measure, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Instructions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "${index + 1}",
                        modifier = Modifier.width(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(step, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ====================== Screen 4: Favorites ======================

@Composable
fun FavoritesScreen(vm: MainViewModel, onBack: () -> Unit, onMeal: (String) -> Unit) {
    val favorites by vm.favorites.collectAsState()

    AppScaffold("Favorites", onBack) { padding ->
        if (favorites.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No favorites yet.\nTap the ♥ on a recipe to save it here.",
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            MealGrid(favorites, Modifier.padding(padding), onMeal)
        }
    }
}