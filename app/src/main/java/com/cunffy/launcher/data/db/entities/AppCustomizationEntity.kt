package com.cunffy.launcher.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Per-app user overrides. Keyed by the flattened component name so it survives reboots.
 * A row exists only for apps the user has actually customized.
 */
@Serializable
@Entity(tableName = "app_customization")
data class AppCustomizationEntity(
    @PrimaryKey val componentKey: String,
    val customLabel: String? = null,
    /** Package of a selected icon pack to source this app's icon from, or null for default. */
    val iconPackPackage: String? = null,
    val hidden: Boolean = false,
    val locked: Boolean = false,
    /** Overrides the auto-assigned [com.cunffy.launcher.data.apps.AppCategory] name, or null. */
    val categoryOverride: String? = null,
)
