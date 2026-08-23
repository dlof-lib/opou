plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

android {
    namespace = "com.OPEN.OU"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.OPEN.OU"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        // عنوان خادم PHP المساعد (server/php) — عدّله ليطابق دومين الاستضافة الفعلي لديك.
        // يُستخدم بواسطة network/PhpApiClient.kt للجسر بين Kotlin و PHP (Firebase auth + FCM + ضغط الصور).
        buildConfigField("String", "PHP_BASE_URL", "\"https://openou.example.com/\"")
    }

    signingConfigs {
        create("release") {
            // تُملأ عبر متغيرات بيئة CI (انظر workflow) — لا تضع مفاتيح حقيقية هنا
            val keystorePath = System.getenv("OPOU_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("OPOU_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("OPOU_KEY_ALIAS")
                keyPassword = System.getenv("OPOU_KEY_PASSWORD")
            }
            // ملاحظة: إن لم يتوفر OPOU_KEYSTORE_PATH (بناء محلي بدون سرّ حقيقي)،
            // يبقى هذا الإعداد بلا storeFile، ويُستخدم عندها توقيع debug تلقائيًا
            // عبر signingConfig = signingConfigs.getByName("debug") في buildTypes.release أدناه.
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            val hasRealKeystore = !System.getenv("OPOU_KEYSTORE_PATH").isNullOrBlank()
            signingConfig = if (hasRealKeystore) {
                signingConfigs.getByName("release")
            } else {
                // بناء محلي/تجريبي بدون أسرار CI: توقيع debug مؤقت حتى لا يفشل assembleRelease
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase (Realtime Database + Auth + Storage)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // ============================================================
    // 10 مكاتب/حزم إضافية لتحسين التطبيق وأداءه وموثوقيته
    // ============================================================
    // 1) دعم تبديل لغة التطبيق (عربي/إنجليزي) لكل تطبيق دون تغيير لغة الجهاز بالكامل
    implementation("androidx.appcompat:appcompat:1.7.0")
    // 2) DataStore لحفظ تفضيلات المستخدم (اللغة، الإعدادات) بشكل غير متزامن وآمن بدل SharedPreferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // 3) Retrofit — عميل شبكة نظيف للتواصل مع خادم PHP المساعد (server/php)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    // 4) محوّل Gson لتحويل JSON تلقائيًا لكائنات Kotlin مع Retrofit
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // 5) OkHttp Logging Interceptor — تشخيص طلبات الشبكة أثناء التطوير
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // 6) Timber — تسجيل (Logging) منظم بدل Log.d المباشر، يسهّل التتبع والتصحيح
    implementation("com.jakewharton.timber:timber:5.0.1")
    // 7) Lottie Compose — رسوم متحركة خفيفة الحجم عالية الأداء بدل GIF/فيديو
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    // 8) WorkManager — مزامنة موثوقة في الخلفية (مثال: إعادة محاولة عمليات فشلت بسبب الشبكة)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    // 9) Firebase Crashlytics — تتبّع الأعطال الحقيقية على أجهزة المستخدمين
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    // 9b) Firebase Performance Monitoring — قياس أداء الشبكة والشاشات فعليًا
    implementation("com.google.firebase:firebase-perf-ktx")
    // 10) LeakCanary — كشف تسريبات الذاكرة تلقائيًا أثناء التطوير (Debug فقط)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
