package com.cunffy.launcher.data.customization

import com.cunffy.launcher.data.db.dao.CustomizationDao
import com.cunffy.launcher.data.db.entities.AppCustomizationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Read/write access to per-app overrides, keyed by flattened component name. */
@Singleton
class CustomizationRepository @Inject constructor(
    private val dao: CustomizationDao,
) {
    val customizations: Flow<Map<String, AppCustomizationEntity>> =
        dao.observeAll().map { list -> list.associateBy { it.componentKey } }

    private suspend fun edit(key: String, transform: (AppCustomizationEntity) -> AppCustomizationEntity) {
        val current = dao.get(key) ?: AppCustomizationEntity(componentKey = key)
        val updated = transform(current)
        // Drop the row entirely once it carries no overrides, keeping the table tidy.
        if (updated.isEmpty()) dao.delete(key) else dao.upsert(updated)
    }

    suspend fun setLabel(key: String, label: String?) =
        edit(key) { it.copy(customLabel = label?.ifBlank { null }) }

    suspend fun setHidden(key: String, hidden: Boolean) =
        edit(key) { it.copy(hidden = hidden) }

    suspend fun setLocked(key: String, locked: Boolean) =
        edit(key) { it.copy(locked = locked) }

    suspend fun setCategoryOverride(key: String, category: String?) =
        edit(key) { it.copy(categoryOverride = category) }

    suspend fun setIconPack(key: String, packPackage: String?) =
        edit(key) { it.copy(iconPackPackage = packPackage) }

    private fun AppCustomizationEntity.isEmpty(): Boolean =
        customLabel == null && iconPackPackage == null && !hidden && !locked &&
            categoryOverride == null
}
