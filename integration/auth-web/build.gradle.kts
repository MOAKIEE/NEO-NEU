plugins { id("com.android.library") }
android { namespace = "edu.neu.campus.authweb"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":core:contract")); implementation(project(":data:session")); implementation(project(":data:network")); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2") }
