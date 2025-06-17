# Timeline Module

This directory contains the code for the timeline screen of the SociaLite application. This screen displays a vertical feed of all photos and videos shared across all chat threads. This screen uses `PreloadManagerWrapper.kt` which builds on ExoPlayer's [DefaultPreloadManager](https://developer.android.com/reference/androidx/media3/exoplayer/source/preload/DefaultPreloadManager) to enable smooth vertical scrolling between media items.

-   **Timeline UI:** Jetpack Compose composables (e.g., `TimelineScreen.kt`) for displaying the list of media items (photos and videos) in a scrollable feed. This involves displaying the media content and potentially some associated information like the sender or timestamp.
-   **ViewModel Logic:** A ViewModel (e.g., `TimelineViewModel`) to manage the state and logic for the timeline screen. This ViewModel is responsible for fetching the list of media items from the data layer and exposing it to the UI.
-   **Integration with Data Layer:** Code within the ViewModel to interact with the data layer (specifically the repositories) to retrieve all shared media content from the database.

This module demonstrates how to display a collection of media items fetched from a local data source.
