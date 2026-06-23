package com.cunffy.launcher.data.home

import com.cunffy.launcher.data.apps.AppCatalog
import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.db.dao.HomeLayoutDao
import com.cunffy.launcher.data.db.entities.FolderItemEntity
import com.cunffy.launcher.data.db.entities.HomeContainer
import com.cunffy.launcher.data.db.entities.HomeItemEntity
import com.cunffy.launcher.data.db.entities.HomeItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** CRUD + resolution for the home-screen grid (apps, folders, widgets) backed by Room. */
@Singleton
class HomeLayoutRepository @Inject constructor(
    private val dao: HomeLayoutDao,
    private val appCatalog: AppCatalog,
) {
    /** Resolved desktop entries (excludes the dock container). */
    val desktop: Flow<List<HomeEntry>> = combine(
        dao.observeItems(),
        dao.observeFolders(),
        dao.observeFolderItems(),
        appCatalog.allApps,
    ) { items, folders, folderItems, apps ->
        val appsByKey = apps.associateBy { it.componentKey }
        val foldersById = folders.associateBy { it.id }
        items.filter { it.container == HomeContainer.DESKTOP }.mapNotNull { item ->
            when (item.type) {
                HomeItemType.APP -> appsByKey[item.componentKey]?.let { HomeEntry.App(item, it) }
                HomeItemType.FOLDER -> {
                    val folder = foldersById[item.folderId] ?: return@mapNotNull null
                    val members = folderItems
                        .filter { it.folderId == folder.id }
                        .sortedBy { it.position }
                        .mapNotNull { appsByKey[it.componentKey] }
                    HomeEntry.Folder(item, folder, members)
                }
                HomeItemType.WIDGET -> HomeEntry.Widget(item)
                else -> null
            }
        }
    }

    suspend fun isEmpty(): Boolean = dao.getItems().none { it.container == HomeContainer.DESKTOP }

    /** Clears all placed items and folders (used by the one-time stale-layout cleanup). */
    suspend fun resetDesktop() {
        dao.clearItems()
        dao.clearFolderItems()
        dao.clearFolders()
    }

    /**
     * Places the given [rows] of apps onto page 0, anchored to the bottom of the grid: the last
     * list lands on the bottom row, the one above it on the next row up, and so on.
     */
    suspend fun seedMainPage(rows: List<List<String>>) {
        rows.forEachIndexed { rowIndex, keys ->
            val cellY = (GRID_ROWS - rows.size + rowIndex).coerceAtLeast(0)
            keys.forEachIndexed { index, key ->
                if (index < GRID_COLUMNS) {
                    dao.insertItem(
                        HomeItemEntity(
                            type = HomeItemType.APP,
                            componentKey = key,
                            cellX = index,
                            cellY = cellY,
                        ),
                    )
                }
            }
        }
    }

    suspend fun addApp(componentKey: String, cellX: Int, cellY: Int): Long =
        dao.insertItem(
            HomeItemEntity(
                type = HomeItemType.APP,
                componentKey = componentKey,
                cellX = cellX,
                cellY = cellY,
            ),
        )

    suspend fun addWidget(
        widgetId: Int,
        spanX: Int,
        spanY: Int,
        cellX: Int,
        cellY: Int,
        page: Int = 0,
    ): Long =
        dao.insertItem(
            HomeItemEntity(
                type = HomeItemType.WIDGET,
                widgetId = widgetId,
                spanX = spanX,
                spanY = spanY,
                cellX = cellX,
                cellY = cellY,
                page = page,
            ),
        )

    suspend fun moveItem(item: HomeItemEntity, cellX: Int, cellY: Int) =
        dao.updateItem(item.copy(cellX = cellX, cellY = cellY))

    suspend fun moveItemToPage(item: HomeItemEntity, page: Int, cellX: Int, cellY: Int) =
        dao.updateItem(item.copy(page = page, cellX = cellX, cellY = cellY))

    suspend fun removeItem(item: HomeItemEntity) {
        if (item.type == HomeItemType.FOLDER) item.folderId?.let { dao.clearFolder(it); dao.deleteFolder(it) }
        dao.deleteItem(item)
    }

    /** Drops [dragged] onto [target], producing a folder containing both apps. */
    suspend fun mergeIntoFolder(dragged: HomeItemEntity, target: HomeItemEntity) {
        val draggedKey = dragged.componentKey ?: return
        val targetKey = target.componentKey ?: return
        val folderId = dao.insertFolder(
            com.cunffy.launcher.data.db.entities.FolderEntity(title = "Folder"),
        )
        dao.insertFolderItem(FolderItemEntity(folderId, targetKey, 0))
        dao.insertFolderItem(FolderItemEntity(folderId, draggedKey, 1))
        // The target cell becomes the folder; remove both original app shortcuts.
        dao.deleteItem(dragged)
        dao.deleteItem(target)
        dao.insertItem(
            HomeItemEntity(
                type = HomeItemType.FOLDER,
                folderId = folderId,
                cellX = target.cellX,
                cellY = target.cellY,
            ),
        )
    }

    suspend fun renameFolder(folderId: Long, title: String) {
        dao.getFolders().firstOrNull { it.id == folderId }?.let {
            dao.updateFolder(it.copy(title = title))
        }
    }

    suspend fun seedDefaultIfEmpty() {
        if (!isEmpty()) return
        val apps = appCatalog.visibleApps.value.take(8)
        apps.forEachIndexed { index, app ->
            dao.insertItem(
                HomeItemEntity(
                    type = HomeItemType.APP,
                    componentKey = app.componentKey,
                    cellX = index % GRID_COLUMNS,
                    cellY = index / GRID_COLUMNS,
                ),
            )
        }
    }

    companion object {
        const val GRID_COLUMNS = 4
        const val GRID_ROWS = 5
    }
}
