plugins { id("com.android.application") }
android { namespace = "edu.neu.campus.verification"; compileSdk = 37; defaultConfig { applicationId = "edu.neu.campus.verification"; minSdk = 24; targetSdk = 36; versionCode = 1; versionName = "0.1" } }
dependencies { implementation(project(":core:contract")); implementation(project(":data:repository")); implementation(project(":integration:auth-web")); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2") }
