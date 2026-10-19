# Zensa — Debugging pass

## Changes made
- Corrected the backend Kotlin JPA plugin version to match the Kotlin 2.0.21 plugins.
- Replaced the Spring Boot `spring-boot-starter-webmvc` dependency with `spring-boot-starter-web` for the Spring Boot 3.4.5 project.
- Replaced the Jackson Kotlin module coordinate with the Jackson 2 module compatible with Spring Boot 3.4.5.
- Removed Android Studio `.idea` workspace files, the Git history folder, and the machine-specific `local.properties` file from this clean ZIP.
- Added `local.properties` to `.gitignore` so each computer can set its own Android SDK path.

## Open and run the Android app
1. Extract this ZIP.
2. Open the `Zensa` folder (the one containing `settings.gradle.kts`) in Android Studio.
3. Install Android SDK Platform 36 and JDK 17 if Android Studio asks.
4. Let Gradle Sync finish, then select **Build > Make Project**.
5. Start an Android emulator and press **Run**.

## Run the backend
The backend is in `Zensa/zensa-api/zensa-api`.
1. Install JDK 21 and configure it in Android Studio or your terminal.
2. Configure a MySQL database and set the database credentials and JWT secret in `src/main/resources/application.properties` (do not commit real secrets).
3. Open the backend folder as a Gradle project and run `bootRun`, or run `./gradlew bootRun` on macOS/Linux (`gradlew.bat bootRun` on Windows).

## Important
This is a static debugging pass. Gradle could not be run in this environment because a Gradle installation/cache and Android SDK are not available here, and dependency downloads/build execution were not available. Therefore, I cannot honestly claim that the Android app and backend have compiled or passed their tests. After opening in Android Studio, if Gradle reports another error, copy the **first** error from the Build output and send it so it can be addressed precisely.

The Android app's emulator API base URL is currently `http://10.0.2.2:8080/`, which points to a backend running on your computer from an Android emulator. A physical phone needs your computer's reachable LAN IP or a deployed HTTPS API URL instead.
