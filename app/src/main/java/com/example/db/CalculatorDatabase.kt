package com.example.db

import androidx.room.*

@Entity(tableName = "variables")
data class VariableEntity(
    @PrimaryKey val name: String,
    val value: Double
)

@Entity(tableName = "calculation_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val input: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CalculatorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVariable(variable: VariableEntity)

    @Query("SELECT * FROM variables")
    suspend fun getAllVariables(): List<VariableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryEntity)

    @Query("SELECT * FROM calculation_history ORDER BY id DESC")
    suspend fun getHistory(): List<HistoryEntity>

    @Query("DELETE FROM calculation_history")
    suspend fun clearHistory()
}

@Database(entities = [VariableEntity::class, HistoryEntity::class], version = 1, exportSchema = false)
abstract class CalculatorDatabase : RoomDatabase() {
    abstract fun dao(): CalculatorDao
}
