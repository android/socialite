/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.google.android.samples.socialite.ui.metadata.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.samples.socialite.R

data class MetadataItem(
    val key: String,
    val value: String,
    val description: String,
)

@Composable
fun MetadataCard(
    iconId: Int,
    title: String,
    items: List<MetadataItem>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 30.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            ) {
                Row(
                    modifier = Modifier.padding(all = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = iconId),
                        contentDescription = null,
                        modifier = Modifier.size(35.dp),
                    )
                    Spacer(modifier = Modifier.width(15.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            items.forEach { item ->
                MetadataRow(item)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Preview(showBackground = true)
@Composable
fun MediaMetadataCardPreview() {
    val metadataItems = listOf(
        MetadataItem(
            stringResource(R.string.key_codec),
            stringResource(R.string.value_codec),
            stringResource(R.string.desc_codec),
        ),
        MetadataItem(
            stringResource(R.string.key_resolution),
            stringResource(R.string.value_resolution),
            stringResource(R.string.desc_resolution),
        ),
        MetadataItem(
            stringResource(R.string.key_bitrate),
            stringResource(R.string.value_bitrate),
            stringResource(R.string.desc_bitrate),
        ),
    )
    MetadataCard(
        iconId = R.drawable.box,
        title = stringResource(R.string.metadata_card_title),
        items = metadataItems,
    )
}
