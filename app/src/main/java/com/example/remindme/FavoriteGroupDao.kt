package com.example.remindme

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteGroupDao {
    @Query("SELECT * FROM favorite_groups ORDER BY lastAccessed DESC")
    fun getAllFavorites(): Flow<List<FavoriteGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(group: FavoriteGroup)

    @Delete
    suspend fun deleteFavorite(group: FavoriteGroup)

    @Query("DELETE FROM favorite_groups WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)
}
