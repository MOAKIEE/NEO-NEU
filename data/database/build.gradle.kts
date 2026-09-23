plugins { id("com.android.library") }
android { namespace = "edu.neu.campus.database"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":core:contract")) }
