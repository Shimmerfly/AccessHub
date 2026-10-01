plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

// Version stamp: versionName is "git+<short sha>", versionCode is the commit count.
// Both stay lazy providers: reading them during configuration would invalidate the
// configuration cache on every commit.
val commitCount = providers.exec {
    workingDir(rootProject.layout.projectDirectory)
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.map { it.trim().toInt() }

val gitHash = providers.exec {
    workingDir(rootProject.layout.projectDirectory)
    commandLine("git", "rev-parse", "--short=7", "HEAD")
}.standardOutput.asText.map { it.trim() }

android {
    namespace = "dev.sol.accesshub"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "dev.sol.accesshub"
        minSdk = 24
        targetSdk = 37
        // Placeholders only; the git stamp below overrides them for every variant.
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
        // The privileged `settings` calls go through a Shizuku UserService, which is declared
        // as AIDL (IShellService).
        aidl = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/*.version")
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.versionCode.set(commitCount)
            output.versionName.set(gitHash.map { "git+$it" })
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
    implementation(libs.compose.material.icons.extended)

    // Shizuku API + provider (auto binder delivery).
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigationevent.compose)

    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.gfm.tables)
    implementation(libs.commonmark.ext.gfm.strikethrough)
    implementation(libs.commonmark.ext.autolink)
    implementation(libs.commonmark.ext.task.list.items)

    implementation(libs.androidx.webkit)

    implementation(libs.hiddenapibypass)

    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.2")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.2")
    implementation("top.yukonga.miuix.kmp:miuix-navigation3-ui-android:0.9.2")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.2")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.2")

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)

    implementation(libs.material.kolor)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
