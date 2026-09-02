import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

val keystorePropertiesFile: File = sequenceOf(
    File(System.getProperty("user.home"), "AppData/Local/phonly-messages-signing/keystore.properties"),
    rootProject.file("keystore.properties"),
).firstOrNull { it.exists() } ?: rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

val notificationPropertiesFile: File? = sequenceOf(
    File(System.getProperty("user.home"), "AppData/Local/phonly-messages-signing/notification.properties"),
    File(System.getProperty("user.home"), "AppData/Local/phonly-phone-signing/notification.properties"),
).firstOrNull { it.exists() }
val notificationProperties = Properties()
if (notificationPropertiesFile != null) {
    notificationProperties.load(FileInputStream(notificationPropertiesFile))
}

// Rebuild BuildConfig when the local token/imei file changes (file is not in git).
if (notificationPropertiesFile != null) {
    tasks.configureEach {
        if (name.contains("BuildConfig", ignoreCase = true)) {
            inputs.file(notificationPropertiesFile)
        }
    }
}

fun hasSigningVars(): Boolean {
    return providers.environmentVariable("SIGNING_KEY_ALIAS").orNull != null
            && providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull != null
            && providers.environmentVariable("SIGNING_STORE_FILE").orNull != null
            && providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull != null
}

fun escapeBuildConfig(value: String): String {
    return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")
}

fun notificationSecret(): String {
    return providers.environmentVariable("NOTIFICATION_SECRET").orNull
        ?: providers.environmentVariable("DEVICE_NOTIFICATION_LAB_TOKEN").orNull
        ?: notificationProperties.getProperty("token").orEmpty()
}

fun deviceImeiOverride(): String {
    return providers.environmentVariable("DEVICE_IMEI").orNull
        ?: notificationProperties.getProperty("imei").orEmpty()
}

val debugKeystoreFile = File(System.getProperty("user.home"), ".android/debug.keystore")

base {
    val versionCode = project.property("VERSION_CODE").toString().toInt()
    archivesName = "messages-$versionCode"
}

android {
    compileSdk = project.libs.versions.app.build.compileSDKVersion.get().toInt()

    defaultConfig {
        applicationId = project.property("APP_ID").toString()
        minSdk = project.libs.versions.app.build.minimumSDK.get().toInt()
        targetSdk = project.libs.versions.app.build.targetSDK.get().toInt()
        versionName = project.property("VERSION_NAME").toString()
        versionCode = project.property("VERSION_CODE").toString().toInt()
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
        buildConfigField("String", "NOTIFICATION_API_BASE", "\"https://phonlyv1.onrender.com\"")
        buildConfigField(
            "String",
            "NOTIFICATION_SECRET",
            "\"${escapeBuildConfig(notificationSecret())}\""
        )
        buildConfigField(
            "String",
            "DEVICE_IMEI_OVERRIDE",
            "\"${escapeBuildConfig(deviceImeiOverride())}\""
        )
    }

    signingConfigs {
        getByName("debug") {
            storeFile = debugKeystoreFile
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (keystorePropertiesFile.exists()) {
            register("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        } else if (hasSigningVars()) {
            register("release") {
                keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").get()
                storeFile = file(providers.environmentVariable("SIGNING_STORE_FILE").get())
                storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").get()
            }
        } else {
            logger.warn("Warning: No signing config found. Build will be unsigned.")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            // Same debug cert as Phone so co.phonly.permission.NOTIFICATION (signature) works.
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            // First Esper cut: do not minify.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists() || hasSigningVars()) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Any in-service IMEI at runtime. Do not bake AAAAY into a library APK.
            buildConfigField("String", "DEVICE_IMEI_OVERRIDE", "\"\"")
        }
    }

    flavorDimensions.add("variants")
    productFlavors {
        register("core")
        register("foss")
        register("gplay")
    }

    sourceSets {
        getByName("main").java.directories.add("src/main/kotlin")
    }

    compileOptions {
        val currentJavaVersionFromLibs = JavaVersion.valueOf(libs.versions.app.build.javaVersion.get())
        sourceCompatibility = currentJavaVersionFromLibs
        targetCompatibility = currentJavaVersionFromLibs
    }

    dependenciesInfo {
        includeInApk = false
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(
            JvmTarget.fromTarget(project.libs.versions.app.build.kotlinJVMTarget.get())
        )
    }

    // Install id is APP_ID (co.phonly.messages). Kotlin/R namespace stays Fossify.
    namespace = "org.fossify.messages"

    lint {
        checkReleaseBuilds = false
        abortOnError = true
        warningsAsErrors = false
        baseline = file("lint-baseline.xml")
        lintConfig = rootProject.file("lint.xml")
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

detekt {
    baseline = file("detekt-baseline.xml")
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
}

dependencies {
    implementation(libs.fossify.commons)
    implementation(libs.eventbus)
    implementation(libs.indicator.fast.scroll)
    implementation(libs.mmslib)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.ez.vcard)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)
    detektPlugins(libs.compose.detekt)
}

androidComponents {
    onVariants { variant ->
        variant.instrumentation.transformClassesWith(
            PhonlyFossifyClassVisitorFactory::class.java,
            InstrumentationScope.ALL
        ) {}
        variant.instrumentation.setAsmFramesComputationMode(
            FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
        )
    }
}

/**
 * Fossify Commons treats any applicationId other than org.fossify.* as a
 * "fake" app. Rewrite those string checks so co.phonly.messages is accepted.
 */
abstract class PhonlyFossifyClassVisitorFactory :
    AsmClassVisitorFactory<InstrumentationParameters.None> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return object : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {
            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
                return object : MethodVisitor(Opcodes.ASM9, mv) {
                    override fun visitLdcInsn(value: Any?) {
                        val mapped = when (value) {
                            "org.fossify." -> "co.phonly."
                            "yfissof" -> "ylnohp."
                            else -> value
                        }
                        super.visitLdcInsn(mapped)
                    }
                }
            }
        }
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.className == "org.fossify.commons.activities.BaseSimpleActivity"
    }
}
