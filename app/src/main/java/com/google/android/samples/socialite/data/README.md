# Data Layer

This directory contains the data layer of the SociaLite application. It is responsible for providing a clean and consistent API for the rest of the application to access and manipulate data, abstracting the underlying data sources.

-   **Data Sources:** Implementations for accessing data from various sources. In this application, the primary data source is the **Room Database**, which is used to persist messages and chat thread information. You will find classes defining DAOs (Data Access Objects) and the database itself here.
-   **Repositories:** Classes (e.g., `MessageRepository`, `ChatThreadRepository`) that abstract the data sources. They provide methods for fetching, saving, and updating data, often exposing data streams (like `Flow` in Kotlin) to the UI layer. Repositories are the main interface for ViewModels to interact with data.
-   **Data Logic:** Any business logic related to data fetching, processing, or transformation before it is exposed to the UI. This might include handling data conflicts, mapping data models, or performing background operations related to data synchronization.

This layer follows the recommended Android architecture guidelines, separating concerns and making the application more testable and maintainable.
