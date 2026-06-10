package com.example.api

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.ui.DuniaViewModel
import com.example.ui.MonthlyOverview
import com.example.ui.HealthScore
import com.example.data.TransactionEntity
import com.example.data.SavingGoalEntity
import com.example.data.CicilanEntity
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportGenerator {

    private fun formatRupiah(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            // Remove currency symbol and fractional parts for cleaner printing in reports
            format.format(amount).replace("Rp", "Rp ").replace(",00", "")
        } catch (e: Exception) {
            "Rp " + String.format(Locale.getDefault(), "%,.0f", amount)
        }
    }

    /**
     * Generates a structural, highly polished, and professional multi-page PDF financial report using
     * native android.graphics.pdf.PdfDocument. It features custom vector-drawn visual charts, thematic
     * styling (Navy-Teal accents), structured data columns, and customized advisory algorithms.
     */
    fun generateProfessionalPdf(context: Context, viewModel: DuniaViewModel): Uri? {
        val pdfDocument = PdfDocument()
        
        // Use standard A4 page dimensions in PostScript points: 595 width x 842 height
        val pageInfoPage1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val pageInfoPage2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        
        val stats = viewModel.monthlyStats.value
        val health = viewModel.financialHealthScore.value
        val transactions = viewModel.transactions.value
        val goals = viewModel.savingGoals.value
        val debts = viewModel.cicilanList.value
        
        val weddingYear = viewModel.configs.value["TARGET_NIKAH"] ?: "2029"
        val haikalJob = viewModel.configs.value["PEKERJAAN_HAIKAL"] ?: "Staff"
        val ummuJob = viewModel.configs.value["PEKERJAAN_UMMU"] ?: "Staff"

        val currentDateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        
        // Colors & Paint configurations
        val paint = Paint()
        
        // ---------------- PAGE 1: EXECUTIVE BRIEFING, FINANCIAL STATS & VISUAL ANALYTICAL CHART ----------------
        val page1 = pdfDocument.startPage(pageInfoPage1)
        val canvas1 = page1.canvas
        
        // A. Draw Header Banner
        paint.color = Color.parseColor("#004D40") // Deep Teal
        canvas1.drawRect(40f, 40f, 555f, 130f, paint)
        
        // Header Text
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 15f
        canvas1.drawText("LAPORAN ANALISIS FINANSIAL & SINERGI DUNIA", 55f, 75f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.parseColor("#B2DFDB") // Light Mint
        canvas1.drawText("Fase 0: Fondasi Masa Depan Bersama • Rencana Target Nikah: $weddingYear", 55f, 95f, paint)
        canvas1.drawText("Dunia Bersama Haikal ($haikalJob) & Ummu ($ummuJob)", 55f, 112f, paint)
        
        // Right Head Tag
        paint.color = Color.parseColor("#FFD54F") // Amber
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        canvas1.drawText("EDISI PREMIUM V1", 440f, 85f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 7f
        canvas1.drawText("Real-time & Trusted", 440f, 100f, paint)
        
        // B. Print Meta Information
        paint.color = Color.parseColor("#374151") // Dark slate gray
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        canvas1.drawText("Dicetak Tanggal : $currentDateStr", 40f, 155f, paint)
        
        paint.color = Color.parseColor("#10B981") // Active emerald green
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        canvas1.drawText("Status Koneksi  : Tersinkronisasi Aktif (Nirkabel Cloud)", 330f, 155f, paint)
        
        // Draw Divider
        paint.color = Color.parseColor("#E5E7EB") // Border light gray
        canvas1.drawLine(40f, 168f, 555f, 168f, paint)
        
        // C. Two Executive Financial Summary Cards (Joint Cashflow vs Financial Health Score)
        // LEFT CARD: Cashflow
        paint.color = Color.parseColor("#F0FDF4") // Soft green bg
        val leftCard = RectF(40f, 180f, 285f, 310f)
        canvas1.drawRoundRect(leftCard, 8f, 8f, paint)
        
        paint.color = Color.parseColor("#0F766E") // Dark Teal border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas1.drawRoundRect(leftCard, 8f, 8f, paint)
        paint.style = Paint.Style.FILL // Reset to fill
        
        paint.color = Color.parseColor("#111827") // Near black
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        canvas1.drawText("1. ARUS KAS GABUNGAN BULANAN", 52f, 202f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        paint.color = Color.parseColor("#374151")
        canvas1.drawText("Total Pemasukan       :", 52f, 222f, paint)
        canvas1.drawText("Total Pengeluaran      :", 52f, 238f, paint)
        
        paint.color = Color.parseColor("#059669") // Green
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText(formatRupiah(stats.totalIncome), 175f, 222f, paint)
        
        paint.color = Color.parseColor("#EF4444") // Red
        canvas1.drawText(formatRupiah(stats.totalExpense), 175f, 238f, paint)
        
        paint.color = Color.parseColor("#D1D5DB")
        canvas1.drawLine(52f, 252f, 273f, 252f, paint)
        
        paint.color = Color.parseColor("#1F2937")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("ARUS KAS BERSIH (SURPLUS)", 52f, 271f, paint)
        
        paint.color = Color.parseColor("#059669")
        paint.textSize = 10f
        canvas1.drawText(formatRupiah(stats.surplus), 52f, 289f, paint)
        
        val sRate = health.savingRate
        paint.color = if (sRate >= 30) Color.parseColor("#059669") else Color.parseColor("#F59E0B")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("Saving Rate: $sRate% (${if (sRate >= 30) "SEHAT PRIMA" else "OPTIMALKAN"})", 172f, 287f, paint)
        
        // RIGHT CARD: Financial Health Score
        paint.color = Color.parseColor("#F8FAFC") // Slate card bg
        val rightCard = RectF(310f, 180f, 555f, 310f)
        canvas1.drawRoundRect(rightCard, 8f, 8f, paint)
        
        paint.color = Color.parseColor("#475569") // Grey border
        paint.style = Paint.Style.STROKE
        canvas1.drawRoundRect(rightCard, 8f, 8f, paint)
        paint.style = Paint.Style.FILL // Reset
        
        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        canvas1.drawText("2. SKOR KESEHATAN FINANSIAL", 322f, 202f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        paint.color = Color.parseColor("#374151")
        canvas1.drawText("Sinergi Score (XP)    :", 322f, 222f, paint)
        canvas1.drawText("Kategori Penilaian    :", 322f, 238f, paint)
        canvas1.drawText("Debt-To-Income (DTI)  :", 322f, 254f, paint)
        canvas1.drawText("Over-Budget Limits      :", 322f, 270f, paint)
        
        // Values
        paint.color = Color.parseColor("#6366F1") // Indigo
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("${health.score} XP", 445f, 222f, paint)
        
        paint.color = when {
            health.score >= 80 -> Color.parseColor("#10B981") // Excellent Green
            health.score >= 60 -> Color.parseColor("#F59E0B") // Warning Yellow
            else -> Color.parseColor("#EF4444") // Danger red
        }
        val healthTitle = when {
            health.score >= 80 -> "SANGAT FIT"
            health.score >= 60 -> "MENCUKUPI"
            else -> "BUTUH RESTRUKTUR"
        }
        canvas1.drawText(healthTitle, 445f, 238f, paint)
        
        val dti = health.debtRatio
        paint.color = if (dti <= 35) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
        canvas1.drawText("$dti% (${if (dti <= 35) "Aman" else "Bahaya!"})", 445f, 254f, paint)
        
        paint.color = if (health.overLimitCount == 0) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
        canvas1.drawText("${health.overLimitCount} Pos Over", 445f, 270f, paint)
        
        paint.color = Color.parseColor("#4B5563")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 7f
        canvas1.drawText("*) DTI di bawah 35% menepis resiko gagal bayar.", 322f, 297f, paint)
        
        // Draw Divider
        paint.color = Color.parseColor("#E5E7EB")
        canvas1.drawLine(40f, 325f, 555f, 325f, paint)
        
        // D. GRAFIK BREAKDOWN ALOKASI PENGELUARAN (Custom Canvas Vertical/Horizontal Bar Chart)
        paint.color = Color.parseColor("#1F2937")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10.5f
        canvas1.drawText("📊 GRAFIK BREAKDOWN ALOKASI PENGELUARAN AKTIF", 40f, 342f, paint)
        
        // Draw Chart Area Box Background
        paint.color = Color.parseColor("#F9FAFB")
        val chartBox = RectF(40f, 355f, 555f, 495f)
        canvas1.drawRoundRect(chartBox, 6f, 6f, paint)
        
        // Fetch top categories (Maximum 5 for visual fidelity)
        val sortedCategories = stats.categoryBreakdown.entries
            .sortedByDescending { it.value }
            .take(5)
            
        val maxVal = if (sortedCategories.isNotEmpty()) sortedCategories.first().value else 1.0
        
        var chartY = 380f
        if (sortedCategories.isEmpty()) {
            paint.color = Color.parseColor("#9CA3AF")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            paint.textSize = 9f
            canvas1.drawText("Belum ada data pengeluaran terdaftar untuk divisualisasikan.", 155f, 425f, paint)
        } else {
            sortedCategories.forEach { entry ->
                // Draw label
                paint.color = Color.parseColor("#1F2937")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 8.5f
                val catLabel = if (entry.key.length > 15) entry.key.take(12) + "..." else entry.key
                canvas1.drawText(catLabel, 55f, chartY + 11f, paint)
                
                // Draw Gray Progress Track (width 260)
                paint.color = Color.parseColor("#E5E7EB")
                val trackRect = RectF(160f, chartY, 420f, chartY + 13f)
                canvas1.drawRoundRect(trackRect, 4f, 4f, paint)
                
                // Draw filled progress
                val pctOfMax = entry.value / maxVal
                val fillWidth = (pctOfMax * 260f).toFloat().coerceIn(10f, 260f)
                paint.color = Color.parseColor("#0D9488") // Cool Teal
                val filledRect = RectF(160f, chartY, 160f + fillWidth, chartY + 13f)
                canvas1.drawRoundRect(filledRect, 4f, 4f, paint)
                
                // Draw text percentage and amount
                val pctTotal = (entry.value / (if (stats.totalExpense > 0) stats.totalExpense else 1.0)) * 100
                paint.color = Color.parseColor("#1E293B")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 8f
                val valStr = "${formatRupiah(entry.value)} (${String.format(Locale.getDefault(), "%.1f", pctTotal)}%)"
                canvas1.drawText(valStr, 430f, chartY + 10f, paint)
                
                chartY += 21f
            }
        }
        
        // E. ADVISOR KEUANGAN & REKOMENDASI SINERGI
        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10.5f
        canvas1.drawText("💡 REKOMENDASI ADVISOR STRATEGIS (HA-UM)", 40f, 520f, paint)
        
        // Card Border
        paint.color = Color.parseColor("#E0F2FE") // Soft baby blue/navy theme card
        val advisorCard = RectF(40f, 532f, 555f, 785f)
        canvas1.drawRoundRect(advisorCard, 10f, 10f, paint)
        
        paint.color = Color.parseColor("#0284C7") // Deep blue border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas1.drawRoundRect(advisorCard, 10f, 10f, paint)
        paint.style = Paint.Style.FILL
        
        // Advisor recommendations paragraphs based on actual numerical indicators
        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas1.drawText("Analisis Posisi Finansial:", 55f, 555f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.parseColor("#334155")
        
        // 1. Defisit / Surplus Check
        val paramCashflowText = if (stats.surplus > 0) {
            "Arus kas Anda secara kolektif mengalami SURPLUS POSITIF sebesar ${formatRupiah(stats.surplus)}. Hal ini menandakan daya tahan tabungan yang prima. Sangat disarankan untuk memprioritaskan alokasi surplus ini ke pos Tabungan Menikah ($weddingYear)."
        } else {
            "PERINGATAN: Arus kas Anda mengalami DEFISIT sebesar ${formatRupiah(-stats.surplus)}. Segera setop pengeluaran opsional dan kurangi beban anggaran pada kategori-kategori non-prioritas demi kelangsungan sinergi impian!"
        }
        drawWrappedText(canvas1, paramCashflowText, 55f, 569f, 480, paint)
        
        // 2. Debt Burden Check
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#0F172A")
        canvas1.drawText("Rekomendasi Manajemen Hutang / Cicilan:", 55f, 622f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#334155")
        val paramDebtText = if (health.debtRatio <= 15) {
            "Beban cicilan Anda saat ini berada pada level SANGAT SEHAT (${health.debtRatio}% dari kapasitas bulanan). Kondisi ini memberikan fleksibilitas tinggi bagi Haikal & Ummu untuk mengalokasikan dana cadangan darurat tanpa tekanan angsuran jangka pendek."
        } else if (health.debtRatio <= 35) {
            "Beban cicilan Anda dalam tahap NORMAL-MODERAT (${health.debtRatio}% dari kapasitas bulanan). Hindari pengajuan kredit konsumtif baru. Segera selesaikan sisa tenor cicilan yang ada sebelum melakukan peningkatan anggaran belanja bulanan."
        } else {
            "BAHAYA KRITIS: Rasio utang (DTI) Anda sebesar ${health.debtRatio}% melampaui ambang batas sehat (35%). Hal ini membahayakan stabilitas keuangan masa depan. Prioritaskan penundaan impian konsumtif sekunder dan fokus melunasi hutang berjalan."
        }
        drawWrappedText(canvas1, paramDebtText, 55f, 636f, 480, paint)
        
        // 3. Goals Sinergi
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#0F172A")
        canvas1.drawText("Sinergi Pasangan & Milestone Pernikahan ($weddingYear):", 55f, 694f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#334155")
        val numGoalsActive = goals.size
        val paramGoalsText = if (numGoalsActive > 0) {
            "Terdapat $numGoalsActive rencana besar (Saving Goals) yang sedang diperjuangkan bersama. Mempertahankan kebiasaan mencatat transaksi harian minimal 5 hari berturut-turut akan terus melipatgandakan Sinergi Score (saat ini ${health.score} XP). Saling dukung adalah kunci keberhasilan."
        } else {
            "Belum ada Saving Goals finansial yang terdaftar secara aktif. Segera diskusikan bersama pasangan dan buat target tabungan pertama Anda (misalnya DP Pernikahan, Sewa Rumah, atau Tabungan Umroh) agar impian terukur secara rahasia dan nyata."
        }
        drawWrappedText(canvas1, paramGoalsText, 55f, 708f, 480, paint)
        
        // F. Footer Page 1
        paint.color = Color.parseColor("#94A3B8")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 7.5f
        canvas1.drawText("Sinergi DUNIA App © Haikal & Ummu • Halaman 1 dari 2", 210f, 810f, paint)
        
        pdfDocument.finishPage(page1)
        
        // ---------------- PAGE 2: DETAILED DATA TABLE (TRANSACTIONS, PLANNING & DEBTS) ----------------
        val page2 = pdfDocument.startPage(pageInfoPage2)
        val canvas2 = page2.canvas
        
        // Mini Header
        paint.color = Color.parseColor("#00796B") // Medium Teal Accent
        canvas2.drawRect(40f, 40f, 555f, 75f, paint)
        
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        canvas2.drawText("DAFTAR RINCIAN DATA TRANSAKSI & PERENCANAAN", 55f, 61f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        paint.color = Color.parseColor("#E0F2FE")
        canvas2.drawText("Lanjutan Transaksi Harian Lengkap & Target Sinergi", 380f, 61f, paint)
        
        // Table Title
        paint.color = Color.parseColor("#1E293B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        canvas2.drawText("A. BUKU BESAR ALIRAN KAS TERAKHIR (Max 15)", 40f, 96f, paint)
        
        // Table Header
        paint.color = Color.parseColor("#F1F5F9") // Table header bg
        canvas2.drawRect(40f, 105f, 555f, 123f, paint)
        
        paint.color = Color.parseColor("#334155")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        canvas2.drawText("Tanggal", 45f, 117f, paint)
        canvas2.drawText("Tipe", 125f, 117f, paint)
        canvas2.drawText("Kategori", 175f, 117f, paint)
        canvas2.drawText("Pencatat", 255f, 117f, paint)
        canvas2.drawText("Deskripsi / Catatan", 320f, 117f, paint)
        canvas2.drawText("Jumlah (IDR)", 485f, 117f, paint) // Width limit 550
        
        // Render Transaction list
        var tableY = 135f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        
        val displayTx = transactions.sortedByDescending { it.timestamp }.take(15)
        
        if (displayTx.isEmpty()) {
            paint.color = Color.parseColor("#64748B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas2.drawText("Belum ada mutasi transaksi saat ini di database.", 180f, 160f, paint)
            tableY = 175f
        } else {
            var isAlternating = false
            displayTx.forEach { tx ->
                // Row BG highlight
                if (isAlternating) {
                    paint.color = Color.parseColor("#F8FAFC")
                    canvas2.drawRect(40f, tableY - 10f, 555f, tableY + 5f, paint)
                }
                isAlternating = !isAlternating
                
                // Color text based on type
                paint.color = Color.parseColor("#334155")
                val dateStr = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                canvas2.drawText(dateStr, 45f, tableY, paint)
                
                // TYPE (PEMASUKAN or PENGELUARAN)
                if (tx.type == "PEMASUKAN") {
                    paint.color = Color.parseColor("#059669")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas2.drawText("[+] MASUK", 125f, tableY, paint)
                } else {
                    paint.color = Color.parseColor("#EF4444")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas2.drawText("[-] KELUAR", 125f, tableY, paint)
                }
                
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.parseColor("#334155")
                
                // Category
                val catText = if (tx.category.length > 15) tx.category.take(12) + ".." else tx.category
                canvas2.drawText(catText, 175f, tableY, paint)
                
                // Pencatat (User)
                val userText = if (tx.user.length > 10) tx.user.take(8) + ".." else tx.user
                canvas2.drawText(userText, 255f, tableY, paint)
                
                // Description / Catatan
                val cleanDesc = tx.description.replace("\n", " ")
                val descText = if (cleanDesc.length > 30) cleanDesc.take(27) + "..." else cleanDesc
                canvas2.drawText(descText, 320f, tableY, paint)
                
                // Amount (Right-aligned at x=545)
                val amtStr = formatRupiah(tx.amount)
                val textWidth = paint.measureText(amtStr)
                if (tx.type == "PEMASUKAN") {
                    paint.color = Color.parseColor("#059669")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                } else {
                    paint.color = Color.parseColor("#EF4444")
                }
                canvas2.drawText(amtStr, 545f - textWidth, tableY, paint)
                
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.parseColor("#334155")
                tableY += 16f
            }
        }
        
        // Draw bottom table boundary line
        paint.color = Color.parseColor("#CBD5E1")
        canvas2.drawLine(40f, tableY - 5f, 555f, tableY - 5f, paint)
        
        // SECTION B: GOALS AND TENOR INSTALLMENTS (SAVING GOALS & CICILAN) - Stacked layout
        var goalsY = tableY + 20f
        paint.color = Color.parseColor("#1F2937")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        canvas2.drawText("B. PROGRES RENCANA TABUNGAN IMPIAN (SAVING GOALS)", 40f, goalsY, paint)
        
        goalsY += 12f
        // Header saving goals
        paint.color = Color.parseColor("#F1F5F9")
        canvas2.drawRect(40f, goalsY, 555f, goalsY + 16f, paint)
        
        paint.color = Color.parseColor("#334155")
        paint.textSize = 7.5f
        canvas2.drawText("Nama Target Tabungan", 45f, goalsY + 11f, paint)
        canvas2.drawText("Penanggung", 185f, goalsY + 11f, paint)
        canvas2.drawText("Target Anggaran", 255f, goalsY + 11f, paint)
        canvas2.drawText("Dana Terhimpun", 355f, goalsY + 11f, paint)
        canvas2.drawText("Progres Rencana", 465f, goalsY + 11f, paint)
        
        goalsY += 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val activeGoals = goals.take(4)
        if (activeGoals.isEmpty()) {
            paint.color = Color.parseColor("#64748B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas2.drawText("Belum membuat target tabungan berencana bersama.", 160f, goalsY + 6f, paint)
            goalsY += 18f
        } else {
            activeGoals.forEach { g ->
                paint.color = Color.parseColor("#111827")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas2.drawText(g.name, 45f, goalsY, paint)
                
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.parseColor("#475569")
                canvas2.drawText(g.owner, 185f, goalsY, paint)
                canvas2.drawText(formatRupiah(g.targetAmount), 255f, goalsY, paint)
                canvas2.drawText(formatRupiah(g.currentAmount), 355f, goalsY, paint)
                
                // Calculate progress %
                val pct = if (g.targetAmount > 0) (g.currentAmount / g.targetAmount * 100) else 0.0
                val pctStr = "${String.format(Locale.getDefault(), "%.1f", pct)}%"
                paint.color = if (pct >= 100) Color.parseColor("#10B981") else Color.parseColor("#0284C7")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas2.drawText(pctStr, 465f, goalsY, paint)
                
                goalsY += 14f
            }
        }
        
        // Draw bottom boundary
        paint.color = Color.parseColor("#E2E8F0")
        canvas2.drawLine(40f, goalsY - 4f, 555f, goalsY - 4f, paint)
        
        // SECTION C: ACTIVE INVOICES & TENOR BILLS (CICILAN TENOR)
        var debtsY = goalsY + 15f
        paint.color = Color.parseColor("#1F2937")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        canvas2.drawText("C. BEBAN ANGSURAN & TENOR BERJALAN (CICILAN)", 40f, debtsY, paint)
        
        debtsY += 12f
        paint.color = Color.parseColor("#F1F5F9")
        canvas2.drawRect(40f, debtsY, 555f, debtsY + 16f, paint)
        
        paint.color = Color.parseColor("#334155")
        paint.textSize = 7.5f
        canvas2.drawText("Nama Tagihan / Angsuran", 45f, debtsY + 11f, paint)
        canvas2.drawText("Penanggung", 185f, debtsY + 11f, paint)
        canvas2.drawText("Angsuran / Bln", 255f, debtsY + 11f, paint)
        canvas2.drawText("Total Pokok", 355f, debtsY + 11f, paint)
        canvas2.drawText("Sisa Tenor", 465f, debtsY + 11f, paint)
        
        debtsY += 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val activeDebts = debts.take(4)
        if (activeDebts.isEmpty()) {
            paint.color = Color.parseColor("#64748B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas2.drawText("Bebas hutang! Tidak ada angsuran tenor berjalan saat ini.", 145f, debtsY + 6f, paint)
            debtsY += 18f
        } else {
            activeDebts.forEach { d ->
                paint.color = Color.parseColor("#111827")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas2.drawText(d.name, 45f, debtsY, paint)
                
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.parseColor("#475569")
                canvas2.drawText(d.owner, 185f, debtsY, paint)
                
                paint.color = Color.parseColor("#EF4444")
                canvas2.drawText(formatRupiah(d.monthlyPayment), 255f, debtsY, paint)
                
                paint.color = Color.parseColor("#334155")
                canvas2.drawText(formatRupiah(d.totalValue), 355f, debtsY, paint)
                canvas2.drawText("${d.remainingMonths} bulan lagi (${d.paidMonths} dibayar)", 465f, debtsY, paint)
                
                debtsY += 14f
            }
        }
        
        // Draw bottom boundary
        paint.color = Color.parseColor("#E2E8F0")
        canvas2.drawLine(40f, debtsY - 4f, 555f, debtsY - 4f, paint)
        
        // G. Professional Legal Closing Stamp
        var closingY = 730f
        if (closingY < debtsY + 10f) {
            closingY = debtsY + 15f
        }
        
        paint.color = Color.parseColor("#F8FAFC")
        val stampBox = RectF(40f, closingY, 555f, closingY + 52f)
        canvas2.drawRoundRect(stampBox, 6f, 6f, paint)
        
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        canvas2.drawRoundRect(stampBox, 6f, 6f, paint)
        paint.style = Paint.Style.FILL
        
        paint.color = Color.parseColor("#475569")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas2.drawText("PENGESAHAN LAPORAN KEUANGAN KELUARGA", 55f, closingY + 18f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 7.5f
        canvas2.drawText("Sinergi dua cinta berkomitmen membangun ketahanan finansial demi kebaikan dunia dan akhirat.", 55f, closingY + 31f, paint)
        canvas2.drawText("Dihasilkan otomatis secara transparan untuk: Haikal & Ummu.", 55f, closingY + 42f, paint)
        
        // Legal Stamp Stamp Graphic Icon Symbol
        paint.color = Color.parseColor("#0F766E")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas2.drawCircle(495f, closingY + 25f, 15f, paint)
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        paint.color = Color.parseColor("#0F766E")
        canvas2.drawText("DUNIA", 484f, closingY + 28f, paint)
        
        // H. Footer Page 2
        paint.color = Color.parseColor("#94A3B8")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 7.5f
        canvas2.drawText("Diproses melalui Sinergi Dunia App • Halaman 2 dari 2", 205f, 810f, paint)
        
        pdfDocument.finishPage(page2)
        
        // I. Capture & Save to File System Cache
        val cacheFile = File(context.cacheDir, "Laporan_Finansial_DUNIA.pdf")
        return try {
            val fos = FileOutputStream(cacheFile)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.flush()
            fos.close()
            
            // Generate FileProvider Uri
            FileProvider.getUriForFile(
                context,
                "com.aistudio.dunia.hkum26.fileprovider",
                cacheFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Upgrades CSV output to function as a professional Financial Spreadsheet:
     * Contains comprehensive summary dashboards, advisory metrics, structured tables, and detailed category ratios.
     */
    fun generateProfessionalCsvFile(context: Context, viewModel: DuniaViewModel): Uri? {
        val stats = viewModel.monthlyStats.value
        val health = viewModel.financialHealthScore.value
        val transactions = viewModel.transactions.value
        val goals = viewModel.savingGoals.value
        val debts = viewModel.cicilanList.value
        
        val weddingYear = viewModel.configs.value["TARGET_NIKAH"] ?: "2029"
        val haikalJob = viewModel.configs.value["PEKERJAAN_HAIKAL"] ?: "Trainer di Hara Chicken"
        val ummuJob = viewModel.configs.value["PEKERJAAN_UMMU"] ?: "Guru Swasta"
        val printDateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val csvContent = buildString {
            // Document Header block
            append("==================================================================================\n")
            append("           LAPORAN KONTROL FINANSIAL DAN ALIRAN SINERGI DUNIA (SPREADSHEET)       \n")
            append("==================================================================================\n")
            append("Waktu Ekspor, $printDateStr\n")
            append("Mitra Terkait, Haikal (Pekerjaan: $haikalJob) & Ummu (Pekerjaan: $ummuJob)\n")
            append("Fase Perencanaan, Fase zero: Fondasi Utama Bersama (Target Nikah: $weddingYear)\n")
            append("Status Database, Terhubung Cloud (Nirkabel Real-time)\n\n")

            // Section 1: Executive Dashboard Statistics
            append("=== 1. RINGKASAN EKSEKUTIF KAS GABUNGAN ===\n")
            append("Parameter Finansial, Jumlah Nilai (Rupiah), Status Kesehatan & Rekomendasi\n")
            append("Total Pemasukan Gabungan, ${stats.totalIncome.toLong()}, Prima - Penyerapan arus kas terkendali\n")
            append("Total Pengeluaran Gabungan, ${stats.totalExpense.toLong()}, Terpantau stabil - amankan cadangan\n")
            append("Sisa Arus Kas Bersih (Surplus), ${stats.surplus.toLong()}, ${if (stats.surplus > 0) "SURPLUS - Potensi investasi pernikahan tinggi" else "DEFISIT - Tekan pengeluaran sekunder harian!"}\n")
            append("Sinergi Health Score (XP), ${health.score} XP, ${if (health.score >= 80) "KATEGORI EXCELLENT" else if (health.score >= 60) "KATEGORI MODERAT" else "KATEGORI RAWAN INTERVENSI"}\n")
            append("Tingkat Rasio Tabungan (Saving Rate), ${health.savingRate}%, ${if (health.savingRate >= 30) "SEHAT - Sangat baik untuk tabungan masa depan" else "KURANG - Target tabungan minimal 30%"}\n")
            append("Beban Debt-to-Income (DTI) Ratio, ${health.debtRatio}%, ${if (health.debtRatio <= 35) "AMAN - Beban utang berada di bawah garis waspada" else "BAHAYA - Rasio utang melampaui batas sehat 35%"}\n")
            append("Banyak Pos Melebihi Anggaran, ${health.overLimitCount} pos, ${if (health.overLimitCount == 0) "Sempurna - Sesuai rencana" else "Warning - Perlu perampingan pos budget"}\n")
            append("Hari Aktif Mencatat, ${health.activeRecordingDays} hari, Semakin giat mencatat keuangan melipatgandakan Sinergi XP\n\n")

            // Section 2: Advisor Recommendation Analysis text block
            append("=== 2. ANALISIS ADVISOR KEUANGAN SINERGI ===\n")
            val cashflowAdvice = if (stats.surplus > 0) {
                "\"Arus kas gabungan mengalami surplus positif ${formatRupiah(stats.surplus)}. Pindahkan sisa surplus ke Saving Goals impian Anda secara terjadwal.\""
            } else {
                "\"Arus kas defisit negatif ${formatRupiah(-stats.surplus)}. Cari kebocoran anggaran pada pengeluaran tersier dan batasi belanja berlebih.\""
            }
            val debtAdvice = if (health.debtRatio <= 15) {
                "\"Rasio beban utang sangat aman (${health.debtRatio}%). Fokus pada pemupukan dana DP Nikah ($weddingYear).\""
            } else {
                "\"Beban utang Anda tinggi (${health.debtRatio}%). Kurangi pengeluaran hobi dan fokus penunasan tenor cepat.\""
            }
            append("Poin Rekomendasi 1, $cashflowAdvice\n")
            append("Poin Rekomendasi 2, $debtAdvice\n")
            append("Poin Rekomendasi 3, \"Konsistensi pelaporan nirkabel niscaya melancarkan jalan masa depan yang penuh berkah.\"\n\n")

            // Section 3: Expense Category Breakdown tables
            append("=== 3. DISTRIBUSI ALOKASI POS PENGELUARAN ===\n")
            append("Pos Kategori Pengeluaran, Total Biaya (Rupiah), Persentase Terhadap Total (\n")
            val totalExp = if (stats.totalExpense > 0) stats.totalExpense else 1.0
            stats.categoryBreakdown.forEach { (category, value) ->
                val ratio = (value / totalExp) * 100
                append("$category, ${value.toLong()}, ${String.format(Locale.getDefault(), "%.1f", ratio)}%\n")
            }
            append("\n")

            // Section 4: Transaction Ledger Record (Actual Sheet style database)
            append("=== 4. DATA MUTASI TRANSAKSI HARIAN ===\n")
            append("ID Transaksi, Waktu (yyyy-MM-dd HH:mm), Tipe Transaksi, Pasangan Pencatat, Jumlah Uang (Rp), Pos Kategori, Deskripsi Catatan, Label Tag\n")
            transactions.sortedByDescending { it.timestamp }.forEach { tx ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                val escDesc = tx.description.replace("\"", "\"\"")
                val escCat = tx.category.replace("\"", "\"\"")
                append("TX_${tx.id}, $dateStr, ${tx.type}, ${tx.user}, ${tx.amount.toLong()}, \"$escCat\", \"$escDesc\", ${tx.tag}\n")
            }
            append("\n")

            // Section 5: Target goals planning sheet
            append("=== 5. ALOKASI DANA RENCANA TABUNGAN (SAVING GOALS) ===\n")
            append("ID Target, Nama Rencana Tabungan, Pemilik Rencana, Target Anggaran (Rp), Dana Terkumpul (Rp), Persentase Progres (%)\n")
            goals.forEach { g ->
                val pct = if (g.targetAmount > 0) (g.currentAmount / g.targetAmount * 100) else 0.0
                append("G_${g.id}, ${g.name}, ${g.owner}, ${g.targetAmount.toLong()}, ${g.currentAmount.toLong()}, ${String.format(Locale.getDefault(), "%.1f", pct)}%\n")
            }
            append("\n")

            // Section 6: Tenor debts list
            append("=== 6. DAFTAR BEBAN TENOR ANGSURAN (CICILAN) ===\n")
            append("ID Cicilan, Nama Tagihan Angsuran, Penanggung Jawab, Angsuran per Bulan (Rp), Total Nilai Pokok (Rp), Sisa Tenor (Bulan), Sudah Terbayar (Bulan)\n")
            debts.forEach { d ->
                append("C_${d.id}, ${d.name}, ${d.owner}, ${d.monthlyPayment.toLong()}, ${d.totalValue.toLong()}, ${d.remainingMonths}, ${d.paidMonths}\n")
            }
            append("\n")
            append("==================================================================================\n")
            append("               Generated Automatically via Sinergi Keuangan Dunia App             \n")
            append("==================================================================================\n")
        }

        // Save CSV to cache memory
        val cacheFile = File(context.cacheDir, "Spreadsheet_Laporan_DUNIA.csv")
        return try {
            val fos = FileOutputStream(cacheFile)
            fos.write(csvContent.toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.close()
            
            // Get Uri for sharing file
            FileProvider.getUriForFile(
                context,
                "com.aistudio.dunia.hkum26.fileprovider",
                cacheFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Helper to draw wrapped paragraph text gracefully on android.graphics.Canvas
     */
    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Int,
        paint: Paint
    ): Float {
        val words = text.split(" ")
        val sb = java.lang.StringBuilder()
        var y = startY
        
        words.forEach { word ->
            val testLine = sb.toString() + (if (sb.isEmpty()) "" else " ") + word
            val width = paint.measureText(testLine)
            if (width > maxWidth) {
                canvas.drawText(sb.toString(), x, y, paint)
                sb.setLength(0)
                sb.append(word)
                y += paint.textSize + 3.5f // Line spacing
            } else {
                sb.append(if (sb.isEmpty()) "" else " ").append(word)
            }
        }
        if (sb.isNotEmpty()) {
            canvas.drawText(sb.toString(), x, y, paint)
            y += paint.textSize + 3.5f
        }
        return y
    }
}
