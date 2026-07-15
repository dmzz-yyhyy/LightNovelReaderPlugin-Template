import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.ksp)
}

android {
    namespace = "io.nightfish.potatolib"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.nightfish.potatolib"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach {
            val outputImpl = it as com.android.build.api.variant.impl.VariantOutputImpl
            val originalFileName = outputImpl.outputFileName.get()
            val newFileName = originalFileName.replace(".apk", ".apk.lnrp")
            outputImpl.outputFileName = newFileName
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.manifests.addStaticManifestFile(
            layout.buildDirectory.file("generated/ksp/${variant.name}/resources/auto_register_manifest.xml").get().toString()
        )
    }
}

afterEvaluate {
    listOf("Debug", "Release").forEach { variant ->
        tasks.findByName("process${variant}MainManifest")?.dependsOn("ksp${variant}Kotlin")
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.foundation.layout)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.serialization.cbor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jaxen)
    implementation(libs.kotlin.result)
    implementation(libs.kotlin.result.coroutines)
    implementation(libs.jsoup)
    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)

    //LNR Api
    compileOnly(libs.lightnovelreader.api)
    ksp(libs.lightnovelreader.compiler)
}

val debugHostPkg = "indi.dmzz_yyhyy.lightnovelreader.debug"
val releaseHostPkg = "indi.dmzz_yyhyy.lightnovelreader"


fun pluginApk(variant: String): File =
    File(layout.buildDirectory.asFile.get(), "outputs/apk/${variant.lowercase()}")
        .walkTopDown()
        .first {
            it.isFile && it.name.endsWith(".apk") || it.name.endsWith(".lnrp")
        }

fun installPluginTask(name: String, hostPkg: String, variant: String) {
    tasks.register(name) {
        description = ""
        group = "plugin"
        dependsOn("assemble$variant")

        doLast {
            val adb = listOf(androidComponents.sdkComponents.adb.get().asFile.absolutePath) +
                    (System.getenv("ANDROID_SERIAL")?.let { listOf("-s", it) } ?: emptyList())
            val src = pluginApk(variant)
            val file =
                if (src.name.endsWith(".apk")) src
                else File(src.parent, src.name.removeSuffix(".lnrp"))
                    .also { src.renameTo(it) }

            try {
                providers.exec {
                    commandLine(adb + listOf("install", "-r", "-t", file))
                }.result.get()
            } finally {
                if (file != src) file.renameTo(src)
            }

            providers.exec {
                commandLine(adb + listOf("shell", "am", "force-stop", hostPkg))
            }.result.get()

            providers.exec {
                commandLine(
                    adb + listOf(
                        "shell", "monkey", "-p", hostPkg, "-c",
                        "android.intent.category.LAUNCHER", "1"
                    )
                )
            }.result.get()
        }
    }
}

installPluginTask("runDebugHostWithDebugPlugin", debugHostPkg, "Debug")
installPluginTask("runReleaseHostWithDebugPlugin", releaseHostPkg, "Debug")
installPluginTask("runDebugHostWithReleasePlugin", debugHostPkg, "Release")
installPluginTask("runReleaseHostWithReleasePlugin", releaseHostPkg, "Release")
