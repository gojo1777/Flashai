FLASH AI - Android project
=========================
What it does:
- Uses the FRONT/SELFIE camera for MediaPipe hand tracking.
- Detects 5 open fingers.
- Calls native Android CameraManager.setTorchMode() to control the REAL rear LED.
- No screen-flash effect is used.

Build:
1. Open this folder in Android Studio or AndroidIDE.
2. Let Gradle sync.
3. Build/install the debug APK.
4. Grant Camera permission.
5. Keep the selfie camera running.
6. Show all 5 fingers -> rear hardware LED ON.
7. Fold/remove fingers -> rear hardware LED OFF.

Important:
- The device must have a rear LED flash.
- Some phones may prevent torch use while the camera is busy; behavior depends on the device camera HAL.
- MediaPipe JS files are loaded from jsDelivr, so internet is needed on first run/use.
