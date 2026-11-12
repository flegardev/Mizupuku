package app.hydronudge.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "drinks", indices = [Index("timestamp")])
data class DrinkEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val enteredUnit: String = "ML"
)

@Entity(tableName = "favorites")
data class FavoriteDrink(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val amountMl: Int
)

@Dao
interface DrinkDao {
    @Query("SELECT * FROM drinks ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<DrinkEntry>>

    @Query("SELECT * FROM drinks ORDER BY timestamp DESC, id DESC")
    suspend fun all(): List<DrinkEntry>

    @Query("SELECT * FROM drinks WHERE timestamp >= :start AND timestamp < :endExclusive ORDER BY timestamp DESC, id DESC")
    suspend fun entriesBetween(start: Long, endExclusive: Long): List<DrinkEntry>

    // IGNORE is deliberate: action IDs are primary keys, so a redelivered action is idempotent.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: DrinkEntry): Long

    @Update
    suspend fun update(entry: DrinkEntry): Int

    @Query("DELETE FROM drinks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM drinks WHERE timestamp >= :start AND timestamp < :endExclusive")
    suspend fun deleteBetween(start: Long, endExclusive: Long)

    @Query("DELETE FROM drinks")
    suspend fun deleteAll()

    @Query("SELECT * FROM favorites ORDER BY name COLLATE NOCASE")
    fun observeFavorites(): Flow<List<FavoriteDrink>>

    @Upsert
    suspend fun saveFavorite(favorite: FavoriteDrink)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavorite(id: String)

    @Query("DELETE FROM favorites")
    suspend fun deleteAllFavorites()

    @Transaction
    suspend fun clearAll() {
        deleteAll()
        deleteAllFavorites()
    }

    @Query("SELECT * FROM favorites ORDER BY name COLLATE NOCASE")
    suspend fun allFavorites(): List<FavoriteDrink>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(entries: List<DrinkEntry>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavorites(favorites: List<FavoriteDrink>)

    @Transaction
    suspend fun merge(entries: List<DrinkEntry>, favorites: List<FavoriteDrink>): Int {
        val inserted = insertMany(entries).count { it != -1L }
        insertFavorites(favorites)
        return inserted
    }

    @Transaction
    suspend fun snapshot(): DrinkBackup = DrinkBackup(all(), allFavorites())
}

data class DrinkBackup(val entries: List<DrinkEntry>, val favorites: List<FavoriteDrink>)

@Database(entities = [DrinkEntry::class, FavoriteDrink::class], version = 1, exportSchema = true)
abstract class HydroDatabase : RoomDatabase() {
    abstract fun drinks(): DrinkDao
}
