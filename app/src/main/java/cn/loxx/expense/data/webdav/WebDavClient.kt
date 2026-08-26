package cn.loxx.expense.data.webdav

import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Thin suspend wrapper around a blocking sardine WebDAV client. */
class WebDavClient(baseUrl: String, username: String, password: String) {
    private val baseUrl = baseUrl.trimEnd('/')
    private val sardine: Sardine = OkHttpSardine().apply {
        setCredentials(username, password)
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            sardine.list(baseUrl)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun upload(remotePath: String, data: ByteArray) = withContext(Dispatchers.IO) {
        sardine.put(fullUrl(remotePath), data)
    }

    suspend fun download(remotePath: String): ByteArray = withContext(Dispatchers.IO) {
        sardine.get(fullUrl(remotePath)).use { it.readBytes() }
    }

    suspend fun list(remotePath: String): List<DavResource> = withContext(Dispatchers.IO) {
        sardine.list(fullUrl(remotePath))
    }

    suspend fun mkdir(remotePath: String) = withContext(Dispatchers.IO) {
        sardine.createDirectory(fullUrl(remotePath))
    }

    private fun fullUrl(path: String): String = "$baseUrl/${path.trimStart('/')}"
}
