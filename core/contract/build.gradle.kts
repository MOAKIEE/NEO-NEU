plugins { id("com.android.library") }
android { namespace = "edu.neu.campus.contract"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2"); testImplementation("junit:junit:4.13.2") }
