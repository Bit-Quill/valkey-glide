package babushka

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register

internal class UniFfiJvmPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = target.run {
//*
        // register a task called buildJvmBinaries which will run something like
        // cargo build --release --target aarch64-apple-darwin
        val buildJvmBinaries by tasks.register<DefaultTask>("buildJvmBinaries") {
            if (operatingSystem == OS.MAC) {
                exec {
                    workingDir("$rootDir/interop")
                    executable("cargo")
                    val cargoArgs: List<String> = listOf("build", "--profile", "release-smaller", "--target", "x86_64-apple-darwin")
                    args(cargoArgs)
                }
                exec {
                    workingDir("$rootDir/interop")
                    executable("cargo")
                    val cargoArgs: List<String> = listOf("build", "--profile", "release-smaller", "--target", "aarch64-apple-darwin")
                    args(cargoArgs)
                }
            } else if (operatingSystem == OS.LINUX) {
                exec {
                    workingDir("$rootDir/interop")
                    executable("cargo")
                    val cargoArgs: List<String> = listOf("build", "--profile", "release-smaller", "--target", "x86_64-unknown-linux-gnu")
                    args(cargoArgs)
                }
            } else if (operatingSystem == OS.WINDOWS) {
                exec {
                    workingDir("$rootDir/interop")
                    executable("cargo")
                    val cargoArgs: List<String> = listOf("build", "--profile", "release-smaller", "--target", "x86_64-pc-windows-msvc")
                    args(cargoArgs)
                }
            }
        }

        // move the native libs build by cargo from target/.../release/
        // to their place in the jvm library
        val moveNativeJvmLibs by tasks.register<DefaultTask>("moveNativeJvmLibs") {

            // dependsOn(buildJvmBinaryX86_64MacOS, buildJvmBinaryAarch64MacOS, buildJvmBinaryLinux)
            dependsOn(buildJvmBinaries)

            data class CopyMetadata(val targetDir: String, val ext: String)
            val libsToCopy: MutableList<CopyMetadata> = mutableListOf()
            var resDir = ""

            if (operatingSystem == OS.MAC) {
                resDir = "darwin-aarch64"
                libsToCopy.add(
                    CopyMetadata(
                        targetDir = "aarch64-apple-darwin",
                        ext = "dylib"
                    )
                )
                libsToCopy.add(
                    CopyMetadata(
                        targetDir = "x86_64-apple-darwin",
                        ext = "dylib"
                    )
                )
            } else if (operatingSystem == OS.LINUX) {
                resDir = "linux-x86-64"
                libsToCopy.add(
                    CopyMetadata(
                        targetDir = "x86_64-unknown-linux-gnu",
                        ext = "so"
                    )
                )
            } else if (operatingSystem == OS.WINDOWS) {
                resDir = "win32-x86-64"
                libsToCopy.add(
                    CopyMetadata(
                        targetDir = "x86_64-pc-windows-msvc",
                        ext = "dll"
                    )
                )
            }
            val libName = when (operatingSystem) {
                OS.WINDOWS -> "kotlinbabushka"
                else       -> "libkotlinbabushka"
            }

            val destDir = "$rootDir/client/src/main/resources/${resDir}/"
            mkdir(destDir)

            libsToCopy.forEach {
                doFirst {
                    copy {
                        with(it) {
                            from("$rootDir/interop/target/${this.targetDir}/release-smaller/${libName}.${this.ext}")
                            into(destDir)
                        }
                    }
                }
            }
        }

        // generate the bindings using the bindgen tool
        val generateJvmBindings by tasks.register<Exec>("generateJvmBindings") {

            dependsOn(moveNativeJvmLibs)

            workingDir("$rootDir/interop")
            val cargoArgs: List<String> = listOf("run", "--bin", "uniffi-bindgen", "generate", "src/babushka.udl",
                "--language", "kotlin", "--out-dir", "../client/src/main/kotlin", "--no-format")

            executable("cargo")
            args(cargoArgs)

            doLast {
                println("JVM bindings file successfully created")
            }
        }
//*/
        // we need an aggregate task which will run the 3 required tasks to build the JVM libs in order
        // the task will also appear in the printout of the ./gradlew tasks task with a group and description
        tasks.register("buildJvmLib") {
            group = "babushka"
            description = "Aggregate task to build JVM library"

            dependsOn(
                //*
                buildJvmBinaries,
                moveNativeJvmLibs,
                generateJvmBindings
                //*/
            )
        }
    }
}
