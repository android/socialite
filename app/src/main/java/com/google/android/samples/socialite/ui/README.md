# UI Layer

This directory contains the user interface (UI) layer of the SociaLite application. It is responsible for everything the user sees and interacts with.

-   **Screens:** Each major screen or feature of the app typically has its own subdirectory within `ui/`.
-   **Composables:** Jetpack Compose functions that define the UI elements.
-   **ViewModels:** Classes that hold UI state and expose data streams, interacting with the data layer.

This layer follows the Model-View-ViewModel (MVVM) architectural pattern, with Composables acting as the View and separate ViewModel classes managing the UI state and logic.
