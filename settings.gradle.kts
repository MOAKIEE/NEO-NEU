pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "NEO NEU"
include(":core:contract", ":data:session", ":data:network", ":data:database", ":data:repository", ":integration:academic", ":integration:portal", ":integration:auth-web", ":verification", ":core:ui", ":app")
