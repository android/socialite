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

package com.google.android.samples.socialite.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.google.android.samples.socialite.ui.SocialTheme

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
    val specs = remember(colors) { createStarSpecs(colors) }
    Box(modifier = modifier) {
        for (spec in specs) {
            Star(
                spec = spec,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Star(
    spec: StarSpec,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.blur(spec.blurRadius)
            .drawWithCache {
                val width = size.width
                val star = RoundedPolygon.star(
                    numVerticesPerRadius = spec.numVertices,
                    radius = width * spec.size / 2,
                    innerRadius = width * spec.size * 0.7f / 2,
                    rounding = CornerRounding(width * 0.05f),
                    centerX = width / 2,
                    centerY = width / 2,
                )
                val path = star.toPath().asComposePath()

                onDrawBehind {
                    translate(
                        width * spec.offset.x,
                        width * spec.offset.y,
                    ) {
                        drawPath(path, color = spec.color)
                    }
                }
            },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewHomeBackground() {
    SocialTheme {
        HomeBackground()
    }
}
