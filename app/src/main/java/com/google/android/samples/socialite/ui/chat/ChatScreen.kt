// [Previous imports remain the same]

@Composable
fun ChatScreen(
    chatId: Long,
    foreground: Boolean,
    modifier: Modifier = Modifier,
    onBackPressed: (() -> Unit)?,
    onCameraClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onVideoClick: (uri: String) -> Unit,
    prefilledText: String? = null,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    LaunchedEffect(chatId) {
        viewModel.setChatId(chatId)
        if (prefilledText != null) {
            viewModel.prefillInput(prefilledText)
        }
    }
    val chat by viewModel.chat.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val sendEnabled by viewModel.sendEnabled.collectAsStateWithLifecycle()
    
    chat?.let { c ->
        ChatContent(
            chat = c,
            messages = messages,
            input = input,
            sendEnabled = sendEnabled,
            onBackPressed = onBackPressed,
            onInputChanged = { viewModel.updateInput(it) },
            onSendClick = { viewModel.send() },
            onCameraClick = onCameraClick,
            onPhotoPickerClick = onPhotoPickerClick,
            onVideoClick = onVideoClick,
            modifier = modifier
                .clip(RoundedCornerShape(5)),
        )
    }
    
    LaunchedEffect(messages) {
        messages.firstOrNull()?.let { message ->
            if (!message.isIncoming) {
                announceForAccessibility(
                    stringResource(
                        R.string.new_message,
                        message.senderName ?: "",
                        message.text
                    )
                )
            }
        }
    }
}

@Composable
private fun SmallContactIcon(iconUri: Uri, size: Dp, contactName: String) {
    Image(
        painter = rememberIconPainter(contentUri = iconUri),
        contentDescription = stringResource(R.string.contact_icon, contactName),
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.LightGray),
    )
}

@Composable
private fun InputBar(
    input: String,
    contentPadding: PaddingValues,
    sendEnabled: Boolean,
    onInputChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onCameraClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(contentPadding)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = onCameraClick,
                modifier = Modifier.semantics { 
                    contentDescription = stringResource(R.string.take_photo)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            
            IconButton(
                onClick = onPhotoPickerClick,
                modifier = Modifier.semantics {
                    contentDescription = stringResource(R.string.select_media)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            
            TextField(
                value = input,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics {
                        contentDescription = stringResource(R.string.message_input)
                    },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                placeholder = { Text(stringResource(R.string.message)) },
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
            
            FilledIconButton(
                onClick = onSendClick,
                modifier = Modifier
                    .size(56.dp)
                    .semantics {
                        contentDescription = stringResource(R.string.send_message)
                    },
                enabled = sendEnabled,
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onVideoClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier.semantics {
            liveRegion = LiveRegion.Polite
        },
        color = if (message.isIncoming) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        },
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(text = message.text)
            if (message.mediaUri != null) {
                val mimeType = message.mediaMimeType
                if (mimeType != null) {
                    if (mimeType.contains("image")) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(message.mediaUri)
                                .build(),
                            contentDescription = stringResource(R.string.message_image),
                            modifier = Modifier
                                .height(250.dp)
                                .padding(10.dp),
                        )
                    } else if (mimeType.contains("video")) {
                        VideoMessagePreview(
                            videoUri = message.mediaUri,
                            onClick = onVideoClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoMessagePreview(videoUri: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(10.dp)
            .semantics {
                contentDescription = stringResource(R.string.play_video)
            },
    ) {
        // Rest of implementation remains same
        Icon(
            Icons.Filled.PlayArrow,
            tint = Color.White,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .align(Alignment.Center)
                .border(3.dp, Color.White, shape = CircleShape),
        )
    }
}