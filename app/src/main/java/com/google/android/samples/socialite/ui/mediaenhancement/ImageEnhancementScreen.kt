package com.google.android.samples.socialite.ui.mediaenhancement

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.media.effect.enhancement.EnhancementMode
import java.text.DecimalFormat

@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEnhancementScreen(
    chatId: Long,
    messageId: Long,
    uri: String,
    onCloseButtonClicked: () -> Unit,
    onFinishEditing: () -> Unit,
    enhancementViewModel: EnhancementViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by enhancementViewModel.uiState.collectAsState()

    LaunchedEffect(uri) {
        enhancementViewModel.setEnhancementMode(EnhancementMode.BITMAP)
        enhancementViewModel.onImageSelected(Uri.parse(uri), context)
    }

    val processingOptions = listOf("Tonemap", "Deblur & DeNoise", "Upscale")

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Module Status: ${uiState.moduleStatus}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(processingOptions) { option ->
                        FilterChip(
                            selected = option in uiState.selectedOptions,
                            onClick = { enhancementViewModel.onOptionSelected(option) },
                            label = { Text(option) }
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Button(
                    onClick = { enhancementViewModel.enhanceImage() },
                    enabled = uiState.isModuleReady && !uiState.isLoading && uiState.originalImage?.bitmap != null
                ) {
                    val buttonText = when {
                        uiState.isModuleInstalling -> "Installing..."
                        uiState.isLoading -> "Enhancing..."
                        else -> "AI Enhance"
                    }
                    Text(buttonText)
                }
            }

            if (uiState.isModuleInstalling) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { uiState.moduleInstallProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Downloading Enhancement module (${uiState.moduleInstallProgress}%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (uiState.moduleInstallError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.moduleInstallError ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (uiState.enhancementError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.enhancementError ?: "Unknown enhancement error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancementImageCard(
                    title = "Original",
                    imageInfo = uiState.originalImage,
                    modifier = Modifier.weight(1f)
                )
                EnhancementImageCard(
                    title = "Enhanced",
                    imageInfo = uiState.enhancedImage,
                    isLoading = uiState.isLoading,
                    error = uiState.enhancementError,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onCloseButtonClicked,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        enhancementViewModel.saveEnhancedImage(messageId, onFinishEditing)
                    },
                    enabled = uiState.enhancedImage?.bitmap != null,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

@Composable
fun EnhancementImageCard(
    title: String,
    imageInfo: ImageInfo?,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    error: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
            }
            error != null -> {
                Text(
                    text = "Enhancement Failed",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            imageInfo?.bitmap != null -> {
                Image(
                    bitmap = imageInfo.bitmap.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                InfoTag(
                    title = title,
                    latency = imageInfo.latency,
                    bitmap = imageInfo.bitmap,
                    quality = imageInfo.qualityScore,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
            else -> {
                Text(
                    text = if (title == "Original") "Loading..." else "Enhanced Image",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatMemorySize(bytes: Int): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val decimalFormat = DecimalFormat("#.##")
    return when {
        mb >= 1 -> "${decimalFormat.format(mb)} MB"
        else -> "${decimalFormat.format(kb)} KB"
    }
}

@Composable
private fun InfoTag(
    title: String,
    latency: Long?,
    bitmap: Bitmap?,
    quality: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(4.dp)
            .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        if (bitmap != null) {
            val memorySize = formatMemorySize(bitmap.byteCount)
            Text(
                text = "$memorySize, ${bitmap.width}x${bitmap.height}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (latency != null && title != "Original") {
            Text(
                text = "AI Latency: ${latency}ms",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (quality != null) {
            Text(
                text = "Quality: $quality/100",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
