repositories {
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("uniFfiJvmBindings") {
            id = "babushka.plugin.generate-jvm-bindings"
            implementationClass = "babushka.UniFfiJvmPlugin"
        }
    }
}
