@file:Suppress("PrivatePropertyName", "ConstPropertyName")

package com.programmersbox.common

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun App() {
    M3MaterialThemeSetup(isSystemInDarkTheme()) {
        val scope = rememberCoroutineScope()
        val game = remember { GameViewModel(scope) }
        val offsetAnimation = remember(game.offset) { Animatable(game.offset, Offset.VectorConverter) }
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        LaunchedEffect(game.canvasSize) {
            game.reset()
            game.newLocation()
        }

        LaunchedEffect(drawerState) {
            snapshotFlow { drawerState.isOpen }
                .distinctUntilChanged()
                .collect { if (it) game.pause() else game.resume() }
        }

        Surface {
            ModalNavigationDrawer(
                drawerContent = {
                    ModalDrawerSheet {
                        TopAppBar(
                            title = { Text("Settings") },
                            actions = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.close() } }
                                ) { Icon(Icons.Default.Close, null) }
                            }
                        )
                        Divider()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            ListItem(
                                headlineText = { Text("Start Next Round Automatically?") },
                                trailingContent = {
                                    Switch(
                                        game.startAutomatically,
                                        onCheckedChange = { game.startAutomatically = it }
                                    )
                                }
                            )
                        }
                    }
                },
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } }
                                ) { Icon(Icons.Default.Settings, null) }
                            },
                            title = { Text("Masking") },
                            actions = {
                                Text("${animateIntAsState(game.points).value} points")
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            offsetAnimation.animateTo(game.center.toOffset()) {
                                                game.offset = value.copy(
                                                    x = value.x - game.size / 2,
                                                    y = value.y - game.size / 2
                                                )
                                            }
                                        }
                                    }
                                ) { Text("Reset Position") }
                            }
                        )
                    },
                    bottomBar = {
                        BottomAppBar(
                            actions = { Text(game.readableTimer) },
                            floatingActionButton = {
                                OutlinedButton(
                                    onClick = {
                                        game.gainPoints()
                                        game.newLocation()
                                        game.reset()
                                    },
                                    enabled = game.showFound
                                ) { Text("Found!") }
                            }
                        )
                    }
                ) { p ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(p)
                            .background(MaterialTheme.colorScheme.onSurface)
                    ) {
                        val data = updateTransitionData(game.foundText, game.checkTime)
                        Box(
                            Modifier
                                .offset { game.itemOffset.round() }
                                .scale(data.scale)
                                .size(BallSize.dp)
                                .background(data.color, shape = CircleShape)
                        )
                    }

                    ShowBehind(
                        offset = { game.offset },
                        offsetChange = { game.offset += it },
                        sourceDrawing = ShowBehindDefaults.defaultSourceDrawing(game.size, game.offset),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(p)
                            .onGloballyPositioned {
                                game.center = it.size.center
                                game.canvasSize = it.size.toSize()
                            }
                    )
                }
            }
        }
    }
}

private data class TransitionItem(
    val foundText: Boolean,
    val timer: Int
)

@Composable
private fun updateTransitionData(foundText: Boolean, timer: Int): TransitionData {
    val transition = updateTransition(TransitionItem(foundText, timer))
    val color = transition.animateColor { state ->
        if (state.foundText) {
            when (state.timer) {
                in 0..500 -> Alizarin
                in 500..800 -> Sunflower
                in 800..1000 -> Emerald
                else -> Emerald
            }
        } else MaterialTheme.colorScheme.surface
    }
    val size = transition.animateFloat { state -> if (state.foundText) 1f else .75f }
    return remember(transition) { TransitionData(color, size) }
}

private class TransitionData(
    color: State<Color>,
    scale: State<Float>
) {
    val color by color
    val scale by scale
}

private val Emerald = Color(0xFF2ecc71)
private val Sunflower = Color(0xFFf1c40f)
private val Alizarin = Color(0xFFe74c3c)
private const val BallSize = 30

internal class GameViewModel(scope: CoroutineScope) {
    private val stopwatch = Stopwatch(tick = 1L)

    var points by mutableStateOf(0)

    var showFound by mutableStateOf(false)

    var center by mutableStateOf(IntOffset.Zero)
    var canvasSize by mutableStateOf(Size.Zero)
    var startAutomatically by mutableStateOf(false)

    val size by mutableStateOf(250f)
    var offset by mutableStateOf(Offset.Zero)

    var itemOffset by mutableStateOf(Offset.Zero)
    val foundText by derivedStateOf {
        itemOffset.x in offset.x..(offset.x + size) &&
                itemOffset.y in offset.y..(offset.y + size)
    }

    var checkTime by mutableStateOf(0)
    var timer by mutableStateOf(0.0)
    val readableTimer by derivedStateOf { LocalTime.fromMillisecondOfDay(timer.roundToInt()).toString() }

    init {
        stopwatch.time
            .onEach {
                if (foundText) checkTime += 1
                else checkTime = 0
            }
            .launchIn(scope)

        stopwatch.time
            .onEach { timer += if (foundText) .5 else 1.0 }
            .launchIn(scope)

        stopwatch.start()

        snapshotFlow { itemOffset }
            .distinctUntilChanged()
            .onEach { showFound = false }
            .launchIn(scope)

        snapshotFlow { foundText && checkTime > 1000 }
            .filter { it }
            .onEach {
                pause()
                showFound = true
                if (startAutomatically) {
                    delay(500)
                    gainPoints()
                    newLocation()
                    reset()
                }
            }
            .launchIn(scope)
    }

    fun reset() {
        checkTime = 0
        timer = 0.0
        stopwatch.reset()
        stopwatch.start()
    }

    fun newLocation() {
        val x = Random.nextInt(0, (canvasSize.width - BallSize).roundToInt().coerceAtLeast(1))
        val y = Random.nextInt(0, (canvasSize.height - BallSize).roundToInt().coerceAtLeast(1))
        itemOffset = Offset(x.toFloat(), y.toFloat())
    }

    fun gainPoints() {
        val pointsGained = itemOffset.getDistance() - timer
        points += pointsGained.absoluteValue.roundToInt()
    }

    fun pause() {
        stopwatch.pause()
    }

    fun resume() {
        stopwatch.start()
    }
}