import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.konan.target.HostManager
import java.time.LocalDate
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// --- Versionado -------------------------------------------------------------
// versionName sigue SemVer. versionCode se deriva de forma monotona con la
// formula MAJOR * 10_000 + MINOR * 100 + PATCH, documentada en docs/INSTALL.md.
val appVersionName = "1.0.0"
val appVersionCode = appVersionName.split(".").let { (major, minor, patch) ->
    major.toInt() * 10_000 + minor.toInt() * 100 + patch.toInt()
}

val gitShortHash: String = runCatching {
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim()
}.getOrElse { "desconocido" }

// --- Generacion de BuildInfo compartido -------------------------------------
val generatedBuildInfoDir: Provider<Directory> =
    layout.buildDirectory.dir("generated/nexapdf/commonMain/kotlin")

val generateBuildInfo = tasks.register("generateBuildInfo") {
    val outputDir = generatedBuildInfoDir
    val versionName = appVersionName
    val versionCode = appVersionCode
    val commit = gitShortHash
    val buildDate = LocalDate.now().toString()
    inputs.property("versionName", versionName)
    inputs.property("versionCode", versionCode)
    inputs.property("commit", commit)
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("es/ghatostudio/nexapdf")
        dir.mkdirs()
        dir.resolve("BuildInfo.kt").writeText(
            """
            package es.ghatostudio.nexapdf

            /** Datos de compilacion generados por Gradle. No editar a mano. */
            object BuildInfo {
                const val VERSION_NAME: String = "$versionName"
                const val VERSION_CODE: Int = $versionCode
                const val COMMIT_HASH: String = "$commit"
                const val BUILD_DATE: String = "$buildDate"
                const val APPLICATION_ID: String = "es.ghatostudio.nexapdf"
                const val DONATION_URL: String = "https://revolut.me/brais2oz6"
                const val PLAY_STORE_URL: String =
                    "https://play.google.com/store/apps/details?id=es.ghatostudio.nexapdf"
                const val PROJECT_URL: String = "https://github.com/braisgaldo/NexaPDF"
                const val PRIVACY_URL: String = "https://braisgaldo.github.io/NexaPDF/privacidad.html"
                const val CONTACT_EMAIL: String = "ghatostudio@proton.me"
            }
            """.trimIndent() + "\n",
        )
    }
}

kotlin {
    // El BackHandler multiplataforma sigue marcado como experimental en Compose
    // 1.12. Se asume a sabiendas: es la unica forma de atender el boton atras
    // desde codigo compartido, y sin el la app se cerraria desde cualquier
    // pantalla en lugar de retroceder.
    compilerOptions {
        optIn.add("androidx.compose.ui.ExperimentalComposeUiApi")
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm("desktop")

    // Los targets de iOS solo se declaran en macOS: Kotlin/Native no puede
    // compilar para Apple desde Windows. Ver docs/adr/0003-portabilidad-ios.md
    if (HostManager.hostIsMac) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        val desktopMain = getByName("desktopMain")

        commonMain {
            kotlin.srcDir(generatedBuildInfoDir)
            dependencies {
                // Los accesores `compose.*` los resuelve el plugin de Compose
                // Multiplatform, que es quien conoce la version correcta de cada
                // artefacto para el release fijado en el catalogo
                // (composeMultiplatform = 1.12.0). Las coordenadas sueltas no
                // sirven aqui: material3 y los iconos llevan su propia linea de
                // versiones, distinta de la del release.
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.materialIconsExtended)
                implementation(libs.compose.ui.backhandler)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.androidx.datastore.preferences.core)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.androidx.lifecycle.runtime.compose)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.browser)
            implementation(libs.androidx.documentfile)
            implementation(libs.androidx.exifinterface)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.pdfbox.android)
            implementation(libs.bouncycastle.prov)
            implementation(libs.bouncycastle.pkix)
            implementation(libs.bouncycastle.util)
        }

        androidInstrumentedTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.test.junit)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.core)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateBuildInfo)
}

configurations.configureEach {
    // PDFBox-Android declara la familia antigua de BouncyCastle; se descarta
    // para que quede una sola version, la fijada en el catalogo.
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    exclude(group = "org.bouncycastle", module = "bcpkix-jdk15on")
}

compose.resources {
    publicResClass = false
    packageOfResClass = "es.ghatostudio.nexapdf.resources"
    generateResClass = org.jetbrains.compose.resources.ResourcesExtension.ResourceClassGeneration.Always
}

// --- Firma ------------------------------------------------------------------
// El keystore NUNCA vive en el repositorio. Se lee de keystore.properties
// (ignorado por git) o de variables de entorno para CI.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

fun secret(key: String, env: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(env)

android {
    namespace = "es.ghatostudio.nexapdf"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "es.ghatostudio.nexapdf"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
    }

    signingConfigs {
        // La ruta del keystore se resuelve contra la raiz del proyecto, no
        // contra este modulo: keystore.properties vive en la raiz y es ahi donde
        // se escriben las rutas. Con `file(...)` a secas se buscaria dentro de
        // composeApp/, no se encontraria, y el AAB saldria sin firmar sin que
        // nada avisara.
        val ficheroKeystore = secret("storeFile", "NEXAPDF_STORE_FILE")
            ?.let { rootProject.file(it) }

        if (ficheroKeystore != null && ficheroKeystore.exists()) {
            create("release") {
                storeFile = ficheroKeystore
                storePassword = secret("storePassword", "NEXAPDF_STORE_PASSWORD")
                keyAlias = secret("keyAlias", "NEXAPDF_KEY_ALIAS")
                keyPassword = secret("keyPassword", "NEXAPDF_KEY_PASSWORD")
            }
        } else {
            logger.warn(
                "NexaPDF: no hay keystore en '${ficheroKeystore?.path ?: "(sin configurar)"}'. " +
                    "La compilacion de release saldra SIN FIRMAR.",
            )
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/DEPENDENCIES",
            "META-INF/INDEX.LIST",
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependenciesInfo {
        // Los bloques de dependencias cifrados impiden compilaciones reproducibles
        // y no aportan nada a una app sin facturacion. Ver docs/ARCHITECTURE.md
        includeInApk = false
        includeInBundle = false
    }

    lint {
        abortOnError = true
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "OldTargetApi")
    }
}

compose.desktop {
    application {
        mainClass = "es.ghatostudio.nexapdf.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "NexaPDF"
            packageVersion = appVersionName
        }
    }
}
