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

### طبقة تحقق ثانية من جهة الخادم (اختيارية)
`server/php/base64_image.php` — نقطة نهاية PHP قوية تُستخدم إن رغبت بمسار خادمي
إضافي: تتحقق من **البصمة الحقيقية** للصورة (magic bytes عبر `getimagesizefromstring`،
وليس مجرد الوثوق بالامتداد)، ترفض أي حمولة غير صورة فعلية، وتعيد ضغطًا تكيّفيًا
مطابقًا لمنطق `ImageCodec.kt`.

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

## ما يحتاج استكمالًا لاحقًا

- ربط `currentReaction` في `FeedScreen` بحالة تفاعل المستخدم الفعلية من `/reactions`.
- شاشة تعديل الغرفة الكاملة (السيرة، اسم المجتمع، تغيير البانر عبر `ImagePickerButton` + `ImageProfile.BANNER`).
- توليد `gradlew`/`gradlew.bat` الفعليين (ملفات ثنائية لم تُدرج هنا).
- توقيع حقيقي لنسخة Release (متغيرات `OPOU_KEYSTORE_*` في CI).
- تحقق فعلي من Firebase ID Token داخل ملفات PHP (حاليًا فقط تحقق من وجود Bearer Token).

---
بُني هذا الهيكل كنقطة انطلاق احترافية وقابلة للتوسّع — وليس تطبيقًا جاهزًا للنشر
على المتاجر مباشرة دون مراجعة أمنية وتجريبية شاملة.
