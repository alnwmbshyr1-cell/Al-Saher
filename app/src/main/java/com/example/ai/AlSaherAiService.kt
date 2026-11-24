package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.ChatbotRole
import com.example.model.GeminiChatTier
import com.example.model.ImageAspectOption
import com.example.model.SlideCardModel
import com.example.model.VideoTimelineClip
import com.example.model.VideoTimelineModel
import com.example.model.WideResearchAgentOutput
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AgentPlanResult(
    val title: String,
    val category: String,
    val steps: List<String>,
    val summary: String,
    val deliverableTitle: String,
    val deliverableContent: String
)

class AlSaherAiService {

    // FIX: موديلات حقيقية موجودة في Firebase AI Logic
    private companion object {
        const val MODEL_CHAT = "gemini-2.5-flash"
        const val MODEL_AGENT = "gemini-2.5-flash"
        const val MODEL_VIDEO = "gemini-2.5-flash"
        const val MODEL_SLIDES = "gemini-2.5-flash"
        const val MODEL_IMAGE = "gemini-2.0-flash-preview-image-generation"
        const val MODEL_TRANSCRIBE = "gemini-2.5-flash"
        const val MODEL_LIVE = "gemini-2.0-flash-live-001"
        const val TAG = "AlSaherAi"
    }

    private fun buildSystemInstruction(role: ChatbotRole) = content {
        text(role.systemRoleInstruction)
    }

    private fun getChatModel(
        tier: GeminiChatTier,
        role: ChatbotRole,
        enableSearchGrounding: Boolean,
        enableMapsGrounding: Boolean = false
    ): GenerativeModel {
        val tools = buildList {
            if (enableSearchGrounding) add(Tool.googleSearch())
            if (enableMapsGrounding) add(Tool.googleMaps())
        }
        // FIX: لا نستخدم tier.modelName لو كان موديل وهمي، نستخدم موديل حقيقي
        val resolvedModelName = MODEL_CHAT
        return Firebase.ai.generativeModel(
            modelName = resolvedModelName,
            systemInstruction = buildSystemInstruction(role),
            tools = tools
        )
    }

    private val chatModel by lazy {
        getChatModel(GeminiChatTier.GENERAL, ChatbotRole.GENERAL_ASSISTANT, enableSearchGrounding = false)
    }

    private val searchChatModel by lazy {
        getChatModel(GeminiChatTier.GENERAL, ChatbotRole.RESEARCH_ANALYST, enableSearchGrounding = true)
    }

    private val agentSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "عنوان مختصر للمهمة بالعربية"),
            "category" to Schema.enumeration(
                values = listOf("GENERAL", "VIDEO", "SLIDES", "DOCS", "WEB", "APP", "GAME", "RESEARCH", "REMOTE"),
                description = "تصنيف المهمة"
            ),
            "steps" to Schema.array(
                items = Schema.string(),
                description = "قائمة من 4 إلى 6 خطوات تنفيذ عملية واضحة على الحاسوب السحابي"
            ),
            "summary" to Schema.string(description = "ملخص تنفيذي احترافي للعمل المنجز بالعربية"),
            "deliverableTitle" to Schema.string(description = "عنوان النتيجة النهائية القابلة للتسليم"),
            "deliverableContent" to Schema.string(description = "محتوى النتيجة النهائية الكامل والمفصل والجاهز للاستخدام")
        )
    )

    private val agentModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_AGENT,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = agentSchema
            }
        )
    }

    private val videoSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(),
            "voiceoverScript" to Schema.string(description = "نص التعليق الصوتي الكامل بالعربية"),
            "musicTrack" to Schema.string(description = "وصف الموسيقى الخلفية والإيقاع"),
            "sfxNotes" to Schema.string(description = "مؤثرات صوتية مقترحة"),
            "clips" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "shotTitle" to Schema.string(),
                        "visualPrompt" to Schema.string(),
                        "durationSeconds" to Schema.integer(),
                        "captionText" to Schema.string()
                    )
                )
            )
        )
    )

    private val videoModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_VIDEO,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = videoSchema
            }
        )
    }

    private val slidesSchema = Schema.obj(
        properties = mapOf(
            "deckTitle" to Schema.string(),
            "slides" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "number" to Schema.integer(),
                        "title" to Schema.string(),
                        "bullets" to Schema.array(items = Schema.string()),
                        "speakerNotes" to Schema.string()
                    )
                )
            )
        )
    )

    private val slidesModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_SLIDES,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = slidesSchema
            }
        )
    }

    fun streamFreeChat(
        prompt: String,
        historyContext: List<Pair<String, String>>,
        tier: GeminiChatTier = GeminiChatTier.GENERAL,
        role: ChatbotRole = ChatbotRole.GENERAL_ASSISTANT,
        enableSearchGrounding: Boolean = false,
        enableMapsGrounding: Boolean = false
    ): Flow<String> = flow {
        try {
            val model = getChatModel(tier, role, enableSearchGrounding, enableMapsGrounding)
            val mergedHistory = mutableListOf<Pair<String, String>>()
            historyContext.takeLast(12).forEach { (msgRole, text) ->
                val clean = text.trim()
                if (clean.isEmpty()) return@forEach
                val geminiRole = if (msgRole == "USER") "user" else "model"
                if (mergedHistory.isEmpty() && geminiRole != "user") return@forEach
                if (mergedHistory.lastOrNull()?.first == geminiRole) {
                    val prev = mergedHistory.removeAt(mergedHistory.lastIndex)
                    mergedHistory.add(geminiRole to "${prev.second}\n$clean")
                } else {
                    mergedHistory.add(geminiRole to clean)
                }
            }
            if (mergedHistory.lastOrNull()?.first == "user") {
                mergedHistory.removeAt(mergedHistory.lastIndex)
            }
            val chatHistory = mergedHistory.map { (geminiRole, text) ->
                content(geminiRole) { text(text) }
            }
            val chat = model.startChat(history = chatHistory)
            val sb = StringBuilder()
            val searchCitations = linkedSetOf<String>()
            val mapsCitations = linkedSetOf<String>()
            withTimeout(60_000L) {
                chat.sendMessageStream(prompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        sb.append(chunkText)
                        emit(sb.toString())
                    }
                    chunk.candidates.firstOrNull()?.groundingMetadata?.groundingChunks?.forEach { gChunk ->
                        val web = gChunk.web
                        if (web != null) {
                            val title = web.title?.takeIf { it.isNotBlank() } ?: web.domain.orEmpty()
                            val uri = web.uri.orEmpty()
                            if (title.isNotBlank() && uri.isNotBlank()) {
                                searchCitations.add("• $title ($uri)")
                            }
                        }
                        val maps = gChunk.maps
                        if (maps != null) {
                            val title = maps.title?.takeIf { it.isNotBlank() } ?: "موقع على خرائط Google"
                            val uri = maps.uri.orEmpty()
                            if (uri.isNotBlank()) mapsCitations.add("• $title ($uri)")
                            else mapsCitations.add("• $title")
                        }
                    }
                }
            }
            if (searchCitations.isNotEmpty()) {
                sb.append("\n\n🔎 مصادر بحث Google:\n")
                searchCitations.take(4).forEach { sb.append(it).append("\n") }
            }
            if (mapsCitations.isNotEmpty()) {
                sb.append("\n\n📍 مواقع خرائط Google:\n")
                mapsCitations.take(4).forEach { sb.append(it).append("\n") }
            }
            if (searchCitations.isNotEmpty() || mapsCitations.isNotEmpty()) {
                emit(sb.toString().trimEnd())
            }
            if (sb.isBlank()) {
                emit("عذراً، لم أستطع توليد رد الآن. حاول مرة أخرى.")
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            Log.e(TAG, "streamFreeChat failed", e)
            emit("حدث خطأ في الاتصال بالذكاء الاصطناعي: ${e.localizedMessage ?: "حاول مرة أخرى"}")
        }
    }

    suspend fun planAndExecuteAgentTask(prompt: String, connectedApps: List<String>): AgentPlanResult {
        return try {
            val connectorsText = if (connectedApps.isEmpty()) "لا توجد موصلات متصلة حالياً"
            else connectedApps.joinToString("، ")
            val fullPrompt = """
                أنت الوكيل الذكي العام (الساحر). لديك حاسوب سحابي لتنفيذ المهام وتقديم مخرجات كاملة جاهزة للاستخدام.
                الموصلات المتصلة حالياً: $connectorsText
                طلب المستخدم: $prompt
                أنشئ خطة عمل متدرجة (4 إلى 6 خطوات) ثم قدّم ملخصاً ومخرجاً نهائياً عملياً ومتكاملاً باللغة العربية.
            """.trimIndent()
            val response = withTimeout(60_000L) { agentModel.generateContent(fullPrompt) }
            val raw = response.text ?: throw IllegalStateException("لم يُرجع الوكيل أي بيانات.")
            // FIX: لو رجع نص مو JSON، لا نطفي، نرجع نتيجة افتراضية
            val json = try { JSONObject(raw) } catch (e: Exception) {
                JSONObject().put("title", prompt.take(50)).put("summary", raw.take(2000))
            }
            val stepsArr = json.optJSONArray("steps")
            val steps = if (stepsArr != null) {
                (0 until stepsArr.length()).map { stepsArr.getString(it) }
            } else emptyList()
            AgentPlanResult(
                title = json.optString("title", prompt.take(50)),
                category = json.optString("category", "GENERAL"),
                steps = steps.ifEmpty {
                    listOf("تحليل الطلب", "البحث وجمع المعطيات", "بناء المخرج النهائي", "المراجعة والتسليم")
                }.take(10),
                summary = json.optString("summary", ""),
                deliverableTitle = json.optString("deliverableTitle", "النتيجة النهائية"),
                deliverableContent = json.optString("deliverableContent", raw.take(4000))
            )
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            Log.e(TAG, "planAndExecuteAgentTask failed", e)
            AgentPlanResult(
                title = prompt.take(50),
                category = "GENERAL",
                steps = listOf("تحليل الطلب", "البحث وجمع المعطيات", "بناء المخرج النهائي", "المراجعة والتسليم"),
                summary = "تعذر الاتصال بالذكاء الاصطناعي: ${e.localizedMessage}",
                deliverableTitle = "النتيجة النهائية",
                deliverableContent = "حاول مرة أخرى لاحقاً."
            )
        }
    }

    suspend fun generateVideoProject(
        prompt: String,
        videoType: String,
        creationMode: String,
        attachedImage: Bitmap?
    ): VideoTimelineModel {
        return try {
            val textPrompt = """
                أنشئ مشروع فيديو قابل للتعديل باللغة العربية.
                النمط: $creationMode
                نوع الفيديو: $videoType
                الوصف: $prompt
                أخرج 4 إلى 5 لقطات متتالية مع نص التعليق الصوتي والموسيقى والمؤثرات الصوتية والترجمة لكل لقطة.
            """.trimIndent()
            val response = withTimeout(60_000L) {
                if (attachedImage != null) {
                    videoModel.generateContent(content { image(attachedImage); text(textPrompt) })
                } else {
                    videoModel.generateContent(textPrompt)
                }
            }
            val raw = response.text ?: throw IllegalStateException("تعذر توليد مشروع الفيديو.")
            val json = JSONObject(raw)
            val clipsArr = json.optJSONArray("clips")
            val clips = if (clipsArr != null) {
                (0 until clipsArr.length()).map { i ->
                    val obj = clipsArr.getJSONObject(i)
                    VideoTimelineClip(
                        id = "clip_${i + 1}",
                        shotTitle = obj.optString("shotTitle", "لقطة ${i + 1}"),
                        visualPrompt = obj.optString("visualPrompt", ""),
                        durationSeconds = obj.optInt("durationSeconds", 5).coerceIn(2, 20),
                        captionText = obj.optString("captionText", "")
                    )
                }
            } else emptyList()
            VideoTimelineModel(
                title = json.optString("title", "مشروع فيديو جديد"),
                videoType = videoType,
                voiceoverScript = json.optString("voiceoverScript", ""),
                musicTrack = json.optString("musicTrack", "موسيقى سينمائية عصرية"),
                sfxNotes = json.optString("sfxNotes", "انتقالات صوتية ناعمة"),
                clips = clips.ifEmpty { listOf(VideoTimelineClip("clip_1", "لقطة افتتاحية", prompt, 5, "")) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "generateVideoProject failed", e)
            VideoTimelineModel(
                title = "مشروع فيديو جديد",
                videoType = videoType,
                voiceoverScript = prompt,
                musicTrack = "موسيقى سينمائية عصرية",
                sfxNotes = "انتقالات صوتية ناعمة",
                clips = listOf(VideoTimelineClip("clip_1", "لقطة افتتاحية", prompt, 5, ""))
            )
        }
    }

    suspend fun generateSlideDeck(prompt: String, deckType: String): Pair<String, List<SlideCardModel>> {
        return try {
            val fullPrompt = """
                أنشئ عرضاً تقديمياً احترافياً متكاملاً باللغة العربية.
                نوع العرض: $deckType
                الموضوع: $prompt
                أخرج 5 إلى 7 شرائح مرتبة مع نقاط واضحة وملاحظات للمقدم.
            """.trimIndent()
            val response = withTimeout(60_000L) { slidesModel.generateContent(fullPrompt) }
            val raw = response.text ?: throw IllegalStateException("تعذر توليد العرض التقديمي.")
            val json = JSONObject(raw)
            val deckTitle = json.optString("deckTitle", "عرض تقديمي")
            val arr = json.optJSONArray("slides")
            val slides = if (arr != null) {
                (0 until arr.length()).map { i ->
                    val s = arr.getJSONObject(i)
                    val bArr = s.optJSONArray("bullets")
                    val bullets = if (bArr != null) (0 until bArr.length()).map { bIdx -> bArr.getString(bIdx) } else emptyList()
                    SlideCardModel(
                        number = s.optInt("number", i + 1),
                        title = s.optString("title", "شريحة ${i + 1}"),
                        bullets = bullets,
                        speakerNotes = s.optString("speakerNotes", "")
                    )
                }
            } else emptyList()
            deckTitle to slides.ifEmpty { listOf(SlideCardModel(1, prompt.take(60), listOf(prompt), "")) }
        } catch (e: Exception) {
            Log.e(TAG, "generateSlideDeck failed", e)
            "عرض تقديمي" to listOf(SlideCardModel(1, prompt.take(60), listOf(prompt), ""))
        }
    }

    suspend fun runWideResearch(topic: String): List<WideResearchAgentOutput> = coroutineScope {
        val tracks = listOf(
            "وكيل تحليل السوق والجمهور" to "حلل السوق والطلب والفرص الرئيسية حول: $topic",
            "وكيل تحليل المنافسين والمقارنة" to "قارن أبرز الحلول والمنافسين ونقاط القوة والضعف حول: $topic",
            "وكيل الجدوى التقنية والتنفيذ" to "اقترح الخطة التقنية والتنفيذية والمخاطر والتكاليف حول: $topic"
        )
        withTimeout(60_000L) {
            tracks.map { (agentName, agentPrompt) ->
                async {
                    try {
                        val res = searchChatModel.generateContent(
                            "أنت $agentName ضمن نظام البحث الموسع المتوازي. أجب بنقاط مركزة ومرتبة بالعربية (4-6 نقاط):\n$agentPrompt"
                        )
                        WideResearchAgentOutput(
                            agentName = agentName,
                            focusArea = agentPrompt.substringBefore(":"),
                            findings = res.text ?: "لا توجد نتائج"
                        )
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        Log.e(TAG, "WideResearch sub-agent failed: $agentName", e)
                        WideResearchAgentOutput(
                            agentName = agentName,
                            focusArea = "خطأ جزئي",
                            findings = e.localizedMessage ?: "تعذر إكمال هذا المسار"
                        )
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun generateStudioArtifact(studioInstruction: String): String {
        return try {
            val response = withTimeout(60_000L) { chatModel.generateContent(studioInstruction) }
            response.text ?: "لم يرجع النموذج محتوى."
        } catch (e: Exception) {
            Log.e(TAG, "generateStudioArtifact failed", e)
            "تعذر توليد المحتوى: ${e.localizedMessage}"
        }
    }

    suspend fun generateOrEditImage(
        prompt: String,
        inputBitmap: Bitmap?,
        aspectOption: ImageAspectOption
    ): Bitmap {
        val targetAspect = when (aspectOption) {
            ImageAspectOption.SQUARE_1X1 -> AspectRatio.SQUARE_1x1
            ImageAspectOption.LANDSCAPE_16X9 -> AspectRatio.LANDSCAPE_16x9
            ImageAspectOption.PORTRAIT_9X16 -> AspectRatio.PORTRAIT_9x16
        }
        val imageModel = Firebase.ai.generativeModel(
            modelName = MODEL_IMAGE,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(targetAspect)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )
        val response = withTimeout(60_000L) {
            if (inputBitmap != null) {
                imageModel.generateContent(content { image(inputBitmap); text(prompt) })
            } else {
                imageModel.generateContent(prompt)
            }
        }
        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
        return imagePart?.image
            ?: throw IllegalStateException("لم يُرجع النموذج أي صورة. تأكد من تفعيل خطة Blaze أو جرّب وصفاً أوضح.")
    }

    private val transcribeModel by lazy {
        Firebase.ai.generativeModel(modelName = MODEL_TRANSCRIBE)
    }

    suspend fun transcribeAudioBytes(audioBytes: ByteArray, mimeType: String = "audio/mp4"): String {
        require(audioBytes.isNotEmpty()) { "التسجيل الصوتي فارغ." }
        require(audioBytes.size <= 19 * 1024 * 1024) { "حجم التسجيل يتجاوز الحد المسموح (20 ميجابايت)." }
        return try {
            val response = withTimeout(60_000L) {
                transcribeModel.generateContent(
                    content {
                        inlineData(audioBytes, mimeType)
                        text("فرّغ هذا التسجيل الصوتي إلى نص دقيق بنفس اللغة المنطوقة دون تلخيص أو إضافات.")
                    }
                )
            }
            response.text?.trim()?.takeIf { it.isNotEmpty() }
                ?: "لم يرجع نموذج التفريغ الصوتي أي نص."
        } catch (e: Exception) {
            Log.e(TAG, "transcribe failed", e)
            "تعذر تفريغ الصوت: ${e.localizedMessage}"
        }
    }

    @OptIn(PublicPreviewAPI::class)
    suspend fun connectLiveVoiceSession(role: ChatbotRole): LiveSession {
        val liveModel = Firebase.ai.liveModel(
            modelName = MODEL_LIVE,
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO
                inputAudioTranscription = AudioTranscriptionConfig()
                outputAudioTranscription = AudioTranscriptionConfig()
            },
            systemInstruction = buildSystemInstruction(role)
        )
        return withTimeout(30_000L) { liveModel.connect() }
    }
}
