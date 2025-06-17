# Model Layer

This directory contains the data models and entities used throughout the SociaLite application. These classes represent the structure of the data that the application operates on.

Examples of data models you might find here include:

-   `Message`: Represents a single message within a chat thread, including its content, sender, timestamp, and status.
-   `ChatThread`: Represents a conversation with a specific animal avatar, including information about the participants and the last message.
-   `User`: Represents a user (although in this sample, it might be simplified or implicitly handled).

These models are typically defined as simple data classes (like Kotlin `data class`) and primarily hold data without containing complex business logic. They serve as the blueprint for the data exchanged between different layers of the application, such as the data layer and the UI layer.
