package com.cunffy.launcher.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.apps.AppCategory
import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.apps.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
) : ViewModel() {

    private val allApps = appRepository.apps

    private val _selectedCategory = MutableStateFlow(AppCategory.ALL)
    val selectedCategory = _selectedCategory.asStateFlow()

    /** Categories with at least one app, in sidebar order, always led by [AppCategory.ALL]. */
    val categories = allApps
        .map { apps ->
            val present = apps.mapTo(mutableSetOf()) { it.category }
            AppCategory.sidebarOrder.filter { it == AppCategory.ALL || it in present }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf(AppCategory.ALL))

    val visibleApps = combine(allApps, _selectedCategory) { apps, category ->
        if (category == AppCategory.ALL) apps else apps.filter { it.category == category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectCategory(category: AppCategory) {
        _selectedCategory.value = category
    }

    fun launch(app: AppInfo) = appRepository.launch(app)
}
