package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// ==========================================
// 1. ENTITIES
// ==========================================

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val type: String, // "PEMASUKAN" or "PENGELUARAN"
    val user: String, // "HAIKAL" or "UMMU" or "BERDUA"
    val amount: Double,
    val category: String,
    val description: String,
    val tag: String, // e.g., "#darurat", "#rutin", "#impulsif"
    val imageUri: String? = null
)

@Entity(tableName = "saving_goals")
data class SavingGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val owner: String // "HAIKAL", "UMMU", "BERDUA"
)

@Entity(tableName = "cicilan")
data class CicilanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val totalValue: Double,
    val monthlyPayment: Double,
    val remainingMonths: Int,
    val paidMonths: Int,
    val dueDateDay: Int,
    val owner: String // "HAIKAL" or "UMMU"
)

@Entity(tableName = "milestones")
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fase: Int, // 0 to 4
    val title: String,
    val description: String,
    val targetDate: String,
    val checked: Boolean
)

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startTime: String, // e.g. "04:30"
    val endTime: String,   // e.g. "05:00"
    val owner: String,     // "HAIKAL" or "UMMU"
    val savesMoney: Boolean,
    val category: String = "Lainnya"
)

@Entity(tableName = "ibadah_logs")
data class IbadahEntity(
    @PrimaryKey val dateStr: String, // "YYYY-MM-DD"
    val subuh: Boolean = false,
    val dzuhur: Boolean = false,
    val ashar: Boolean = false,
    val maghrib: Boolean = false,
    val isya: Boolean = false,
    val ngajiHalaman: Int = 0,
    val sedekahAmount: Double = 0.0
)

@Entity(tableName = "rapat_items")
data class RapatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateStr: String, // "June 2026", "July 2026", etc.
    val agenda: String,
    val minutes: String,
    val decisions: String,
    val actionItems: String
)

@Entity(tableName = "wishlist_items")
data class WishlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val estimatedPrice: Double,
    val category: String, // "Kebutuhan", "Keinginan", "Investasi", "Pengalaman"
    val priority: Int, // 1 to 5
    val owner: String, // "HAIKAL", "UMMU", "BERDUA"
    val addedDate: Long,
    val status: String // "PENDING", "DISETUJUI", "DIBELI", "DIBATALKAN"
)

@Entity(tableName = "sys_config")
data class ConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateStr: String, // YYYY-MM-DD
    val timeStr: String, // HH:MM
    val type: String, // "KENCAN", "PERGI/LIBURAN", "RAPAT", "HARI_PENTING", "LAINNYA"
    val notes: String,
    val owner: String, // "HAIKAL", "UMMU", "BERDUA"
    val reminded: Boolean = false
)

// ==========================================
// 2. DAOS
// ==========================================

@Dao
interface DuniaDao {
    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // Saving Goals
    @Query("SELECT * FROM saving_goals")
    fun getAllSavingGoals(): Flow<List<SavingGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingGoal(goal: SavingGoalEntity)

    @Update
    suspend fun updateSavingGoal(goal: SavingGoalEntity)

    @Delete
    suspend fun deleteSavingGoal(goal: SavingGoalEntity)

    // Cicilan
    @Query("SELECT * FROM cicilan")
    fun getAllCicilan(): Flow<List<CicilanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCicilan(cicilan: CicilanEntity)

    @Update
    suspend fun updateCicilan(cicilan: CicilanEntity)

    @Delete
    suspend fun deleteCicilan(cicilan: CicilanEntity)

    // Milestones
    @Query("SELECT * FROM milestones ORDER BY fase ASC, id ASC")
    fun getAllMilestones(): Flow<List<MilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: MilestoneEntity)

    @Update
    suspend fun updateMilestone(milestone: MilestoneEntity)

    @Delete
    suspend fun deleteMilestone(milestone: MilestoneEntity)

    // Activities
    @Query("SELECT * FROM activities ORDER BY startTime ASC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Delete
    suspend fun deleteActivity(activity: ActivityEntity)

    // Ibadah Logs
    @Query("SELECT * FROM ibadah_logs WHERE dateStr = :dateStr")
    suspend fun getIbadahByDate(dateStr: String): IbadahEntity?

    @Query("SELECT * FROM ibadah_logs ORDER BY dateStr DESC")
    fun getAllIbadahLogs(): Flow<List<IbadahEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIbadah(ibadah: IbadahEntity)

    // Rapat Items
    @Query("SELECT * FROM rapat_items ORDER BY id DESC")
    fun getAllRapat(): Flow<List<RapatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRapat(rapat: RapatEntity)

    // Wishlist Items
    @Query("SELECT * FROM wishlist_items ORDER BY addedDate DESC")
    fun getAllWishlist(): Flow<List<WishlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlist(wishlist: WishlistEntity)

    @Update
    suspend fun updateWishlist(wishlist: WishlistEntity)

    @Delete
    suspend fun deleteWishlist(wishlist: WishlistEntity)

    // Config
    @Query("SELECT * FROM sys_config")
    fun getAllConfigs(): Flow<List<ConfigEntity>>

    @Query("SELECT * FROM sys_config WHERE `key` = :key LIMIT 1")
    suspend fun getConfigByKey(key: String): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigs(configs: List<ConfigEntity>)

    @Query("DELETE FROM sys_config WHERE `key` = :key")
    suspend fun deleteConfigByKey(key: String)

    // Events
    @Query("SELECT * FROM events ORDER BY dateStr ASC, timeStr ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM saving_goals")
    suspend fun clearSavingGoals()

    @Query("DELETE FROM cicilan")
    suspend fun clearCicilan()

    @Query("DELETE FROM milestones")
    suspend fun clearMilestones()

    @Query("DELETE FROM activities")
    suspend fun clearActivities()

    @Query("DELETE FROM ibadah_logs")
    suspend fun clearIbadah()

    @Query("DELETE FROM rapat_items")
    suspend fun clearRapat()

    @Query("DELETE FROM wishlist_items")
    suspend fun clearWishlist()

    @Query("DELETE FROM sys_config")
    suspend fun clearConfigs()

    @Query("DELETE FROM events")
    suspend fun clearEvents()
}

// ==========================================
// 3. DATABASE
// ==========================================

@Database(
    entities = [
        TransactionEntity::class,
        SavingGoalEntity::class,
        CicilanEntity::class,
        MilestoneEntity::class,
        ActivityEntity::class,
        IbadahEntity::class,
        RapatEntity::class,
        WishlistEntity::class,
        ConfigEntity::class,
        EventEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class DuniaDatabase : RoomDatabase() {
    abstract fun duniaDao(): DuniaDao

    companion object {
        @Volatile
        private var INSTANCE: DuniaDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): DuniaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DuniaDatabase::class.java,
                    "dunia_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DuniaDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DuniaDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.duniaDao())
                }
            }
        }

        suspend fun populateInitialData(dao: DuniaDao) {
            // Seed Config Entities
            val initialConfigs = listOf(
                ConfigEntity("NAMA_HAIKAL", "Haikal"),
                ConfigEntity("NAMA_UMMU", "Ummu"),
                ConfigEntity("GAJI_HAIKAL", "2300000"),
                ConfigEntity("GAJI_UMMU", "1500000"),
                ConfigEntity("EMAIL_HAIKAL", "abihaikal48@gmail.com"),
                ConfigEntity("EMAIL_UMMU", "ummu@gmail.com"),
                ConfigEntity("TARGET_NIKAH", "2029"),
                ConfigEntity("TARGET_RUMAH_HARGA", "400000000"),
                ConfigEntity("TARGET_DP_PERSEN", "10"), // 40jt
                ConfigEntity("TANGGAL_RAPAT", "15"),
                ConfigEntity("BUDGET_MAKAN_HAIKAL", "450000"),
                ConfigEntity("BUDGET_TRANSPORT", "150000"),
                ConfigEntity("BUDGET_KENCAN", "200000"),
                ConfigEntity("DARURAT_TARGET_BULAN", "3"),
                ConfigEntity("INVESTASI_PERSEN", "10"),
                ConfigEntity("TABUNGAN_PERSEN", "15"),
                ConfigEntity("MULAI_SISTEM", "10/06/2026"),
                ConfigEntity("FASE_SAAT_INI", "0"),
                ConfigEntity("PEKERJAAN_HAIKAL", "Trainer di Hara Chicken Niten"),
                ConfigEntity("PEKERJAAN_UMMU", "Freelance Graphic Designer"),
                ConfigEntity("HAFIZ_NOTE", "Mulai pacaran 10 Juni 2026 - Rencana nikah 2029/2030")
            )
            dao.insertConfigs(initialConfigs)

            // Seed Saving Goals
            val savingGoals = listOf(
                SavingGoalEntity(0, "Dana Darurat Haikal (Min 3x Gaji)", 4500000.0, 1500000.0, "HAIKAL"),
                SavingGoalEntity(0, "Dana Darurat Ummu", 3000000.0, 600000.0, "UMMU"),
                SavingGoalEntity(0, "Tabungan Pernikahan Bersama", 25000000.0, 1200000.0, "BERDUA"),
                SavingGoalEntity(0, "Tabungan DP Rumah", 40000000.0, 0.0, "BERDUA")
            )
            savingGoals.forEach { dao.insertSavingGoal(it) }

            // Seed Cicilan
            val cicilanItems = listOf(
                CicilanEntity(0, "Cicilan Motor Scoopy (30 bln sisa)", 16500000.0, 550000.0, 30, 6, 5, "HAIKAL"),
                CicilanEntity(0, "Arisan Jaket (3 bln sisa)", 450000.0, 150000.0, 3, 0, 15, "HAIKAL")
            )
            cicilanItems.forEach { dao.insertCicilan(it) }

            // Seed Roadmap Milestones
            val milestones = listOf(
                // FASE 0 (2026)
                MilestoneEntity(0, 0, "Cicilan Jaket Lunas", "Lunasi arisan jaket tepat waktu pada September 2026.", "September 2026", false),
                MilestoneEntity(0, 0, "Dana Darurat Haikal Rp 1.5jt", "Membangun bantalan finansial awal Haikal.", "Agustus 2026", true),
                MilestoneEntity(0, 0, "Review Keuangan Harian", "Mulai membiasakan catat pengeluaran tiap jam 21:00.", "Juni 2026", false),
                MilestoneEntity(0, 0, "Rapat Bulanan Pertama", "Diskusi evaluasi visi & habit bersama via Google Meet/Ketemu.", "15 Juni 2026", true),
                MilestoneEntity(0, 0, "Baca 1 Buku Keuangan", "Membaca buku 'The Psychology of Money' bersama.", "Desember 2026", false),
                MilestoneEntity(0, 0, "Mulai Kebiasaan Masak Sendiri", "Membawa bekal makanan ke tempat kerja untuk memangkas budget jajan.", "Juli 2026", false),
                
                // FASE 1 (2027)
                MilestoneEntity(0, 1, "Dana Darurat Penuh", "Dana darurat Haikal & Ummu terisi penuh sesuai target.", "Juni 2027", false),
                MilestoneEntity(0, 1, "Tabungan Rumah Dimulai", "Mulai menyisihkan budget rutin ke tabungan DP rumah.", "Januari 2027", false),
                MilestoneEntity(0, 1, "Peningkatan Skill Karir", "Haikal naik jabatan trainer utama atau Ummu dapat client baru.", "September 2027", false),
                
                // FASE 2 (2028)
                MilestoneEntity(0, 2, "Tabungan DP Rumah Rp 20jt+", "Terkumpul 50% dari total minimal DP rumah impian.", "Desember 2028", false),
                MilestoneEntity(0, 2, "Diskusi Lamaran Formal", "Komunikasi formal kepada keluarga kedua belah pihak.", "Juni 2028", false),
                
                // FASE 3 (2029)
                MilestoneEntity(0, 3, "KPR Approved / DP Rumah Lunas", "DP rumah terkumpul penuh dan siap akad / pembayaran.", "Juni 2029", false),
                MilestoneEntity(0, 3, "MENIKAH (Target Utama) 💍", "Menikah dengan aman, tenang, berkah dan financially healthy.", "Desember 2029", false)
            )
            milestones.forEach { dao.insertMilestone(it) }

            // Seed Activities (Harian)
            val activities = listOf(
                // ---- HAIKAL ACTIVITIES ----
                ActivityEntity(0, "Bangun Pagi & Qiyamul Lail", "04:00", "04:30", "HAIKAL", false, "Ibadah & Spiritual"),
                ActivityEntity(0, "Shalat Subuh Jamaah di Masjid", "04:30", "05:00", "HAIKAL", false, "Ibadah & Spiritual"),
                ActivityEntity(0, "Olahraga Ringan / Peregangan Mandiri", "05:00", "05:30", "HAIKAL", true, "Kesehatan & Olahraga"),
                ActivityEntity(0, "Mandi Pagi & Bersiap Kerja", "05:30", "06:00", "HAIKAL", false, "Domestik & Kebersihan"),
                ActivityEntity(0, "Menyiapkan Bekal Makan Siang Sehat", "06:00", "06:45", "HAIKAL", true, "Domestik & Kebersihan"),
                ActivityEntity(0, "Perjalanan Hemat & Mulai Kerja Hara Chicken", "07:00", "16:00", "HAIKAL", false, "Produktif & Karir"),
                ActivityEntity(0, "Evaluasi Keuangan Mini & Istirahat Ashar", "16:15", "16:45", "HAIKAL", false, "Ibadah & Spiritual"),
                ActivityEntity(0, "Shalat Maghrib, Isya & Tadarus Mandiri", "18:00", "19:30", "HAIKAL", false, "Ibadah & Spiritual"),
                ActivityEntity(0, "Belajar Keterampilan Manajemen & Bisnis baru", "19:30", "20:30", "HAIKAL", true, "Produktif & Karir"),
                ActivityEntity(0, "Sinergi Evaluasi & Ngobrol Sore Bersama Ummu", "20:30", "21:00", "HAIKAL", true, "Sinergi & Komunikasi"),
                ActivityEntity(0, "Review Anggaran Finansial DUNIA via App", "21:00", "21:15", "HAIKAL", true, "Sinergi & Komunikasi"),
                ActivityEntity(0, "Tidur Malam Teratur & Berkualitas", "21:30", "04:00", "HAIKAL", false, "Hiburan & Lainnya"),

                // ---- UMMU ACTIVITIES ----
                ActivityEntity(0, "Dzikir Pagi & Shalat Subuh Khusyuk", "04:15", "05:00", "UMMU", false, "Ibadah & Spiritual"),
                ActivityEntity(0, "Yoga Ringan / Olahraga Peregangan Rumah", "05:00", "05:30", "UMMU", true, "Kesehatan & Olahraga"),
                ActivityEntity(0, "Berbenah Rumah & Cuci Piring", "05:30", "06:15", "UMMU", false, "Domestik & Kebersihan"),
                ActivityEntity(0, "Membuat Sarapan Sehat Homemade", "06:15", "07:00", "UMMU", true, "Domestik & Kebersihan"),
                ActivityEntity(0, "Eksplorasi Ide Ilustrasi & Sketsa Klien", "07:30", "09:00", "UMMU", false, "Produktif & Karir"),
                ActivityEntity(0, "Eksekusi Desain Grafis (Proyek Freelance)", "09:00", "12:00", "UMMU", false, "Produktif & Karir"),
                ActivityEntity(0, "Shalat Dzuhur Jamaah Ummahat & Istirahat", "12:00", "13:30", "UMMU", false, "Ibadah & Spiritual"),
                ActivityEntity(0, "Belajar & Mengasah Skill UI/UX & Desain Figma", "13:30", "15:30", "UMMU", true, "Produktif & Karir"),
                ActivityEntity(0, "Shalat Ashar & Jalan Kaki Sore Santai", "15:30", "16:30", "UMMU", true, "Kesehatan & Olahraga"),
                ActivityEntity(0, "Membaca Buku Self-Development Finansial", "19:00", "20:00", "UMMU", false, "Produktif & Karir"),
                ActivityEntity(0, "Sharing Partner & Masa Depan bersama Haikal", "20:30", "21:00", "UMMU", true, "Sinergi & Komunikasi"),
                ActivityEntity(0, "Evaluasi Produktivitas Harian & Istirahat", "21:30", "04:15", "UMMU", false, "Hiburan & Lainnya")
            )
            activities.forEach { dao.insertActivity(it) }

            // Seed Wishlist
            val wishlist = listOf(
                WishlistEntity(0, "Laptop Kerja Ummu (Upgrade SSD)", 800000.0, "Kebutuhan", 5, "UMMU", System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 5, "MENUNGGU"),
                WishlistEntity(0, "Buku Self Impovement Finansial", 120000.0, "Kebutuhan", 4, "HAIKAL", System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2, "DISETUJUI"),
                WishlistEntity(0, "Helm Motor Baru (KBR)", 350000.0, "Keinginan", 2, "HAIKAL", System.currentTimeMillis(), "MENUNGGU")
            )
            wishlist.forEach { dao.insertWishlist(it) }

            // Seed some default Transactions
            val transactions = listOf(
                TransactionEntity(0, System.currentTimeMillis() - 3 * 24 * 3600 * 1000L, "PEMASUKAN", "HAIKAL", 2300000.0, "Gaji Pokok", "Gaji Trainer Hara Chicken", "#rutin"),
                TransactionEntity(0, System.currentTimeMillis() - 3 * 24 * 3600 * 1000L, "PENGELUARAN", "HAIKAL", 550000.0, "Cicilan", "Cicilan Motor Bulanan", "#rutin"),
                TransactionEntity(0, System.currentTimeMillis() - 2 * 24 * 3600 * 1000L, "PENGELUARAN", "HAIKAL", 15000.0, "Makan & Jajan", "Makan Siang Hara", "#masak-sendiri"),
                TransactionEntity(0, System.currentTimeMillis() - 1 * 24 * 3600 * 1000L, "PEMASUKAN", "UMMU", 500000.0, "Freelance", "Logo design client Yg", "#freelance"),
                TransactionEntity(0, System.currentTimeMillis() - 12 * 3600 * 1000L, "PENGELUARAN", "BERDUA", 50000.0, "Kencan", "Es Kopi & Cemilan di Bantul", "#kencan-hemat")
            )
            transactions.forEach { dao.insertTransaction(it) }

            // Seed Ibadah Logs (for past few days)
            dao.insertIbadah(IbadahEntity("2026-06-08", true, true, true, true, true, 2, 5000.0))
            dao.insertIbadah(IbadahEntity("2026-06-09", true, true, true, true, true, 3, 2000.0))
            dao.insertIbadah(IbadahEntity("2026-06-10", true, false, false, false, false, 0, 0.0))

            // Seed Rapat
            val rapat = RapatEntity(
                0,
                "Juni 2026",
                "1. Penyelerasan visi pernikahan 2029/2030\n2. Strategi menabung DP Rumah\n3. Pengendalian jajan makan Haikal.",
                "Haikal dan Ummu sepakat untuk menjaga pengeluaran. Haikal mengaktifkan tabungan bekal makan.",
                "1. DP rumah minimal Rp 40jt (10% dari bursa rumah jogja 400jt).\n2. Jajan di luar weekend saja maksimal 50rb per orang.",
                "1. Membawa bekal setiap hari (Haikal)\n2. Mencari tambahan koki sambilan / freelance logo (Ummu)"
            )
            dao.insertRapat(rapat)

            // Seed Events
            val events = listOf(
                EventEntity(0, "Makan Malam Romantis Anniversary #1", "2026-06-20", "19:00", "KENCAN", "Anniversary pacaran 10 hari / 1 bulan pertama. Budget hemat sesuai plan.", "BERDUA"),
                EventEntity(0, "Rapat Finansial Sinergi Bulanan", "2026-06-15", "20:00", "RAPAT", "Evaluasi target tabungan nikah & pos pengeluaran.", "BERDUA"),
                EventEntity(0, "Piknik Akhir Pekan ke Pantai Parangtritis", "2026-06-28", "15:30", "PERGI/LIBURAN", "Refreshing hemat bawa nasi kotak sendiri dari rumah.", "BERDUA"),
                EventEntity(0, "Ummu Sketsa Deadline Review", "2026-06-12", "10:00", "LAINNYA", "Review portfolio brief client bareng-bareng.", "UMMU")
            )
            events.forEach { dao.insertEvent(it) }
        }
    }
}
