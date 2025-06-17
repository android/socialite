# Camera Module

This directory contains the code responsible for the in-app camera feature of the SociaLite application. This includes:

-   **Camera UI:** Jetpack Compose composables (`CameraScreen.kt`, `CameraControls.kt`, etc.) for displaying the camera preview, controls (like capture buttons, camera switch), and handling user interactions. The UI observes state from the ViewModel to update the display.
-   **CameraX Integration:** Logic for integrating with the CameraX library to capture photos and videos. This involves setting up the camera provider, selecting the camera (front/back), binding use cases (Preview, ImageCapture, VideoCapture), and handling the capture process. Look for files related to `ProcessCameraProvider` and `UseCase`.
-   **Permissions Handling:** Code for requesting and managing necessary permissions (like `CAMERA` and `WRITE_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` on newer Android versions). This is crucial before initializing the camera.
-   **File Management:** Handling the saving of captured photos and videos to appropriate directories (e.g., the device's media storage). This involves using `ImageCapture.OutputFileOptions` and `VideoCapture.OutputFileOptions`.

The main entry point for the camera feature is typically the `CameraScreen` composable function within this directory, which is navigated to from the chat screen. The `CameraViewModel` manages the camera state and interacts with the data layer or other components to handle captured media.
