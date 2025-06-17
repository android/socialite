# Video Edit Module

This directory contains the code for the video editing screen of the SociaLite application. After capturing a video with the in-app camera, users can perform minor edits on this screen before sharing. This screen leverages [Media3 Transformer](https://developer.android.com/media/media3/transformer) and [CompositionPlayer](https://github.com/androidx/media/blob/release/libraries/transformer/src/main/java/androidx/media3/transformer/CompositionPlayer.java).

-   **Video Edit UI:** Jetpack Compose composables (e.g., `VideoEditScreen.kt`) for displaying the video player, a timeline or scrubber for seeking, and controls for basic editing operations like adding overlays or applying filters.
-   **ViewModel Logic:** A ViewModel (e.g., `VideoEditViewModel`) to manage the state and logic for the video editing screen. This ViewModel holds the state of the video playback, the editing parameters, and handles user interactions with the editing controls.

This module demonstrates how to integrate video playback and basic editing capabilities within the application.
