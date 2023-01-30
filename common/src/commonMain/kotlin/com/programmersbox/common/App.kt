package com.programmersbox.common

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun App() {
    M3MaterialThemeSetup(isSystemInDarkTheme()) {
        var offset by remember { mutableStateOf(Offset.Zero) }
        val offsetAnimation = remember(offset) { Animatable(offset, Offset.VectorConverter) }
        val size by remember { mutableStateOf(250f) }
        val scope = rememberCoroutineScope()
        var center by remember { mutableStateOf(IntOffset.Zero) }
        var canvasSize by remember { mutableStateOf(Size.Zero) }
        var itemOffset by remember { mutableStateOf(Offset.Zero) }
        val foundText by remember {
            derivedStateOf {
                itemOffset.x in offset.x..(offset.x + size) &&
                        itemOffset.y in offset.y..(offset.y + size)
            }
        }
        var points by remember { mutableStateOf(0.0) }
        var showFound by remember(itemOffset) { mutableStateOf(false) }
        var timer by timer(!showFound, foundText)

        val readableTimer by remember {
            derivedStateOf { LocalTime.fromMillisecondOfDay(timer.roundToInt()).toString() }
        }

        LaunchedEffect(canvasSize) {
            val x = Random.nextInt(0, canvasSize.width.roundToInt().coerceAtLeast(1))
            val y = Random.nextInt(0, canvasSize.height.roundToInt().coerceAtLeast(1))
            itemOffset = Offset(x.toFloat(), y.toFloat())
        }

        LaunchedEffect(offset, foundText) {
            if (foundText) {
                delay(1000)
                println("FOUND IT!")
                showFound = true
            }
        }

        Surface {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Masking") },
                        actions = {
                            Text("${animateIntAsState(points.roundToInt()).value} points")
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        offsetAnimation.animateTo(center.toOffset()) {
                                            offset = value.copy(
                                                x = value.x - size / 2,
                                                y = value.y - size / 2
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
                        actions = { Text(readableTimer) },
                        floatingActionButton = {
                            OutlinedButton(
                                onClick = {
                                    val pointsGained = itemOffset.getDistance() - timer
                                    points += pointsGained.absoluteValue
                                    val x = Random.nextInt(0, canvasSize.width.roundToInt())
                                    val y = Random.nextInt(0, canvasSize.height.roundToInt())
                                    itemOffset = Offset(x.toFloat(), y.toFloat())
                                    showFound = false
                                    timer = 0.0
                                },
                                enabled = showFound
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
                    val checkTime by checkTime(foundText, showFound)
                    val data = updateTransitionData(foundText, checkTime)
                    Box(
                        Modifier
                            .offset { itemOffset.round() }
                            .scale(data.scale)
                            .size(30.dp)
                            .background(data.color, shape = CircleShape)
                    )
                }

                ShowBehind(
                    offset = { offset },
                    offsetChange = { offset += it },
                    sourceDrawing = ShowBehindDefaults.defaultSourceDrawing(size, offset),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(p)
                        .onGloballyPositioned {
                            center = it.size.center
                            canvasSize = it.size.toSize()
                        }
                )
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

@Composable
internal fun timer(runTimer: Boolean, foundText: Boolean): MutableState<Double> {
    val counter = remember { mutableStateOf(0.0) }

    LaunchedEffect(runTimer, foundText) {
        while (runTimer) {
            delay(1)
            counter.value += if (foundText) .5 else 1.0
        }
    }

    return counter
}

@Composable
internal fun checkTime(start: Boolean, isFound: Boolean) = produceState(0, start) {
    if (!isFound) {
        value = 0
        while (start) {
            delay(1)
            value += 1
        }
    }
}

private val Emerald = Color(0xFF2ecc71)
private val Sunflower = Color(0xFFf1c40f)
private val Alizarin = Color(0xFFe74c3c)