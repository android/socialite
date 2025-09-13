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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.samples.socialite.ui.SocialTheme

@Composable
fun MetadataRow(item: MetadataItem) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(
            onClick = { showDialog = true },
            modifier = Modifier.size(30.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info button",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
            )
        }

        Text(
            text = item.key,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.weight(0.8f),
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = item.value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.weight(1.2f),
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    item.key,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                    ),
                )
            },
            text = {
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 18.sp,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(
                        "OK",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                        ),
                    )
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MetadataRowPreview() {
    SocialTheme {
        MetadataRow(
            item = MetadataItem(
                key = "Bitrate",
                value = "128 kbps",
                description = "The number of bits processed per unit of time.",
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AlertDialogPreview() {
    SocialTheme {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    "Bitrate",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                    ),
                )
            },
            text = {
                Text(
                    "The number of bits processed per unit of time.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 18.sp,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {}) {
                    Text(
                        "OK",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                        ),
                    )
                }
            },
        )
    }
}
