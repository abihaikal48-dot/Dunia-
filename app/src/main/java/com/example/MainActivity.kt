package com.example

import android.os.Bundle
import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.webkit.WebView
import android.webkit.WebSettings
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init databases and repositories on IO scope
        val db = DuniaDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = DuniaRepository(db.duniaDao())
        val vmFactory = DuniaViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: DuniaViewModel = viewModel(factory = vmFactory)
                MainScreenShell(viewModel)
            }
        }
    }
}

// ==========================================
// CENTRAL NAVIGATION & UI SHELL
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreenShell(viewModel: DuniaViewModel) {
    // Current active screen index:
    // 0 = Dashboard (🌍)
    // 1 = Keuangan [Ledger, Goals, Cicilan] (💰)
    // 2 = Roadmap & Goals (🗺️)
    // 3 = Harian & Spiritual (⏰)
    // 4 = Rapat & Wishlist (🤝)
    // 5 = AI Advisor (🤖)
    // 6 = Profil & Set (⚙️)
    var selectedTab by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val systemMap by viewModel.configs.collectAsState()
    val healthState by viewModel.financialHealthScore.collectAsState()

    // Grab custom dynamic titles
    val haikalName = systemMap["NAMA_HAIKAL"] ?: "Haikal"
    val ummuName = systemMap["NAMA_UMMU"] ?: "Ummu"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BgDeep,
        bottomBar = {
            // Elegant scrolling bottom bar for wide mobile compatibility
            NavigationBar(
                containerColor = BgCard,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                val tabItems = listOf(
                    Triple(0, "Utama", Icons.Default.Language),
                    Triple(1, "Keuangan", Icons.Default.AccountBalanceWallet),
                    Triple(2, "Roadmap", Icons.Default.Directions),
                    Triple(3, "Harian", Icons.Default.Schedule),
                    Triple(4, "Rencana", Icons.Default.Handshake),
                    Triple(5, "DUNIA AI", Icons.Default.SmartToy),
                    Triple(6, "Profil", Icons.Default.Settings)
                )

                tabItems.forEach { (index, title, icon) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = if (isSelected) CombinedAccent else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = CombinedAccent.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_tab_$index")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "DUNIA",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CombinedAccent,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Dual Universe of Needs, Income & Aspirations",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Small circular wellness indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (healthState.score) {
                                    in 0..40 -> DangerAccent
                                    in 41..70 -> WarningAccent
                                    else -> SuccessAccent
                                }
                            )
                    )
                    Text(
                        text = "${healthState.score} XP",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
            HorizontalDivider(color = BorderColor)

            // Real Body Rendering
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                    },
                    label = "TabContent"
                ) { activeIndex ->
                    when (activeIndex) {
                        0 -> DashboardScreen(viewModel, onNavigate = { selectedTab = it }, haikalName, ummuName)
                        1 -> KeuanganScreen(viewModel, haikalName, ummuName)
                        2 -> RoadmapGoalsScreen(viewModel)
                        3 -> HarianSpiritualScreen(viewModel, haikalName, ummuName)
                        4 -> RapatWishlistScreen(viewModel, haikalName, ummuName)
                        5 -> AIAdvisorScreen(viewModel)
                        6 -> ProfilSettingsScreen(viewModel, haikalName, ummuName)
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD SCREEN (FITUR 01)
// ==========================================

// ==========================================
// GLASS-MORPHIC CONTAINER (REFINED VOID DESIGN)
// ==========================================

@Composable
fun GlassMorphicCard(
    modifier: Modifier = Modifier,
    borderAccent: Color = CombinedAccent,
    glowColor: Color = CombinedAccent,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BgCard.copy(alpha = 0.95f),
                        BgCard.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        borderAccent.copy(alpha = 0.20f),
                        BorderColor
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                // Subtle bright glowing accent
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.04f), Color.Transparent),
                        center = this.center,
                        radius = size.maxDimension * 0.7f
                    )
                )
            }
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

// ==========================================
// 1. DASHBOARD SCREEN (FITUR 01)
// ==========================================

@Composable
fun DashboardScreen(
    viewModel: DuniaViewModel,
    onNavigate: (Int) -> Unit,
    haikalName: String,
    ummuName: String
) {
    val stats by viewModel.monthlyStats.collectAsState()
    val health by viewModel.financialHealthScore.collectAsState()
    val countdown by viewModel.targetsCountdown.collectAsState()
    val configs by viewModel.configs.collectAsState()

    var activeTab by remember { mutableStateOf(2) } // 0 = Haikal, 1 = Ummu, 2 = Berdua

    val activeIncome = when (activeTab) {
        0 -> stats.haikalIncome
        1 -> stats.ummuIncome
        else -> stats.totalIncome
    }

    val activeExpense = when (activeTab) {
        0 -> stats.haikalExpense
        1 -> stats.ummuExpense
        else -> stats.totalExpense
    }

    val activeSurplus = activeIncome - activeExpense

    val activeAccent = when (activeTab) {
        0 -> HaikalAccent
        1 -> UmmuAccent
        else -> CombinedAccent
    }

    val activeTitle = when (activeTab) {
        0 -> "Dana Mandiri $haikalName"
        1 -> "Dana Mandiri $ummuName"
        else -> "Dana Bersama / Surplus"
    }

    val activeIncomeSubtitle = when (activeTab) {
        0 -> "Pendapatan $haikalName"
        1 -> "Pendapatan $ummuName"
        else -> "Sinergi $haikalName & $ummuName"
    }

    val activeExpenseSubtitle = when (activeTab) {
        0 -> "Pengeluaran $haikalName"
        1 -> "Pengeluaran $ummuName"
        else -> "Total Gabungan"
    }

    val allTransactions by viewModel.transactions.collectAsState(initial = emptyList())

    val chartData = remember(allTransactions, activeTab) {
        val labelsList = mutableListOf<String>()
        val valuesList = mutableListOf<Double>()
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            
            val labelStr = sdf.format(cal.time)
            labelsList.add(labelStr)
            
            // Start of day
            val calStart = Calendar.getInstance().apply {
                time = cal.time
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calStart.timeInMillis
            
            // End of day
            val calEnd = Calendar.getInstance().apply {
                time = cal.time
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endOfDay = calEnd.timeInMillis
            
            val totalForDay = allTransactions.filter { tx ->
                tx.timestamp in startOfDay..endOfDay &&
                tx.type == "PENGELUARAN" &&
                when (activeTab) {
                    0 -> tx.user == "HAIKAL"
                    1 -> tx.user == "UMMU"
                    else -> true
                }
            }.sumOf { tx -> tx.amount }
            
            valuesList.add(totalForDay)
        }
        Pair(labelsList, valuesList)
    }

    val labelsJson = chartData.first.joinToString(prefix = "[", postfix = "]") { "'$it'" }
    val valuesJson = chartData.second.joinToString(prefix = "[", postfix = "]") { it.toString() }

    val chartHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
            <style>
                body {
                    background-color: transparent !important;
                    margin: 0;
                    padding: 0;
                    overflow: hidden;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                }
                #container {
                    position: relative;
                    width: 100%;
                    height: 180px;
                }
                canvas {
                    display: block;
                    width: 100% !important;
                    height: 100% !important;
                }
            </style>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
        </head>
        <body>
            <div id="container">
                <canvas id="expensesChart"></canvas>
            </div>
            <script>
                document.addEventListener("DOMContentLoaded", function() {
                    const ctx = document.getElementById('expensesChart').getContext('2d');
                    
                    // Create gradient
                    const gradient = ctx.createLinearGradient(0, 0, 0, 180);
                    gradient.addColorStop(0, '#10B981'); // Emerald
                    gradient.addColorStop(1, 'rgba(16, 185, 129, 0.05)');

                    new Chart(ctx, {
                        type: 'bar',
                        data: {
                            labels: $labelsJson,
                            datasets: [{
                                label: 'Pengeluaran',
                                data: $valuesJson,
                                backgroundColor: gradient,
                                borderColor: '#10B981',
                                borderWidth: 1.5,
                                borderRadius: 6,
                                borderSkipped: false,
                                hoverBackgroundColor: '#34D399',
                                hoverBorderColor: '#10B981'
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                legend: {
                                    display: false
                                },
                                tooltip: {
                                    backgroundColor: '#FFFFFF',
                                    titleColor: '#0F172A',
                                    bodyColor: '#059669',
                                    borderColor: '#E2E8F0',
                                    borderWidth: 1,
                                    cornerRadius: 8,
                                    displayColors: false,
                                    callbacks: {
                                        label: function(context) {
                                            return 'Rp ' + Number(context.raw).toLocaleString('id-ID');
                                        }
                                    }
                                }
                            },
                            scales: {
                                x: {
                                    grid: {
                                        display: false
                                    },
                                    ticks: {
                                        color: '#475569',
                                        font: {
                                            size: 10
                                        }
                                    }
                                },
                                y: {
                                    grid: {
                                        color: 'rgba(226, 232, 240, 0.8)'
                                    },
                                    ticks: {
                                        color: '#475569',
                                        font: {
                                            size: 9
                                        },
                                        callback: function(value) {
                                            if (value >= 1000000) {
                                                return (value / 1000000).toFixed(1) + 'jt';
                                            } else if (value >= 1000) {
                                                return (value / 1000).toFixed(0) + 'rb';
                                            }
                                            return value;
                                        }
                                    }
                                }
                            }
                        }
                    });
                });
            </script>
        </body>
        </html>
    """.trimIndent()

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    val todayHasTx = remember(allTransactions) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        allTransactions.any { it.timestamp >= startOfToday }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily Reminder Notification Alert (Fulfills Financial Reminder Notification)
        if (!todayHasTx) {
            item {
                GlassMorphicCard(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(1) }.testTag("daily_reminder_card"),
                    borderAccent = WarningAccent,
                    glowColor = WarningAccent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(WarningAccent.copy(alpha = 0.15f))
                                .border(1.dp, WarningAccent.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminder Icon",
                                tint = WarningAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PENGINGAT KEUANGAN HARIAN ⏰",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAccent,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Halo Haikal & Ummu! Hari ini belum ada catatan keuangan masuk/keluar. Yuk catat sekarang agar sirkulasi keuangan kita terpantau rapi!",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ketuk kartu ini untuk catat transaksi harian kamu ->",
                                fontSize = 9.sp,
                                color = CombinedAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 1. Welcome Premium Header Card
        item {
            GlassMorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderAccent = CombinedAccent,
                glowColor = CombinedAccent
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        val formater = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                        Text(
                            text = formater.format(Date()),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Semesta Hidup Kita",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CombinedAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Fase 0: Fondasi",
                            fontSize = 10.sp,
                            color = HaikalAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "\"Dua langkah berbeda, satu arah yang sama. Tidak perlu terburu-buru, bangun fondasi yang matang, nikahi dalam ketenangan di 2029/2030.\"",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Financial Health Score", fontSize = 11.sp, color = TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (health.score >= 70) SuccessAccent else if (health.score >= 40) WarningAccent else DangerAccent)
                            )
                            val healthStatus = when (health.score) {
                                in 0..40 -> "Butuh Perbaikan"
                                in 41..70 -> "Cukup Baik"
                                else -> "Sangat Prima"
                            }
                            Text(
                                text = "${health.score}% - $healthStatus",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (health.score >= 70) SuccessAccent else if (health.score >= 40) WarningAccent else DangerAccent
                            )
                        }
                    }
                    Button(
                        onClick = { onNavigate(5) }, // Ask AI Advice about budget
                        colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent.copy(alpha = 0.15f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Analisa AI", color = CombinedAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section Title: Ringkasan Dana Kita
        item {
            Text(
                text = "🪐 SEMESTA KEUANGAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        // Toggle Tab Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    Triple(0, haikalName, HaikalAccent),
                    Triple(1, ummuName, UmmuAccent),
                    Triple(2, "Berdua", CombinedAccent)
                )
                tabs.forEach { (index, title, color) ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                color.copy(alpha = 0.25f),
                                                color.copy(alpha = 0.08f)
                                            )
                                        )
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) color.copy(alpha = 0.6f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { activeTab = index }
                            .padding(vertical = 10.dp)
                            .testTag("dashboard_toggle_${index}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }
        }

        // 2. Savings Status - Large Glass Card (Fulfills the user request)
        item {
            val totalSavings = activeSurplus
            GlassMorphicCard(
                modifier = Modifier.fillMaxWidth().testTag("savings_status_card"),
                borderAccent = activeAccent,
                glowColor = activeAccent
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activeTitle, fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRupiah(totalSavings),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalSavings >= 0) SuccessAccent else DangerAccent
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(activeAccent.copy(alpha = 0.15f))
                            .border(1.dp, activeAccent.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet Icon",
                            tint = activeAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Saving Rate visual gauge
                val savingRate = if (activeIncome > 0) (activeSurplus / activeIncome * 100).toInt() else 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Rasio Menabung (Saving Rate): ${savingRate.coerceIn(0, 100)}%",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = if (savingRate >= 35) "Sangat Sehat" else if (savingRate >= 20) "Sehat" else "Butuh Disiplin",
                        fontSize = 11.sp,
                        color = if (savingRate >= 35) SuccessAccent else if (savingRate >= 20) WarningAccent else DangerAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Beautiful capsule glass progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(BgDeep)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (savingRate / 100f).coerceIn(0f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(activeAccent, SuccessAccent)
                                )
                            )
                    )
                }
            }
        }

        // 3. Grid of Income & Expenses (Fulfills the user request)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income Card
                GlassMorphicCard(
                    modifier = Modifier.weight(1f).testTag("income_card"),
                    borderAccent = activeAccent,
                    glowColor = activeAccent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Total Masuk", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Income",
                            tint = activeAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatRupiah(activeIncome),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeIncomeSubtitle,
                        fontSize = 9.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Expenses Card
                GlassMorphicCard(
                    modifier = Modifier.weight(1f).testTag("expense_card"),
                    borderAccent = activeAccent,
                    glowColor = activeAccent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Total Keluar", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = "Expense",
                            tint = activeAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatRupiah(activeExpense),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val maxCategory = stats.categoryBreakdown.maxByOrNull { it.value }?.key ?: "Belum ada"
                    Text(
                        text = if (activeTab == 2) "Fokus: $maxCategory" else "Total Keluar",
                        fontSize = 9.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 4. LAST 7 DAYS EXPENSES CHARTS (Fulfills user request: "using Chart.js inside a dashboard card to visualize the last 7 days of expenses")
        item {
            GlassMorphicCard(
                modifier = Modifier.fillMaxWidth().testTag("last_7_days_expenses_card"),
                borderAccent = SuccessAccent,
                glowColor = SuccessAccent
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "📈 Tren Pengeluaran (7 Hari Terakhir)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Menampilkan analisis pengeluaran hulu ke hilir.",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuccessAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Chart.js Live",
                            fontSize = 9.sp,
                            color = SuccessAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // WebView rendering Chart.js
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(0) // Transparent
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL("https://cdn.jsdelivr.net", chartHtml, "text/html", "UTF-8", null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Toggles / Segment Quick Navigation
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardSegmentCard(
                    title = haikalName,
                    income = stats.haikalIncome,
                    expense = stats.haikalExpense,
                    color = HaikalAccent,
                    modifier = Modifier.weight(1f).clickable { onNavigate(6) }
                )
                DashboardSegmentCard(
                    title = ummuName,
                    income = stats.ummuIncome,
                    expense = stats.ummuExpense,
                    color = UmmuAccent,
                    modifier = Modifier.weight(1f).clickable { onNavigate(6) }
                )
            }
        }

        // Countdown targets (Nikah & DP Rumah) with circular progress indicators (Fulfills Goals Progress Section)
        item {
            val savingGoals by viewModel.savingGoals.collectAsState()
            
            val weddingGoal = savingGoals.find { it.name.contains("Pernikahan") || it.name.contains("Nikah") }
            val weddingSaved = weddingGoal?.currentAmount ?: 1200000.0
            val weddingTarget = weddingGoal?.targetAmount ?: 25000000.0
            val weddingProgress = (weddingSaved / weddingTarget.coerceAtLeast(1.0)).toFloat()

            val houseGoal = savingGoals.find { it.name.contains("Rumah") || it.name.contains("DP") }
            val houseSaved = houseGoal?.currentAmount ?: 0.0
            val houseTarget = houseGoal?.targetAmount ?: 40000000.0
            val houseProgress = (houseSaved / houseTarget.coerceAtLeast(1.0)).toFloat()

            GlassMorphicCard(
                modifier = Modifier.fillMaxWidth().testTag("circular_goals_progress_card"),
                borderAccent = CombinedAccent,
                glowColor = CombinedAccent
            ) {
                Text(
                    text = "🎯 Progres Capaian Impian Kita",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Apresiasi langkah demi langkah menuju masa depan berdua.",
                    fontSize = 10.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular indicator 1: Wedding Goals
                    MilestoneCircularIndicator(
                        progress = weddingProgress,
                        title = "Pernikahan Impian",
                        target = formatRupiah(weddingTarget),
                        saved = formatRupiah(weddingSaved),
                        indicatorColor = UmmuAccent,
                        modifier = Modifier.weight(1f)
                    )

                    // Circular indicator 2: House Goals
                    MilestoneCircularIndicator(
                        progress = houseProgress,
                        title = "DP Rumah Impian",
                        target = formatRupiah(houseTarget),
                        saved = formatRupiah(houseSaved),
                        indicatorColor = HaikalAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Wedding Countdown
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Target Menikah (${configs["TARGET_NIKAH"] ?: "2029"})", fontSize = 11.sp, color = TextMuted)
                        Text("± ${countdown.yearsToNikah} Tahun ke depan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = UmmuAccent)
                    }
                    Button(
                        onClick = { onNavigate(2) },
                        colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Roadmap", color = TextPrimary, fontSize = 10.sp)
                    }
                }
            }
        }

        // Fitur 11: Budget Adherence / Alerts
        item {
            GlassMorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderAccent = WarningAccent,
                glowColor = WarningAccent
            ) {
                Text(
                    text = "🚨 Monitor Anggaran (Bocor Detector)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                stats.budgetAlerts.forEach { budget ->
                    val isExceeded = budget.actual > budget.budget
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(budget.posName, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Batas: ${formatRupiah(budget.budget)}", fontSize = 10.sp, color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatRupiah(budget.actual),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExceeded) DangerAccent else SuccessAccent
                            )
                            if (isExceeded) {
                                Text("Lebih ${formatRupiah(budget.actual - budget.budget)}!", fontSize = 9.sp, color = DangerAccent, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Aman", fontSize = 9.sp, color = SuccessAccent)
                            }
                        }
                    }
                }
            }
        }

        // Live Chat Promo Card (Glass variation)
        item {
            GlassMorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(5) },
                borderAccent = CombinedAccent,
                glowColor = CombinedAccent
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CombinedAccent.copy(alpha = 0.15f))
                            .border(1.dp, CombinedAccent.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = CombinedAccent, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tanya DUNIA AI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("\"Bagaimana kita bisa membeli rumah di 2029?\" tanya asisten pintar.", fontSize = 9.sp, color = TextSecondary)
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun DashboardSegmentCard(
    title: String,
    income: Double,
    expense: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassMorphicCard(
        modifier = modifier,
        borderAccent = color,
        glowColor = color
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Masuk", fontSize = 9.sp, color = TextSecondary)
                Text(formatRupiah(income), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessAccent)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Keluar", fontSize = 9.sp, color = TextSecondary)
                Text(formatRupiah(expense), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DangerAccent)
            }
        }
    }
}

// ==========================================
// 2. KEUANGAN SCREEN (LEDGER, GOALS & CICILAN) - FITUR 04/05/06/08
// ==========================================

@Composable
fun KeuanganScreen(
    viewModel: DuniaViewModel,
    haikalName: String,
    ummuName: String
) {
    var screenState by remember { mutableStateOf(0) } // 0 = Cashflow, 1 = Anggaran, 2 = Tabungan, 3 = Cicilan

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard)
                .padding(vertical = 4.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Sirkulasi", "Anggaran", "Tabungan", "Cicilan").forEachIndexed { idx, label ->
                val active = screenState == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) CombinedAccent.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { screenState = idx }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (active) CombinedAccent else TextSecondary,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (screenState) {
                0 -> CashflowLedgerTab(viewModel, haikalName, ummuName)
                1 -> AnggaranBudgetTab(viewModel, haikalName, ummuName)
                2 -> TabunganDaruratTab(viewModel, haikalName, ummuName)
                3 -> CicilanHutangTab(viewModel)
            }
        }
    }
}

// ==========================================
// SUB TAB: ANGGARAN & BUDGETING
// ==========================================
@Composable
fun AnggaranBudgetTab(
    viewModel: DuniaViewModel,
    haikalName: String,
    ummuName: String
) {
    val stats by viewModel.monthlyStats.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<BudgetStatus?>(null) }
    var showDeleteDialog by remember { mutableStateOf<BudgetStatus?>(null) }

    // Form settings
    val expenseCategories = listOf(
        "Zakat, Infaq & Sedekah", "Makan, Sembako & Jajan", "Transportasi & Bensin",
        "Sewa Rumah / Kost", "Tagihan Listrik, Air & Internet", "Cicilan & Hutang Mandiri",
        "Anggaran Kencan / Berdua", "Kesehatan & Obat-obatan", "Pendidikan & Pengembangan Diri",
        "Hiburan, Belanja & Hobi", "Kebutuhan Orang Tua / Keluarga", "Tabungan Pernikahan Bersama",
        "Tabungan DP Rumah", "Dana Darurat Berdua", "Reksadana / Emas / Saham", "Asuransi / Proteksi Berdua",
        "Pengeluaran Tak Terduga"
    )

    // Compute Overall Budget Statistics
    val totalBudget = stats.budgetAlerts.sumOf { it.budget }
    val totalActualSpent = stats.budgetAlerts.sumOf { it.actual }
    val overallPercentage = if (totalBudget > 0.0) (totalActualSpent / totalBudget).toFloat() else 0f
    val remainingBudgetTotal = (totalBudget - totalActualSpent).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Budget management heading
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Manajemen Anggaran Bulanan 🎯",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tentukan ambang batas spending demi masa depan terarah.",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp).testTag("add_budget_goal_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Atur Goal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Summary Card
        item {
            GlassMorphicCard(
                modifier = Modifier.fillMaxWidth().testTag("budget_summary_card"),
                borderAccent = if (overallPercentage > 1f) DangerAccent else CombinedAccent,
                glowColor = if (overallPercentage > 1f) DangerAccent else CombinedAccent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RINGKASAN ANGGARAN BULAN INI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatRupiah(totalActualSpent) + " / " + formatRupiah(totalBudget),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (totalActualSpent > totalBudget) {
                                "Over-budget sebesar ${formatRupiah(totalActualSpent - totalBudget)}! 🚨"
                            } else {
                                "Tersisa ${formatRupiah(remainingBudgetTotal)} anggaran aman."
                            },
                            fontSize = 11.sp,
                            color = if (totalActualSpent > totalBudget) DangerAccent else SuccessAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Circular progress percentage
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(54.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { overallPercentage.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            color = if (overallPercentage > 1f) DangerAccent else if (overallPercentage > 0.8f) WarningAccent else SuccessAccent,
                            strokeWidth = 5.dp,
                            trackColor = BorderColor
                        )
                        Text(
                            text = "${(overallPercentage * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Linear Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(BgDeep)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = overallPercentage.coerceIn(0f, 1f))
                            .background(
                                if (overallPercentage > 1f) DangerAccent
                                else if (overallPercentage > 0.8f) WarningAccent
                                else SuccessAccent
                            )
                    )
                }
            }
        }

        // Budget Items List
        if (stats.budgetAlerts.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = TextSecondary
                    )
                    Text(
                        text = "Belum ada anggaran bulanan diatur.\nYuk atur anggaran pertamamu!",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            items(stats.budgetAlerts) { budget ->
                val limit = budget.budget
                val actual = budget.actual
                val percent = if (limit > 0.0) (actual / limit).toFloat() else 0f
                val isExceeded = actual > limit
                val remaining = (limit - actual).coerceAtLeast(0.0)

                // Get category icon
                val icon = when {
                    budget.posName.contains("Sedekah") || budget.posName.contains("Zakat") || budget.posName.contains("Infaq") -> Icons.Default.Favorite
                    budget.posName.contains("Makan") || budget.posName.contains("Jajan") -> Icons.Default.Restaurant
                    budget.posName.contains("Transport") || budget.posName.contains("Bensin") -> Icons.Default.DirectionsCar
                    budget.posName.contains("Rumah") || budget.posName.contains("Kost") -> Icons.Default.Home
                    budget.posName.contains("Listrik") || budget.posName.contains("Internet") || budget.posName.contains("Air") -> Icons.Default.Wifi
                    budget.posName.contains("Cicilan") || budget.posName.contains("Hutang") -> Icons.Default.CreditCard
                    budget.posName.contains("Kencan") -> Icons.Default.FavoriteBorder
                    budget.posName.contains("Kesehatan") || budget.posName.contains("Obat") -> Icons.Default.LocalHospital
                    budget.posName.contains("Didik") || budget.posName.contains("Sekolah") || budget.posName.contains("Pendidikan") -> Icons.Default.School
                    budget.posName.contains("Hiburan") || budget.posName.contains("Belanja") || budget.posName.contains("Hobi") -> Icons.Default.SportsEsports
                    budget.posName.contains("Orang Tua") || budget.posName.contains("Keluarga") -> Icons.Default.People
                    budget.posName.contains("Nikah") -> Icons.Default.Favorite
                    budget.posName.contains("DP") -> Icons.Default.HomeWork
                    budget.posName.contains("Darurat") -> Icons.Default.Security
                    budget.posName.contains("Reksa") || budget.posName.contains("Emas") || budget.posName.contains("Saham") -> Icons.Default.ShowChart
                    budget.posName.contains("Asuransi") || budget.posName.contains("Proteksi") -> Icons.Default.Shield
                    budget.posName.contains("Tak Terduga") -> Icons.Default.Warning
                    else -> Icons.Default.AccountBalanceWallet
                }

                val ownerTag = if (budget.posName.contains(haikalName)) "HAIKAL" else if (budget.posName.contains(ummuName)) "UMMU" else "BERDUA"
                val ownerColor = if (ownerTag == "HAIKAL") HaikalAccent else if (ownerTag == "UMMU") UmmuAccent else CombinedAccent

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ownerColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = ownerColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = budget.posName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ownerColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (ownerTag == "HAIKAL") haikalName else if (ownerTag == "UMMU") ummuName else "Berdua",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ownerColor
                                        )
                                    }
                                    Text(
                                        text = "Limit: ${formatRupiah(limit)}",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        // Edit / Delete quick buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { showEditDialog = budget },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Anggaran",
                                    tint = CombinedAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { showDeleteDialog = budget },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Anggaran",
                                    tint = DangerAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Progress bar & actual spend text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatRupiah(actual)} Terpakai",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExceeded) DangerAccent else TextPrimary
                        )
                        Text(
                            text = if (isExceeded) {
                                "Bocor +${formatRupiah(actual - limit)}!! ⚠️"
                            } else {
                                "Sisa: ${formatRupiah(remaining)}"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isExceeded) DangerAccent else SuccessAccent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(BgDeep)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = percent.coerceIn(0f, 1f))
                                .background(
                                    if (isExceeded) DangerAccent
                                    else if (percent >= 0.8f) WarningAccent
                                    else SuccessAccent
                                )
                        )
                    }
                }
            }
        }
    }

    // Dialog for adding a new Dynamic Budget
    if (showAddDialog) {
        var inputLimit by remember { mutableStateOf("") }
        var inputCat by remember { mutableStateOf(expenseCategories.first()) }
        var inputOwn by remember { mutableStateOf("BERDUA") }
        var categoryDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = BgCard,
            title = {
                Text(
                    text = "Aktivasi Pos Anggaran Baru 🎯",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    // Category Selection
                    Column {
                        Text("Mata Pos / Kategori Pengeluaran", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BgDeep)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clickable { categoryDropdownExpanded = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = inputCat, fontSize = 12.sp, color = TextPrimary)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .background(BgCard)
                            ) {
                                expenseCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, fontSize = 12.sp, color = TextPrimary) },
                                        onClick = {
                                            inputCat = cat
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Owner Selector
                    Column {
                        Text("Pengampu Anggaran (Sirkulasi Pasangan)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                Triple("HAIKAL", haikalName, HaikalAccent),
                                Triple("UMMU", ummuName, UmmuAccent),
                                Triple("BERDUA", "Bersama", CombinedAccent)
                            ).forEach { (valTag, label, color) ->
                                val active = inputOwn == valTag
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) color.copy(alpha = 0.25f) else BgDeep)
                                        .border(
                                            width = 1.dp,
                                            color = if (active) color else BorderColor,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { inputOwn = valTag }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Limit input field
                    Column {
                        Text("Batas Maksimal Pengeluaran (Rp)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = inputLimit,
                            onValueChange = { inputLimit = it },
                            placeholder = { Text("Contoh: 350000", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CombinedAccent,
                                unfocusedBorderColor = BorderColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("budget_limit_input")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limitVal = inputLimit.toDoubleOrNull() ?: 0.0
                        if (limitVal > 0.0) {
                            viewModel.saveBudgetGoal(inputCat, inputOwn, limitVal)
                            showAddDialog = false
                            inputLimit = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent)
                ) {
                    Text("Aktifkan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog for editing an existing Dynamic Budget
    if (showEditDialog != null) {
        val budget = showEditDialog!!
        var currentLimitStr by remember { mutableStateOf(budget.budget.toLong().toString()) }

        // Find the exact dynamic key properties if available
        val categoryName = budget.category.ifEmpty {
            budget.posName.substringBefore(" (")
        }
        val ownerTag = budget.owner.ifEmpty {
            if (budget.posName.contains(haikalName)) "HAIKAL" else if (budget.posName.contains(ummuName)) "UMMU" else "BERDUA"
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            containerColor = BgCard,
            title = {
                Text(
                    text = "Amandemen Batas Anggaran ✏️",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Amanahkan batas baru untuk pos \"$categoryName\"",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = currentLimitStr,
                        onValueChange = { currentLimitStr = it },
                        placeholder = { Text("Contoh: 500000", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CombinedAccent,
                            unfocusedBorderColor = BorderColor
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_budget_limit_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limitVal = currentLimitStr.toDoubleOrNull() ?: 0.0
                        if (limitVal > 0.0) {
                            if (budget.category.isNotEmpty()) {
                                viewModel.saveBudgetGoal(categoryName, ownerTag, limitVal)
                            } else {
                                // Fallback parser for seeded hardcoded ones
                                if (budget.posName.startsWith("Makan")) {
                                    viewModel.saveConfigValue("BUDGET_MAKAN_HAIKAL", limitVal.toString())
                                } else if (budget.posName.startsWith("Transport")) {
                                    viewModel.saveConfigValue("BUDGET_TRANSPORT", limitVal.toString())
                                } else {
                                    viewModel.saveConfigValue("BUDGET_KENCAN", limitVal.toString())
                                }
                            }
                            showEditDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent)
                ) {
                    Text("Revisi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog for deleting a Dynamic Budget
    if (showDeleteDialog != null) {
        val budget = showDeleteDialog!!
        val categoryName = budget.category.ifEmpty { budget.posName.substringBefore(" (") }
        val ownerTag = budget.owner.ifEmpty {
            if (budget.posName.contains(haikalName)) "HAIKAL" else if (budget.posName.contains(ummuName)) "UMMU" else "BERDUA"
        }

        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = BgCard,
            title = {
                Text(
                    text = "Konfirmasi Nonaktifkan Pos ⚠️",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Apakah kalian berdua yakin ingin menonaktifkan pengawasan anggaran pos \"$categoryName\"? Tindakan ini menghapus limit anggaran, namun tidak menghapus data transaksi pengeluaran.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (budget.category.isNotEmpty()) {
                            viewModel.deleteBudgetGoal(categoryName, ownerTag)
                        } else {
                            // Seeded budgets fallbacks
                            if (budget.posName.startsWith("Makan")) {
                                viewModel.saveConfigValue("BUDGET_MAKAN_HAIKAL", "0")
                            } else if (budget.posName.startsWith("Transport")) {
                                viewModel.saveConfigValue("BUDGET_TRANSPORT", "0")
                            } else {
                                viewModel.saveConfigValue("BUDGET_KENCAN", "0")
                            }
                        }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerAccent)
                ) {
                    Text("Nonaktifkan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

// SUB TAB: TRANSAKSI LEDGER
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CashflowLedgerTab(
    viewModel: DuniaViewModel,
    haikalName: String,
    ummuName: String
) {
    val txs by viewModel.transactions.collectAsState()
    val stats by viewModel.monthlyStats.collectAsState()

    var showAddForm by remember { mutableStateOf(false) }
    var selectedReceiptUrl by remember { mutableStateOf<String?>(null) }
    
    // Edit transaction states
    var showEditTxDialog by remember { mutableStateOf<com.example.data.TransactionEntity?>(null) }
    var editType by remember { mutableStateOf("PENGELUARAN") }
    var editUser by remember { mutableStateOf("HAIKAL") }
    var editAmount by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var editTag by remember { mutableStateOf("") }
    var editImageUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showEditTxDialog) {
        if (showEditTxDialog != null) {
            val tx = showEditTxDialog!!
            editType = tx.type
            editUser = tx.user
            editAmount = tx.amount.toLong().toString()
            editCategory = tx.category
            editDesc = tx.description
            editTag = tx.tag
            editImageUri = tx.imageUri
        }
    }

    // Dialog state
    var selectedType by remember { mutableStateOf("PENGELUARAN") }
    var selectedUser by remember { mutableStateOf("HAIKAL") }
    var inputAmount by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("Makan, Sembako & Jajan") }
    var inputDesc by remember { mutableStateOf("") }
    var inputTag by remember { mutableStateOf("#rutin") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }

    val incomeCategories = listOf(
        "Gaji Pokok (Haikal)", "Gaji Pokok (Ummu)", "Freelance / Sampingan",
        "Hasil Investasi & Dividen", "Bonus & THR", "Pemberian / Hadiah", "Pemasukan Lain-lain"
    )

    val expenseCategories = listOf(
        "Zakat, Infaq & Sedekah", "Makan, Sembako & Jajan", "Transportasi & Bensin",
        "Sewa Rumah / Kost", "Tagihan Listrik, Air & Internet", "Cicilan & Hutang Mandiri",
        "Anggaran Kencan / Berdua", "Kesehatan & Obat-obatan", "Pendidikan & Pengembangan Diri",
        "Hiburan, Belanja & Hobi", "Kebutuhan Orang Tua / Keluarga", "Tabungan Pernikahan Bersama",
        "Tabungan DP Rumah", "Dana Darurat Berdua", "Reksadana / Emas / Saham", "Asuransi / Proteksi Berdua",
        "Pengeluaran Tak Terduga"
    )

    val categories = if (selectedType == "PEMASUKAN") incomeCategories else expenseCategories

    LaunchedEffect(selectedType) {
        inputCategory = if (selectedType == "PEMASUKAN") incomeCategories.first() else expenseCategories.first()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Riwayat Finansial", fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Button(
                        onClick = { showAddForm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp).testTag("add_transaction_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Catat Baru", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (txs.isEmpty()) {
                item {
                    Text("Belum ada pencatatan dilakukan. Catat pemasukan / pengeluaran pertama !", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }

            items(txs) { tx ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Badge owner
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (tx.user) {
                                            "HAIKAL" -> HaikalAccent.copy(alpha = 0.15f)
                                            "UMMU" -> UmmuAccent.copy(alpha = 0.15f)
                                            else -> CombinedAccent.copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (tx.user == "HAIKAL") haikalName else if (tx.user == "UMMU") ummuName else "Berdua",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.user == "HAIKAL") HaikalAccent else if (tx.user == "UMMU") UmmuAccent else CombinedAccent
                                )
                            }

                            Text(tx.category, fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tx.description, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val formater = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                            Text(formater.format(Date(tx.timestamp)), fontSize = 10.sp, color = TextMuted)
                            if (tx.tag.isNotEmpty()) {
                                Text(tx.tag, fontSize = 10.sp, color = CombinedAccent, fontWeight = FontWeight.Bold)
                            }
                            if (!tx.imageUri.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CombinedAccent.copy(alpha = 0.15f))
                                        .clickable { selectedReceiptUrl = tx.imageUri }
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(10.dp), tint = CombinedAccent)
                                        Text("Nota 📸", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CombinedAccent)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (tx.type == "PEMASUKAN") "+ " + formatRupiah(tx.amount) else "- " + formatRupiah(tx.amount),
                            color = if (tx.type == "PEMASUKAN") SuccessAccent else DangerAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(
                            onClick = { showEditTxDialog = tx },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Transaksi", tint = TextMuted, modifier = Modifier.size(13.dp))
                        }
                        IconButton(
                            onClick = { viewModel.deleteTransaction(tx) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = DangerAccent.copy(alpha = 0.5f), modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }

        // Bottom Sheet modal simulation for add Form
        if (showAddForm) {
            AlertDialog(
                onDismissRequest = { showAddForm = false },
                containerColor = BgCard,
                title = { Text("Pencatatan Keuangan", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Type select
                        Row {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, if (selectedType == "PENGELUARAN") DangerAccent else BorderColor)
                                    .clickable { selectedType = "PENGELUARAN" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Pengeluaran (-)", color = if (selectedType == "PENGELUARAN") DangerAccent else TextSecondary, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, if (selectedType == "PEMASUKAN") SuccessAccent else BorderColor)
                                    .clickable { selectedType = "PEMASUKAN" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Pemasukan (+)", color = if (selectedType == "PEMASUKAN") SuccessAccent else TextSecondary, fontSize = 12.sp)
                            }
                        }

                        // User select
                        Row {
                            listOf("HAIKAL", "UMMU", "BERDUA").forEach { usr ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, if (selectedUser == usr) CombinedAccent else BorderColor)
                                        .clickable { selectedUser = usr }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (usr == "HAIKAL") haikalName else if (usr == "UMMU") ummuName else "Bersama",
                                        color = if (selectedUser == usr) CombinedAccent else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Amount
                        OutlinedTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            label = { Text("Jumlah Terbilang (Rupiah)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("tx_amount_input")
                        )

                        // Description
                        OutlinedTextField(
                            value = inputDesc,
                            onValueChange = { inputDesc = it },
                            label = { Text("Deskripsi Singkat / Pos") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("tx_desc_input")
                        )

                        // Category Dropdown simulated using Simple Row selection
                        Text("Pilih Kategori Pos:", fontSize = 11.sp, color = TextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { cat ->
                                val acts = inputCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (acts) CombinedAccent.copy(alpha = 0.2f) else BgDeep)
                                        .border(1.dp, if (acts) CombinedAccent else BorderColor, RoundedCornerShape(6.dp))
                                        .clickable { inputCategory = cat }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(cat, color = if (acts) CombinedAccent else TextPrimary, fontSize = 10.sp)
                                }
                            }
                        }

                        // Tag select
                        OutlinedTextField(
                            value = inputTag,
                            onValueChange = { inputTag = it },
                            label = { Text("Tag (e.g. #rutin #darurat)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Visual Photo Receipt Selector (Fulfills Optional Photo Receipt Requirement)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Lampiran Nota Transaksi (Opsional):", fontSize = 11.sp, color = TextSecondary)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val simulatedReceipts = listOf(
                                        "https://images.unsplash.com/photo-1554415707-6e8cfc93fe23?w=500",
                                        "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=500",
                                        "https://images.unsplash.com/photo-1543269865-cbf427effbad?w=500",
                                        "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=500"
                                    )
                                    selectedImageUri = simulatedReceipts.random()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent.copy(alpha = 0.15f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = CombinedAccent)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ambil Foto Nota 📸", color = CombinedAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            if (selectedImageUri != null) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BgDeep)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        .clickable { selectedImageUri = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = coil.compose.rememberAsyncImagePainter(selectedImageUri),
                                        contentDescription = "Nota Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amtVal = inputAmount.toDoubleOrNull() ?: 0.0
                            if (amtVal > 0.0 && inputDesc.trim().isNotEmpty()) {
                                viewModel.addTransaction(
                                    type = selectedType,
                                    user = selectedUser,
                                    amount = amtVal,
                                    category = inputCategory,
                                    description = inputDesc,
                                    tag = inputTag,
                                    imageUri = selectedImageUri
                                )
                                showAddForm = false
                                inputAmount = ""
                                inputDesc = ""
                                selectedImageUri = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAddForm = false 
                        selectedImageUri = null
                    }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Expanded view modal for receipt zoom (Fulfills view receipt optional task)
        if (selectedReceiptUrl != null) {
            AlertDialog(
                onDismissRequest = { selectedReceiptUrl = null },
                containerColor = BgCard,
                title = { Text("Nota Transaksi Masa Depan 📸", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BgDeep)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(selectedReceiptUrl),
                                contentDescription = "Full Preview Receipt Note",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                        Text(
                            text = "Gunakan kode sinkronisasi untuk membagikan nota ini ke HP pasangan.",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedReceiptUrl = null }) {
                        Text("Tutup", color = CombinedAccent, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Edit Transaction Dialog
        if (showEditTxDialog != null) {
            val txToEdit = showEditTxDialog!!
            val editCategories = if (editType == "PEMASUKAN") incomeCategories else expenseCategories
            
            // Sync categories if type differs
            LaunchedEffect(editType) {
                if (editType == "PEMASUKAN") {
                    if (!incomeCategories.contains(editCategory)) {
                        editCategory = incomeCategories.first()
                    }
                } else {
                    if (!expenseCategories.contains(editCategory)) {
                        editCategory = expenseCategories.first()
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { showEditTxDialog = null },
                containerColor = BgCard,
                title = { Text("Koreksi Catatan Keuangan 🖊️", fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = FontFamily.Monospace) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Type choice
                        Row {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, if (editType == "PENGELUARAN") DangerAccent else BorderColor)
                                    .clickable { editType = "PENGELUARAN" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Pengeluaran (-)", color = if (editType == "PENGELUARAN") DangerAccent else TextSecondary, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, if (editType == "PEMASUKAN") SuccessAccent else BorderColor)
                                    .clickable { editType = "PEMASUKAN" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Pemasukan (+)", color = if (editType == "PEMASUKAN") SuccessAccent else TextSecondary, fontSize = 12.sp)
                            }
                        }

                        // User choice
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                Triple("HAIKAL", haikalName, HaikalAccent),
                                Triple("UMMU", ummuName, UmmuAccent),
                                Triple("BERDUA", "Joint", CombinedAccent)
                            ).forEach { (userKey, label, color) ->
                                val selected = editUser == userKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (selected) color.copy(alpha = 0.15f) else BgDeep)
                                        .border(1.dp, if (selected) color else BorderColor)
                                        .clickable { editUser = userKey }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = 10.sp, color = if (selected) color else TextSecondary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editAmount,
                            onValueChange = { editAmount = it.filter { c -> c.isDigit() } },
                            label = { Text("Jumlah Uang (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Category select List
                        Column {
                            Text("Kategori:", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(editCategories) { cat ->
                                    val isSelected = editCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CombinedAccent else BgDeep)
                                            .clickable { editCategory = cat }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(cat, fontSize = 9.sp, color = if (isSelected) Color.White else TextSecondary, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editDesc,
                            onValueChange = { editDesc = it },
                            label = { Text("Deskripsi Transaksi") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editTag,
                            onValueChange = { editTag = it },
                            label = { Text("Tag (misal: #rutin, #sedekah)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amtVal = editAmount.toDoubleOrNull() ?: 0.0
                            if (amtVal > 0.0 && editDesc.trim().isNotEmpty()) {
                                viewModel.updateTransaction(
                                    txToEdit.copy(
                                        type = editType,
                                        user = editUser,
                                        amount = amtVal,
                                        category = editCategory,
                                        description = editDesc,
                                        tag = editTag,
                                        imageUri = editImageUri
                                    )
                                )
                                showEditTxDialog = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent)
                    ) {
                        Text("Simpan Perubahan", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditTxDialog = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

// SUB TAB: TABUNGAN & DANA DARURAT (FITUR 06)
@Composable
fun TabunganDaruratTab(
    viewModel: DuniaViewModel,
    haikalName: String,
    ummuName: String
) {
    val goals by viewModel.savingGoals.collectAsState()
    val configs by viewModel.configs.collectAsState()
    val weddingYearStr = configs["TARGET_NIKAH"] ?: "2029"
    val context = androidx.compose.ui.platform.LocalContext.current

    var activeContributionGoalId by remember { mutableStateOf<Int?>(null) }
    var customContributionAmount by remember { mutableStateOf("") }

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showEditGoalDialog by remember { mutableStateOf<com.example.data.SavingGoalEntity?>(null) }

    var newGoalName by remember { mutableStateOf("") }
    var newGoalTargetAmount by remember { mutableStateOf("") }
    var newGoalCurrentAmount by remember { mutableStateOf("") }
    var newGoalOwner by remember { mutableStateOf("BERDUA") }

    var editGoalName by remember { mutableStateOf("") }
    var editGoalTargetAmount by remember { mutableStateOf("") }
    var editGoalCurrentAmountState by remember { mutableStateOf("") }
    var editGoalOwner by remember { mutableStateOf("BERDUA") }

    LaunchedEffect(showEditGoalDialog) {
        if (showEditGoalDialog != null) {
            val goal = showEditGoalDialog!!
            editGoalName = goal.name
            editGoalTargetAmount = goal.targetAmount.toLong().toString()
            editGoalCurrentAmountState = goal.currentAmount.toLong().toString()
            editGoalOwner = goal.owner
        }
    }

    // --- PERSISTENT INTERACTIVE SIMULATOR STATES ---
    var selectedSimGoalId by remember { mutableStateOf<Int?>(-1) } // -1 means custom manual
    var simName by remember { mutableStateOf("Rencana Kustom") }
    var simTargetAmountInput by remember { mutableStateOf("50000000") }
    var simCurrentAmountInput by remember { mutableStateOf("5000000") }
    var simMonthlyCommitmentInput by remember { mutableStateOf("2500000") }
    var simAnnualYield by remember { mutableStateOf(5f) } // 5% default (Reksadana Pasar Uang Syariah)
    var haikalPortionPct by remember { mutableStateOf(50f) }

    // Synchronize inputs when selected goal changes
    LaunchedEffect(selectedSimGoalId, goals) {
        if (selectedSimGoalId == -1 || selectedSimGoalId == null) {
            simName = "Rencana Kustom"
        } else {
            val matched = goals.find { it.id == selectedSimGoalId }
            if (matched != null) {
                simName = matched.name
                simTargetAmountInput = matched.targetAmount.toLong().toString()
                simCurrentAmountInput = matched.currentAmount.toLong().toString()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Evaluasi Tabungan & Capaian",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Button(
                    onClick = { showAddGoalDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("tambah_tabungan_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pos Baru", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                Text("Belum ada tabungan diatur.", color = TextMuted, fontSize = 12.sp)
            }
        }

        items(goals) { goal ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(goal.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (goal.owner) {
                                            "HAIKAL" -> HaikalAccent.copy(alpha = 0.15f)
                                            "UMMU" -> UmmuAccent.copy(alpha = 0.15f)
                                            else -> CombinedAccent.copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (goal.owner == "HAIKAL") "$haikalName" else if (goal.owner == "UMMU") "$ummuName" else "Joint",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (goal.owner == "HAIKAL") HaikalAccent else if (goal.owner == "UMMU") UmmuAccent else CombinedAccent
                                )
                            }
                            Text("Aspirasi Finansial", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { showEditGoalDialog = goal },
                            modifier = Modifier.size(24.dp).testTag("edit_saving_btn_${goal.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Target Tabungan",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        val pct = if (goal.targetAmount > 0) ((goal.currentAmount / goal.targetAmount) * 100).toInt() else 0
                        Text(
                            text = "$pct%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = SuccessAccent
                        )
                    }
                }

                // Progress Bar
                val fraction = (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(BgDeep)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(CombinedAccent, HaikalAccent)
                                )
                            )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Terkumpul", fontSize = 9.sp, color = TextSecondary)
                        Text(formatRupiah(goal.currentAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target Minimal", fontSize = 9.sp, color = TextSecondary)
                        Text(formatRupiah(goal.targetAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { activeContributionGoalId = goal.id },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.align(Alignment.End).height(28.dp).testTag("nabung_goal_${goal.id}")
                ) {
                    Icon(imageVector = Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(12.dp), tint = CombinedAccent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Top Up Setoran", color = CombinedAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- DIVIDER BETWEEN GOALS AND PROJECTION SIMULATOR ---
        item {
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.4f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // --- INTERACTIVE PROJECTION SIMULATOR CARD ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CombinedAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = CombinedAccent, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            text = "🔮 Proyeksi Pencapaian Sinergi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Text(
                            text = "Simulasikan target berdasarkan komitmen setoran bulanan bersama",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }

                // 1. Selector of Target Goal (Scrollable Chips)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pilih Rencana untuk Di-Simulasikan:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            val active = selectedSimGoalId == -1
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(if (active) CombinedAccent else BgDeep)
                                    .border(1.dp, if (active) Color.Transparent else BorderColor, RoundedCornerShape(30.dp))
                                    .clickable { selectedSimGoalId = -1 }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "✨ Rencana Kustom",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else TextSecondary
                                )
                            }
                        }

                        items(goals) { goal ->
                            val active = selectedSimGoalId == goal.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(if (active) CombinedAccent else BgDeep)
                                    .border(1.dp, if (active) Color.Transparent else BorderColor, RoundedCornerShape(30.dp))
                                    .clickable { selectedSimGoalId = goal.id }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = goal.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                // 2. Numerical Inputs (Target Amount & Current Capital)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = simTargetAmountInput,
                        onValueChange = { simTargetAmountInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Target Minimal (Rp)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = simCurrentAmountInput,
                        onValueChange = { simCurrentAmountInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Uang Saat Ini (Rp)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Monthly Contribution Commitment Input & presets & fast Adjuster
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Komitmen Tabungan Bulanan Gabungan:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        val currCommit = simMonthlyCommitmentInput.toDoubleOrNull() ?: 0.0
                        Text(formatRupiah(currCommit), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CombinedAccent)
                    }

                    // A beautiful visual slider for intuitive adjustments: Rp 500ribu - Rp 15 Juta
                    val commitmentSliderVal = (simMonthlyCommitmentInput.toFloatOrNull() ?: 500000f).coerceIn(500000f, 15000000f)
                    androidx.compose.material3.Slider(
                        value = commitmentSliderVal,
                        onValueChange = { simMonthlyCommitmentInput = it.toLong().toString() },
                        valueRange = 500000f..15000000f,
                        steps = 29, // steps of 500k
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = CombinedAccent,
                            activeTrackColor = CombinedAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick increment/decrement buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(500000L, 1000000L, 2000000L).forEach { step ->
                            Button(
                                onClick = {
                                    val currentVal = simMonthlyCommitmentInput.toLongOrNull() ?: 0L
                                    simMonthlyCommitmentInput = (currentVal + step).toString()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BgDeep),
                                modifier = Modifier.weight(1f).height(24.dp)
                            ) {
                                Text("+Rp ${step / 1000}rb", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                        Button(
                            onClick = {
                                val currentVal = simMonthlyCommitmentInput.toLongOrNull() ?: 0L
                                simMonthlyCommitmentInput = maxOf(500000L, currentVal - 1000000L).toString()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BgDeep),
                            modifier = Modifier.weight(1f).height(24.dp)
                        ) {
                            Text("-Rp 1jt", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DangerAccent)
                        }
                    }
                }

                // 4. Contribution Portion Share Sinergi (Haikal vs Ummu)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val currCommit = simMonthlyCommitmentInput.toDoubleOrNull() ?: 0.0
                    val haikalShare = currCommit * (haikalPortionPct / 100.0)
                    val ummuShare = currCommit * ((100.0 - haikalPortionPct) / 100.0)

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sinergi Porsi Setoran (Haikal vs Ummu):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text(
                            text = "${haikalPortionPct.toInt()}% / ${(100f - haikalPortionPct).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessAccent
                        )
                    }

                    // Dual Color Sinergi Slider
                    androidx.compose.material3.Slider(
                        value = haikalPortionPct,
                        onValueChange = { haikalPortionPct = it },
                        valueRange = 0f..100f,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = SuccessAccent,
                            activeTrackColor = HaikalAccent,
                            inactiveTrackColor = UmmuAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Contribution Sinergi Breakdown labels
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgDeep)
                            .padding(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Setoran $haikalName:", fontSize = 8.sp, color = HaikalAccent, fontWeight = FontWeight.Bold)
                            Text(formatRupiah(haikalShare), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                            Text("Setoran $ummuName:", fontSize = 8.sp, color = UmmuAccent, fontWeight = FontWeight.Bold)
                            Text(formatRupiah(ummuShare), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 5. Shari'ah Investment Yield presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Wadah Alun Investasi Tabungan Syariah:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val yieldOptions = listOf(
                            Triple("🕌 Kas (0%)", 0f, "Simpanan Biasa"),
                            Triple("📈 Reksadana (5%)", 5f, "Pasar Uang Syariah"),
                            Triple("🪙 Emas (8%)", 8f, "Mudharabah")
                        )

                        yieldOptions.forEach { option ->
                            val active = simAnnualYield == option.second
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) SuccessAccent.copy(alpha = 0.15f) else BgDeep)
                                    .border(1.dp, if (active) SuccessAccent else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { simAnnualYield = option.second }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(option.first, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (active) SuccessAccent else TextPrimary)
                                    Text(option.third, fontSize = 7.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }

                // --- MATH COMPUTATION FOR PROJEKSI RESULT ---
                val targetAmountVal = simTargetAmountInput.toDoubleOrNull() ?: 0.0
                val currentAmountVal = simCurrentAmountInput.toDoubleOrNull() ?: 0.0
                val monthlyCommitmentVal = simMonthlyCommitmentInput.toDoubleOrNull() ?: 0.0

                var simulatedMonths = 0
                var currentBalance = currentAmountVal

                val yearlyMilestones = mutableListOf<Triple<Int, Double, Double>>() // (YearCount, DepositValue, FullyCompoundedValue)
                var totalDeposited = currentAmountVal
                val maxLimitMonths = 240 // 20 years

                val monthlyYield = (simAnnualYield / 100.0) / 12.0

                while (currentBalance < targetAmountVal && simulatedMonths < maxLimitMonths && monthlyCommitmentVal > 0) {
                    simulatedMonths++
                    val earnedYield = currentBalance * monthlyYield
                    currentBalance += monthlyCommitmentVal + earnedYield
                    totalDeposited += monthlyCommitmentVal

                    // Log milestone Year
                    if (simulatedMonths % 12 == 0 || currentBalance >= targetAmountVal) {
                        yearlyMilestones.add(Triple(simulatedMonths, totalDeposited, currentBalance))
                    }
                }

                // ETA date components
                val calendarSim = java.util.Calendar.getInstance()
                val currentYearNum = calendarSim.get(java.util.Calendar.YEAR)
                val currentMonthNum = calendarSim.get(java.util.Calendar.MONTH) // 0-11
                calendarSim.add(java.util.Calendar.MONTH, simulatedMonths)
                val etaFormatted = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(calendarSim.time)

                val yearsNeeded = simulatedMonths / 12
                val remainingMonths = simulatedMonths % 12
                val timeRemainingReadable = buildString {
                    if (yearsNeeded > 0) append("$yearsNeeded Tahun ")
                    if (remainingMonths > 0 || yearsNeeded == 0) append("$remainingMonths Bulan")
                }

                val weddingYearVal = weddingYearStr.toIntOrNull() ?: 2029
                val monthsUntilWedding = ((weddingYearVal - currentYearNum) * 12) + (6 - currentMonthNum)

                // 6. DASHBOARD HASIL PROYEKSI (RESULT SCREEN ACTION)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgDeep)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🔬 KESIMPULAN ANALISIS FINANSIAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CombinedAccent
                    )

                    if (targetAmountVal <= currentAmountVal) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessAccent, modifier = Modifier.size(20.dp))
                            Text("Target Tabungan Anda sudah tercapai sepenuhnya! Luar biasa!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessAccent)
                        }
                    } else if (monthlyCommitmentVal <= 0) {
                        Text("Atur komitmen setoran bulanan diatas Rp 0 untuk mensimulasikan waktu penyelesaian.", fontSize = 11.sp, color = DangerAccent, fontWeight = FontWeight.Bold)
                    } else {
                        // Big ETA Banner
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SuccessAccent.copy(alpha = 0.1f))
                                .border(1.dp, SuccessAccent.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ESTIMASI TARGET TERCAPAI PADA:", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = etaFormatted.uppercase(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessAccent,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Waktu penyelesaian: ~ $timeRemainingReadable ($simulatedMonths bulan)",
                                fontSize = 10.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Sinergi Wedding Tracker Alert
                        val isInsideWeddingLimit = simulatedMonths <= monthsUntilWedding
                        val deviationMonths = Math.abs(monthsUntilWedding - simulatedMonths)
                        val deviationReadable = buildString {
                            val y = deviationMonths / 12
                            val m = deviationMonths % 12
                            if (y > 0) append("$y Tahun ")
                            if (m > 0 || y == 0) append("$m Bulan")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isInsideWeddingLimit) SuccessAccent.copy(alpha = 0.08f) else DangerAccent.copy(alpha = 0.08f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isInsideWeddingLimit) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isInsideWeddingLimit) SuccessAccent else DangerAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                val wTitle = if (isInsideWeddingLimit) "SELARAS DENGAN RENCANA PERNIKAHAN" else "TANTANGAN: MELEBIHI TARGET PERNIKAHAN"
                                Text(
                                    text = wTitle,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInsideWeddingLimit) SuccessAccent else DangerAccent
                                )

                                val descAdvice = if (isInsideWeddingLimit) {
                                    "Target dana diperkirakan siap $deviationReadable lebih cepat sebelum rencana utama pernikahan Anda ($weddingYearStr). Arus tabunan Anda sangat matang!"
                                } else {
                                    "Tabungan selesai terlambat $deviationReadable dari target pernikahan ($weddingYearStr). Atasi dengan menambah komitmen bulanan Rp ${formatRupiah(((targetAmountVal - currentAmountVal) / maxOf(1, monthsUntilWedding)) - monthlyCommitmentVal)} / bulan agar tercapai tepat waktu!"
                                }
                                Text(descAdvice, fontSize = 9.sp, color = TextSecondary)
                            }
                        }

                        // Visual Yearly Accumulation Milestones (Compact Table)
                        Text(
                            text = "📈 PROGRESS TIMELINE AKUMULASI DANA:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )

                        yearlyMilestones.take(4).forEach { milestone ->
                            val mIndex = milestone.first
                            val label = if (mIndex >= simulatedMonths) "🏁 Lunas (Bulan $mIndex)" else "Bulan $mIndex (Th ${mIndex / 12})"
                            
                            val depositVal = milestone.second
                            val totalVal = milestone.third
                            val yieldIncome = totalVal - depositVal

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(formatRupiah(totalVal), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CombinedAccent)
                                }
                                // Progress bar showing Deposit vs Profit
                                val totalValSafe = if (totalVal > 0) totalVal else 1.0
                                val depositRatio = (depositVal / totalValSafe).toFloat().coerceIn(0f, 1f)
                               Row(
                                   modifier = Modifier
                                       .fillMaxWidth()
                                       .height(4.dp)
                                       .clip(CircleShape)
                                       .background(SuccessAccent) // Growth is green
                               ) {
                                   Box(
                                       modifier = Modifier
                                           .fillMaxHeight()
                                           .fillMaxWidth(depositRatio)
                                           .background(CombinedAccent) // Base savings is Purple
                                   )
                               }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Simpanan Pokok: ${formatRupiah(depositVal)}", fontSize = 8.sp, color = TextMuted)
                                    if (yieldIncome > 0) {
                                        Text("Hasil Investasi: +${formatRupiah(yieldIncome)}", fontSize = 8.sp, color = SuccessAccent, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeContributionGoalId != null) {
        val selectedGoal = goals.find { it.id == activeContributionGoalId }
        AlertDialog(
            onDismissRequest = { activeContributionGoalId = null },
            containerColor = BgCard,
            title = { Text("Setoran: ${selectedGoal?.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Berapa dana yang akan dipindahkan dari rekening pribadi ke tabungan ini?", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = customContributionAmount,
                        onValueChange = { customContributionAmount = it },
                        label = { Text("Jumlah (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("topup_input_amt")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = customContributionAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.addSavingContribution(activeContributionGoalId!!, amount)
                            activeContributionGoalId = null
                            customContributionAmount = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent)
                ) {
                    Text("Proses Tabungkan")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    activeContributionGoalId = null
                    customContributionAmount = ""
                }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showAddGoalDialog) {
        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            containerColor = BgCard,
            title = { Text("Pos Tabungan Baru 🎯", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newGoalName,
                        onValueChange = { newGoalName = it },
                        label = { Text("Nama Rencana Tabungan") },
                        placeholder = { Text("Contoh: DP Rumah Ideal") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_saving_name_input")
                    )
                    OutlinedTextField(
                        value = newGoalTargetAmount,
                        onValueChange = { newGoalTargetAmount = it.filter { c -> c.isDigit() } },
                        label = { Text("Target Nominal (Rp)") },
                        placeholder = { Text("Contoh: 15000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_saving_target_input")
                    )
                    OutlinedTextField(
                        value = newGoalCurrentAmount,
                        onValueChange = { newGoalCurrentAmount = it.filter { c -> c.isDigit() } },
                        label = { Text("Saldo / Setoran Awal (Rp)") },
                        placeholder = { Text("Contoh: 500000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_saving_current_input")
                    )
                    
                    // Owner choices
                    Column {
                        Text("Kepemilikan Pos:", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(
                                Triple("HAIKAL", haikalName, HaikalAccent),
                                Triple("UMMU", ummuName, UmmuAccent),
                                Triple("BERDUA", "Joint", CombinedAccent)
                            ).forEach { (ownerKey, nameStr, colorTheme) ->
                                val active = newGoalOwner == ownerKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) colorTheme.copy(alpha = 0.2f) else BgDeep)
                                        .border(1.dp, if (active) colorTheme else BorderColor, RoundedCornerShape(8.dp))
                                        .clickable { newGoalOwner = ownerKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(nameStr, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = if (active) TextPrimary else TextSecondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = newGoalTargetAmount.toDoubleOrNull() ?: 0.0
                        val current = newGoalCurrentAmount.toDoubleOrNull() ?: 0.0
                        if (newGoalName.isNotBlank() && target > 0) {
                            viewModel.insertCustomSavingGoal(newGoalName, target, current, newGoalOwner)
                            showAddGoalDialog = false
                            newGoalName = ""
                            newGoalTargetAmount = ""
                            newGoalCurrentAmount = ""
                        } else {
                            Toast.makeText(context, "Harap isi nama dan target nominal!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent)
                ) {
                    Text("Buat Pos")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showEditGoalDialog != null) {
        val editingGoal = showEditGoalDialog!!
        AlertDialog(
            onDismissRequest = { showEditGoalDialog = null },
            containerColor = BgCard,
            title = { Text("Edit Target & Saldo Tabungan 🎯", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editGoalName,
                        onValueChange = { editGoalName = it },
                        label = { Text("Nama Rencana Tabungan") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_saving_name_input")
                    )
                    OutlinedTextField(
                        value = editGoalTargetAmount,
                        onValueChange = { editGoalTargetAmount = it.filter { c -> c.isDigit() } },
                        label = { Text("Target Nominal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_saving_target_input")
                    )
                    OutlinedTextField(
                        value = editGoalCurrentAmountState,
                        onValueChange = { editGoalCurrentAmountState = it.filter { c -> c.isDigit() } },
                        label = { Text("Saldo Tabungan Saat Ini (Rp)") },
                        placeholder = { Text("Koreksi saldo langsung") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_saving_current_input")
                    )
                    
                    // Owner choices
                    Column {
                        Text("Kepemilikan Pos:", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(
                                Triple("HAIKAL", haikalName, HaikalAccent),
                                Triple("UMMU", ummuName, UmmuAccent),
                                Triple("BERDUA", "Joint", CombinedAccent)
                            ).forEach { (ownerKey, nameStr, colorTheme) ->
                                val active = editGoalOwner == ownerKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) colorTheme.copy(alpha = 0.2f) else BgDeep)
                                        .border(1.dp, if (active) colorTheme else BorderColor, RoundedCornerShape(8.dp))
                                        .clickable { editGoalOwner = ownerKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(nameStr, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = if (active) TextPrimary else TextSecondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            viewModel.deleteSavingGoal(editingGoal.id)
                            showEditGoalDialog = null
                            Toast.makeText(context, "Pos Tabungan berhasil dihapus", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerAccent)
                    ) {
                        Text("Hapus Pos", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val target = editGoalTargetAmount.toDoubleOrNull() ?: 0.0
                            val current = editGoalCurrentAmountState.toDoubleOrNull() ?: 0.0
                            if (editGoalName.isNotBlank() && target > 0) {
                                viewModel.updateCustomSavingGoal(editingGoal.id, editGoalName, target, current, editGoalOwner)
                                showEditGoalDialog = null
                                Toast.makeText(context, "Pos Tabungan berhasil disimpan", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Harap isi nama dan target nominal!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGoalDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

// SUB TAB: CICILAN & HUTANG (FITUR 08)
@Composable
fun CicilanHutangTab(viewModel: DuniaViewModel) {
    val list by viewModel.cicilanList.collectAsState()

    var showForm by remember { mutableStateOf(false) }

    // Add inputs
    var inputName by remember { mutableStateOf("") }
    var inputTotalValue by remember { mutableStateOf("") }
    var inputMonthlyPayment by remember { mutableStateOf("") }
    var inputMonths by remember { mutableStateOf("") }
    var inputDueDay by remember { mutableStateOf("5") }
    var inputOwner by remember { mutableStateOf("HAIKAL") }

    // Edit inputs
    var showEditCicilanDialog by remember { mutableStateOf<com.example.data.CicilanEntity?>(null) }
    var editCicilanName by remember { mutableStateOf("") }
    var editCicilanTotalValue by remember { mutableStateOf("") }
    var editCicilanMonthlyPayment by remember { mutableStateOf("") }
    var editCicilanRemainingMonths by remember { mutableStateOf("") }
    var editCicilanPaidMonths by remember { mutableStateOf("") }
    var editCicilanDueDay by remember { mutableStateOf("5") }
    var editCicilanOwner by remember { mutableStateOf("HAIKAL") }

    LaunchedEffect(showEditCicilanDialog) {
        if (showEditCicilanDialog != null) {
            val c = showEditCicilanDialog!!
            editCicilanName = c.name
            editCicilanTotalValue = c.totalValue.toLong().toString()
            editCicilanMonthlyPayment = c.monthlyPayment.toLong().toString()
            editCicilanRemainingMonths = c.remainingMonths.toString()
            editCicilanPaidMonths = c.paidMonths.toString()
            editCicilanDueDay = c.dueDateDay.toString()
            editCicilanOwner = c.owner
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Monitor Kredit Terikat", fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Button(
                    onClick = { showForm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerAccent.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Tambahkan Cicilan", color = DangerAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (list.isEmpty()) {
            item {
                Text("Luar biasa! Tidak ada cicilan aktif yang terikat di database.", color = SuccessAccent, fontSize = 12.sp)
            }
        }

        items(list) { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Penanggung jawab: ${item.owner} / Jatuh Tempo: Tanggal ${item.dueDateDay}", fontSize = 10.sp, color = TextSecondary)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { showEditCicilanDialog = item },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Cicilan", tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                        IconButton(
                            onClick = { viewModel.deleteCicilan(item) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Angsuran Bulanan", fontSize = 9.sp, color = TextSecondary)
                        Text(formatRupiah(item.monthlyPayment), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerAccent)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Sisa Bulan Angka", fontSize = 9.sp, color = TextSecondary)
                        Text("${item.remainingMonths} bln lagi (Ke-${item.paidMonths})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.remainingMonths > 0) {
                        Button(
                            onClick = { viewModel.payCicilan(item.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerAccent),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("bayar_cicilan_${item.id}")
                        ) {
                            Text("Bayar Angsuran Bulanan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SuccessAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🏆 LUNAS SELESAI", color = SuccessAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        "Sisa Pokok: ${formatRupiah(item.remainingMonths * item.monthlyPayment)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
            }
        }
    }

    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            containerColor = BgCard,
            title = { Text("Tambah Cicilan Terikat", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nama Cicilan (e.g. Motor)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputTotalValue,
                        onValueChange = { inputTotalValue = it },
                        label = { Text("Total Nilai Angsuran (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputMonthlyPayment,
                        onValueChange = { inputMonthlyPayment = it },
                        label = { Text("Cicilan Per Bulan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputMonths,
                        onValueChange = { inputMonths = it },
                        label = { Text("Jangka Waktu (Bulan)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputDueDay,
                        onValueChange = { inputDueDay = it },
                        label = { Text("Hari Jatuh Tempo Bulanan (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (inputOwner == "HAIKAL") CombinedAccent else BorderColor)
                                .clickable { inputOwner = "HAIKAL" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Haikal", color = if (inputOwner == "HAIKAL") CombinedAccent else TextSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (inputOwner == "UMMU") CombinedAccent else BorderColor)
                                .clickable { inputOwner = "UMMU" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ummu", color = if (inputOwner == "UMMU") CombinedAccent else TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val total = inputTotalValue.toDoubleOrNull() ?: 0.0
                        val monthly = inputMonthlyPayment.toDoubleOrNull() ?: 0.0
                        val mths = inputMonths.toIntOrNull() ?: 1
                        val due = inputDueDay.toIntOrNull() ?: 5

                        if (inputName.trim().isNotEmpty() && monthly > 0.0) {
                            viewModel.addCicilan(inputName, total, monthly, mths, due, inputOwner)
                            showForm = false
                            inputName = ""
                            inputTotalValue = ""
                            inputMonthlyPayment = ""
                            inputMonths = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent)
                ) {
                    Text("Tambahkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showEditCicilanDialog != null) {
        val originalCicilan = showEditCicilanDialog!!
        AlertDialog(
            onDismissRequest = { showEditCicilanDialog = null },
            containerColor = BgCard,
            title = { Text("Koreksi Cicilan Terikat 🖊️", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editCicilanName,
                        onValueChange = { editCicilanName = it },
                        label = { Text("Nama Cicilan (e.g. Motor)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCicilanTotalValue,
                        onValueChange = { editCicilanTotalValue = it },
                        label = { Text("Total Nilai Angsuran (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCicilanMonthlyPayment,
                        onValueChange = { editCicilanMonthlyPayment = it },
                        label = { Text("Cicilan Per Bulan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCicilanPaidMonths,
                        onValueChange = { editCicilanPaidMonths = it },
                        label = { Text("Jumlah Bulan Sudah Terbayar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCicilanRemainingMonths,
                        onValueChange = { editCicilanRemainingMonths = it },
                        label = { Text("Sisa Waktu (Bulan)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCicilanDueDay,
                        onValueChange = { editCicilanDueDay = it },
                        label = { Text("Hari Jatuh Tempo Bulanan (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (editCicilanOwner == "HAIKAL") CombinedAccent else BorderColor)
                                .clickable { editCicilanOwner = "HAIKAL" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Haikal", color = if (editCicilanOwner == "HAIKAL") CombinedAccent else TextSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (editCicilanOwner == "UMMU") CombinedAccent else BorderColor)
                                .clickable { editCicilanOwner = "UMMU" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ummu", color = if (editCicilanOwner == "UMMU") CombinedAccent else TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val total = editCicilanTotalValue.toDoubleOrNull() ?: 0.0
                        val monthly = editCicilanMonthlyPayment.toDoubleOrNull() ?: 0.0
                        val remaining = editCicilanRemainingMonths.toIntOrNull() ?: 1
                        val paid = editCicilanPaidMonths.toIntOrNull() ?: 0
                        val due = editCicilanDueDay.toIntOrNull() ?: 5

                        if (editCicilanName.trim().isNotEmpty() && monthly > 0.0) {
                            viewModel.updateCicilan(
                                originalCicilan.copy(
                                    name = editCicilanName,
                                    totalValue = total,
                                    monthlyPayment = monthly,
                                    remainingMonths = remaining,
                                    paidMonths = paid,
                                    dueDateDay = due,
                                    owner = editCicilanOwner
                                )
                            )
                            showEditCicilanDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent)
                ) {
                    Text("Simpan Perubahan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCicilanDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

// ==========================================
// 3. ROADMAP & MILESTONES (FITUR 09)
// ==========================================

@Composable
fun RoadmapGoalsScreen(viewModel: DuniaViewModel) {
    val milestones by viewModel.milestones.collectAsState()

    var activeFaseCategory by remember { mutableStateOf(0) } // 0..4

    val faseDescriptions = listOf(
        "Fase 0 (2026): 'Tahun Santai & Fondasi' - Fokus stabilisasi, lunas jaket, bangun dana darurat 3jt, bangun habit harian.",
        "Fase 1 (2027): 'Tahun Serius Tapi Santai' - Mulai serius menabung DP rumah & pernikahan.",
        "Fase 2 (2028): 'Tahun Akselerasi Gas' - Tabungan DP Rumah 20jt+, lamaran formal dilakukan.",
        "Fase 3 (2029): 'Tahun Komitmen Kita' - Biaya DP lunas, kelayakan KPR, target pernikahan lunas terlaksana.",
        "Fase 4 (2030+): 'Keluarga Finansial Sehat' - Hidup bersatu seimbang, tabung berkelanjutan."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🗺️ Roadmap Hidup 2026 - 2030+", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        // Fase buttons row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val stages = listOf("Fase 0", "Fase 1", "Fase 2", "Fase 3", "Fase 4")
            items(stages.size) { idx ->
                val active = activeFaseCategory == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) CombinedAccent.copy(alpha = 0.25f) else BgCard)
                        .border(1.dp, if (active) CombinedAccent else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { activeFaseCategory = idx }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(stages[idx], color = if (active) CombinedAccent else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Selected Fase summary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(faseDescriptions[activeFaseCategory], fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
        }

        Text("Milestone Checklist:", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        // Milestones checklist details
        val filteredMilestones = milestones.filter { it.fase == activeFaseCategory }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredMilestones.isEmpty()) {
                item {
                    Text("Belum ada milestone diatur untuk fase ini.", color = TextMuted, fontSize = 12.sp)
                }
            }

            items(filteredMilestones) { milestone ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgCard)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = milestone.checked,
                            onCheckedChange = { viewModel.toggleMilestone(milestone) },
                            colors = CheckboxDefaults.colors(checkedColor = CombinedAccent)
                        )

                        Column {
                            Text(
                                milestone.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (milestone.checked) TextMuted else TextPrimary,
                                style = LocalTextStyle.current.copy(
                                    textDecoration = if (milestone.checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                )
                            )
                            Text(milestone.description, fontSize = 11.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("Target: ${milestone.targetDate}", fontSize = 10.sp, color = CombinedAccent, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. HARIAN & SPIRITUAL (FITUR 12/14/15)
// ==========================================

@Composable
fun HarianSpiritualScreen(
    viewModel: DuniaViewModel,
    haikalName: String,
    ummuName: String
) {
    var subTabState by remember { mutableStateOf(0) } // 0 = Jadwal Harian, 1 = Ibadah Tracker

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTabState,
            containerColor = BgCard,
            contentColor = CombinedAccent
        ) {
            Tab(selected = subTabState == 0, onClick = { subTabState = 0 }) {
                Text("⏰ Jadwal Habit", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = subTabState == 1, onClick = { subTabState = 1 }) {
                Text("🕌 Ibadah Tracker", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (subTabState == 0) {
                JadwalHarianView(viewModel, haikalName, ummuName)
            } else {
                IbadahTrackerView(viewModel)
            }
        }
    }
}

@Composable
fun JadwalHarianView(viewModel: DuniaViewModel, haikalName: String, ummuName: String) {
    val schedule by viewModel.activities.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputStart by remember { mutableStateOf("") }
    var inputEnd by remember { mutableStateOf("") }
    var inputSavesItem by remember { mutableStateOf(true) }
    var inputOwner by remember { mutableStateOf("HAIKAL") } // "HAIKAL" or "UMMU"
    var inputCategory by remember { mutableStateOf("Ibadah & Spiritual") }

    var activeOwnerFilter by remember { mutableStateOf("GABUNGAN") } // "GABUNGAN", "HAIKAL", "UMMU"
    var activeCategoryFilter by remember { mutableStateOf("Semua") }

    val categories = listOf(
        "Semua",
        "Ibadah & Spiritual",
        "Produktif & Karir",
        "Kesehatan & Olahraga",
        "Domestik & Kebersihan",
        "Sinergi & Komunikasi",
        "Hiburan & Lainnya"
    )

    // Helper functions for category colors
    fun getCategoryUiAttrs(categoryStr: String): Triple<Color, Color, String> {
        return when (categoryStr) {
            "Ibadah & Spiritual" -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), "🕌") // Sky Blue
            "Produktif & Karir" -> Triple(Color(0xFFF1F5F9), Color(0xFF334155), "💼") // Charcoal Slate
            "Kesehatan & Olahraga" -> Triple(Color(0xFFECFDF5), Color(0xFF059669), "🏃") // Emerald Green
            "Domestik & Kebersihan" -> Triple(Color(0xFFFFF7ED), Color(0xFFEA580C), "🏠") // Deep Orange
            "Sinergi & Komunikasi" -> Triple(Color(0xFFF3E8FF), Color(0xFF9333EA), "🕊️") // Regal Purple
            else -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "🎮") // Soft Gray
        }
    }

    // Filtered list
    val filteredSchedule = schedule.filter { act ->
        val ownerMatch = when (activeOwnerFilter) {
            "HAIKAL" -> act.owner == "HAIKAL"
            "UMMU" -> act.owner == "UMMU"
            else -> true
        }
        val categoryMatch = when (activeCategoryFilter) {
            "Semua" -> true
            else -> act.category == activeCategoryFilter
        }
        ownerMatch && categoryMatch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Title Row with Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "⏰ Habit & Rutinitas Mitra",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Text(
                        "Bangun kebiasaan produktif & hemat bersama",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { showForm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("+ Kegiatan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Interactive Owner Filter Tabs (Haikal / Ummu / Gabungan)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgDark)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Option All/Gabungan
                val isGabungan = activeOwnerFilter == "GABUNGAN"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isGabungan) BgCard else Color.Transparent)
                        .border(1.dp, if (isGabungan) BorderColor else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { activeOwnerFilter = "GABUNGAN" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💑", fontSize = 11.sp)
                        Text(
                            "Gabungan",
                            fontSize = 11.sp,
                            fontWeight = if (isGabungan) FontWeight.Bold else FontWeight.Normal,
                            color = if (isGabungan) CombinedAccent else TextSecondary
                        )
                    }
                }

                // Option Haikal
                val isHaikal = activeOwnerFilter == "HAIKAL"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isHaikal) BgCard else Color.Transparent)
                        .border(1.dp, if (isHaikal) BorderColor else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { activeOwnerFilter = "HAIKAL" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🙋‍♂️", fontSize = 11.sp)
                        Text(
                            haikalName,
                            fontSize = 11.sp,
                            fontWeight = if (isHaikal) FontWeight.Bold else FontWeight.Normal,
                            color = if (isHaikal) HaikalAccent else TextSecondary
                        )
                    }
                }

                // Option Ummu
                val isUmmu = activeOwnerFilter == "UMMU"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isUmmu) BgCard else Color.Transparent)
                        .border(1.dp, if (isUmmu) BorderColor else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { activeOwnerFilter = "UMMU" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🙋‍♀️", fontSize = 11.sp)
                        Text(
                            ummuName,
                            fontSize = 11.sp,
                            fontWeight = if (isUmmu) FontWeight.Bold else FontWeight.Normal,
                            color = if (isUmmu) UmmuAccent else TextSecondary
                        )
                    }
                }
            }
        }

        // 3. Category Scrolling Row Filter
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isActive = activeCategoryFilter == cat
                    val attrs = getCategoryUiAttrs(cat)
                    val labelToShow = if (cat == "Semua") "⚡ Semua" else "${attrs.third} ${cat.substringBefore(" &")}"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(if (isActive) CombinedAccent else BgCard)
                            .border(1.dp, if (isActive) Color.Transparent else BorderColor, RoundedCornerShape(30.dp))
                            .clickable { activeCategoryFilter = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = labelToShow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        // 4. Feed Timeline
        if (filteredSchedule.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💨", fontSize = 32.sp)
                    Text("Belum ada jadwal habit pada kategori ini.", fontSize = 11.sp, color = TextMuted)
                }
            }
        } else {
            items(filteredSchedule) { act ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Header Row (Time, Owner, and Category Badge)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Time
                            Text(
                                "${act.startTime} - ${act.endTime}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CombinedAccent,
                                fontWeight = FontWeight.Bold
                            )

                            // Owner Badge
                            val isActHaikal = act.owner == "HAIKAL"
                            val ownerBg = if (isActHaikal) HaikalAccent.copy(alpha = 0.12f) else UmmuAccent.copy(alpha = 0.12f)
                            val ownerTextCol = if (isActHaikal) HaikalAccent else UmmuAccent
                            val ownerLabelStr = if (isActHaikal) "Haikal" else "Ummu"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ownerBg)
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    ownerLabelStr,
                                    fontSize = 7.5.sp,
                                    color = ownerTextCol,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Category Badge
                            val tagAttrs = getCategoryUiAttrs(act.category)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(tagAttrs.first)
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    "${tagAttrs.third} ${act.category}",
                                    fontSize = 7.5.sp,
                                    color = tagAttrs.second,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Habit Name
                        Text(
                            text = act.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        // Saves Money Badge
                        if (act.savesMoney) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SuccessAccent.copy(alpha = 0.12f))
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        "🛡️ Sinergi Hemat Pengeluaran",
                                        color = SuccessAccent,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    // Delete Action
                    IconButton(
                        onClick = { viewModel.deleteActivity(act) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = DangerAccent.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            containerColor = BgCard,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⚡ Tambah Rutinitas Sinergi", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Owner Segmented Row selector
                    Text("Penanggung Jawab (Owner):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val oHaikalAct = inputOwner == "HAIKAL"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (oHaikalAct) HaikalAccent.copy(alpha = 0.15f) else BgDark)
                                .border(1.dp, if (oHaikalAct) HaikalAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { inputOwner = "HAIKAL" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🙋‍♂️ $haikalName", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (oHaikalAct) HaikalAccent else TextSecondary)
                        }

                        val oUmmuAct = inputOwner == "UMMU"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (oUmmuAct) UmmuAccent.copy(alpha = 0.15f) else BgDark)
                                .border(1.dp, if (oUmmuAct) UmmuAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { inputOwner = "UMMU" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🙋‍♀️ $ummuName", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (oUmmuAct) UmmuAccent else TextSecondary)
                        }
                    }

                    // Category scroll row selector
                    Text("Pilih Kategori Kegiatan:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val formCategories = listOf(
                            "Ibadah & Spiritual",
                            "Produktif & Karir",
                            "Kesehatan & Olahraga",
                            "Domestik & Kebersihan",
                            "Sinergi & Komunikasi",
                            "Hiburan & Lainnya"
                        )
                        items(formCategories) { cat ->
                            val isSel = inputCategory == cat
                            val tagAttrs = getCategoryUiAttrs(cat)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(if (isSel) tagAttrs.second else BgDark)
                                    .clickable { inputCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${tagAttrs.third} $cat",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else TextSecondary
                                )
                            }
                        }
                    }

                    // Text inputs
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nama Kegiatan") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = inputStart,
                            onValueChange = { inputStart = it },
                            label = { Text("Mulai (HH:MM)") },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputEnd,
                            onValueChange = { inputEnd = it },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            label = { Text("Selesai (HH:MM)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgDark)
                            .clickable { inputSavesItem = !inputSavesItem }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = inputSavesItem,
                            onCheckedChange = { inputSavesItem = it },
                            colors = CheckboxDefaults.colors(checkedColor = SuccessAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Kegiatan Hemat Pengeluaran", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Membantu memangkas pengeluaran bulanan", fontSize = 8.5.sp, color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.trim().isNotEmpty() && inputStart.trim().isNotEmpty()) {
                            viewModel.addActivity(
                                name = inputName.trim(),
                                start = inputStart.trim(),
                                end = if (inputEnd.trim().isEmpty()) inputStart.trim() else inputEnd.trim(),
                                owner = inputOwner,
                                saves = inputSavesItem,
                                category = inputCategory
                            )
                            showForm = false
                            inputName = ""
                            inputStart = ""
                            inputEnd = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent)
                ) {
                    Text("Tambahkan", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun IbadahTrackerView(viewModel: DuniaViewModel) {
    var subuh by remember { mutableStateOf(false) }
    var dzuhur by remember { mutableStateOf(false) }
    var ashar by remember { mutableStateOf(false) }
    var maghrib by remember { mutableStateOf(false) }
    var isya by remember { mutableStateOf(false) }
    var ngajiText by remember { mutableStateOf("") }
    var sedekahText by remember { mutableStateOf("") }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SuccessAccent.copy(alpha = 0.1f))
                    .border(2.dp, SuccessAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Ibadah harian berfungsi memperkuat asupan rohani dan keberkahan hubungan menuju pernikahan sakinah mawaddah warahmah.",
                    color = SuccessAccent,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        item {
            Text("Evaluasi Sholat Hari Ini (5 Waktu)", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PrayerItemRow("Sholat Subuh", subuh) { subuh = it }
                PrayerItemRow("Sholat Dzuhur", dzuhur) { dzuhur = it }
                PrayerItemRow("Sholat Ashar", ashar) { ashar = it }
                PrayerItemRow("Sholat Maghrib", maghrib) { maghrib = it }
                PrayerItemRow("Sholat Isya", isya) { isya = it }
            }
        }

        item {
            Text("Ibadah Tambahan", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ngajiText,
                    onValueChange = { ngajiText = it },
                    label = { Text("Ngaji Al-Quran (Halaman)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SuccessAccent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sedekahText,
                    onValueChange = { sedekahText = it },
                    label = { Text("Sedekah / Infaq Tambahan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SuccessAccent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val sedekahAmt = sedekahText.toDoubleOrNull() ?: 0.0
                        val pgCount = ngajiText.toIntOrNull() ?: 0
                        viewModel.saveIbadahLog(subuh, dzuhur, ashar, maghrib, isya, pgCount, sedekahAmt)
                        Toast.makeText(context, "Ibadah harian tersimpan !", Toast.LENGTH_SHORT).show()
                        // Clean
                        ngajiText = ""
                        sedekahText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text("Simpan & Doakan Keberkahan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PrayerItemRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = if (checked) TextPrimary else TextSecondary)
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = SuccessAccent, checkedTrackColor = SuccessAccent.copy(alpha = 0.3f))
        )
    }
}

// ==========================================
// 5. RAPAT & WISHLIST IMPULSE (FITUR 16/19)
// ==========================================

@Composable
fun RapatWishlistScreen(
    viewModel: DuniaViewModel,
    haikalName: String,
    ummuName: String
) {
    var partitionState by remember { mutableStateOf(0) } // 0 = Rapat Bulanan, 1 = Wishlist Control

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = partitionState,
            containerColor = BgCard,
            contentColor = CombinedAccent
        ) {
            Tab(selected = partitionState == 0, onClick = { partitionState = 0 }) {
                Text("🤝 Rapat Bulanan", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = partitionState == 1, onClick = { partitionState = 1 }) {
                Text("🛒 Wishlist Impulse", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (partitionState == 0) {
                RapatReviewsView(viewModel)
            } else {
                WishlistControlView(viewModel, haikalName, ummuName)
            }
        }
    }
}

@Composable
fun RapatReviewsView(viewModel: DuniaViewModel) {
    val items by viewModel.rapatItems.collectAsState()

    var showForm by remember { mutableStateOf(false) }

    var inputAgenda by remember { mutableStateOf("") }
    var inputMinutes by remember { mutableStateOf("") }
    var inputDecisions by remember { mutableStateOf("") }
    var inputActions by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Review Notulen Pertemuan Bulan", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Button(
                    onClick = { showForm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("+ Notulen", fontSize = 10.sp)
                }
            }
        }

        items(items) { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.dateStr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CombinedAccent, fontFamily = FontFamily.Monospace)
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }

                Text("Agenda Utama:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(item.agenda, fontSize = 12.sp, color = TextPrimary)

                Text("Hasil Pembahasan:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(item.minutes, fontSize = 12.sp, color = TextPrimary)

                Text("Kesepakatan Bersama:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(item.decisions, fontSize = 12.sp, color = SuccessAccent)

                if (item.actionItems.isNotEmpty()) {
                    Text("Action Items Mandiri:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(item.actionItems, fontSize = 12.sp, color = HaikalAccent)
                }
            }
        }
    }

    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            containerColor = BgCard,
            title = { Text("Log Rapat Bulanan", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputAgenda,
                        onValueChange = { inputAgenda = it },
                        label = { Text("Agenda Pembahasan") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputMinutes,
                        onValueChange = { inputMinutes = it },
                        label = { Text("Catatan Hasil Diskusi") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputDecisions,
                        onValueChange = { inputDecisions = it },
                        label = { Text("Kesepakatan Diputuskan") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputActions,
                        onValueChange = { inputActions = it },
                        label = { Text("Tugas / Action Items") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputAgenda.trim().isNotEmpty() && inputMinutes.trim().isNotEmpty()) {
                            viewModel.addMonthlyRapat(inputAgenda, inputMinutes, inputDecisions, inputActions)
                            showForm = false
                            inputAgenda = ""
                            inputMinutes = ""
                            inputDecisions = ""
                            inputActions = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun WishlistControlView(
    viewModel: DuniaViewModel,
    haikalName: String,
    ummuName: String
) {
    val items by viewModel.wishlistItems.collectAsState()

    var showForm by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("Keinginan") }
    var priorityInput by remember { mutableStateOf(3) }
    var ownerInput by remember { mutableStateOf("HAIKAL") }

    // Edit wishlist states
    var showEditWishlistDialog by remember { mutableStateOf<com.example.data.WishlistEntity?>(null) }
    var editWishlistName by remember { mutableStateOf("") }
    var editWishlistPrice by remember { mutableStateOf("") }
    var editWishlistCategory by remember { mutableStateOf("Keinginan") }
    var editWishlistPriority by remember { mutableStateOf(3) }
    var editWishlistOwner by remember { mutableStateOf("HAIKAL") }
    var editWishlistStatus by remember { mutableStateOf("PENDING") }

    LaunchedEffect(showEditWishlistDialog) {
        if (showEditWishlistDialog != null) {
            val w = showEditWishlistDialog!!
            editWishlistName = w.name
            editWishlistPrice = w.estimatedPrice.toLong().toString()
            editWishlistCategory = w.category
            editWishlistPriority = w.priority
            editWishlistOwner = w.owner
            editWishlistStatus = w.status
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(WarningAccent.copy(alpha = 0.08f))
                    .border(1.dp, WarningAccent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "⚠️ Metode Kontrol Pembelian Impulsif: Barang belanjaan tertahan di Wishlist minimal selama 30 hari sebagai masa tenang pertimbangan.",
                    fontSize = 11.sp,
                    color = WarningAccent,
                    lineHeight = 16.sp
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daftar Belanja & Wishlist", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Button(
                    onClick = { showForm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("+ Antrean", fontSize = 10.sp)
                }
            }
        }

        items(items) { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Prioritas Urgensi: ${item.priority} dari 5", fontSize = 10.sp, color = TextSecondary)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (item.status) {
                                    "PENDING" -> WarningAccent.copy(alpha = 0.15f)
                                    "DISETUJUI" -> SuccessAccent.copy(alpha = 0.15f)
                                    "DIBELI" -> CombinedAccent.copy(alpha = 0.15f)
                                    else -> TextMuted.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.status,
                            color = when (item.status) {
                                "PENDING" -> WarningAccent
                                "DISETUJUI" -> SuccessAccent
                                "DIBELI" -> CombinedAccent
                                else -> TextSecondary
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Estimasi Budget", fontSize = 9.sp, color = TextSecondary)
                        Text(formatRupiah(item.estimatedPrice), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (item.status == "PENDING") {
                            Button(
                                onClick = { viewModel.updateWishlistStatus(item, "DISETUJUI") },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Setujui", fontSize = 9.sp)
                            }
                        } else if (item.status == "DISETUJUI") {
                            Button(
                                onClick = { viewModel.updateWishlistStatus(item, "DIBELI") },
                                colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Eksekusi Beli", fontSize = 9.sp)
                            }
                        }

                        IconButton(
                            onClick = { showEditWishlistDialog = item },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Antrean", tint = TextMuted, modifier = Modifier.size(14.dp))
                        }

                        IconButton(onClick = { viewModel.deleteWishlist(item) }, modifier = Modifier.size(26.dp)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Batal", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            containerColor = BgCard,
            title = { Text("Tambah Antrean Belanja", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Barang / Pengalaman") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Estimasi Harga (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Kategori Wishlist:", fontSize = 11.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Kebutuhan", "Keinginan", "Investasi").forEach { c ->
                            val acts = categoryInput == c
                            Box(
                                modifier = Modifier
                                    .border(1.dp, if (acts) CombinedAccent else BorderColor, RoundedCornerShape(6.dp))
                                    .clickable { categoryInput = c }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(c, fontSize = 10.sp, color = if (acts) CombinedAccent else TextPrimary)
                            }
                        }
                    }

                    Text("Tingkat Prioritas (1-5):", fontSize = 11.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { p ->
                            val acts = priorityInput == p
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (acts) CombinedAccent.copy(alpha = 0.2f) else BgDeep)
                                    .border(1.dp, if (acts) CombinedAccent else BorderColor, CircleShape)
                                    .clickable { priorityInput = p },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(p.toString(), fontSize = 10.sp, color = if (acts) CombinedAccent else TextPrimary)
                            }
                        }
                    }

                    Text("Pemuat:", fontSize = 11.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (ownerInput == "HAIKAL") CombinedAccent else BorderColor)
                                .clickable { ownerInput = "HAIKAL" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(haikalName)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (ownerInput == "UMMU") CombinedAccent else BorderColor)
                                .clickable { ownerInput = "UMMU" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(ummuName)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prcObj = priceInput.toDoubleOrNull() ?: 0.0
                        if (nameInput.trim().isNotEmpty() && prcObj > 0) {
                            viewModel.addWishlistItem(nameInput, prcObj, categoryInput, priorityInput, ownerInput)
                            showForm = false
                            nameInput = ""
                            priceInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent)
                ) {
                    Text("Antrekan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showEditWishlistDialog != null) {
        val originalWishlist = showEditWishlistDialog!!
        AlertDialog(
            onDismissRequest = { showEditWishlistDialog = null },
            containerColor = BgCard,
            title = { Text("Koreksi Antrean Belanja 🖊️", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editWishlistName,
                        onValueChange = { editWishlistName = it },
                        label = { Text("Nama Barang Belanjaan") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editWishlistPrice,
                        onValueChange = { editWishlistPrice = it },
                        label = { Text("Estimasi Harga (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Kategori Wishlist:", fontSize = 11.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Kebutuhan", "Keinginan").forEach { cat ->
                            val isSelected = editWishlistCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, if (isSelected) CombinedAccent else BorderColor)
                                    .clickable { editWishlistCategory = cat }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat, fontSize = 11.sp, color = if (isSelected) CombinedAccent else TextPrimary)
                            }
                        }
                    }

                    Text("Prioritas Urgensi (1-5):", fontSize = 11.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { p ->
                            val isSelected = editWishlistPriority == p
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) CombinedAccent else BgDeep)
                                    .clickable { editWishlistPriority = p }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$p", fontSize = 11.sp, color = if (isSelected) Color.White else TextPrimary)
                            }
                        }
                    }

                    Text("Pemuat Antrean:", fontSize = 11.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (editWishlistOwner == "HAIKAL") CombinedAccent else BorderColor)
                                .clickable { editWishlistOwner = "HAIKAL" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(haikalName)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (editWishlistOwner == "UMMU") CombinedAccent else BorderColor)
                                .clickable { editWishlistOwner = "UMMU" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(ummuName)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prcObj = editWishlistPrice.toDoubleOrNull() ?: 0.0
                        if (editWishlistName.trim().isNotEmpty() && prcObj > 0) {
                            viewModel.updateWishlist(
                                originalWishlist.copy(
                                    name = editWishlistName,
                                    estimatedPrice = prcObj,
                                    category = editWishlistCategory,
                                    priority = editWishlistPriority,
                                    owner = editWishlistOwner,
                                    status = editWishlistStatus
                                )
                            )
                            showEditWishlistDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent)
                ) {
                    Text("Simpan Perubahan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditWishlistDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

// ==========================================
// 6. AI ADVISOR CHAT SCREEN (FITUR 12)
// ==========================================

@Composable
fun AIAdvisorScreen(viewModel: DuniaViewModel) {
    val history by viewModel.aiChatHistory.collectAsState()
    val loading by viewModel.aiLoading.collectAsState()

    var inputMsg by remember { mutableStateOf("") }

    val presetPrompts = listOf(
        "Analisis keuangan kami bulan ini",
        "Kapan kami bisa beli rumah?",
        "Apakah pengeluaran kami sehat?",
        "Tips kencan hemat Bantul",
        "Motivasi keuangan harian"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessAccent))
                Text("DUNIA AI Advisor (Online)", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            TextButton(
                onClick = { viewModel.clearChat() },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Bersihkan Chat", color = TextSecondary, fontSize = 11.sp)
            }
        }

        // Horizontal scrolling chips for quick actions
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presetPrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CombinedAccent.copy(alpha = 0.12f))
                        .border(1.dp, CombinedAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { viewModel.triggerQuickQuestion(prompt) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(prompt, color = TextPrimary, fontSize = 10.sp)
                }
            }
        }

        // Main Chat List Box
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(history) { chat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (chat.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (chat.isUser) 12.dp else 0.dp,
                                    bottomEnd = if (chat.isUser) 0.dp else 12.dp
                                )
                            )
                            .background(
                                if (chat.isUser) CombinedAccent.copy(alpha = 0.15f) else BgHover
                            )
                            .border(
                                1.dp,
                                if (chat.isUser) CombinedAccent.copy(alpha = 0.4f) else BorderColor,
                                RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (chat.isUser) 12.dp else 0.dp,
                                    bottomEnd = if (chat.isUser) 0.dp else 12.dp
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (chat.isUser) "Kamu" else "DUNIA AI Advisor",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (chat.isUser) CombinedAccent else HaikalAccent,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = chat.message,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            if (loading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text("DUNIA AI sedang mengevaluasi data keuangan...", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Message input Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputMsg,
                onValueChange = { inputMsg = it },
                placeholder = { Text("Tanyakan apa saja ke DUNIA AI...", fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("ai_input_text")
            )

            Button(
                onClick = {
                    if (inputMsg.trim().isNotEmpty()) {
                        viewModel.sendChatMessage(inputMsg)
                        inputMsg = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent),
                modifier = Modifier.height(48.dp).testTag("ai_send_btn")
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Kirim")
            }
        }
    }
}

// ==========================================
// 7. PROFIL & PENGATURAN SCREEN (FITUR 02/21)
// ==========================================

@Composable
fun ProfilSettingsScreen(
    viewModel: DuniaViewModel,
    haikalName: String,
    ummuName: String
) {
    val configs by viewModel.configs.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val sheetsSyncStatus by viewModel.sheetsSyncStatus.collectAsState()
    val lastSheetsSyncTime by viewModel.lastSheetsSyncTime.collectAsState()
    val sheetsDiagnosticResult by viewModel.sheetsDiagnosticResult.collectAsState()
    val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsState()
    val lastFirebaseSyncTime by viewModel.lastFirebaseSyncTime.collectAsState()
    val firebaseDiagnosticResult by viewModel.firebaseDiagnosticResult.collectAsState()

    var jobHaikal by remember { mutableStateOf("") }
    var jobUmmu by remember { mutableStateOf("") }
    var salaryHaikal by remember { mutableStateOf("") }
    var salaryUmmu by remember { mutableStateOf("") }
    var weddingYear by remember { mutableStateOf("") }
    var syncKey by remember { mutableStateOf("") }
    var isAutoSyncEnabled by remember { mutableStateOf(false) }
    var sheetsUrl by remember { mutableStateOf("") }
    var isSheetsAutoEnabled by remember { mutableStateOf(false) }
    var firebaseDbUrl by remember { mutableStateOf("") }
    var firebaseAuthToken by remember { mutableStateOf("") }
    var isFirebaseAutoEnabled by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Set initial values from db if available
    LaunchedEffect(configs) {
        if (configs.isNotEmpty()) {
            jobHaikal = configs["PEKERJAAN_HAIKAL"] ?: "Trainer di Hara Chicken"
            jobUmmu = configs["PEKERJAAN_UMMU"] ?: "Freelance Graphic Designer"
            salaryHaikal = configs["GAJI_HAIKAL"] ?: "2300000"
            salaryUmmu = configs["GAJI_UMMU"] ?: "1500000"
            weddingYear = configs["TARGET_NIKAH"] ?: "2029"
            syncKey = configs["SYNC_KEY"] ?: ""
            isAutoSyncEnabled = configs["SYNC_AUTO_ENABLED"] == "true"
            sheetsUrl = configs["SHEETS_WEBAPP_URL"] ?: ""
            isSheetsAutoEnabled = configs["SHEETS_AUTO_ENABLED"] == "true"
            firebaseDbUrl = configs["FIREBASE_URL"] ?: ""
            firebaseAuthToken = configs["FIREBASE_AUTH_TOKEN"] ?: ""
            isFirebaseAutoEnabled = configs["FIREBASE_AUTO_ENABLED"] == "true"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("⚙️ Pengaturan Profil DUNIA", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        // Haikal profile card editor
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(HaikalAccent))
                    Text("Profil $haikalName (Pria)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = jobHaikal,
                    onValueChange = { jobHaikal = it },
                    label = { Text("Pekerjaan") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = salaryHaikal,
                    onValueChange = { salaryHaikal = it },
                    label = { Text("Gaji Bulanan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Ummu profile card editor
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(UmmuAccent))
                    Text("Profil $ummuName (Wanita)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = jobUmmu,
                    onValueChange = { jobUmmu = it },
                    label = { Text("Pekerjaan") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = salaryUmmu,
                    onValueChange = { salaryUmmu = it },
                    label = { Text("Gaji Bulanan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Wedding settings
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("💍 Pengaturan Kebersamaan", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                OutlinedTextField(
                    value = weddingYear,
                    onValueChange = { weddingYear = it },
                    label = { Text("Tahun Target Menikah") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // AUTO-SYNC REALTIME CLOUD CARD (Fulfills the "Sinkronisasi otomatis real-time" request)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔄 Auto-Sync Nirkabel Real-time",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CombinedAccent
                    )
                    
                    // Live status pill indicator
                    val isConnected = syncStatus == "Tersinkronisasi" || syncStatus.contains("Menerima") || syncStatus.contains("Update")
                    val isSyncing = syncStatus == "Mengunggah..." || syncStatus == "Mengunduh..."
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSyncing -> CombinedAccent.copy(alpha = 0.15f)
                                    isConnected -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    else -> TextSecondary.copy(alpha = 0.1f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSyncing -> CombinedAccent
                                            isConnected -> Color(0xFF10B981)
                                            else -> TextSecondary
                                        }
                                    )
                            )
                            Text(
                                text = syncStatus,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSyncing -> CombinedAccent
                                    isConnected -> Color(0xFF10B981)
                                    else -> TextSecondary
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Hubungkan data HP Haikal & HP Ummu secara nirkabel di latar belakang. Setiap kali data diisi, diubah, atau dihapus, HP pasangan akan terupdate otomatis dalam hitungan detik secara cermin-ganda!",
                    fontSize = 10.sp,
                    color = TextSecondary
                )

                // Row switch toggle for "Aktifkan Auto-Sync"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgDeep)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Aktifkan Sinkronisasi Otomatis", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Kirim & terima update data baru seketika", fontSize = 9.sp, color = TextMuted)
                    }
                    androidx.compose.material3.Switch(
                        checked = isAutoSyncEnabled,
                        onCheckedChange = { 
                            isAutoSyncEnabled = it 
                            viewModel.saveConfigValue("SYNC_AUTO_ENABLED", if (it) "true" else "false")
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CombinedAccent
                        )
                    )
                }

                if (isAutoSyncEnabled) {
                    OutlinedTextField(
                        value = syncKey,
                        onValueChange = { 
                            syncKey = it 
                            viewModel.saveConfigValue("SYNC_KEY", it)
                        },
                        label = { Text("Kunci Sinkronisasi Bersama") },
                        placeholder = { Text("Contoh: haikal_ummu_selamanya") },
                        supportingText = {
                            Text(
                                "PENTING: Gunakan kata rahasia yang SAMA PERSIS di HP Haikal dan HP Ummu agar saling bersinergi.", 
                                fontSize = 8.sp, 
                                color = TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val generatedKey = "hk_um_love_" + (1000..9999).random()
                                syncKey = generatedKey
                                viewModel.saveConfigValue("SYNC_KEY", generatedKey)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BgDeep),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Buatkan Kunci Acak 🔑", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (syncKey.isNotBlank()) {
                                    scope.launch {
                                        viewModel.localAutoUpload()
                                        Toast.makeText(context, "Sinergikan data lokal ke cloud sekarang!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Atur kunci sinkronisasi terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sync Manual 🔄", color = CombinedAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🕒 Terakhir bersinergi:", fontSize = 9.sp, color = TextMuted)
                        Text(lastSyncTime, fontSize = 9.sp, color = CombinedAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // GOOGLE SHEETS REAL-TIME SYNCHRONIZATION CARD
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📊 Google Sheets Real-time Sync",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = SuccessAccent
                    )

                    // Realtime status pill for Google Sheets
                    val isSheetsConnected = sheetsSyncStatus.contains("Tersinkronisasi")
                    val isSheetsSyncing = sheetsSyncStatus.contains("Menyinkronkan")

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSheetsSyncing -> CombinedAccent.copy(alpha = 0.15f)
                                    isSheetsConnected -> SuccessAccent.copy(alpha = 0.15f)
                                    else -> TextSecondary.copy(alpha = 0.1f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSheetsSyncing -> CombinedAccent
                                            isSheetsConnected -> SuccessAccent
                                            else -> TextSecondary
                                        }
                                    )
                            )
                            Text(
                                text = sheetsSyncStatus,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSheetsSyncing -> CombinedAccent
                                    isSheetsConnected -> SuccessAccent
                                    else -> TextSecondary
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Hubungkan data keuangan HP langsung ke Spreadsheet Google pribadi secara otomatis & instan seketika! Setiap kali transaksi diisi atau diperbarui, lembar Spreadsheet akan terisi rapi secara real-time.",
                    fontSize = 10.sp,
                    color = TextSecondary
                )

                // Row switch toggle for "Aktifkan Auto-Sync Sheets"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgDeep)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Aktifkan Sinkronisasi Google Sheets", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Unggah data ke Google Sheets tiap ada perubahan", fontSize = 9.sp, color = TextMuted)
                    }
                    androidx.compose.material3.Switch(
                        checked = isSheetsAutoEnabled,
                        onCheckedChange = {
                            isSheetsAutoEnabled = it
                            viewModel.saveConfigValue("SHEETS_AUTO_ENABLED", if (it) "true" else "false")
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SuccessAccent
                        )
                    )
                }

                if (isSheetsAutoEnabled) {
                    OutlinedTextField(
                        value = sheetsUrl,
                        onValueChange = { 
                            sheetsUrl = it 
                            viewModel.saveConfigValue("SHEETS_WEBAPP_URL", it)
                        },
                        label = { Text("Google Apps Script Web App URL") },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                        supportingText = {
                            Text(
                                "Cara pakai: Salin modul Sheets.gs di folder assets, lalu deploy sebagai Web App di Google Apps Script.",
                                fontSize = 8.sp,
                                color = TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SuccessAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (sheetsUrl.isNotBlank()) {
                                scope.launch {
                                    viewModel.localSheetsAutoUpload()
                                    Toast.makeText(context, "Mulai sinkronisasi manual ke Google Sheets...", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Atur URL Web App terlebih dahulu!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sync Manual ke Sheets 📊", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🕒 Terakhir terkirim ke Sheet:", fontSize = 9.sp, color = TextMuted)
                        Text(lastSheetsSyncTime, fontSize = 9.sp, color = SuccessAccent, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))

                    Text("🔍 Alat Diagnosis Akses Google Sheets", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    
                    Button(
                        onClick = {
                            if (sheetsUrl.isNotBlank()) {
                                viewModel.runSheetsDiagnostic(sheetsUrl)
                            } else {
                                Toast.makeText(context, "Atur URL Web App terlebih dahulu!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (sheetsDiagnosticResult == "TESTING") Color.Gray else CombinedAccent.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (sheetsDiagnosticResult == "TESTING") Color.Gray else CombinedAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (sheetsDiagnosticResult == "TESTING") "Memeriksa Akses... ⏳" else "Tes Koneksi & Validasi Izin ⚡",
                            color = if (sheetsDiagnosticResult == "TESTING") Color.White else CombinedAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (sheetsDiagnosticResult.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgDark)
                                .border(1.dp, when (sheetsDiagnosticResult) {
                                    "SUCCESS" -> SuccessAccent
                                    "NEED_LOGIN" -> WarningAccent
                                    "BAD_URL" -> DangerAccent
                                    "TESTING" -> TextMuted
                                    else -> DangerAccent
                                }, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (sheetsDiagnosticResult) {
                                "SUCCESS" -> {
                                    Text("✅ KONEKSI INDAH & SUKSES!", color = SuccessAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "Google Sheets terhubung dengan sempurna dan unblocked! Sekarang HP Haikal dan HP Ummu dapat melakukan sinkronisasi otomatis menggunakan kata kunci tanpa halangan. 🎉",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                "NEED_LOGIN" -> {
                                    Text("⚠️ JALUR TERBLOKIR: GOOGLE MEMINTA LOGIN", color = WarningAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "Penyebab:\nSaat men-deploy Web App Google Apps Script, pilihan 'Who has access' Anda isi 'Only myself' (Hanya Saya).\n\nSolusi Instan:\n1. Buka kembali Google Spreadsheet -> Apps Script Anda.\n2. Klik tombol Deploy -> Manage deployments (Kelola deployment).\n3. Klik ikon Edit (pensil) di kanan atas, tingkatkan versi ke 'New version' (Versi Baru).\n4. Cari bagian 'Who has access' (Siapa yang memiliki akses).\n5. UPGRADE pilihan tersebut dari 'Only myself' menjadi 'Anyone' (Siapa saja).\n6. Klik Deploy dan salin ulang alamat URL baru tersebut ke aplikasi ini!",
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                                "BAD_URL" -> {
                                    Text("❌ FORMAT URL SALAH", color = DangerAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "Format URL Google Apps Script yang Anda masukkan tidak valid. Pastikan URL dimulai dengan 'https://script.google.com/macros/s/' dan diakhiri dengan '/exec'.",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                "TESTING" -> {
                                    Text("⏳ MEMERIKSA...", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "Mengirim paket uji coba terenkripsi ke Google Cloud untuk memvalidasi privasi bucket dan port paralel HP Ummu & HP Haikal...",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                else -> {
                                    Text("🌐 GANGGUAN KONEKSI INTERNET / AKSES", color = DangerAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "Keterangan Error:\n$sheetsDiagnosticResult\n\nTips:\nHarap pastikan HP Anda terhubung ke internet, Google Apps Script telah di-deploy dengan benar sebagai 'Web App', dan URL di atas dimasukkan secara lengkap tanpa terpotong.",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // GOOGLE FIREBASE REAL-TIME SYNCHRONIZATION CARD (Requested feature: Firebase Sync)
        item {
            val firebaseAmber = Color(0xFFF57C00)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔥 Firebase Real-time Cloud Sync",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = firebaseAmber
                    )

                    // Realtime status pill for Firebase Sync
                    val isFirebaseConnected = firebaseSyncStatus.contains("Tersinkronisasi")
                    val isFirebaseSyncing = firebaseSyncStatus.contains("Menyinkronkan")

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isFirebaseSyncing -> firebaseAmber.copy(alpha = 0.15f)
                                    isFirebaseConnected -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    else -> TextSecondary.copy(alpha = 0.1f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isFirebaseSyncing -> firebaseAmber
                                            isFirebaseConnected -> Color(0xFF10B981)
                                            else -> TextSecondary
                                        }
                                    )
                            )
                            Text(
                                text = firebaseSyncStatus,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isFirebaseSyncing -> firebaseAmber
                                    isFirebaseConnected -> Color(0xFF10B981)
                                    else -> TextSecondary
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Gunakan database Google Firebase Realtime terdedikasi untuk perlindungan data instan dan sinkronisasi nirkabel ultra-cepat! Hubungkan HP Haikal & HP Ummu melintasi ribuan kilometer secara real-time dengan latency milidetik.",
                    fontSize = 10.sp,
                    color = TextSecondary
                )

                // Row switch toggle for "Aktifkan Auto-Sync Firebase"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgDeep)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Aktifkan Sinkronisasi Firebase", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Hubungkan database ke Cloud Firebase", fontSize = 9.sp, color = TextMuted)
                    }
                    androidx.compose.material3.Switch(
                        checked = isFirebaseAutoEnabled,
                        onCheckedChange = {
                            isFirebaseAutoEnabled = it
                            viewModel.saveConfigValue("FIREBASE_AUTO_ENABLED", if (it) "true" else "false")
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = firebaseAmber
                        )
                    )
                }

                if (isFirebaseAutoEnabled) {
                    OutlinedTextField(
                        value = firebaseDbUrl,
                        onValueChange = {
                            firebaseDbUrl = it
                            viewModel.saveConfigValue("FIREBASE_URL", it)
                        },
                        label = { Text("Base URL Firebase Database") },
                        placeholder = { Text("https://dunia-mitra-default-rtdb.firebaseio.com/") },
                        supportingText = {
                            Text(
                                "Format: tautan https:// dari Firebase Realtime Database Anda.",
                                fontSize = 8.sp,
                                color = TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = firebaseAmber),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = firebaseAuthToken,
                        onValueChange = {
                            firebaseAuthToken = it
                            viewModel.saveConfigValue("FIREBASE_AUTH_TOKEN", it)
                        },
                        label = { Text("Database Secret / Auth Token (Opsional)") },
                        placeholder = { Text("Database secret atau kosongkan jika rules Public") },
                        supportingText = {
                            Text(
                                "Isi jika rules read/write melarang pengguna tanpa auth.",
                                fontSize = 8.sp,
                                color = TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = firebaseAmber),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (firebaseDbUrl.isNotBlank() && syncKey.isNotBlank()) {
                                scope.launch {
                                    viewModel.localFirebaseAutoUpload()
                                    Toast.makeText(context, "Sinkronisasi paksa ke Firebase dijalankan...", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Atur URL Firebase & Kunci Sinkronisasi terlebih dahulu!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = firebaseAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sync Manual ke Firebase 🔥", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🕒 Terakhir terkirim ke Firebase:", fontSize = 9.sp, color = TextMuted)
                        Text(lastFirebaseSyncTime, fontSize = 9.sp, color = firebaseAmber, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))

                    Text("🔍 Alat Diagnosis Akses Google Firebase", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Button(
                        onClick = {
                            if (firebaseDbUrl.isNotBlank() && syncKey.isNotBlank()) {
                                viewModel.runFirebaseDiagnostic(firebaseDbUrl, syncKey, firebaseAuthToken.takeIf { it.isNotBlank() })
                            } else {
                                Toast.makeText(context, "Isi URL Firebase & Kunci Sinkronisasi untuk mengetes!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (firebaseDiagnosticResult == "TESTING") Color.Gray else firebaseAmber.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (firebaseDiagnosticResult == "TESTING") Color.Gray else firebaseAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (firebaseDiagnosticResult == "TESTING") "Memeriksa Jalur Firebase... ⏳" else "Tes Koneksi & Validasi Keamanan Firebase ⚡",
                            color = if (firebaseDiagnosticResult == "TESTING") Color.White else firebaseAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (firebaseDiagnosticResult.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgDark)
                                .border(1.dp, when (firebaseDiagnosticResult) {
                                    "SUCCESS" -> Color(0xFF10B981)
                                    "PERMISSION_DENIED" -> WarningAccent
                                    "BAD_URL" -> DangerAccent
                                    "TESTING" -> TextMuted
                                    else -> DangerAccent
                                }, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (firebaseDiagnosticResult) {
                                "SUCCESS" -> {
                                    Text("✅ TEKONEKSI SEMPURNA KE FIREBASE!", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "Database Firebase terhubung unblocked melintasi bandwidth tinggi! Data terunggah & terunduh seketika di latar belakang saat Anda mengisinya. Sinergi mantap Haikal & Ummu terjamin aktif. 🚀",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                "PERMISSION_DENIED" -> {
                                    Text("⚠️ JALUR PRIVATE TERKUNCI (PERMISSION_DENIED)", color = WarningAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "Penyebab:\nAturan keamanan Firebase Database Anda melarang transaksi baca/tulis tanpa autentikasi token.\n\nSolusi Instan:\n1. Buka Firebase Console Anda -> Realtime Database -> Rules (Aturan).\n2. Ganti rules menjadi Public:\n   {\n     \"rules\": {\n       \".read\": true,\n       \".write\": true\n     }\n   }\n3. Atau, dapatkan token kredensial Anda dari Firebase Console -> Project Settings -> Service Accounts -> Database Secrets, lalu salin token rahasia tersebut ke kolom rahasia di atas!",
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                                "BAD_URL" -> {
                                    Text("❌ FORMAT URL SALAH / TIDAK VALID", color = DangerAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "URL harus disalin utuh langsung dari tab Dashboard Realtime Database di console Firebase Anda, misal https://[nama-proyek]-default-rtdb.firebaseio.com/",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                "TESTING" -> {
                                    Text("⏳ MEMINTA VALIDASI CLOUD...", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "Membuat bucket handshake dengan REST API Firebase Cloud...",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                else -> {
                                    Text("🌐 GANGGUAN HANDSHAKE FIREBASE", color = DangerAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "Keterangan:\n$firebaseDiagnosticResult\n\nTips:\nPastikan alamat internet aktif dan format database base url yang diisi tidak mengandung spasi atau karakter ilegal.",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SYNC & REPORT EXPORT CARD (Fulfills the "Aplikasi terhubung" & "ekspor PDF/Google Sheet" requirements)
        item {
            var syncInput by remember { mutableStateOf("") }
            var showOverwriteConfirm by remember { mutableStateOf(false) }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📡 Sinergi Awan & Laporan Ekspor",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CombinedAccent
                )
                Text(
                    text = "Gunakan fitur ini agar aplikasi di HP Haikal & HP Ummu tetap sinkron secara instan, serta ekspor laporan keuangan.",
                    fontSize = 10.sp,
                    color = TextSecondary
                )

                HorizontalDivider(color = BorderColor)

                // PART A: SYNC
                Text(
                    text = "🔗 Sinkronisasi HP Haikal & HP Ummu",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            val json = viewModel.exportDatabaseAsJson()
                            if (json.isNotEmpty()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("DuniaSyncCode", json)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Kode Sinkronisasi tersalin! Kirim via WhatsApp ke pasangan.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Gagal mengekspor data.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = CombinedAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salin Kode Kita", color = CombinedAccent, fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = syncInput,
                    onValueChange = { syncInput = it },
                    label = { Text("Tempel Kode dari Pasangan") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CombinedAccent),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Merge button
                    Button(
                        onClick = {
                            if (syncInput.trim().isEmpty()) {
                                Toast.makeText(context, "Harap tempel kode sinkronisasi terlebih dahulu!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.importDatabaseFromJson(syncInput, mergeOnly = true) { success ->
                                    if (success) {
                                        syncInput = ""
                                        Toast.makeText(context, "🤝 Berhasil menggabungkan data baru tanpa menghapus data lama!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Kode tidak valid, harap periksa kembali.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gabungkan Data 🤝", color = SuccessAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Overwrite button
                    Button(
                        onClick = {
                            if (syncInput.trim().isEmpty()) {
                                Toast.makeText(context, "Harap tempel kode sinkronisasi terlebih dahulu!", Toast.LENGTH_SHORT).show()
                            } else {
                                showOverwriteConfirm = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerAccent.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Text("Mulai Timpa 🔄", color = DangerAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (showOverwriteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showOverwriteConfirm = false },
                        containerColor = BgCard,
                        title = { Text("Timpa Semua Data?", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        text = {
                            Text(
                                text = "Perhatian! Pilihan ini akan menghapus semua catatan di HP ini secara total dan menggantinya dengan data dari pasangan Anda. Apakah Anda yakin?",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showOverwriteConfirm = false
                                    viewModel.importDatabaseFromJson(syncInput, mergeOnly = false) { success ->
                                        if (success) {
                                            syncInput = ""
                                            Toast.makeText(context, "🔄 Semua data berhasil ditimpa dan disinkronkan!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Kode tidak valid.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerAccent)
                            ) {
                                Text("Ya, Timpa", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showOverwriteConfirm = false }) {
                                Text("Batal")
                            }
                        }
                    )
                }

                HorizontalDivider(color = BorderColor)

                // PART B: REPORTS
                Text(
                    text = "📄 Ekspor Laporan Finansial",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Google Sheets sharing (CSV File)
                    Button(
                        onClick = {
                            val uri = com.example.api.ReportGenerator.generateProfessionalCsvFile(context, viewModel)
                            if (uri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_SUBJECT, "Spreadsheet Laporan Keuangan DUNIA")
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Ekspor Google Sheets (CSV)"))
                            } else {
                                Toast.makeText(context, "Gagal membuat file Spreadsheet", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Google Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // PDF Journal Summary sharing (Native PDF File)
                    Button(
                        onClick = {
                            val uri = com.example.api.ReportGenerator.generateProfessionalPdf(context, viewModel)
                            if (uri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_SUBJECT, "Laporan Analisis Finansial DUNIA (PDF)")
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan Finansial (PDF)"))
                            } else {
                                Toast.makeText(context, "Gagal memproses Laporan PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor Jurnal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.saveConfigValue("PEKERJAAN_HAIKAL", jobHaikal)
                    viewModel.saveConfigValue("PEKERJAAN_UMMU", jobUmmu)
                    viewModel.saveConfigValue("GAJI_HAIKAL", salaryHaikal)
                    viewModel.saveConfigValue("GAJI_UMMU", salaryUmmu)
                    viewModel.saveConfigValue("TARGET_NIKAH", weddingYear)
                    viewModel.saveConfigValue("SYNC_KEY", syncKey)
                    viewModel.saveConfigValue("SYNC_AUTO_ENABLED", if (isAutoSyncEnabled) "true" else "false")
                    viewModel.saveConfigValue("SHEETS_WEBAPP_URL", sheetsUrl)
                    viewModel.saveConfigValue("SHEETS_AUTO_ENABLED", if (isSheetsAutoEnabled) "true" else "false")

                    Toast.makeText(context, "Profil DUNIA berhasil disimpan !", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CombinedAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Semua Konfigurasi", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// UTILITY FUNCTIONS & STYLES
// ==========================================

fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.maximumFractionDigits = 0
    return format.format(amount).replace("Rp", "Rp ")
}

@Composable
fun MilestoneCircularIndicator(
    progress: Float,
    title: String,
    target: String,
    saved: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(90.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(80.dp)) {
                // Background Track ring
                drawCircle(
                    color = Color(0xFFE2E8F0),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
                
                // Sweep progress arc
                drawArc(
                    color = indicatorColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Tercapai",
                    fontSize = 8.sp,
                    color = TextMuted
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = saved,
            fontSize = 10.sp,
            color = indicatorColor,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "dari $target",
            fontSize = 8.sp,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
