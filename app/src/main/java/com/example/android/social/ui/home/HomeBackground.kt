/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.social.ui.home

import android.graphics.Paint
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.drawPolygon
import androidx.graphics.shapes.star
import com.example.android.social.ui.SocialTheme

data class StarSpec(
    val numVertices: Int,
    /** Size in relative to the available width. */
    val size: Float,
    /** 0f: Center, 1f: Outside to the right, 1f: Outside to the left. */
    val offset: Offset,
    val color: Color,
    val blurRadius: Dp,
)

class StarColors(
    val firstColor: Color,
    val secondColor: Color,
    val thirdColor: Color,
) {
    companion object {
        @Composable
        fun defaults() = StarColors(
            firstColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            secondColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            thirdColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        )
    }
}

private fun createStarSpecs(colors: StarColors) = listOf(
    StarSpec(
        numVertices = 8,
        size = 1.5f,
        offset = Offset(-0.2f, 0.2f),
        color = colors.firstColor,
        blurRadius = 8.dp,
    ),
    StarSpec(
        numVertices = 10,
        size = 1.2f,
        offset = Offset(0.2f, 0.2f),
        color = colors.secondColor,
        blurRadius = 24.dp,
    ),
    StarSpec(
        numVertices = 6,
        size = 0.7f,
        offset = Offset(-0.5f, 0.5f),
        color = colors.thirdColor,
        blurRadius = 32.dp,
    ),
    StarSpec(
        numVertices = 6,
        size = 0.7f,
        offset = Offset(0.3f, 0.2f),
        color = colors.thirdColor,
        blurRadius = 40.dp,
    ),
    StarSpec(
        numVertices = 12,
        size = 0.5f,
        offset = Offset(0f, 0.5f),
        color = colors.firstColor,
        blurRadius = 48.dp,
    ),
)

@Composable
fun HomeBackground(
    modifier: Modifier = Modifier,
    colors: StarColors = StarColors.defaults(),
) {
    val paint = remember { Paint() }
    val specs = remember(colors) { createStarSpecs(colors) }
    Box(modifier = modifier) {
        for (spec in specs) {
            Star(
                spec = spec,
                paint = paint,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Star(
    spec: StarSpec,
    paint: Paint,
    modifier: Modifier = Modifier,
) {
    var starCache: RoundedPolygon? by remember { mutableStateOf(null) }
    Canvas(
        modifier = modifier.blur(spec.blurRadius),
    ) {
        val width = size.width
        drawIntoCanvas { canvas ->
            canvas.translate(
                dx = width * spec.offset.x,
                dy = width * spec.offset.y,
            )
            val star = starCache ?: RoundedPolygon.star(
                numVerticesPerRadius = spec.numVertices,
                radius = width * spec.size / 2,
                innerRadius = width * spec.size * 0.7f / 2,
                rounding = CornerRounding(width * 0.05f),
                center = PointF(width / 2, width / 2),
            ).also { starCache = it }
            paint.color = spec.color.toArgb()
            canvas.nativeCanvas.drawPolygon(star, paint)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewHomeBackground() {
    SocialTheme {
        HomeBackground()
    }
}
