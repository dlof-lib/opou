# OPOU — منصّة تواصل اجتماعي عربية 🟢

مشروع Android كامل بلغة Kotlin/Jetpack Compose مع طبقة أصلية C++ (JNI)، مبني على
Firebase Realtime Database، بمصطلحات عربية أصيلة بدل المصطلحات الإنجليزية التقليدية.

## المصطلحات الخاصة بأوبو

| المصطلح | المعنى المكافئ |
|---|---|
| **الفقرة** (Paragraph) | المنشور النصي |
| **الغرفة** (Room) | الملف الشخصي |
| **اسم المجتمع** | تصنيف صاحب الغرفة (مثال: يوتيوبر، مصمم، لاعب...) |
| **تيك / تيكينغ / تيكر** (Tek / Teking / Teker) | إعادة النشر / أنا أتابع / متابعيّ |
| **الشعبيات** (Shaabiyat) | تبويب الأكثر شعبية |
| ⭐ | إعجاب |
| 💔 | لم يعجبني (قلب مكسور) |

## هيكل المشروع

```
OPOU/
├── app/
│   ├── src/main/java/com/OPEN/OU/
│   │   ├── data/model/          # User, Post, Comment, Reaction
│   │   ├── data/repository/     # Auth, User, Post repositories (Firebase RTDB)
│   │   ├── ui/theme/            # ألوان أوبو (أخضر/ذهبي) وطباعة
│   │   ├── ui/screens/          # تسجيل الدخول، التسجيل، التغذية، الغرفة، النشر
│   │   ├── ui/components/       # PostCard, ReactionBar, CommentsSheet
│   │   ├── navigation/          # OpouNavGraph
│   │   ├── service/             # FCM Messaging Service
│   │   └── util/NativeBridge.kt # جسر JNI
│   ├── src/main/cpp/            # مكتبة C++ الأصلية (CMake)
│   ├── src/main/res/            # موارد عربية + أيقونة التطبيق
│   └── google-services.json     # (تم تضمينه من الملف الذي رفعته)
├── server/php/                  # خادم PHP مساعد اختياري (رفع صور + إشعارات)
├── .github/workflows/build.yml  # بناء APK تلقائيًا عبر GitHub Actions
├── database.rules.json          # قواعد أمان Realtime Database
└── build.gradle.kts / settings.gradle.kts
```

## قاعدة بيانات Firebase Realtime Database

معرّف المشروع المستخرج من ملفك: **openou**

```
/users/{uid}                     -> بيانات الغرفة
/posts/{postId}                  -> الفقرات
/comments/{postId}/{commentId}   -> التعليقات
/reactions/{postId}/{uid}        -> "LIKE" | "DISLIKE"
/teking/{uid}/{tekerId}          -> من يتابعهم المستخدم
/tekers/{uid}/{tekingId}         -> من يتابع المستخدم
/usernames/{username}            -> uid (لضمان تفرّد الاسم)
```

انشر ملف `database.rules.json` على مشروعك عبر:
```bash
firebase deploy --only database
```

## طبقة C++ (JNI)

- `native-lib.cpp` — نقاط دخول JNI
- `shaabiya.cpp` — حساب نقاط الشعبية محليًا: `(إعجاب×3) + (تيك×5) + تعليقات − عدم_إعجاب`
- `crc32.cpp` — بصمة سريعة لمحتوى الفقرة لكشف التكرار الفوري بدون طلب شبكة

## نظام تحويل الصور إلى Base64 (احترافي وقوي)

بما أن المشروع يعتمد على **Realtime Database فقط** (بدون Firebase Storage)، تُخزَّن
جميع الصور (الأفتار، البانر، صور الفقرات) كسلاسل **Base64** مباشرة داخل عقد قاعدة
البيانات. تم بناء خط معالجة كامل عبر ثلاث طبقات متكاملة:

### 1) الطبقة الأصلية C++ (`base64.cpp` / `base64.h`)
ترميز/فك ترميز Base64 بجدول بحث ثابت (Lookup Table) ومعالجة بالكتل (Chunked)
لأعلى أداء ممكن، مع تسامح في القراءة (يتجاهل الرموز غير الصالحة بدل الانهيار) —
مربوط عبر JNI في `native-lib.cpp` بدالتين: `encodeBase64Native` و `decodeBase64Native`.

### 2) الجسر الآمن `NativeBridge.kt`
- يحاول تحميل المكتبة الأصلية عند بدء التشغيل، ويسجّل حالة النجاح في `isNativeAvailable`.
- كل دالة (`encodeBase64` / `decodeBase64`) محاطة بـ `try/catch` مع **نسخة احتياطية
  تلقائية** عبر `android.util.Base64` — بحيث لا يتعطل التطبيق أبدًا حتى لو فشل تحميل
  `.so` على جهاز أو معمارية معينة.

### 3) `ImageCodec.kt` — خط معالجة الصور الكامل
لكل صورة يختارها المستخدم:
1. قراءة الأبعاد فقط أولًا (`inJustDecodeBounds`) لتفادي استهلاك ذاكرة زائد.
2. تصغير ذكي (Downsampling) حسب حجم الصورة الأصلي.
3. تصحيح تلقائي لدوران الصورة عبر بيانات **EXIF** (مشكلة شائعة جدًا في صور الكاميرا).
4. **ضغط تكيّفي (Adaptive Compression)**: يخفّض جودة JPEG تدريجيًا (95 → 35) حتى
   الوصول للحجم المستهدف، حسب ملف الاستخدام:

   | الاستخدام | أقصى بُعد | الحجم المستهدف |
   |---|---|---|
   | صورة رمزية (Avatar) | 512px | ~180KB |
   | بانر الغرفة (Banner) | 1280px | ~350KB |
   | صورة داخل فقرة (Post) | 1600px | ~700KB |

5. ترميز النتيجة النهائية Base64 عبر الطبقة الأصلية.

### الواجهة (Compose)
- `ImagePickerButton.kt` — يفتح منتقي الصور، يعالج في الخلفية (`Dispatchers.Default`)
  دون تجميد الواجهة، ويعيد النتيجة أو رسالة خطأ عربية واضحة.
- `Base64Image.kt` — يعرض الصورة بكفاءة (فك ترميز مرة واحدة فقط عبر `remember`).
- مربوطة الآن في: `CreatePostScreen` (إرفاق صورة بالفقرة)، `ProfileScreen`
  (تغيير الصورة الرمزية)، و`PostCard`/`FeedScreen` (عرض الصور).

### لا اعتماد على PHP لمعالجة الصور
خط معالجة الصور بالكامل (قراءة → تصغير → تصحيح دوران → ضغط تكيّفي → ترميز
Base64) يعمل الآن محليًا داخل التطبيق فقط، عبر ثلاث طبقات:
**Kotlin** (`ImageCodec.kt` + `NativeBridge.kt`) + **C++/JNI** (`base64.cpp`) +
**XML** (أذونات `AndroidManifest.xml` وإعدادات الشبكة في `network_security_config.xml`).
لم يعد هناك أي نقطة نهاية PHP لضغط أو التحقق من الصور — تمت إزالة
`server/php/base64_image.php` نهائيًا لأنها كانت غير مستخدمة أصلًا وتكرارًا
لمنطق موجود بالفعل بأداء أعلى في الطبقة الأصلية. خادم PHP المساعد أصبح
مسؤولاً حصريًا عن الإشعارات (`notify.php`) ورفع ملف خام اختياري
(`upload_avatar.php`، غير مرتبط بـ Realtime Database).

### حماية على مستوى قاعدة البيانات
تم تحديث `database.rules.json` لفرض حدود صارمة على حجم كل حقل صورة
(`imageBase64`, `avatarBase64`, `bannerBase64`, `authorAvatarBase64`) لمنع أي
محاولة لتخزين حمولات ضخمة تتجاوز حدود Realtime Database أو تُبطئ التطبيق.



### محليًا
1. افتح المجلد في Android Studio (Hedgehog أو أحدث، يدعم AGP 8.5 و NDK).
2. تأكد من تثبيت **NDK 26.1** و **CMake 3.22.1** عبر SDK Manager.
3. ملف `app/google-services.json` موجود مسبقًا (منسوخ من الملف الذي أرفقته).
4. شغّل `Run` مباشرة.

> ملاحظة: هذا المستودع **لا يتضمن** ملفات `gradlew` الثنائية (jar)؛ لتوليدها محليًا نفّذ:
> ```bash
> gradle wrapper --gradle-version 8.7
> ```

### عبر GitHub Actions
- ادفع المشروع إلى مستودع GitHub (منظمة/حساب حسب رغبتك).
- أضف Secret باسم `GOOGLE_SERVICES_JSON` يحتوي محتوى ملف `google-services.json`
  (اختياري إن كان الملف موجودًا فعليًا في المستودع).
- الـ workflow `.github/workflows/build.yml` يبني نسخة Debug و Release تلقائيًا
  عند كل Push إلى `main`، وتُرفع كـ Artifacts قابلة للتنزيل من تبويب Actions.

## خادم PHP المساعد (اختياري)

يوجد في `server/php/`:
- `upload_avatar.php` — استقبال وتصغير صور الأفتار/البانر
- `notify.php` — إرسال إشعارات FCM يدويًا (تعليق/تيك/تفاعل جديد)
- `config.php` — الإعدادات المشتركة (عدّل `FCM_SERVER_KEY` قبل الاستخدام)

هذا الخادم اختياري تمامًا؛ التطبيق يعمل بالكامل عبر Firebase وحده بدونه.

## ما تم استكماله (الجولة الثانية)

### 1) ربط `currentReaction` الفعلي
تمت إضافة فهرس معكوس `/userReactions/{uid}/{postId}` يُحدَّث تلقائيًا مع كل تفاعل
(`PostRepository.react`)، ويراقبه `FeedViewModel.myReactions` فوريًا (Realtime)،
بحيث تعرض `FeedScreen` الآن اللون/الحالة الصحيحة لزر ⭐/💔 لكل فقرة فعليًا — بلا أي بيانات وهمية.

### 2) شاشة تعديل الغرفة الكاملة
`EditRoomScreen.kt` — تعديل اسم المجتمع والسيرة الذاتية، وتغيير الصورة الرمزية
والبانر عبر `ImagePickerButton` (`ImageProfile.AVATAR` / `ImageProfile.BANNER`)
مع معاينة فورية قبل الحفظ. مربوطة في `OpouNavGraph` عبر مسار `edit_profile/{uid}`
ومفعّلة من زر "تعديل الغرفة" في `ProfileScreen`.

### 3) ملفات `gradlew` / `gradlew.bat`
تمت إضافة السكربتات النصية القياسية (`gradlew`, `gradlew.bat`) و
`gradle/wrapper/gradle-wrapper.properties` (يشير إلى Gradle 8.7).

> ⚠️ **تنويه مهم**: ملف `gradle-wrapper.jar` نفسه ملف **ثنائي** (bytecode مُصرَّف)
> يتطلب اتصال إنترنت أو تثبيت Gradle محليًا لتوليده — تعذّر إنشاؤه في هذه البيئة.
> الحل بأي من الطريقتين:
> - افتح المشروع في **Android Studio** مباشرة؛ يقوم تلقائيًا بتوليد الملف عند أول مزامنة (Sync).
> - أو نفّذ يدويًا (بوجود Gradle مثبت): `gradle wrapper --gradle-version 8.7`
>
> ملاحظة: الـ CI (`build.yml`) **لا يعتمد على gradlew إطلاقًا** — يستخدم
> `gradle/actions/setup-gradle` لتثبيت Gradle مباشرة، لذا يعمل البناء التلقائي
> بشكل طبيعي حتى بدون هذا الملف.

### 4) توقيع حقيقي لنسخة Release
- `app/build.gradle.kts`: `signingConfigs.release` يقرأ من متغيرات البيئة
  (`OPOU_KEYSTORE_PATH`, `OPOU_KEYSTORE_PASSWORD`, `OPOU_KEY_ALIAS`, `OPOU_KEY_PASSWORD`)
  فقط عند توفرها؛ وإلا يُستخدم توقيع `debug` تلقائيًا لبناء محلي/تجريبي آمن.
- `.github/workflows/build.yml`: خطوة جديدة تفكّ تشفير Secret باسم
  `OPOU_KEYSTORE_BASE64` (ملف keystore بصيغة Base64) إلى ملف مؤقت، وتمرر بقية
  الأسرار (`OPOU_KEYSTORE_PASSWORD`, `OPOU_KEY_ALIAS`, `OPOU_KEY_PASSWORD`) كمتغيرات بيئة.

**لإعداد التوقيع الحقيقي في مستودعك:**
```bash
# 1) توليد keystore حقيقي (مرة واحدة فقط، احتفظ به في مكان آمن جدًا)
keytool -genkeypair -v -keystore opou-release.keystore \
  -alias opou -keyalg RSA -keysize 2048 -validity 10000

# 2) تحويله إلى Base64 لوضعه كـ GitHub Secret
base64 -w0 opou-release.keystore > opou-release.b64
```
ثم أضف في إعدادات GitHub → Secrets and variables → Actions:
`OPOU_KEYSTORE_BASE64`, `OPOU_KEYSTORE_PASSWORD`, `OPOU_KEY_ALIAS`, `OPOU_KEY_PASSWORD`.

### 5) تحقق فعلي من Firebase ID Token في PHP
`server/php/firebase_auth.php` — تحقق كامل وحقيقي (بدون أي مكتبة Composer خارجية،
فقط `curl` و `openssl` المدمجتان في PHP):
- التحقق من توقيع JWT (RS256) عبر شهادات Google الرسمية (مع تخزين مؤقت لها).
- التحقق من `iss` و `aud` مطابقين لمشروع `openou`.
- التحقق من `exp`/`iat`/`auth_time` بمنطق زمني صحيح.
- استخراج `uid` حقيقي وموثّق من الحقل `sub`.

كل نقاط النهاية المتبقية (`upload_avatar.php`, `notify.php`) تستخدم الآن
`require_bearer_token()` المحدَّثة، والتي **ترفض أي رمز غير موقّع بشكل صحيح** بدل
الاكتفاء بالتحقق من وجود Header فقط كما كان سابقًا.

### إصلاح مشكلة CI: "google-services.json is missing"
حسب السجل المرفق (`logs_88446634056.zip`)، فشل البناء لأن `app/google-services.json`
غير موجود في مستودع GitHub الفعلي (رغم وجوده في هذا الأرشيف). تم تحديث خطوة
"Restore google-services.json" في الـ workflow لتفشل برسالة عربية واضحة فورًا
بدل الانتظار حتى خطوة البناء، وتوضّح الحل: إمّا commit الملف نفسه، أو إضافة
قيمته كـ Secret باسم `GOOGLE_SERVICES_JSON`.



## ما يتبقّى (اختياري / تحسينات إضافية)

- توليد `gradle-wrapper.jar` الثنائي فعليًا (يحدث تلقائيًا عند فتح المشروع في Android Studio).
- اختبارات آلية (Unit/UI Tests) لمنطق الشعبية والتفاعلات.
- ترقيم صفحات (Pagination) للتغذية عند نمو عدد الفقرات بشكل كبير جدًا.
- Cloud Functions اختيارية لإرسال إشعارات FCM تلقائيًا عند تعليق/تيك جديد (بدل الاعتماد فقط على `notify.php` اليدوي).

---
بُني هذا الهيكل كنقطة انطلاق احترافية وقابلة للتوسّع — وليس تطبيقًا جاهزًا للنشر
على المتاجر مباشرة دون مراجعة أمنية وتجريبية شاملة.
