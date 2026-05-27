package com.example

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules")
    fun getAllRules(): Flow<List<ReplyRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ReplyRule)

    @Update
    suspend fun updateRule(rule: ReplyRule)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteRuleById(id: String)
}
