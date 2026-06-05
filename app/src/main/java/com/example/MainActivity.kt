package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FicSwipeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: FicSwipeViewModel = viewModel()
                val currentTab by viewModel.currentTab.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == "discover",
                                onClick = { viewModel.selectTab("discover") },
                                label = { Text("Descubrir") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Descubrir Fanfics"
                                    )
                                }
                            )

                            NavigationBarItem(
                                selected = currentTab == "library",
                                onClick = { viewModel.selectTab("library") },
                                label = { Text("Biblioteca") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.LibraryBooks,
                                        contentDescription = "Biblioteca Offline"
                                    )
                                }
                            )

                            NavigationBarItem(
                                selected = currentTab == "reader",
                                onClick = { viewModel.selectTab("reader") },
                                label = { Text("Lector") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "Modo Lectura"
                                    )
                                }
                            )

                            NavigationBarItem(
                                selected = currentTab == "browser",
                                onClick = { viewModel.selectTab("browser") },
                                label = { Text("Navegador") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Fusión Web Fanfiction.net"
                                    )
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (currentTab) {
                        "discover" -> DiscoverScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        "library" -> LibraryScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        "reader" -> ReaderScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        "browser" -> BrowserScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
