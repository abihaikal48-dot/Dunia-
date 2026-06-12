package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DuniaRepository(private val dao: DuniaDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allSavingGoals: Flow<List<SavingGoalEntity>> = dao.getAllSavingGoals()
    val allCicilan: Flow<List<CicilanEntity>> = dao.getAllCicilan()
    val allMilestones: Flow<List<MilestoneEntity>> = dao.getAllMilestones()
    val allActivities: Flow<List<ActivityEntity>> = dao.getAllActivities()
    val allIbadahLogs: Flow<List<IbadahEntity>> = dao.getAllIbadahLogs()
    val allRapat: Flow<List<RapatEntity>> = dao.getAllRapat()
    val allWishlist: Flow<List<WishlistEntity>> = dao.getAllWishlist()
    val allConfigs: Flow<List<ConfigEntity>> = dao.getAllConfigs()
    val allEvents: Flow<List<EventEntity>> = dao.getAllEvents()

    // Key-Value config helper map
    val configMap: Flow<Map<String, String>> = dao.getAllConfigs().map { list ->
        list.associate { it.key to it.value }
    }

    suspend fun getConfigValue(key: String): String? {
        return dao.getConfigByKey(key)?.value
    }

    suspend fun saveConfig(key: String, value: String) {
        dao.insertConfig(ConfigEntity(key, value))
    }

    suspend fun deleteConfig(key: String) {
        dao.deleteConfigByKey(key)
    }

    suspend fun insertTransaction(transaction: TransactionEntity) = dao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) = dao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = dao.deleteTransaction(transaction)

    suspend fun insertSavingGoal(goal: SavingGoalEntity) = dao.insertSavingGoal(goal)
    suspend fun updateSavingGoal(goal: SavingGoalEntity) = dao.updateSavingGoal(goal)
    suspend fun deleteSavingGoal(goal: SavingGoalEntity) = dao.deleteSavingGoal(goal)

    suspend fun insertCicilan(cicilan: CicilanEntity) = dao.insertCicilan(cicilan)
    suspend fun updateCicilan(cicilan: CicilanEntity) = dao.updateCicilan(cicilan)
    suspend fun deleteCicilan(cicilan: CicilanEntity) = dao.deleteCicilan(cicilan)

    suspend fun insertMilestone(milestone: MilestoneEntity) = dao.insertMilestone(milestone)
    suspend fun updateMilestone(milestone: MilestoneEntity) = dao.updateMilestone(milestone)
    suspend fun deleteMilestone(milestone: MilestoneEntity) = dao.deleteMilestone(milestone)

    suspend fun insertActivity(activity: ActivityEntity) = dao.insertActivity(activity)
    suspend fun deleteActivity(activity: ActivityEntity) = dao.deleteActivity(activity)

    suspend fun getIbadahByDate(dateStr: String): IbadahEntity? = dao.getIbadahByDate(dateStr)
    suspend fun insertIbadah(ibadah: IbadahEntity) = dao.insertIbadah(ibadah)

    suspend fun insertRapat(rapat: RapatEntity) = dao.insertRapat(rapat)

    suspend fun insertWishlist(wishlist: WishlistEntity) = dao.insertWishlist(wishlist)
    suspend fun updateWishlist(wishlist: WishlistEntity) = dao.updateWishlist(wishlist)
    suspend fun deleteWishlist(wishlist: WishlistEntity) = dao.deleteWishlist(wishlist)

    suspend fun insertEvent(event: EventEntity) = dao.insertEvent(event)
    suspend fun updateEvent(event: EventEntity) = dao.updateEvent(event)
    suspend fun deleteEvent(event: EventEntity) = dao.deleteEvent(event)

    suspend fun clearAllData() {
        dao.clearTransactions()
        dao.clearSavingGoals()
        dao.clearCicilan()
        dao.clearMilestones()
        dao.clearActivities()
        dao.clearIbadah()
        dao.clearRapat()
        dao.clearWishlist()
        dao.clearConfigs()
        dao.clearEvents()
    }

    suspend fun insertTransactions(list: List<TransactionEntity>) {
        list.forEach { dao.insertTransaction(it) }
    }
    suspend fun insertSavingGoals(list: List<SavingGoalEntity>) {
        list.forEach { dao.insertSavingGoal(it) }
    }
    suspend fun insertCicilanList(list: List<CicilanEntity>) {
        list.forEach { dao.insertCicilan(it) }
    }
    suspend fun insertMilestones(list: List<MilestoneEntity>) {
        list.forEach { dao.insertMilestone(it) }
    }
    suspend fun insertActivities(list: List<ActivityEntity>) {
        list.forEach { dao.insertActivity(it) }
    }
    suspend fun insertIbadahLogs(list: List<IbadahEntity>) {
        list.forEach { dao.insertIbadah(it) }
    }
    suspend fun insertRapatList(list: List<RapatEntity>) {
        list.forEach { dao.insertRapat(it) }
    }
    suspend fun insertWishlistItems(list: List<WishlistEntity>) {
        list.forEach { dao.insertWishlist(it) }
    }
    suspend fun insertEventsList(list: List<EventEntity>) {
        list.forEach { dao.insertEvent(it) }
    }
    suspend fun insertConfigs(list: List<ConfigEntity>) {
        dao.insertConfigs(list)
    }
}
