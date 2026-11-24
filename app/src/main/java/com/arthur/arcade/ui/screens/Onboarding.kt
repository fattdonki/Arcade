package com.arthur.arcade.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arthur.arcade.MainActivity
import com.arthur.arcade.R
import com.arthur.arcade.ui.components.REQUEST_CODE
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

@Composable
fun Onboarding(context: Context, modifier: Modifier = Modifier, onContinue: () -> Unit) {
	val shizuku = remember { Shizuku.pingBinder() }
	val pagerState = rememberPagerState(pageCount = {
		if (shizuku) 3 else 4
	})
	val coroutineScope = rememberCoroutineScope()

	fun next() {
		val nextPage = (pagerState.currentPage + 1)
		if (nextPage < pagerState.pageCount) {
			coroutineScope.launch {
				pagerState.animateScrollToPage(nextPage)
			}
		} else {
			onContinue()
		}
	}

	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(16.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = "Welcome",
			style = MaterialTheme.typography.displayMedium,
			fontWeight = FontWeight.Black
		)

		Spacer(modifier = Modifier.height(12.dp))
		OnboardingPagerIndicator(pagerState)

		HorizontalPager(
			state = pagerState,
		) { pageIndex ->

			when (pageIndex) {
				0 -> {
					Box {
						Column(
							modifier = modifier
								.fillMaxSize()
								.padding(12.dp),
							horizontalAlignment = Alignment.CenterHorizontally
						) {
							Text(
								"Arcade lets you choose what happens when you launch a game, like turning on DND, " +
										"and blocking a game's internet access. To configure this for each game, access its " +
										"settings by sliding the game card to the right. Try it here:"
							)

							Spacer(modifier = Modifier.height(24.dp))


							val posThreshold = 0.5f
							val velThreshold = 1500f
							var swipeSuccessful by remember { mutableStateOf(false) }

							val rotation = remember { Animatable(0f) }
							var rowWidthPx by remember { mutableFloatStateOf(1f) }
							val offsetAnim = remember { Animatable(0f) }
							val scope = rememberCoroutineScope()

							val fraction = (offsetAnim.value / rowWidthPx).coerceIn(0f, 1f)
							val density = LocalDensity.current.density
							val cornerDp = (rowWidthPx / 2f / density).coerceAtMost(75f)


							LaunchedEffect(offsetAnim.value > 0f) {
								if (offsetAnim.value > 0f) {
									rotation.snapTo(0f)
									rotation.animateTo(
										360f,
										animationSpec = tween(
											durationMillis = 400,
											easing = FastOutSlowInEasing
										)
									)
								}
							}

							Box(
								modifier = Modifier
									.fillMaxWidth()
									.onSizeChanged { rowWidthPx = it.width.toFloat() }
									.draggable(
										orientation = Orientation.Horizontal,
										state = rememberDraggableState { delta ->
											scope.launch {
												offsetAnim.snapTo(
													(offsetAnim.value + delta).coerceAtLeast(
														0f
													)
												)
											}
										},
										onDragStopped = { velocity ->
											scope.launch {
												val shouldOpen =
													fraction >= posThreshold || velocity >= velThreshold

												if (shouldOpen) {
													val remaining =
														rowWidthPx - offsetAnim.value
													val durationToEnd =
														(remaining / (velocity.coerceAtLeast(
															800f
														) * 0.8f) * 1000).toInt()
															.coerceIn(80, 220)

													offsetAnim.animateTo(
														rowWidthPx,
														tween(
															durationToEnd,
															easing = LinearOutSlowInEasing
														)
													)

													swipeSuccessful = true

													offsetAnim.animateTo(
														0f,
														tween(220, easing = FastOutSlowInEasing)
													)
												} else {
													offsetAnim.animateTo(0f, tween(180))
												}
											}
										}
									)
							) {
								Box(
									Modifier
										.matchParentSize()
										.padding(vertical = 2.dp),
									contentAlignment = Alignment.CenterStart
								) {
									Box(
										Modifier
											.fillMaxHeight()
											.fillMaxWidth(fraction.coerceAtLeast(0f))
											.clip(RoundedCornerShape(cornerDp.coerceAtLeast(4f).dp))
											.background(MaterialTheme.colorScheme.tertiaryContainer)
									)
									Icon(
										imageVector = Icons.Default.Settings,
										contentDescription = "Settings",
										modifier = Modifier
											.offset {
												IntOffset(
													((offsetAnim.value / 2f).coerceAtLeast(35f * density) - 12f * density).toInt(),
													0
												)
											}
											.rotate(rotation.value)
											.requiredSize(24.dp),
										tint = MaterialTheme.colorScheme.onTertiaryContainer
									)
								}

								Card(
									modifier = Modifier
										.fillMaxWidth()
										.offset { IntOffset(offsetAnim.value.toInt(), 0) }
										.clickable {
											Toast.makeText(
												context,
												"Tapping the card opens the game. Slide right to open settings.",
												Toast.LENGTH_SHORT
											).show()
										},
									shape = RoundedCornerShape(4.dp),
									colors = CardDefaults.cardColors(
										containerColor = MaterialTheme.colorScheme.secondaryContainer,
										contentColor = MaterialTheme.colorScheme.onSecondaryContainer
									)
								) {
									Row(
										modifier = Modifier
											.fillMaxWidth()
											.padding(16.dp),
										verticalAlignment = Alignment.CenterVertically
									) {
										AsyncImage(
											model = R.mipmap.ic_launcher_round,
											contentDescription = null,
											modifier = Modifier
												.size(56.dp)
												.clip(RoundedCornerShape(16.dp))
										)
										Spacer(modifier = Modifier.width(16.dp))
										Text(
											text = "Game",
											style = MaterialTheme.typography.titleMedium,
											modifier = Modifier.weight(1f)
										)
									}
								}
							}
						}

						FloatingActionButton(
							onClick = {
								next()
							},
							modifier = Modifier
								.align(Alignment.BottomEnd)
								.navigationBarsPadding()
								.padding(32.dp)
						) {
							Icon(
								imageVector = Icons.AutoMirrored.Filled.ArrowForward,
								contentDescription = "Next",
								tint = MaterialTheme.colorScheme.onPrimaryContainer
							)
						}
					}
				}
				1 -> {
					Column(
						modifier = modifier
							.fillMaxSize()
							.padding(16.dp),
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						Icon(
							imageVector = Icons.Default.DoNotDisturbOn,
							contentDescription = "DND",
							modifier = Modifier.size(48.dp)
						)
						Spacer(modifier = Modifier.height(24.dp))
						Text(
							"For Arcade to automatically enable Do Not Disturb, " +
									"you need to grant permission to access notification " +
									"settings."
						)

						Spacer(modifier = Modifier.weight(1f))

						ConnectedPermissionButtonGroup(
							onGrantClick = {
								val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
								if (context is MainActivity) {
									context.dndPermissionLauncher.launch(intent)
								}
								next()
							},
							onDismissClick = {
								next()
							}
						)
					}
				}
				2 -> {
					if (shizuku) {
						Column(
							modifier = modifier
								.fillMaxSize()
								.padding(16.dp),
							horizontalAlignment = Alignment.CenterHorizontally
						) {
							Icon(
								imageVector = Icons.Default.Adb,
								contentDescription = "Shizuku",
								modifier = Modifier.size(48.dp)
							)
							Spacer(modifier = Modifier.height(24.dp))
							Text("Arcade supports a Shizuku powered firewall for the best performance.")

							Spacer(modifier = Modifier.weight(1f))

							ConnectedPermissionButtonGroup(
								onGrantClick = {
									if (context is MainActivity) {
										Shizuku.requestPermission(REQUEST_CODE)
									}
									next()
								},
								onDismissClick = {
									next()
								}
							)
						}
					} else {
						Column(
							modifier = modifier
								.fillMaxSize()
								.padding(16.dp),
							horizontalAlignment = Alignment.CenterHorizontally
						) {
							Icon(
								imageVector = Icons.Default.VpnKey,
								contentDescription = "VPN",
								modifier = Modifier.size(48.dp)
							)
							Spacer(modifier = Modifier.height(24.dp))
							Text(
								"For Arcade to block internet access per game, you need to " +
									"grant it VPN permission. However, if you want to have " +
									"minimal overhead it is recommended that you use Shizuku " +
									"instead. You can always come back and set it up later in " +
									"Settings."
							)

							Spacer(modifier = Modifier.weight(1f))

							ConnectedPermissionButtonGroup(
								onGrantClick = {
									if (context is MainActivity) {
										val intent = VpnService.prepare(context)
										if (intent != null) {
											context.vpnPermissionLauncher.launch(intent)
										}
									}
									next()
								},
								onDismissClick = {
									next()
								}
							)
						}
					}
				}
				3 -> {
					Column(
						modifier = modifier
							.fillMaxSize()
							.padding(16.dp),
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						Icon(
							imageVector = Icons.Default.Notifications,
							contentDescription = "Notification",
							modifier = Modifier.size(48.dp)
						)
						Spacer(modifier = Modifier.height(24.dp))
						Text(
							"Arcade uses notifications to allow you to manually turn off the " +
									"Arcade VPN."
						)

						Spacer(modifier = Modifier.weight(1f))

						ConnectedPermissionButtonGroup(
							onGrantClick = {
								if (context is MainActivity) {
									context.notificationPermissionLauncher
										.launch(Manifest.permission.POST_NOTIFICATIONS)
								}
								next()
							},
							onDismissClick = {
								next()
							}
						)
					}
				}
			}
		}
	}
}

@Composable
fun OnboardingPagerIndicator(
	pagerState: PagerState,
) {
	Row(
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		repeat(pagerState.pageCount) { iteration ->
			val isSelected = pagerState.currentPage == iteration

			val width by animateDpAsState(
				targetValue = if (isSelected) 24.dp else 8.dp,
				label = "IndicatorWidth"
			)
			val color by animateColorAsState(
				targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
				label = "IndicatorColor"
			)

			Box(
				modifier = Modifier
					.height(8.dp)
					.width(width)
					.clip(CircleShape)
					.background(color)
			)
		}
	}
}

@Composable
fun ConnectedPermissionButtonGroup(
	onGrantClick: () -> Unit,
	onDismissClick: () -> Unit,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
	) {
		FilledTonalButton(
			onClick = onDismissClick,
			modifier = Modifier
				.weight(1f)
				.height(100.dp),
			shape = ButtonGroupDefaults.connectedLeadingButtonShape
		) {
			Text("No thanks", style = MaterialTheme.typography.titleMedium)
		}

		Button(
			onClick = onGrantClick,
			modifier = Modifier
				.weight(1f)
				.height(100.dp),
			shape = ButtonGroupDefaults.connectedTrailingButtonShape
		) {
			Text("Grant", style = MaterialTheme.typography.titleMedium)
		}
	}
}