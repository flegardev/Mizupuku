package app.hydronudge.data

import app.hydronudge.data.local.*
import app.hydronudge.domain.*

class DrinkRepository(private val dao: DrinkDao, private val onChanged: suspend () -> Unit = {}) {
    val entries = dao.observeAll()
    val favorites = dao.observeFavorites()

    suspend fun add(entry: DrinkEntry): Boolean {
        require(entry.isWater()) { "New entries must be water." }
        validate(entry)
        val inserted = dao.insert(entry) != -1L
        if (inserted) onChanged()
        return inserted
    }

    suspend fun addBeverage(entry: DrinkEntry, allowBeverageCategories: Boolean): Boolean {
        if (!allowBeverageCategories) require(entry.isWater()) { "New entries must be water." }
        validate(entry)
        val inserted = dao.insert(entry) != -1L
        if (inserted) onChanged()
        return inserted
    }

    suspend fun edit(entry: DrinkEntry) {
        require(entry.isWater())
        validate(entry)
        check(dao.update(entry) == 1) { "This drink no longer exists." }
        onChanged()
    }

    suspend fun editBeverage(entry: DrinkEntry, allowBeverageCategories: Boolean) {
        if (!allowBeverageCategories) require(entry.isWater())
        validate(entry)
        check(dao.update(entry) == 1) { "This drink no longer exists." }
        onChanged()
    }

    suspend fun delete(id: String) {
        dao.delete(id)
        onChanged()
    }

    suspend fun deleteRange(start: Long, endExclusive: Long) {
        dao.deleteBetween(start, endExclusive)
        onChanged()
    }

    suspend fun restore(entry: DrinkEntry): Boolean {
        validate(entry)
        val inserted = dao.insert(entry) != -1L
        if (inserted) onChanged()
        return inserted
    }

    suspend fun restoreMany(entries: List<DrinkEntry>): Int {
        entries.forEach { validate(it) }
        val count = dao.insertMany(entries).count { it != -1L }
        if (count > 0) onChanged()
        return count
    }

    suspend fun all() = dao.all()

    suspend fun deleteAll() {
        dao.deleteAll()
        onChanged()
    }

    suspend fun saveFavorite(favorite: FavoriteDrink) {
        validateFavorite(favorite)
        dao.saveFavorite(favorite)
        onChanged()
    }

    suspend fun deleteFavorite(id: String) {
        dao.deleteFavorite(id)
        onChanged()
    }

    suspend fun snapshot() = dao.snapshot()

    suspend fun clearAll() {
        dao.clearAll()
        onChanged()
    }

    suspend fun merge(backup: DrinkBackup): Int {
        backup.entries.forEach { validate(it) }
        backup.favorites.forEach { validateFavorite(it) }
        val count = dao.merge(backup.entries, backup.favorites)
        onChanged()
        return count
    }

    private fun validateFavorite(favorite: FavoriteDrink) {
        validateVolume(favorite.amountMl)
        require(favorite.name.isNotBlank() && favorite.name.length <= 80)
        require(favorite.id.isNotBlank() && favorite.id.length <= 300)
        require(favorite.category.isNotBlank() && favorite.category.length <= 80)
    }

    private fun validate(entry: DrinkEntry) {
        validateVolume(entry.amountMl)
        require(entry.id.isNotBlank() && entry.id.length <= 300) { "Invalid entry ID." }
        require(entry.timestamp >= 0 && entry.timestamp <= System.currentTimeMillis() + 60000) { "Invalid drink timestamp." }
        require(entry.name.isNotBlank() && entry.name.length <= 80) { "Use a drink name of 1–80 characters." }
        require(entry.note.length <= 1000) { "Notes may contain up to 1,000 characters." }
        require(entry.category.isNotBlank() && entry.category.length <= 80)
        VolumeUnit.valueOf(entry.enteredUnit)
    }
}
