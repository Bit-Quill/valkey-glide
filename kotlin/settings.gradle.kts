pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "kotlinbabushka"

include("client")
includeBuild("plugin")
