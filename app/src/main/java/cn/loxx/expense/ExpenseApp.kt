package cn.loxx.expense

import android.app.Application
import cn.loxx.expense.data.webdav.AutoBackup
import cn.loxx.expense.data.webdav.WebDavClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExpenseApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Apache POI must use Aalto's StAX implementation on Android; these
        // properties must be set before any POI class is touched.
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLInputFactory",
            "com.fasterxml.aalto.stax.InputFactoryImpl",
        )
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLOutputFactory",
            "com.fasterxml.aalto.stax.OutputFactoryImpl",
        )
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLEventFactory",
            "com.fasterxml.aalto.evt.EventFactoryImpl",
        )

        container = AppContainer(this)

        appScope.launch { container.categoryRepository.ensureSeeded() }
        appScope.launch { runDailyAutoBackup() }
    }

    /** Fire-and-forget daily WebDAV backup; records the outcome for the settings page. */
    private suspend fun runDailyAutoBackup() {
        val settings = container.settingsRepository
        val now = System.currentTimeMillis()
        if (!AutoBackup.shouldRun(now, settings.autoBackupEnabled, settings.webdavUrl.isNotBlank(), settings.lastAutoBackupAt)) {
            return
        }
        try {
            container.syncManager.backup(
                WebDavClient(settings.webdavUrl, settings.webdavUser, settings.webdavPass),
            )
            settings.lastAutoBackupAt = now
            settings.lastAutoBackupResult = "成功"
        } catch (e: Exception) {
            settings.lastAutoBackupResult = "失败：${e.message ?: "未知错误"}"
        }
    }
}
