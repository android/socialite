# Settings Module

This directory contains the code for the settings screen of the SociaLite application. This includes:

-   **Settings UI:** Jetpack Compose composables (e.g., `SettingsScreen.kt`) for displaying various settings options, such as the option to reset chat history or potentially other application preferences.
-   **ViewModel Logic:** A ViewModel (e.g., `SettingsViewModel`) to manage the state and logic for the settings screen. This ViewModel handles user interactions with the settings UI and triggers the necessary actions based on user selections.
-   **Settings Logic:** Code for handling user interactions with settings and applying changes. This might involve clearing data in the database (e.g., resetting chat history by interacting with the data layer) or updating application preferences.

This module provides a simple interface for users to configure certain aspects of the application.
