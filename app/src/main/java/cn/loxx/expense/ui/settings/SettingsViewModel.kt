package cn.loxx.expense.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.loxx.expense.data.local.CategoryEntity
import cn.loxx.expense.data.repository.CategoryRepository
import cn.loxx.expense.data.repository.SettingsRepository
import cn.loxx.expense.data.webdav.SyncManager
import cn.loxx.expense.data.webdav.WebDavClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val categoryRepository: CategoryRepository,
    val settingsRepository: SettingsRepository,
    private val syncManager: SyncManager,
) : ViewModel() {
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(name: String, icon: String) {
        viewModelScope.launch { categoryRepository.add(name, icon) }
    }

    fun renameCategory(category: CategoryEntity, newName: String) {
        viewModelScope.launch { categoryRepository.rename(category, newName) }
    }

    fun deleteCategory(category: CategoryEntity, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { categoryRepository.delete(category) }.exceptionOrNull()?.toMessage())
        }
    }

    fun testConnection(client: WebDavClient, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { client.testConnection() }.getOrDefault(false))
        }
    }

    fun backup(client: WebDavClient, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { syncManager.backup(client) }.exceptionOrNull()?.toMessage())
        }
    }

    fun restore(client: WebDavClient, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { syncManager.restore(client) }.exceptionOrNull()?.toMessage())
        }
    }

    fun listBackups(client: WebDavClient, onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { syncManager.listBackups(client) }.getOrDefault(emptyList()))
        }
    }

    private fun Throwable.toMessage(): String = message ?: javaClass.simpleName
}
