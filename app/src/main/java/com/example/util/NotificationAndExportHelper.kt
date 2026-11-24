package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.SlideCardModel

object NotificationAndExportHelper {
    private const val CHANNEL_ID = "al_saher_agent_updates"

    fun showAgentCompletionNotification(context: Context, title: String, summary: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "إشعارات الوكيل الساحر",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات اكتمال مهام الوكيل السحابي في الخلفية"
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("الساحر: اكتملت المهمة — $title")
            .setContentText(summary.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary.take(400)))
            .setAutoCancel(true)
            .build()

        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    fun exportSlidesToPdfUri(
        context: Context,
        uri: Uri,
        deckTitle: String,
        slides: List<SlideCardModel>
    ): Result<Unit> {
        return try {
            val pdf = PdfDocument()
            val pageWidth = 842
            val pageHeight = 595

            val bgPaint = Paint().apply { color = Color.parseColor("#0B1120") }
            val accentPaint = Paint().apply { color = Color.parseColor("#22D3EE") }
            val titlePaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 26f
                isFakeBoldText = true
            }
            val bodyPaint = TextPaint().apply {
                color = Color.parseColor("#E2E8F0")
                textSize = 16f
            }
            val notePaint = TextPaint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 12f
            }

            slides.forEachIndexed { idx, slide ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx + 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
                canvas.drawRect(40f, 40f, 48f, 110f, accentPaint)

                canvas.drawText("$deckTitle  •  ${slide.number}/${slides.size}", 64f, 65f, notePaint)
                canvas.drawText(slide.title, 64f, 102f, titlePaint)

                val bulletsText = slide.bullets.joinToString("\n\n") { "• $it" }
                val staticLayout = StaticLayout.Builder.obtain(
                    bulletsText, 0, bulletsText.length, bodyPaint, pageWidth - 128
                ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

                canvas.save()
                canvas.translate(64f, 145f)
                staticLayout.draw(canvas)
                canvas.restore()

                if (slide.speakerNotes.isNotBlank()) {
                    canvas.drawText("ملاحظات: ${slide.speakerNotes.take(90)}", 64f, pageHeight - 40f, notePaint)
                }

                pdf.finishPage(page)
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdf.writeTo(out)
            } ?: error("تعذر فتح مسار الحفظ")
            pdf.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
