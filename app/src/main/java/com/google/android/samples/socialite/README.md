# SociaLite Application Source Code

This directory represents the root package (`com.google.android.samples.socialite`) for the main application source code of SociaLite. It contains the core components and features of the application, organized into subdirectories based on their function or layer within the app's architecture, following recommended Android development practices.

Key subdirectories include:

-   `data/`: Contains the **data layer**, responsible for handling data sources (like the Room database) and repositories that provide data to the rest of the application. This layer abstracts the origin of the data, making the application less dependent on specific data technologies.
-   `model/`: Houses the **data models and entities** used throughout the application. These are simple classes defining the structure of the data, such as `Message` and `ChatThread`. They represent the core data structures of the application.
-   `repository/`: Contains logic for persisting data, leveraging resources from the data layer to do so. Also handles sending notifications when messages are sent.
-   `ui/`: Represents the **user interface layer**, built using Jetpack Compose. This directory contains subdirectories for each major screen or feature (e.g., `camera`, `chat`, `settings`, `timeline`, `videoedit`), along with their respective Composables and ViewModels. This layer is responsible for everything the user sees and interacts with.

This structure promotes a clean separation of concerns, making the codebase more modular, maintainable, and testable. It adheres to the principles of the Model-View-ViewModel (MVVM) architectural pattern, which is widely used in modern Android development.
