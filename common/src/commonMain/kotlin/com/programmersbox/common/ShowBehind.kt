package com.programmersbox.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput

internal object ShowBehindDefaults {
    fun defaultSourceDrawing(size: Float, offset: Offset): DrawScope.(color: Color, blendMode: BlendMode) -> Unit =
        { color, blendMode ->
            drawCircle(
                color = color,
                radius = size / 2f,
                center = offset + Offset(size / 2f, size / 2f),
                blendMode = blendMode
            )
        }

    const val defaultSize = 100f
}

@Composable
internal fun ShowBehind(
    offsetChange: (Offset) -> Unit,
    offset: () -> Offset = { Offset.Zero },
    modifier: Modifier = Modifier.fillMaxSize(),
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    sourceDrawing: DrawScope.(color: Color, blendMode: BlendMode) -> Unit = ShowBehindDefaults
        .defaultSourceDrawing(ShowBehindDefaults.defaultSize, offset()),
) {
    Canvas(
        modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel
            ) { change, dragAmount ->
                change.consume()
                offsetChange(dragAmount)
            }
        }
    ) {
        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)

            // Destination
            drawRect(surfaceColor)

            // Source
            sourceDrawing(Color.Transparent, BlendMode.DstIn)

            restoreToCount(checkPoint)
        }
    }
}