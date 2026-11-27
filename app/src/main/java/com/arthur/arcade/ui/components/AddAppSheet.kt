package com.arthur.arcade.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arthur.arcade.data.AppListViewModel
import com.arthur.arcade.data.SettingsRepository
import com.arthur.arcade.util.highlightMatchingText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddAppSheet(viewModel: AppListViewModel, closeSheet: () -> Unit, updateGames: () -> Unit) {
	val context = LocalContext.current

	val apps by viewModel.appsState
	val isLoading by viewModel.isLoading

	val textFieldState = rememberTextFieldState()

	val filteredApps = remember(apps, textFieldState.text) {
		if (textFieldState.text.isBlank()) {
			apps
		} else {
			apps.filter {
				it.name.contains(textFieldState.text, ignoreCase = true)
			}
		}
	}

	ModalBottomSheet(onDismissRequest = closeSheet) {
		if (isLoading) {
			Box(
				Modifier
					.fillMaxWidth()
					.height(200.dp), contentAlignment = Alignment.Center
			) {
				LoadingIndicator()
			}
		} else {
			Box(
				Modifier
					.fillMaxSize()
			) {
				Column(
					Modifier
						.fillMaxWidth()
						.padding(horizontal = 24.dp)
						.padding(bottom = 24.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Text(
						"Add app",
						style = MaterialTheme.typography.headlineMedium
					)

					TextField(
						state = textFieldState,
						modifier = Modifier
							.fillMaxWidth()
							.padding(vertical = 8.dp),
						lineLimits = TextFieldLineLimits.SingleLine,
						placeholder = { Text("Search") },
						leadingIcon = {
							Icon(Icons.Default.Search, contentDescription = "Search")
						},
						trailingIcon = {
							if (textFieldState.text.isNotEmpty()) {
								IconButton(onClick = { textFieldState.clearText() }) {
									Icon(Icons.Default.Clear, contentDescription = "Clear search")
								}
							}
						},
						shape = CircleShape,
						colors = TextFieldDefaults.colors(
							focusedIndicatorColor = Color.Transparent,
							unfocusedIndicatorColor = Color.Transparent,
							disabledIndicatorColor = Color.Transparent
						)
					)
					LazyColumn {
						items(filteredApps, key = { it.info.packageName }) { app ->
							Row(
								modifier = Modifier.fillMaxWidth(),
								verticalAlignment = Alignment.CenterVertically,
								horizontalArrangement = Arrangement.spacedBy(4.dp)
							) {
								var appIcon by remember(app.info.packageName) {
									mutableStateOf<Drawable?>(null)
								}

								LaunchedEffect(app.info.packageName) {
									withContext(Dispatchers.IO) {
										try {
											appIcon = context.packageManager
												.getApplicationIcon(app.info.packageName)
										} catch (_: Exception) {
										}
									}
								}

								AsyncImage(
									model = appIcon,
									contentDescription = "App Icon",
									modifier = Modifier
										.size(40.dp)
										.clip(RoundedCornerShape(16.dp))
								)

								Text(
									text = highlightMatchingText(
										text = app.name,
										query = textFieldState.text.toString(),
									)
								)

								Spacer(Modifier.weight(1f))

								Checkbox(
									checked = app.isChecked,
									onCheckedChange = {
										viewModel.toggleAppChecked(app.info.packageName)
									}
								)
							}
						}
						item {
							Spacer(Modifier.height(88.dp))
						}
					}
				}

				FloatingActionButton(
					onClick = {
						viewModel.appsState.value
							.filter { it.isChecked }
							.map { it.info.packageName }
							.forEach {
								SettingsRepository.addNonGameApps(
									context,
									it
								)
							}
						updateGames()
						closeSheet()
					},
					modifier = Modifier
						.align(Alignment.BottomEnd)
						.navigationBarsPadding()
						.padding(32.dp)
				) {
					Icon(
						imageVector = Icons.Default.Done,
						contentDescription = "Done",
						tint = MaterialTheme.colorScheme.onPrimaryContainer
					)
				}
			}
		}
	}
}