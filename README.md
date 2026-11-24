# الساحر — Al-Saher

تطبيق أندرويد متكامل مبني بلغة **Kotlin** وواجهة **Jetpack Compose (Material Design 3)** مع دعم كامل للغة العربية (RTL)، ومرتبط بخدمات **Firebase** (Authentication وFirestore وApp Check وFirebase AI Logic).

## أبرز المزايا
- **الوكيل الذكي والمحادثة المباشرة (`ChatScreen` & `CenterChatPane`):** محادثة متعددة الأدوار مع تدفق فوري للردود عبر `Firebase.ai` ودعم النماذج `gemini-3.8-flash` و`gemini-3.5-flash-lite` و`gemini-4-argon` و`gemini-4-hydrogen`.
- **البحث الحي والخرائط:** تفعيل `Tool.googleSearch()` و`Tool.googleMaps()` مع عرض المصادر والمواقع.
- **المحادثة الصوتية الحية والتفريغ الصوتي:** جلسات صوتية ثنائية الاتجاه عبر `gemini-3.8-live` وتفريغ التسجيلات الصوتية عبر `gemini-3.5-transcribe`.
- **استوديو الصور (`ImageStudioScreen`):** توليد وتعديل الصور عبر `gemini-nano-banana-2.1` مع اختيار نسبة الأبعاد والحفظ في المعرض.
- **تصدير السجل والمزامنة السحابية:** تخزين سحابي آمن عبر Cloud Firestore وتصدير المحادثات بصيغتي `JSON` و`TXT`.

## هيكل المشروع الرئيسي
- `app/src/main/java/com/example/MainActivity.kt` — نقطة التشغيل وتهيئة App Check والمصادقة.
- `app/src/main/java/com/example/ui/screens/ChatScreen.kt` — واجهة المحادثة المباشرة مع وكيل Firebase AI.
- `app/src/main/java/com/example/ui/screens/ImageStudioScreen.kt` — استوديو إنشاء وتعديل الصور.
- `app/src/main/java/com/example/data/AlSaherRepository.kt` — طبقة البيانات والمزامنة مع Firestore.
- `app/src/main/java/com/example/viewmodel/ChatViewModel.kt` و`AlSaherViewModel.kt` — إدارة الحالة والاتصال بنماذج Gemini.
- `build.gradle.kts` و`app/build.gradle.kts` — إعدادات البناء والاعتماديات.

## البناء والتشغيل
```bash
gradle assembleDebug
gradle :app:testDebugUnitTest
```
