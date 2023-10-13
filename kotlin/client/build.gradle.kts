import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    `java-library`

    // Custom plugin to generate the native libs and bindings file
    id("babushka.plugin.generate-jvm-bindings")
}

// This task dependency ensures that we build the bindings
// binaries before running the tests
tasks.withType<KotlinCompile> {
    dependsOn("buildJvmLib")

    kotlinOptions {
        jvmTarget = "11"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
    implementation("net.java.dev.jna:jna:5.8.0")
}

tasks.jar {
    manifest {
        attributes(mapOf("Implementation-Title" to "kotlinbabushka",
                         "Implementation-Version" to project.version))
    }
    archiveBaseName.set("kotlinbabushka")
}
