# Model Layer

This directory contains the data models and entities used throughout the SociaLite application. These classes represent the structure of the data that the application operates on.

-   `Message`: Represents a single message within a chat thread, including its content, sender, timestamp, and status.
-   `Chat` and `ChatDetail`: Represents a conversation with a specific animal avatar, including information about the participants and the last message.
-   `Contact`: Represents an animal avatar that you can chat with in Socialite. It also includes the default behavior for each animal.

These models are typically defined as simple data classes (like Kotlin `data class`) and primarily hold data without containing complex business logic. They serve as the blueprint for the data exchanged between different layers of the application, such as the data layer and the UI layer.
