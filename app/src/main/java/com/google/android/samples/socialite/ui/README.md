# UI Layer

This directory contains the user interface (UI) layer of the SociaLite application. It is responsible for everything the user sees and interacts with, built using Jetpack Compose.

-   **Screens:** Each major screen or feature of the app typically has its own subdirectory within `ui/`, such as `camera/`, `chat/`, `settings/`, `timeline/`, and `videoedit/`. These subdirectories contain the Composables and ViewModels specific to that screen.
-   **Composables:** Jetpack Compose functions that define the individual UI elements and screen layouts. Composables are declarative and describe the UI based on the current state.
-   **ViewModels:** Classes that hold UI state and expose data streams, interacting with the data layer to provide the data needed by the UI. ViewModels survive configuration changes and are central to managing UI-related logic.

This layer strictly follows the Model-View-ViewModel (MVVM) architectural pattern. The Composables act as the View, observing state provided by the ViewModels, which in turn interact with the data layer to retrieve and update data. This separation of concerns improves testability and maintainability.
