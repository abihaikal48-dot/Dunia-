package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- Chat Message for AI Advisor ---
data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class DuniaViewModel(private val repository: DuniaRepository) : ViewModel() {

    // --- REAL-TIME AUTO SYNC FIELDS & FLOWS ---
    private var isSyncingNetwork = false
    private val clientId = java.util.UUID.randomUUID().toString().take(8)
    private var localLastUpdated = 0L
    private var lastUploadedHash = ""

    private val _syncStatus = MutableStateFlow("Terputus (Belum Diatur)")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("Belum pernah")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    // --- GOOGLE SHEETS AUTO SYNC FIELDS & FLOWS ---
    private var isSheetsSyncingNetwork = false
    private var lastSheetsUploadedHash = ""
    private var sheetsUploadJob: kotlinx.coroutines.Job? = null

    private val _sheetsSyncStatus = MutableStateFlow("Google Sheets Nonaktif")
    val sheetsSyncStatus: StateFlow<String> = _sheetsSyncStatus.asStateFlow()

    private val _lastSheetsSyncTime = MutableStateFlow("Belum pernah")
    val lastSheetsSyncTime: StateFlow<String> = _lastSheetsSyncTime.asStateFlow()

    // --- State Streams From Room ---
    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingGoals: StateFlow<List<SavingGoalEntity>> = repository.allSavingGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cicilanList: StateFlow<List<CicilanEntity>> = repository.allCicilan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val milestones: StateFlow<List<MilestoneEntity>> = repository.allMilestones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activities: StateFlow<List<ActivityEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ibadahLogs: StateFlow<List<IbadahEntity>> = repository.allIbadahLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rapatItems: StateFlow<List<RapatEntity>> = repository.allRapat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishlistItems: StateFlow<List<WishlistEntity>> = repository.allWishlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val configs: StateFlow<Map<String, String>> = repository.configMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        observeAndUploadOnChanges()
        startCloudPolling()
    }

    // --- Dynamic UI State ---
    private val _aiChatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("Halo Haikal & Ummu! Aku adalah DUNIA AI. Tanyakan apa saja tentang pencapaian goals, evaluasi budget bulanan, strategi cicilan, atau draf kencan hemat!", false)
        )
    )
    val aiChatHistory: StateFlow<List<ChatMessage>> = _aiChatHistory.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    // --- CURRENT SELECTIONS ---
    private val _currentMonthFilter = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // 0-11
    val currentMonthFilter: StateFlow<Int> = _currentMonthFilter.asStateFlow()

    private val _currentYearFilter = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYearFilter: StateFlow<Int> = _currentYearFilter.asStateFlow()

    fun updateMonthFilter(month: Int) {
        _currentMonthFilter.value = month
    }

    // ==========================================
    // COMPLEX CALCULATIONS & METRICS (EXPOSED AS REACTIVE STATE)
    // ==========================================

    // Haikal Pemasukan & Pengeluaran Bulan Ini
    val monthlyStats = combine(
        transactions,
        configs,
        _currentMonthFilter,
        _currentYearFilter
    ) { txList, configData, activeMonth, activeYear ->
        val cal = Calendar.getInstance()
        val monthTx = txList.filter {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.MONTH) == activeMonth && cal.get(Calendar.YEAR) == activeYear
        }

        // Pemasukan
        val haikalIncome = monthTx.filter { it.type == "PEMASUKAN" && it.user == "HAIKAL" }.sumOf { it.amount }
        val ummuIncome = monthTx.filter { it.type == "PEMASUKAN" && it.user == "UMMU" }.sumOf { it.amount }
        val jointIncome = monthTx.filter { it.type == "PEMASUKAN" && it.user == "BERDUA" }.sumOf { it.amount }
        val totalIncome = haikalIncome + ummuIncome + jointIncome

        // Pengeluaran
        val haikalExpense = monthTx.filter { it.type == "PENGELUARAN" && it.user == "HAIKAL" }.sumOf { it.amount }
        val ummuExpense = monthTx.filter { it.type == "PENGELUARAN" && it.user == "UMMU" }.sumOf { it.amount }
        val jointExpense = monthTx.filter { it.type == "PENGELUARAN" && it.user == "BERDUA" }.sumOf { it.amount }
        val totalExpense = haikalExpense + ummuExpense + jointExpense

        val surplus = totalIncome - totalExpense

        // Kategori breakdown
        val categoryBreakdown = monthTx.filter { it.type == "PENGELUARAN" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Parse dynamic budget goals from configs
        val haikalName = configData["NAMA_HAIKAL"] ?: "Haikal"
        val ummuName = configData["NAMA_UMMU"] ?: "Ummu"

        val dynamicBudgets = mutableListOf<BudgetStatus>()
        configData.forEach { (key, value) ->
            if (key.startsWith("BUDGET_GOAL:")) {
                val parts = key.split(":")
                if (parts.size >= 3) {
                    val categoryName = parts[1]
                    val owner = parts[2]
                    val limitAmt = value.toDoubleOrNull() ?: 0.0

                    if (limitAmt > 0.0) {
                        val actualAmt = monthTx.filter { tx ->
                            tx.type == "PENGELUARAN" &&
                            tx.category.trim().lowercase() == categoryName.trim().lowercase() &&
                            (owner == "BERDUA" || tx.user == owner)
                        }.sumOf { it.amount }

                        val displayOwner = if (owner == "HAIKAL") " ($haikalName)" else if (owner == "UMMU") " ($ummuName)" else " (Bersama)"
                        dynamicBudgets.add(BudgetStatus(categoryName + displayOwner, actualAmt, limitAmt, categoryName, owner))
                    }
                }
            }
        }

        // Fallback to seeded budgets if no custom dynamic budgets are present
        val finalBudgetAlerts = if (dynamicBudgets.isNotEmpty()) {
            dynamicBudgets
        } else {
            val budgetMakanHaikal = configData["BUDGET_MAKAN_HAIKAL"]?.toDoubleOrNull() ?: 450000.0
            val budgetTransportHaikal = configData["BUDGET_TRANSPORT"]?.toDoubleOrNull() ?: 150000.0
            val budgetKencan = configData["BUDGET_KENCAN"]?.toDoubleOrNull() ?: 200000.0

            val actualMakan = monthTx.filter { it.type == "PENGELUARAN" && (it.category == "Makan & Jajan" || it.category == "Makan, Sembako & Jajan") && it.user == "HAIKAL" }.sumOf { it.amount }
            val actualTransport = monthTx.filter { it.type == "PENGELUARAN" && (it.category == "Transportasi" || it.category == "Transportasi & Bensin") && it.user == "HAIKAL" }.sumOf { it.amount }
            val actualKencan = monthTx.filter { it.type == "PENGELUARAN" && (it.category == "Kencan" || it.category == "Anggaran Kencan / Berdua") }.sumOf { it.amount }

            listOf(
                BudgetStatus("Makan $haikalName", actualMakan, budgetMakanHaikal, "Makan, Sembako & Jajan", "HAIKAL"),
                BudgetStatus("Transport $haikalName", actualTransport, budgetTransportHaikal, "Transportasi & Bensin", "HAIKAL"),
                BudgetStatus("Kencan Bersama", actualKencan, budgetKencan, "Anggaran Kencan / Berdua", "BERDUA")
            )
        }

        MonthlyOverview(
            haikalIncome = haikalIncome,
            ummuIncome = ummuIncome,
            jointIncome = jointIncome,
            totalIncome = totalIncome,
            haikalExpense = haikalExpense,
            ummuExpense = ummuExpense,
            jointExpense = jointExpense,
            totalExpense = totalExpense,
            surplus = surplus,
            categoryBreakdown = categoryBreakdown,
            budgetAlerts = finalBudgetAlerts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyOverview())

    // 22. Financial Health Score (0-100) per person and combined
    val financialHealthScore = combine(
        monthlyStats,
        savingGoals,
        cicilanList,
        transactions
    ) { stats, goals, cicilan, txs ->
        // 1. Saving Rate (25% weight) -> 100 if saving rate >= 30%, 0 if <= 5%
        val savingRate = if (stats.totalIncome > 0) (stats.surplus / stats.totalIncome) * 100 else 0.0
        val savingScore = when {
            savingRate >= 30.0 -> 100.0
            savingRate <= 5.0 -> 0.0
            else -> ((savingRate - 5.0) / 25.0) * 100.0
        }

        // 2. Debt to Income Ratio (20% weight) -> 100 if ratio <= 20%, 0 if >= 50%
        val totalMonthlyCicilan = cicilan.sumOf { it.monthlyPayment }
        val debtRatio = if (stats.totalIncome > 0) (totalMonthlyCicilan / stats.totalIncome) * 100 else 0.0
        val debtScore = when {
            debtRatio <= 20.0 -> 100.0
            debtRatio >= 50.0 -> 0.0
            else -> (1 - ((debtRatio - 20.0) / 30.0)) * 100.0
        }

        // 3. Emergency Fund Progress (20% weight) -> average progress of emergency goals met
        val emGoals = goals.filter { it.name.contains("Dana Darurat", ignoreCase = true) }
        val emScore = if (emGoals.isNotEmpty()) {
            emGoals.map { (it.currentAmount / it.targetAmount).coerceIn(0.0, 1.0) * 100.0 }.average()
        } else 100.0

        // 4. Budget Adherence (15% weight) -> 100 if no budget over-limits, penalize per warning
        val overLimitCount = stats.budgetAlerts.count { it.actual > it.budget }
        val budgetScore = (100.0 - (overLimitCount * 33.3)).coerceIn(0.0, 100.0)

        // 5. Goals Progress (10% weight) -> average of all goals
        val goalsProgress = if (goals.isNotEmpty()) {
            goals.map { (it.currentAmount / it.targetAmount).coerceIn(0.0, 1.0) * 100.0 }.average()
        } else 100.0

        // 6. Streak Catat (10% weight) -> calculate number of unique active days in past 30 days
        val cal = Calendar.getInstance()
        val nowT = System.currentTimeMillis()
        val logDays = txs.filter { nowT - it.timestamp < 30 * 24 * 3600 * 1000L }
            .map {
                cal.timeInMillis = it.timestamp
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
            }
            .distinct()
            .count()
        val streakScore = ((logDays / 20.0) * 100.0).coerceIn(0.0, 100.0) // 100 if recorded 20 days or more

        // Final score
        val finalScore = (savingScore * 0.25) + (debtScore * 0.20) + (emScore * 0.20) + (budgetScore * 0.15) + (goalsProgress * 0.10) + (streakScore * 0.10)
        
        HealthScore(
            score = finalScore.toInt(),
            savingRate = savingRate.toInt(),
            debtRatio = debtRatio.toInt(),
            overLimitCount = overLimitCount,
            activeRecordingDays = logDays
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HealthScore())

    // Countdown targets (Nikah & DP Rumah)
    val targetsCountdown = combine(configs, savingGoals) { configData, goalsList ->
        val targetNikahYear = configData["TARGET_NIKAH"]?.toIntOrNull() ?: 2029
        val targetHousePrice = configData["TARGET_RUMAH_HARGA"]?.toDoubleOrNull() ?: 400000000.0
        val targetDPPercent = configData["TARGET_DP_PERSEN"]?.toDoubleOrNull() ?: 10.0
        val dpTargetVal = targetHousePrice * (targetDPPercent / 100.0)

        // Current Saved DP Rumah
        val dpGoal = goalsList.find { it.name.contains("DP Rumah", ignoreCase = true) }
        val dpSaved = dpGoal?.currentAmount ?: 0.0

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val yearsToNikah = (targetNikahYear - currentYear).coerceAtLeast(0)

        CountdownInfo(
            yearsToNikah = yearsToNikah,
            dpTargetVal = dpTargetVal,
            dpSaved = dpSaved,
            dpPercent = if (dpTargetVal > 0) (dpSaved / dpTargetVal * 100).toInt() else 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CountdownInfo())

    // ==========================================
    // OPERATIONS (DATABASE ACTIONS)
    // ==========================================

    fun addTransaction(
        type: String,
        user: String,
        amount: Double,
        category: String,
        description: String,
        tag: String,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    user = user,
                    amount = amount,
                    category = category,
                    description = description,
                    tag = tag,
                    imageUri = imageUri
                )
            )
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    fun addSavingContribution(goalId: Int, amount: Double) {
        viewModelScope.launch {
            val goal = savingGoals.value.find { it.id == goalId }
            if (goal != null) {
                repository.updateSavingGoal(
                    goal.copy(currentAmount = goal.currentAmount + amount)
                )
                // Log this as a special positive transaction that tracks saving activity!
                repository.insertTransaction(
                    TransactionEntity(
                        timestamp = System.currentTimeMillis(),
                        type = "PENGELUARAN", // saving acts as cash out flow from wallet
                        user = goal.owner,
                        amount = amount,
                        category = "Tabungan & Investasi",
                        description = "Nabung ke: ${goal.name}",
                        tag = "#nabung"
                    )
                )
            }
        }
    }

    fun editGoalCurrentAmount(goalId: Int, amount: Double) {
        viewModelScope.launch {
            val goal = savingGoals.value.find { it.id == goalId }
            if (goal != null) {
                repository.updateSavingGoal(goal.copy(currentAmount = amount))
            }
        }
    }

    fun insertCustomSavingGoal(name: String, targetAmount: Double, currentAmount: Double, owner: String) {
        viewModelScope.launch {
            repository.insertSavingGoal(
                com.example.data.SavingGoalEntity(
                    id = 0,
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    owner = owner
                )
            )
        }
    }

    fun updateCustomSavingGoal(goalId: Int, name: String, targetAmount: Double, currentAmount: Double, owner: String) {
        viewModelScope.launch {
            val goal = savingGoals.value.find { it.id == goalId }
            if (goal != null) {
                repository.updateSavingGoal(
                    goal.copy(
                        name = name,
                        targetAmount = targetAmount,
                        currentAmount = currentAmount,
                        owner = owner
                    )
                )
            }
        }
    }

    fun deleteSavingGoal(goalId: Int) {
        viewModelScope.launch {
            val goal = savingGoals.value.find { it.id == goalId }
            if (goal != null) {
                repository.deleteSavingGoal(goal)
            }
        }
    }

    fun payCicilan(cicilanId: Int) {
        viewModelScope.launch {
            val cicilanObj = cicilanList.value.find { it.id == cicilanId }
            if (cicilanObj != null && cicilanObj.remainingMonths > 0) {
                val updated = cicilanObj.copy(
                    remainingMonths = cicilanObj.remainingMonths - 1,
                    paidMonths = cicilanObj.paidMonths + 1
                )
                repository.updateCicilan(updated)

                // Insert pay transaction log
                repository.insertTransaction(
                    TransactionEntity(
                        timestamp = System.currentTimeMillis(),
                        type = "PENGELUARAN",
                        user = cicilanObj.owner,
                        amount = cicilanObj.monthlyPayment,
                        category = "Cicilan",
                        description = "Bayar ${cicilanObj.name} (Bulan ${updated.paidMonths})",
                        tag = "#rutin"
                    )
                )
            }
        }
    }

    fun addCicilan(name: String, total: Double, monthly: Double, months: Int, dueDay: Int, owner: String) {
        viewModelScope.launch {
            repository.insertCicilan(
                CicilanEntity(0, name, total, monthly, months, 0, dueDay, owner)
            )
        }
    }

    fun deleteCicilan(cicilan: CicilanEntity) {
        viewModelScope.launch {
            repository.deleteCicilan(cicilan)
        }
    }

    fun toggleMilestone(milestone: MilestoneEntity) {
        viewModelScope.launch {
            repository.updateMilestone(milestone.copy(checked = !milestone.checked))
        }
    }

    fun addActivity(name: String, start: String, end: String, owner: String, saves: Boolean, category: String) {
        viewModelScope.launch {
            repository.insertActivity(ActivityEntity(0, name, start, end, owner, saves, category))
        }
    }

    fun deleteActivity(activity: ActivityEntity) {
        viewModelScope.launch {
            repository.deleteActivity(activity)
        }
    }

    fun addWishlistItem(name: String, price: Double, category: String, priority: Int, owner: String) {
        viewModelScope.launch {
            repository.insertWishlist(
                WishlistEntity(
                    0, name, price, category, priority, owner,
                    System.currentTimeMillis(), "PENDING"
                )
            )
        }
    }

    fun updateWishlistStatus(item: WishlistEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateWishlist(item.copy(status = newStatus))

            if (newStatus == "DIBELI") {
                // If bought, auto log transaction!
                repository.insertTransaction(
                    TransactionEntity(
                        timestamp = System.currentTimeMillis(),
                        type = "PENGELUARAN",
                        user = item.owner,
                        amount = item.estimatedPrice,
                        category = "Belanja Wishlist",
                        description = "Wishlist dibeli: ${item.name}",
                        tag = "#wishlist"
                    )
                )
            }
        }
    }

    fun deleteWishlist(item: WishlistEntity) {
        viewModelScope.launch {
            repository.deleteWishlist(item)
        }
    }

    fun saveConfigValue(key: String, value: String) {
        viewModelScope.launch {
            repository.saveConfig(key, value)
        }
    }

    fun saveBudgetGoal(category: String, owner: String, limit: Double) {
        viewModelScope.launch {
            repository.saveConfig("BUDGET_GOAL:$category:$owner", limit.toString())
        }
    }

    fun deleteBudgetGoal(category: String, owner: String) {
        viewModelScope.launch {
            repository.deleteConfig("BUDGET_GOAL:$category:$owner")
        }
    }

    fun saveIbadahLog(subuh: Boolean, dzuhur: Boolean, ashar: Boolean, maghrib: Boolean, isya: Boolean, pages: Int, sedekah: Double) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.insertIbadah(
                IbadahEntity(
                    dateStr = dateStr,
                    subuh = subuh,
                    dzuhur = dzuhur,
                    ashar = ashar,
                    maghrib = maghrib,
                    isya = isya,
                    ngajiHalaman = pages,
                    sedekahAmount = sedekah
                )
            )

            if (sedekah > 0.0) {
                // Log sedekah transaction automatically!
                repository.insertTransaction(
                    TransactionEntity(
                        timestamp = System.currentTimeMillis(),
                        type = "PENGELUARAN",
                        user = "BERDUA",
                        amount = sedekah,
                        category = "Ibadah & Sosial",
                        description = "Sedekah harian via Ibadah Tracker",
                        tag = "#ibadah"
                    )
                )
            }
        }
    }

    fun addMonthlyRapat(agenda: String, minutes: String, decisions: String, actionItems: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
            repository.insertRapat(
                RapatEntity(0, dateStr, agenda, minutes, decisions, actionItems)
            )
        }
    }

    // ==========================================
    // 12. AI ADVISOR CHAT ENGINE
    // ==========================================

    fun sendChatMessage(text: String) {
        if (text.trim().isEmpty()) return

        val userMsg = ChatMessage(text, true)
        _aiChatHistory.value = _aiChatHistory.value + userMsg

        _aiLoading.value = true

        viewModelScope.launch {
            // Build Context Data from local flows
            val haikalName = configs.value["NAMA_HAIKAL"] ?: "Haikal"
            val ummuName = configs.value["NAMA_UMMU"] ?: "Ummu"
            val stats = monthlyStats.value
            val goals = savingGoals.value
            val score = financialHealthScore.value
            val countdown = targetsCountdown.value
            val milestonesList = milestones.value.filter { !it.checked }

            // Dynamic Context Prompt for Gemini response
            val systemPrompt = """
                Kamu adalah DUNIA AI - penasihat keuangan dan roadmap kehidupan personal cerdas untuk pasangan $haikalName dan $ummuName.
                Status mereka: Pacaran aktif, target menikah tahun 2029/2030, fase hidup 0 (Tahun Santai & Fondasi).
                
                DATA KEUANGAN AKTUAL (REAL-TIME LOCAL DB):
                - Pengguna: $haikalName & $ummuName
                - $haikalName Pekerjaan/Gaji: ${configs.value["PEKERJAAN_HAIKAL"]} | Rp ${configs.value["GAJI_HAIKAL"]}
                - $ummuName Pekerjaan/Gaji: ${configs.value["PEKERJAAN_UMMU"]} | Rp ${configs.value["GAJI_UMMU"]}
                - total pemasukan bulan ini: Rp ${stats.totalIncome}
                - total pengeluaran bulan ini: Rp ${stats.totalExpense}
                - Surplus tabungan bulan ini: Rp ${stats.surplus}
                - Health Score Keuangan: ${score.score} / 100 (${getHealthLabel(score.score)})
                - Rasio cicilan bulanan: ${score.debtRatio}% dari gaji
                - Tabungan darurat / pernikahan: ${goals.joinToString { "${it.name}: Rp ${it.currentAmount} dari target Rp ${it.targetAmount}" }}
                - Progres DP Rumah: Rp ${countdown.dpSaved} dari target Rp ${countdown.dpTargetVal} (${countdown.dpPercent}% tercapai)
                - Milestone belum lunas: ${milestonesList.take(3).joinToString { it.title }}
                
                GAYA JAWABAN:
                - Jawab dalam Bahasa Indonesia yang sangat hangat, menyemangati, bijak namun to-the-point.
                - Sertakan angka spesifik dari data diatas jika menyangkut performa mereka.
                - Berikan persis 3 saran konkret & actionable (langsung bisa dipraktekkan) berpoin 1, 2, 3.
                - Jujur dan objektif, tetapi dilarang toxic atau menghakimi secara kasar. Ingat 2026 adalah tahun santai dan fondasi.
            """.trimIndent()

            val aiResponseText = GeminiClient.generateAdvisorResponse(text, systemPrompt)
            _aiLoading.value = false
            _aiChatHistory.value = _aiChatHistory.value + ChatMessage(aiResponseText, false)
        }
    }

    private fun getHealthLabel(score: Int): String {
        return when (score) {
            in 0..30 -> "KRITIS"
            in 31..60 -> "PERLU PERHATIAN"
            in 61..80 -> "SEHAT/BAIK"
            else -> "EXCELLENT"
        }
    }

    fun triggerQuickQuestion(preset: String) {
        sendChatMessage(preset)
    }

    fun clearChat() {
        _aiChatHistory.value = listOf(
            ChatMessage("Halo Haikal & Ummu! Chat riwayat telah dirapikan. Ada yang ingin dianotasi atau dievaluasi hari ini?", false)
        )
    }

    // --- PEER-TO-PEER DATA SYNC ENGINE ---
    fun exportDatabaseAsJson(): String {
        return try {
            val rootObj = org.json.JSONObject()
            
            // 1. Transactions
            val txArray = org.json.JSONArray()
            transactions.value.forEach { tx ->
                val txObj = org.json.JSONObject().apply {
                    put("id", tx.id)
                    put("timestamp", tx.timestamp)
                    put("type", tx.type)
                    put("user", tx.user)
                    put("amount", tx.amount)
                    put("category", tx.category)
                    put("description", tx.description)
                    put("tag", tx.tag)
                    put("imageUri", tx.imageUri ?: "")
                }
                txArray.put(txObj)
            }
            rootObj.put("transactions", txArray)

            // 2. Saving Goals
            val goalsArray = org.json.JSONArray()
            savingGoals.value.forEach { goal ->
                val goalObj = org.json.JSONObject().apply {
                    put("id", goal.id)
                    put("name", goal.name)
                    put("targetAmount", goal.targetAmount)
                    put("currentAmount", goal.currentAmount)
                    put("owner", goal.owner)
                }
                goalsArray.put(goalObj)
            }
            rootObj.put("saving_goals", goalsArray)

            // 3. Cicilan
            val cicilanArray = org.json.JSONArray()
            cicilanList.value.forEach { item ->
                val cObj = org.json.JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("totalValue", item.totalValue)
                    put("monthlyPayment", item.monthlyPayment)
                    put("remainingMonths", item.remainingMonths)
                    put("paidMonths", item.paidMonths)
                    put("dueDateDay", item.dueDateDay)
                    put("owner", item.owner)
                }
                cicilanArray.put(cObj)
            }
            rootObj.put("cicilan", cicilanArray)

            // 4. Milestones
            val milestonesArray = org.json.JSONArray()
            milestones.value.forEach { ms ->
                val mObj = org.json.JSONObject().apply {
                    put("id", ms.id)
                    put("fase", ms.fase)
                    put("title", ms.title)
                    put("description", ms.description)
                    put("targetDate", ms.targetDate)
                    put("checked", ms.checked)
                }
                milestonesArray.put(mObj)
            }
            rootObj.put("milestones", milestonesArray)

            // 5. Activities
            val actArray = org.json.JSONArray()
            activities.value.forEach { act ->
                val aObj = org.json.JSONObject().apply {
                    put("id", act.id)
                    put("name", act.name)
                    put("startTime", act.startTime)
                    put("endTime", act.endTime)
                    put("owner", act.owner)
                    put("savesMoney", act.savesMoney)
                    put("category", act.category)
                }
                actArray.put(aObj)
            }
            rootObj.put("activities", actArray)

            // 6. Ibadah
            val ibadahArray = org.json.JSONArray()
            ibadahLogs.value.forEach { ib ->
                val iObj = org.json.JSONObject().apply {
                    put("dateStr", ib.dateStr)
                    put("subuh", ib.subuh)
                    put("dzuhur", ib.dzuhur)
                    put("ashar", ib.ashar)
                    put("maghrib", ib.maghrib)
                    put("isya", ib.isya)
                    put("ngajiHalaman", ib.ngajiHalaman)
                    put("sedekahAmount", ib.sedekahAmount)
                }
                ibadahArray.put(iObj)
            }
            rootObj.put("ibadah_logs", ibadahArray)

            // 7. Rapat
            val rapatArray = org.json.JSONArray()
            rapatItems.value.forEach { r ->
                val rObj = org.json.JSONObject().apply {
                    put("id", r.id)
                    put("dateStr", r.dateStr)
                    put("agenda", r.agenda)
                    put("minutes", r.minutes)
                    put("decisions", r.decisions)
                    put("actionItems", r.actionItems)
                }
                rapatArray.put(rObj)
            }
            rootObj.put("rapat_items", rapatArray)

            // 8. Wishlist
            val wishArray = org.json.JSONArray()
            wishlistItems.value.forEach { w ->
                val wObj = org.json.JSONObject().apply {
                    put("id", w.id)
                    put("name", w.name)
                    put("estimatedPrice", w.estimatedPrice)
                    put("category", w.category)
                    put("priority", w.priority)
                    put("owner", w.owner)
                    put("addedDate", w.addedDate)
                    put("status", w.status)
                }
                wishArray.put(wObj)
            }
            rootObj.put("wishlist_items", wishArray)

            // 9. Config
            val configArray = org.json.JSONArray()
            configs.value.forEach { (k, v) ->
                val cObj = org.json.JSONObject().apply {
                    put("key", k)
                    put("value", v)
                }
                configArray.put(cObj)
            }
            rootObj.put("sys_config", configArray)

            rootObj.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun importDatabaseFromJson(jsonStr: String, mergeOnly: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(jsonStr)

                if (!mergeOnly) {
                    repository.clearAllData()
                }

                // 1. Sys Config
                val configArray = root.optJSONArray("sys_config")
                if (configArray != null) {
                    val configsList = mutableListOf<ConfigEntity>()
                    for (i in 0 until configArray.length()) {
                        val obj = configArray.getJSONObject(i)
                        configsList.add(
                            ConfigEntity(
                                key = obj.getString("key"),
                                value = obj.getString("value")
                            )
                        )
                    }
                    repository.insertConfigs(configsList)
                }

                // 2. Saving goals
                val goalsArray = root.optJSONArray("saving_goals")
                if (goalsArray != null) {
                    val goalsList = mutableListOf<SavingGoalEntity>()
                    for (i in 0 until goalsArray.length()) {
                        val obj = goalsArray.getJSONObject(i)
                        goalsList.add(
                            SavingGoalEntity(
                                id = if (mergeOnly) 0 else obj.optInt("id", 0),
                                name = obj.getString("name"),
                                targetAmount = obj.getDouble("targetAmount"),
                                currentAmount = obj.getDouble("currentAmount"),
                                owner = obj.getString("owner")
                            )
                        )
                    }
                    repository.insertSavingGoals(goalsList)
                }

                // 3. Cicilan
                val cicilanArray = root.optJSONArray("cicilan")
                if (cicilanArray != null) {
                    val cList = mutableListOf<CicilanEntity>()
                    for (i in 0 until cicilanArray.length()) {
                        val obj = cicilanArray.getJSONObject(i)
                        cList.add(
                            CicilanEntity(
                                id = if (mergeOnly) 0 else obj.optInt("id", 0),
                                name = obj.getString("name"),
                                totalValue = obj.getDouble("totalValue"),
                                monthlyPayment = obj.getDouble("monthlyPayment"),
                                remainingMonths = obj.getInt("remainingMonths"),
                                paidMonths = obj.getInt("paidMonths"),
                                dueDateDay = obj.getInt("dueDateDay"),
                                owner = obj.getString("owner")
                            )
                        )
                    }
                    repository.insertCicilanList(cList)
                }

                // 4. Milestones
                val milestonesArray = root.optJSONArray("milestones")
                if (milestonesArray != null) {
                    val msList = mutableListOf<MilestoneEntity>()
                    for (i in 0 until milestonesArray.length()) {
                        val obj = milestonesArray.getJSONObject(i)
                        msList.add(
                            MilestoneEntity(
                                id = if (mergeOnly) 0 else obj.optInt("id", 0),
                                fase = obj.getInt("fase"),
                                title = obj.getString("title"),
                                description = obj.getString("description"),
                                targetDate = obj.getString("targetDate"),
                                checked = obj.getBoolean("checked")
                            )
                        )
                    }
                    repository.insertMilestones(msList)
                }

                // 5. Activities
                val actArray = root.optJSONArray("activities")
                if (actArray != null) {
                    val aList = mutableListOf<ActivityEntity>()
                    for (i in 0 until actArray.length()) {
                        val obj = actArray.getJSONObject(i)
                        aList.add(
                            ActivityEntity(
                                id = if (mergeOnly) 0 else obj.optInt("id", 0),
                                name = obj.getString("name"),
                                startTime = obj.getString("startTime"),
                                endTime = obj.getString("endTime"),
                                owner = obj.getString("owner"),
                                savesMoney = obj.getBoolean("savesMoney"),
                                category = obj.optString("category", "Lainnya")
                            )
                        )
                    }
                    repository.insertActivities(aList)
                }

                // 6. Ibadah
                val ibadahArray = root.optJSONArray("ibadah_logs")
                if (ibadahArray != null) {
                    val ibList = mutableListOf<IbadahEntity>()
                    for (i in 0 until ibadahArray.length()) {
                        val obj = ibadahArray.getJSONObject(i)
                        ibList.add(
                            IbadahEntity(
                                dateStr = obj.getString("dateStr"),
                                subuh = obj.optBoolean("subuh", false),
                                dzuhur = obj.optBoolean("dzuhur", false),
                                ashar = obj.optBoolean("ashar", false),
                                maghrib = obj.optBoolean("maghrib", false),
                                isya = obj.optBoolean("isya", false),
                                ngajiHalaman = obj.optInt("ngajiHalaman", 0),
                                sedekahAmount = obj.optDouble("sedekahAmount", 0.0)
                            )
                        )
                    }
                    ibList.forEach { repository.insertIbadah(it) }
                }

                // 7. Rapat
                val rapatArray = root.optJSONArray("rapat_items")
                if (rapatArray != null) {
                    val rList = mutableListOf<RapatEntity>()
                    for (i in 0 until rapatArray.length()) {
                        val obj = rapatArray.getJSONObject(i)
                        rList.add(
                            RapatEntity(
                                id = if (mergeOnly) 0 else obj.optInt("id", 0),
                                dateStr = obj.getString("dateStr"),
                                agenda = obj.getString("agenda"),
                                minutes = obj.getString("minutes"),
                                decisions = obj.getString("decisions"),
                                actionItems = obj.getString("actionItems")
                            )
                        )
                    }
                    repository.insertRapatList(rList)
                }

                // 8. Wishlist
                val wishArray = root.optJSONArray("wishlist_items")
                if (wishArray != null) {
                    val wList = mutableListOf<WishlistEntity>()
                    for (i in 0 until wishArray.length()) {
                        val obj = wishArray.getJSONObject(i)
                        wList.add(
                            WishlistEntity(
                                id = if (mergeOnly) 0 else obj.optInt("id", 0),
                                name = obj.getString("name"),
                                estimatedPrice = obj.getDouble("estimatedPrice"),
                                category = obj.getString("category"),
                                priority = obj.getInt("priority"),
                                owner = obj.getString("owner"),
                                addedDate = obj.getLong("addedDate"),
                                status = obj.getString("status")
                            )
                        )
                    }
                    repository.insertWishlistItems(wList)
                }

                // 9. Transactions
                val txArray = root.optJSONArray("transactions")
                if (txArray != null) {
                    val txsList = mutableListOf<TransactionEntity>()
                    for (i in 0 until txArray.length()) {
                        val obj = txArray.getJSONObject(i)
                        
                        val isDup = if (mergeOnly) {
                            val curDesc = obj.optString("description")
                            val curAmt = obj.optDouble("amount")
                            transactions.value.any { it.description == curDesc && it.amount == curAmt }
                        } else false

                        if (!isDup) {
                            txsList.add(
                                TransactionEntity(
                                    id = if (mergeOnly) 0 else obj.optInt("id", 0),
                                    timestamp = obj.getLong("timestamp"),
                                    type = obj.getString("type"),
                                    user = obj.getString("user"),
                                    amount = obj.getDouble("amount"),
                                    category = obj.getString("category"),
                                    description = obj.getString("description"),
                                    tag = obj.getString("tag"),
                                    imageUri = if (obj.isNull("imageUri")) null else obj.optString("imageUri", null)
                                )
                            )
                        }
                    }
                    repository.insertTransactions(txsList)
                }

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    // --- FINANCIAL EXPORTS ENGINE ---
    fun getTransactionsCsvContent(): String {
        val sb = java.lang.StringBuilder()
        sb.append("Tanggal,Tipe,Nama Pasangan,Jumlah,Pos Kategori,Keterangan,Tag\n")
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        transactions.value.forEach { tx ->
            val dateStr = sdf.format(Date(tx.timestamp))
            val escapedDesc = tx.description.replace("\"", "\"\"")
            val escapedCategory = tx.category.replace("\"", "\"\"")
            sb.append("$dateStr,${tx.type},${tx.user},${tx.amount.toLong()},\"$escapedCategory\",\"$escapedDesc\",${tx.tag}\n")
        }
        return sb.toString()
    }

    // --- REAL-TIME CLOUD AUTO SYNC CONTROLLERS ---
    private var uploadJob: kotlinx.coroutines.Job? = null

    private fun observeAndUploadOnChanges() {
        viewModelScope.launch {
            combine(
                transactions,
                savingGoals,
                cicilanList,
                milestones,
                activities,
                ibadahLogs,
                rapatItems,
                wishlistItems,
                configs
            ) { _ ->
                System.currentTimeMillis()
            }.collect {
                triggerAutoUpload()
                triggerSheetsAutoUpload()
            }
        }
    }

    private fun triggerSheetsAutoUpload() {
        if (isSheetsSyncingNetwork) return
        val sheetsUrl = configs.value["SHEETS_WEBAPP_URL"] ?: ""
        val sheetsEnabled = configs.value["SHEETS_AUTO_ENABLED"] == "true"
        if (sheetsUrl.isBlank() || !sheetsEnabled) {
            _sheetsSyncStatus.value = "Google Sheets Nonaktif"
            return
        }

        sheetsUploadJob?.cancel()
        sheetsUploadJob = viewModelScope.launch {
            kotlinx.coroutines.delay(4000) // Debounce 4 seconds to protect API quotas
            localSheetsAutoUpload()
        }
    }

    suspend fun localSheetsAutoUpload() {
        if (isSheetsSyncingNetwork) return
        val sheetsUrl = configs.value["SHEETS_WEBAPP_URL"] ?: ""
        if (sheetsUrl.isBlank()) return

        val rawDbJson = exportDatabaseAsJson()
        if (rawDbJson.isEmpty()) return

        val currentHash = com.example.api.DuniaSyncClient.hashSyncKey(rawDbJson)
        if (currentHash == lastSheetsUploadedHash) {
            return
        }

        isSheetsSyncingNetwork = true
        _sheetsSyncStatus.value = "Menyinkronkan..."

        try {
            val success = com.example.api.DuniaSyncClient.syncWithGoogleSheets(sheetsUrl, rawDbJson)
            isSheetsSyncingNetwork = false

            if (success) {
                lastSheetsUploadedHash = currentHash
                _sheetsSyncStatus.value = "Tersinkronisasi ✅"
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                _lastSheetsSyncTime.value = "Hari ini ${sdf.format(Date())}"
            } else {
                _sheetsSyncStatus.value = "Gagal Mengunggah ❌"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isSheetsSyncingNetwork = false
            _sheetsSyncStatus.value = "Gangguan Koneksi ❌"
        }
    }

    private fun triggerAutoUpload() {
        if (isSyncingNetwork) return
        val syncKey = configs.value["SYNC_KEY"] ?: ""
        val autoEnabled = configs.value["SYNC_AUTO_ENABLED"] == "true"
        if (syncKey.isBlank() || !autoEnabled) {
            _syncStatus.value = "Auto-Sync Nonaktif"
            return
        }

        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            kotlinx.coroutines.delay(3500) // Debounce 3.5 seconds
            localAutoUpload()
        }
    }

    suspend fun localAutoUpload() {
        if (isSyncingNetwork) return
        val syncKey = configs.value["SYNC_KEY"] ?: ""
        if (syncKey.isBlank()) return

        val rawDbJson = exportDatabaseAsJson()
        if (rawDbJson.isEmpty()) return

        val currentHash = com.example.api.DuniaSyncClient.hashSyncKey(rawDbJson)
        if (currentHash == lastUploadedHash) {
            return
        }

        isSyncingNetwork = true
        _syncStatus.value = "Mengunggah..."

        try {
            val envelope = org.json.JSONObject().apply {
                put("last_updated", System.currentTimeMillis())
                put("sender_id", clientId)
                put("db", org.json.JSONObject(rawDbJson))
            }

            val sheetsUrl = configs.value["SHEETS_WEBAPP_URL"] ?: ""
            val success = if (sheetsUrl.isNotBlank()) {
                com.example.api.DuniaSyncClient.uploadSyncEnvelopeToGAS(sheetsUrl, syncKey, envelope.toString())
            } else {
                com.example.api.DuniaSyncClient.uploadSyncEnvelope(syncKey, envelope.toString())
            }
            isSyncingNetwork = false

            if (success) {
                lastUploadedHash = currentHash
                localLastUpdated = envelope.getLong("last_updated")
                _syncStatus.value = "Tersinkronisasi"
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                _lastSyncTime.value = "Hari ini ${sdf.format(Date())}"
            } else {
                _syncStatus.value = "Gagal mengunggah"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isSyncingNetwork = false
            _syncStatus.value = "Koneksi Terganggu"
        }
    }

    private fun startCloudPolling() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // Poll every 5 seconds for sub-decasecond mirror sync
                val syncKey = configs.value["SYNC_KEY"] ?: ""
                val autoEnabled = configs.value["SYNC_AUTO_ENABLED"] == "true"
                if (syncKey.isNotBlank() && autoEnabled && !isSyncingNetwork) {
                    try {
                        val sheetsUrl = configs.value["SHEETS_WEBAPP_URL"] ?: ""
                        val envelopeStr = if (sheetsUrl.isNotBlank()) {
                            com.example.api.DuniaSyncClient.fetchSyncEnvelopeFromGAS(sheetsUrl, syncKey)
                        } else {
                            com.example.api.DuniaSyncClient.fetchSyncEnvelope(syncKey)
                        }
                        if (!envelopeStr.isNullOrBlank()) {
                            val envelope = org.json.JSONObject(envelopeStr)
                            val senderId = envelope.optString("sender_id")
                            val cloudLastUpdated = envelope.optLong("last_updated", 0L)

                            if (senderId != clientId && cloudLastUpdated > localLastUpdated) {
                                isSyncingNetwork = true
                                _syncStatus.value = "Mengunduh..."

                                val dbObj = envelope.optJSONObject("db")
                                if (dbObj != null) {
                                    importDatabaseFromJson(dbObj.toString(), mergeOnly = false) { success ->
                                        isSyncingNetwork = false
                                        if (success) {
                                            localLastUpdated = cloudLastUpdated
                                            lastUploadedHash = com.example.api.DuniaSyncClient.hashSyncKey(dbObj.toString())
                                            _syncStatus.value = "Tersinkronisasi"
                                            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                            _lastSyncTime.value = "Update received ${sdf.format(Date())}"
                                        } else {
                                            _syncStatus.value = "Gagal sinkron data baru"
                                        }
                                    }
                                } else {
                                    isSyncingNetwork = false
                                }
                            } else {
                                // If already up to date, ensure correct status
                                if (_syncStatus.value == "Mengunduh..." || _syncStatus.value == "Mengunggah...") {
                                    _syncStatus.value = "Tersinkronisasi"
                                } else if (syncKey.isNotBlank() && autoEnabled && _syncStatus.value.startsWith("Terputus")) {
                                    _syncStatus.value = "Tersinkronisasi"
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}

// --- Data classes for state ---

data class MonthlyOverview(
    val haikalIncome: Double = 0.0,
    val ummuIncome: Double = 0.0,
    val jointIncome: Double = 0.0,
    val totalIncome: Double = 0.0,
    val haikalExpense: Double = 0.0,
    val ummuExpense: Double = 0.0,
    val jointExpense: Double = 0.0,
    val totalExpense: Double = 0.0,
    val surplus: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val budgetAlerts: List<BudgetStatus> = emptyList()
)

data class BudgetStatus(
    val posName: String,
    val actual: Double,
    val budget: Double,
    val category: String = "",
    val owner: String = ""
)

data class HealthScore(
    val score: Int = 0,
    val savingRate: Int = 0,
    val debtRatio: Int = 0,
    val overLimitCount: Int = 0,
    val activeRecordingDays: Int = 0
)

data class CountdownInfo(
    val yearsToNikah: Int = 0,
    val dpTargetVal: Double = 0.0,
    val dpSaved: Double = 0.0,
    val dpPercent: Int = 0
)

// --- ViewModel Factory ---

class DuniaViewModelFactory(private val repository: DuniaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DuniaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DuniaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
