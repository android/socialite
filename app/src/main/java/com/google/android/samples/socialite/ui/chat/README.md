# Chat Module

This directory contains the code for the chat features of the SociaLite application, including the chat list screen and the individual chat screen. This involves:

-   **Chat List UI:** Jetpack Compose composables (e.g., `ChatListScreen.kt`) for displaying the list of chat threads. This screen typically fetches a list of threads from the ViewModel and displays them, allowing users to select a thread to view.
-   **Chat Screen UI:** Jetpack Compose composables (e.g., `ChatScreen.kt`) for displaying messages within a specific chat thread, providing an input field for composing new messages, and handling actions like sending text or attaching media (photos/videos).
-   **ViewModel Logic:** ViewModels (e.g., `ChatListViewModel`, `ChatViewModel`) to manage the UI state for both the chat list and individual chat screens. These ViewModels expose data streams (like messages for a specific thread) and handle user input, interacting with the data layer to perform operations like sending messages or fetching chat history.
-   **Integration with Data Layer:** Code within the ViewModels and potentially other helper classes to interact with the data layer (specifically the repositories) to fetch chat threads, load messages for a thread, and save new messages.

This module is central to the application's core functionality, demonstrating how to build interactive chat interfaces using Compose and manage state with ViewModels.
