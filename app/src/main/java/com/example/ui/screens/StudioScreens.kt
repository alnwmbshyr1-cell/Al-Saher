package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.SlideCardModel
import com.example.model.StudioProject
import com.example.model.VideoTimelineModel
import com.example.model.WideResearchAgentOutput
import com.example.util.NotificationAndExportHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoStudioScreen(
    isBusy: Boolean,
    timeline: VideoTimelineModel?,
    savedProjects: List<StudioProject>,
    onGenerateVideo: (String, String, String, Bitmap?) -> Unit,
    onUpdateClip: (String, Int, String) -> Unit,
    onSwapMusic: (String) -> Unit,
    onAddCustomClip: (String) -> Unit,
    onSaveVideoEdits: () -> Unit,
    onLoadProject: (StudioProject) -> Unit
) {
    val context = LocalContext.current
    var prompt by rememberSaveable { mutableStateOf("") }
    val videoTypes = listOf(
        "إعلان منتج", "فيلم إطلاق", "فيديو AI UGC", "فيديو توضيحي",
        "شرح تعليمي", "فيديو ترويجي", "عرض لعبة", "فيديو كليب", "فلوج", "فيديو قصير عمودي"
    )
    var selectedType by rememberSaveable { mutableStateOf(videoTypes.first()) }
    val modes = listOf("نص إلى فيديو", "صورة إلى فيديو", "من لقطاتك وصور المنتج")
    var selectedMode by rememberSaveable { mutableStateOf(modes.first()) }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Interactive timeline playhead state
    var isPlayingPreview by remember { mutableStateOf(false) }
    var playheadSeconds by remember { mutableIntStateOf(0) }
    var selectedClipId by remember { mutableStateOf<String?>(null) }

    val totalDuration = remember(timeline) {
        timeline?.clips?.sumOf { it.durationSeconds }?.coerceAtLeast(1) ?: 15
    }

    LaunchedEffect(isPlayingPreview, totalDuration) {
        while (isPlayingPreview) {
            delay(1000L)
            playheadSeconds = if (playheadSeconds + 1 >= totalDuration) 0 else playheadSeconds + 1
        }
    }

    val activePlayClip = remember(timeline, playheadSeconds) {
        var acc = 0
        timeline?.clips?.firstOrNull { clip ->
            acc += clip.durationSeconds
            playheadSeconds < acc
        } ?: timeline?.clips?.firstOrNull()
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedBitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StudioHeader(
                title = "مولد ومحرر الفيديو الذكي",
                subtitle = "صف الفيديو بجملة واحدة: بحث الفكرة، السيناريو، اللقطات، التعليق الصوتي، الموسيقى، والترجمة في مشروع قابل للتعديل."
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                modes.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        label = { Text(mode) }
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                videoTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("صف الفيديو بجملة واحدة") },
                placeholder = { Text("مثال: إعلان فاخر لساعة ذكية مقاومة للماء بأسلوب سينمائي") },
                minLines = 2,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_prompt_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "إرفاق صورة منتج")
                    Spacer(Modifier.width(6.dp))
                    Text(if (attachedBitmap != null) "تم إرفاق صورة" else "إرفاق صورة/لقطة")
                }

                Button(
                    onClick = { onGenerateVideo(prompt, selectedType, selectedMode, attachedBitmap) },
                    enabled = !isBusy && prompt.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("generate_video_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Movie, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("إنشاء مشروع الفيديو")
                    }
                }
            }

            attachedBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "صورة المنتج المرفقة",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Video Preview & Multi-Track Timeline Editor
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeline?.title ?: "معاينة محرر الفيديو (Timeline Editor)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (timeline != null) {
                            OutlinedButton(
                                onClick = onSaveVideoEdits,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("save_video_edits_button")
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = "حفظ التعديلات", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("حفظ", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        val previewBmp = attachedBitmap
                        if (previewBmp != null) {
                            Image(
                                bitmap = previewBmp.asImageBitmap(),
                                contentDescription = "معاينة صورة الفيديو",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_video_preview),
                                contentDescription = "معاينة لقطة الفيديو",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        if (activePlayClip != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = activePlayClip.shotTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = activePlayClip?.captionText
                                    ?: "الترجمة التلقائية تظهر هنا على الإطار",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (timeline != null) {
                        // Playhead transport bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = { isPlayingPreview = !isPlayingPreview },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingPreview) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlayingPreview) "إيقاف المعاينة" else "تشغيل المعاينة",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { playheadSeconds.toFloat() / totalDuration.toFloat() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                            Text(
                                text = "${playheadSeconds}ث / ${totalDuration}ث",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TimelineTrackHeader(Icons.Filled.ContentCut, "مسار اللقطات (اضغط لقص المدة وتعديل الترجمة)")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timeline.clips.forEach { clip ->
                                val isSelected = selectedClipId == clip.id || activePlayClip?.id == clip.id
                                Surface(
                                    onClick = { selectedClipId = clip.id },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .width(200.dp)
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = clip.shotTitle,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = clip.captionText,
                                            onValueChange = { nextCap ->
                                                onUpdateClip(clip.id, clip.durationSeconds, nextCap)
                                            },
                                            label = { Text("الترجمة") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "المدة: ${clip.durationSeconds} ثوانٍ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Slider(
                                            value = clip.durationSeconds.toFloat(),
                                            onValueChange = { next ->
                                                onUpdateClip(clip.id, next.toInt(), clip.captionText)
                                            },
                                            valueRange = 2f..15f
                                        )
                                    }
                                }
                            }
                            Surface(
                                onClick = { onAddCustomClip("لقطة مضافة #${timeline.clips.size + 1}") },
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(165.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "إضافة لقطة", tint = MaterialTheme.colorScheme.primary)
                                    Text("إضافة لقطة", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        TimelineTrackHeader(Icons.Filled.RecordVoiceOver, "مسار التعليق الصوتي")
                        Text(
                            text = timeline.voiceoverScript,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        TimelineTrackHeader(Icons.Filled.MusicNote, "مسار الموسيقى: ${timeline.musicTrack}")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("إيقاع سينمائي حماسي", "موسيقى هادئة فاخرة", "نبض إلكتروني سريع").forEach { m ->
                                FilterChip(
                                    selected = timeline.musicTrack == m,
                                    onClick = { onSwapMusic(m) },
                                    label = { Text(m, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }

                        TimelineTrackHeader(Icons.Filled.GraphicEq, "المؤثرات الصوتية والترجمة")
                        Text(
                            text = timeline.sfxNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SavedProjectsSection(
                projects = savedProjects.filter { it.studioType == "VIDEO" },
                onLoadProject = onLoadProject
            )
        }
    }
}

@Composable
private fun TimelineTrackHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SlidesStudioScreen(
    isBusy: Boolean,
    slidesDeck: Pair<String, List<SlideCardModel>>?,
    savedProjects: List<StudioProject>,
    onGenerateSlides: (String, String) -> Unit
) {
    val context = LocalContext.current
    var prompt by rememberSaveable { mutableStateOf("") }
    val deckTypes = listOf("عرض استثماري Pitch Deck", "تقرير عمل", "درس تعليمي", "عرض مبيعات")
    var selectedType by rememberSaveable { mutableStateOf(deckTypes.first()) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var activeSlideIndex by remember { mutableIntStateOf(0) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val deck = slidesDeck
        if (uri != null && deck != null) {
            NotificationAndExportHelper.exportSlidesToPdfUri(context, uri, deck.first, deck.second)
                .onSuccess { exportMessage = "تم تصدير العرض بصيغة PDF بنجاح." }
                .onFailure { exportMessage = it.localizedMessage ?: "فشل التصدير." }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StudioHeader(
                title = "العروض التقديمية والشرائح الذكية",
                subtitle = "حوّل أي فكرة أو نص إلى عرض تقديمي متكامل قابل للتصدير بصيغة PDF أو المشاركة كملف PPT."
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deckTypes.forEach { t ->
                    FilterChip(
                        selected = selectedType == t,
                        onClick = { selectedType = t },
                        label = { Text(t) }
                    )
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("موضوع العرض التقديمي") },
                placeholder = { Text("مثال: عرض استثماري لتطبيق توصيل سريع في الرياض") },
                minLines = 2,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slides_prompt_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        activeSlideIndex = 0
                        onGenerateSlides(prompt, selectedType)
                    },
                    enabled = !isBusy && prompt.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("generate_slides_button")
                ) {
                    if (isBusy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Filled.PresentToAll, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("إنشاء الشرائح")
                    }
                }

                if (slidesDeck != null) {
                    OutlinedButton(
                        onClick = { pdfLauncher.launch("${slidesDeck.first.take(24)}.pdf") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "تصدير PDF")
                        Spacer(Modifier.width(4.dp))
                        Text("PDF")
                    }

                    OutlinedButton(
                        onClick = {
                            val shareText = buildString {
                                append("# ").append(slidesDeck.first).append("\n\n")
                                slidesDeck.second.forEach { s ->
                                    append("## الشريحة ${s.number}: ${s.title}\n")
                                    s.bullets.forEach { append("- $it\n") }
                                    if (s.speakerNotes.isNotBlank()) append("ملاحظات: ${s.speakerNotes}\n")
                                    append("\n")
                                }
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, slidesDeck.first)
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة وتصدير العرض التقديمي"))
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("share_ppt_outline_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "مشاركة PPT")
                    }
                }
            }

            exportMessage?.takeIf { it.isNotBlank() }?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            slidesDeck?.let { (title, slides) ->
                if (slides.isNotEmpty()) {
                    val safeIdx = activeSlideIndex.coerceIn(0, slides.lastIndex)
                    val currentSlide = slides[safeIdx]

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (safeIdx > 0) activeSlideIndex = safeIdx - 1 },
                                enabled = safeIdx > 0
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الشريحة السابقة")
                            }
                            Text("${safeIdx + 1} / ${slides.size}", style = MaterialTheme.typography.labelLarge)
                            IconButton(
                                onClick = { if (safeIdx < slides.lastIndex) activeSlideIndex = safeIdx + 1 },
                                enabled = safeIdx < slides.lastIndex
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "الشريحة التالية")
                            }
                        }
                    }

                    // Featured Interactive Slide Preview Canvas
                    Surface(
                        color = Color(0xFF0B1120),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "شريحة ${currentSlide.number}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF22D3EE)
                            )
                            Text(
                                text = currentSlide.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            currentSlide.bullets.forEach { b ->
                                Text("• $b", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE2E8F0))
                            }
                            if (currentSlide.speakerNotes.isNotBlank()) {
                                Text(
                                    text = "ملاحظات المقدم: ${currentSlide.speakerNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            SavedProjectsSection(
                projects = savedProjects.filter { it.studioType == "SLIDES" },
                onLoadProject = {}
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocsResearchStudioScreen(
    isBusy: Boolean,
    wideOutputs: List<WideResearchAgentOutput>,
    docResult: Pair<String, String>?,
    savedProjects: List<StudioProject>,
    onRunDocsOrResearch: (String, String, Boolean) -> Unit,
    onLoadProject: (StudioProject) -> Unit
) {
    var topic by rememberSaveable { mutableStateOf("") }
    val subTypes = listOf("تقرير احترافي", "تحليل جدول بيانات", "تلخيص PDF ومستندات", "بحث موسع متوازي")
    var selectedType by rememberSaveable { mutableStateOf(subTypes.first()) }
    var useWideResearch by rememberSaveable { mutableStateOf(true) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StudioHeader(
                title = "المستندات والجداول والبحث الموسع (Wide Research)",
                subtitle = "كتابة المستندات، تحليل الجداول، تلخيص PDF، وتشغيل عدة وكلاء بالتوازي للبحوث الكبيرة."
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                subTypes.forEach { t ->
                    FilterChip(
                        selected = selectedType == t,
                        onClick = {
                            selectedType = t
                            if (t == "بحث موسع متوازي") useWideResearch = true
                        },
                        label = { Text(t) }
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تفعيل البحث الموسع المتوازي (Wide Research)", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "تشغيل 3 وكلاء ذكاء اصطناعي بالتوازي (السوق + المنافسون + الجدوى)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = useWideResearch, onCheckedChange = { useWideResearch = it })
                }
            }

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("أدخل الموضوع أو الصق بيانات الجدول أو النص") },
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("docs_research_input")
            )

            Button(
                onClick = { onRunDocsOrResearch(topic, selectedType, useWideResearch) },
                enabled = !isBusy && topic.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("run_docs_research_button")
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else {
                    Icon(Icons.Filled.TravelExplore, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (useWideResearch) "بدء البحث الموسع المتوازي" else "تحليل وإنشاء المستند")
                }
            }

            if (wideOutputs.isNotEmpty()) {
                wideOutputs.forEach { agent ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(agent.agentName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(agent.findings, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else if (docResult != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(docResult.first, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(docResult.second, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            SavedProjectsSection(
                projects = savedProjects.filter { it.studioType == "DOCS" },
                onLoadProject = onLoadProject
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuilderStudioScreen(
    studioType: String,
    isBusy: Boolean,
    generatedDoc: Pair<String, String>?,
    savedProjects: List<StudioProject>,
    onGenerate: (String, String, String) -> Unit,
    onLoadProject: (StudioProject) -> Unit
) {
    var prompt by rememberSaveable(studioType) { mutableStateOf("") }
    val subTypes = when (studioType) {
        "WEB" -> listOf("صفحة هبوط", "متجر إلكتروني", "معرض أعمال", "موقع حجوزات", "تطبيق ويب كامل + مدفوعات")
        "APP" -> listOf("تطبيق iOS & Android متكامل", "تطبيق توصيل وطلبات", "تطبيق مالي وبنكي", "تطبيق تواصل")
        else -> listOf("لعبة منصات Platformer", "لعبة ألغاز", "لعبة سباقات", "لعبة أركيد", "لعبة 3D")
    }
    var selectedSubType by rememberSaveable(studioType) { mutableStateOf(subTypes.first()) }

    val (headerTitle, headerSub) = when (studioType) {
        "WEB" -> "منشئ المواقع وتطبيقات الويب" to "أنشئ مواقع كاملة من طلب واحد: صفحات هبوط، متاجر، حجوزات، وتسجيل دخول ومدفوعات."
        "APP" -> "منشئ تطبيقات الجوال (iOS & Android)" to "صف فكرة التطبيق لبناء الواجهة الأمامية والخلفية وقاعدة البيانات."
        else -> "صانع الألعاب الذكي (AI Game Maker)" to "صف أسلوب اللعب والعالم والفن لبناء لعبة بقواعدها ورسومها وصوتها + معاينة قابلة للعب."
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StudioHeader(title = headerTitle, subtitle = headerSub)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                subTypes.forEach { t ->
                    FilterChip(
                        selected = selectedSubType == t,
                        onClick = { selectedSubType = t },
                        label = { Text(t) }
                    )
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("صف الفكرة بالتفصيل") },
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("builder_prompt_input")
            )

            Button(
                onClick = { onGenerate(studioType, selectedSubType, prompt) },
                enabled = !isBusy && prompt.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("builder_generate_button")
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else {
                    Icon(if (studioType == "GAME") Icons.Filled.SportsEsports else Icons.Filled.Code, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("بناء المشروع الآن")
                }
            }

            if (studioType == "GAME") {
                PlayableMiniArcadeWidget()
            }

            if (generatedDoc != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(generatedDoc.first, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(generatedDoc.second, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            SavedProjectsSection(
                projects = savedProjects.filter { it.studioType == studioType },
                onLoadProject = onLoadProject
            )
        }
    }
}

@Composable
private fun PlayableMiniArcadeWidget() {
    var running by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var bestScore by remember { mutableIntStateOf(0) }
    var playerY by remember { mutableFloatStateOf(0.74f) }
    var velocityY by remember { mutableFloatStateOf(0f) }
    var obstacleX by remember { mutableFloatStateOf(0.95f) }
    var coinX by remember { mutableFloatStateOf(0.65f) }

    LaunchedEffect(running) {
        while (running) {
            delay(24L)
            obstacleX -= 0.022f
            coinX -= 0.019f
            if (obstacleX < 0f) {
                obstacleX = 1f
                score += 10
                if (score > bestScore) bestScore = score
            }
            if (coinX < 0f) {
                coinX = 1.05f
            }
            playerY += velocityY
            velocityY += 0.0045f
            if (playerY >= 0.74f) {
                playerY = 0.74f
                velocityY = 0f
            }
            if (coinX in 0.12f..0.24f && playerY < 0.60f) {
                score += 25
                if (score > bestScore) bestScore = score
                coinX = 1.15f
            }
            if (obstacleX in 0.13f..0.23f && playerY > 0.62f) {
                running = false
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("معاينة أركيد قابلة للعب (اضغط للقفز)", style = MaterialTheme.typography.labelLarge)
                Text("النقاط: $score • الأعلى: $bestScore", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF070B14))
                    .clickable {
                        if (!running) {
                            score = 0
                            obstacleX = 0.95f
                            coinX = 0.65f
                            playerY = 0.74f
                            velocityY = 0f
                            running = true
                        } else if (playerY >= 0.72f) {
                            velocityY = -0.068f
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = Color(0xFF22D3EE),
                        start = Offset(0f, size.height * 0.85f),
                        end = Offset(size.width, size.height * 0.85f),
                        strokeWidth = 3f
                    )
                    drawCircle(
                        color = Color(0xFFFACC15),
                        radius = 12f,
                        center = Offset(size.width * coinX, size.height * 0.44f)
                    )
                    drawRoundRect(
                        color = Color(0xFF22D3EE),
                        topLeft = Offset(size.width * 0.15f, size.height * playerY),
                        size = Size(28f, 28f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0xFFF43F5E),
                        topLeft = Offset(size.width * obstacleX, size.height * 0.72f),
                        size = Size(22f, 36f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
                if (!running) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF22D3EE))
                            Text("اضغط للبدء والقفز", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SavedProjectsSection(
    projects: List<StudioProject>,
    onLoadProject: (StudioProject) -> Unit
) {
    if (projects.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("المشاريع المحفوظة (اضغط للفتح والتعديل)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        projects.forEach { p ->
            Surface(
                onClick = { onLoadProject(p) },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(p.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(p.subType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = p.tracksSummary.ifBlank { p.content.take(120) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
