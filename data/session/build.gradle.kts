plugins { id("com.android.library") }
android { namespace = "edu.neu.campus.session"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":core:contract")); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2") }
