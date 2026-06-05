package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.CatalogItem
import com.example.ui.viewmodel.FicSwipeViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DiscoverScreen(
    viewModel: FicSwipeViewModel,
    modifier: Modifier = Modifier
) {
    val discoverQueue by viewModel.discoverQueue.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App title header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text(
                text = "FicSwipe 🎴",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Desliza a la derecha para guardar offline y escuchar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Active Card Deck
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (discoverQueue.isEmpty()) {
                // Empty state card
                EmptyDeckView(onReset = { viewModel.resetDiscoverDeck() })
            } else {
                // Secondary background item card for depth
                if (discoverQueue.size > 1) {
                    val nextItem = discoverQueue[1]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(440.dp)
                            .rotate(-2f)
                            .offset(y = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {}
                }

                // Active Top Card
                val topItem = discoverQueue[0]
                SwipeCard(
                    item = topItem,
                    onSwipeLeft = { viewModel.swipeLeft() },
                    onSwipeRight = { viewModel.swipeRight(topItem) }
                )
            }
        }

        // Tactile Tinder Action Buttons Row
        if (discoverQueue.isNotEmpty()) {
            val topItem = discoverQueue[0]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip Button (Left / Circle Button)
                IconButton(
                    onClick = { viewModel.swipeLeft() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE57373).copy(alpha = 0.2f))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Omitir Fanfic",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Reset Catalog Deck Button
                IconButton(
                    onClick = { viewModel.resetDiscoverDeck() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reiniciar Catálogo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Save/Download Button (Right / Circle Button)
                IconButton(
                    onClick = { viewModel.swipeRight(topItem) },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF81C784).copy(alpha = 0.2f))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Guardar Fanfic Offline",
                        tint = Color(0xFF388E3C),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun SwipeCard(
    item: CatalogItem,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    
    // Smooth custom physics positioning animatables
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    
    // Keep track of swiping progress to color overlay banners
    var dragProgress by remember { mutableStateOf(0f) }

    val rotation = animOffsetX.value * 0.04f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(440.dp)
            .offset { IntOffset(animOffsetX.value.roundToInt(), animOffsetY.value.roundToInt()) }
            .rotate(rotation)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        val threshold = widthPx * 0.35f
                        scope.launch {
                            if (animOffsetX.value > threshold) {
                                // Swipe right
                                animOffsetX.animateTo(widthPx, animationSpec = tween(200))
                                onSwipeRight()
                            } else if (animOffsetX.value < -threshold) {
                                // Swipe left
                                animOffsetX.animateTo(-widthPx, animationSpec = tween(200))
                                onSwipeLeft()
                            } else {
                                // Snap back to center
                                launch { animOffsetX.animateTo(0f, animationSpec = tween(150)) }
                                launch { animOffsetY.animateTo(0f, animationSpec = tween(150)) }
                                dragProgress = 0f
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            animOffsetX.snapTo(animOffsetX.value + dragAmount.x)
                            animOffsetY.snapTo(animOffsetY.value + dragAmount.y)
                            dragProgress = animOffsetX.value / (widthPx * 0.35f)
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Category aesthetic backdrop based on universes
            val gradientColors = when (item.category) {
                "Harry Potter" -> listOf(Color(0xFF2C1E3D), Color(0xFF4A3B66), Color(0xFF1E132B))
                "Naruto" -> listOf(Color(0xFF3E2723), Color(0xFFD84315), Color(0xFF211412))
                "Percy Jackson" -> listOf(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF0A2240))
                "Twilight" -> listOf(Color(0xFF263238), Color(0xFF455A64), Color(0xFF1A2327))
                else -> listOf(Color(0xFF2E3440), Color(0xFF3B4252), Color(0xFF22262E))
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Dynamic universe card header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Brush.verticalGradient(colors = gradientColors))
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = item.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Card body contents (Author and Synopsis)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Escrito por ${item.author}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = item.synopsis,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Bottom info label chips
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.chaptersCount} Capítulos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    isHighlightColor(item.category).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${item.wordCount} plds",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = isHighlightColor(item.category)
                            )
                        }
                    }
                }
            }

            // Interactive visual tags overlay (LIKE / SKIP) based on slide direction
            if (dragProgress > 0.1f) {
                Box(
                    modifier = Modifier
                        .offset(x = 16.dp, y = 16.dp)
                        .rotate(-15f)
                        .background(Color(0xFF388E3C).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "GUARDAR",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            } else if (dragProgress < -0.1f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-16).dp, y = 16.dp)
                        .rotate(15f)
                        .background(Color(0xFFD32F2F).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "OMITIR",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyDeckView(onReset: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "¡Has completado el catálogo!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Puedes restaurar los fics del catálogo o importar enlaces desde tu biblioteca para escuchar offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recargar Catálogo")
            }
        }
    }
}

// Helpers for color styling
private fun isHighlightColor(category: String): Color {
    return when (category) {
        "Harry Potter" -> Color(0xFF9C27B0)
        "Naruto" -> Color(0xFFFF5722)
        "Percy Jackson" -> Color(0xFF1976D2)
        "Twilight" -> Color(0xFF607D8B)
        else -> Color(0xFFE91E63)
    }
}
