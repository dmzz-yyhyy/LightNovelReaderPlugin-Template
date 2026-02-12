import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.nightfish.potatolib"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.nightfish.potatolib"
        minSdk = 24
        targetSdk = 36
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
    implementation(libs.okhttp3.okhttp)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.cxhttp)
    implementation(libs.jaxen)
    implementation(libs.kotlin.result)
    implementation(libs.kotlin.result.coroutines)
    implementation(libs.jsoup)

    //LNR Api
    implementation(libs.lightnovelreader.api)
}

val debugHostPkg = "indi.dmzz_yyhyy.lightnovelreader.debug"
val releaseHostPkg = "indi.dmzz_yyhyy.lightnovelreader"


fun pluginApk(): File =
    File(layout.buildDirectory.asFile.get(), "outputs/apk/debug")
        .walkTopDown()
        .first {
            it.isFile && it.name.endsWith(".apk") || it.name.endsWith(".lnrp")
        }

fun installPluginTask(name: String, hostPkg: String) {
    tasks.register(name) {
        group = "plugin"
        dependsOn("assembleDebug")

        doLast {
            val adb = listOf(androidComponents.sdkComponents.adb.get().asFile.absolutePath) +
                    (System.getenv("ANDROID_SERIAL")?.let { listOf("-s", it) } ?: emptyList())
            val src = pluginApk()
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

installPluginTask("runDebugHost", debugHostPkg)
installPluginTask("runReleaseHost", releaseHostPkg)
