package dev.plw.pigeon.network

import android.content.Context
import android.net.Uri
import dev.plw.pigeon.model.SharedFile
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URLEncoder
import java.util.Collections
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class NetworkController(private val context: Context) {
    private var server: EmbeddedServer<*, *>? = null

    private var phoneFiles: List<SharedFile> = emptyList()
    private val uploadedFiles = Collections.synchronizedList(mutableListOf<SharedFile>())

    val allFiles: List<SharedFile>
        get() = phoneFiles + uploadedFiles

    var filesList: List<SharedFile>
        get() = allFiles
        set(value) {
            phoneFiles = value
        }

    var onFileReceived: ((File) -> Unit)? = null

    private fun loadAsset(path: String): String {
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }

    private fun openFileStream(uri: Uri): InputStream? {
        return if (uri.scheme == "file") {
            uri.path?.let { File(it).inputStream() }
        } else {
            context.contentResolver.openInputStream(uri)
        }
    }

    fun startServer(port: Int = 8080) {
        if (server != null) return

        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            routing {
                get("/") {
                    val currentFiles = allFiles
                    val totalBytes = currentFiles.sumOf { it.sizeBytes }
                    val totalMb = totalBytes / (1024 * 1024)

                    val infoText = if (currentFiles.isEmpty()) {
                        "0 items"
                    } else {
                        "${currentFiles.size} items • ~$totalMb MB"
                    }

                    val itemsHtml = if (currentFiles.isEmpty()) {
                        """<div class="empty-placeholder">No files on server yet</div>"""
                    } else {
                        buildString {
                            currentFiles.forEachIndexed { index, file ->
                                append("""
                                    <div class="file-card">
                                        <div class="file-info">
                                            <span class="file-name" title="${file.name}">${file.name}</span>
                                            <span class="file-size">${file.sizeMb}</span>
                                        </div>
                                        <a href="/download/$index" class="btn btn-secondary">Download</a>
                                    </div>
                                """.trimIndent())
                            }
                        }
                    }

                    val downloadBtnStyle = if (currentFiles.isEmpty()) "display: none;" else ""

                    val pageHtml = loadAsset("web/index.html")
                        .replace("{{INFO}}", infoText)
                        .replace("{{DOWNLOAD_BTN_STYLE}}", downloadBtnStyle)
                        .replace("{{ITEMS}}", itemsHtml)

                    call.respondText(pageHtml, ContentType.Text.Html)
                }

                get("/download/{index}") {
                    val index = call.parameters["index"]?.toIntOrNull()
                    val file = index?.let { allFiles.getOrNull(it) }

                    if (file == null) {
                        call.respond(HttpStatusCode.NotFound, "File not found")
                        return@get
                    }

                    val inputStream = openFileStream(file.uri)
                    if (inputStream == null) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to read file")
                        return@get
                    }

                    val encodedName = URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        "attachment; filename=\"$encodedName\"; filename*=UTF-8''$encodedName"
                    )
                    call.response.header(HttpHeaders.ContentLength, file.sizeBytes.toString())

                    call.respondOutputStream(ContentType.Application.OctetStream) {
                        inputStream.use { input ->
                            input.copyTo(this)
                        }
                    }
                }

                get("/download-all") {
                    val currentFiles = allFiles
                    if (currentFiles.isEmpty()) {
                        call.respond(HttpStatusCode.NotFound, "No files to download")
                        return@get
                    }

                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        "attachment; filename=\"pigeon_share.zip\""
                    )

                    call.respondOutputStream(ContentType.Application.Zip) {
                        ZipOutputStream(this.buffered()).use { zipOut ->
                            val usedNames = mutableSetOf<String>()

                            currentFiles.forEach { file ->
                                var entryName = file.name
                                var counter = 1
                                while (usedNames.contains(entryName)) {
                                    val base = file.name.substringBeforeLast('.', file.name)
                                    val ext = file.name.substringAfterLast('.', "")
                                    entryName = if (ext.isNotEmpty()) "$base ($counter).$ext" else "$base ($counter)"
                                    counter++
                                }
                                usedNames.add(entryName)

                                val entry = ZipEntry(entryName)
                                zipOut.putNextEntry(entry)

                                openFileStream(file.uri)?.use { input ->
                                    input.copyTo(zipOut)
                                }
                                zipOut.closeEntry()
                            }
                            zipOut.finish()
                        }
                    }
                }

                post("/upload") {
                    val multipartData = call.receiveMultipart()
                    withContext(Dispatchers.IO) {
                        multipartData.forEachPart { part ->
                            if (part is PartData.FileItem) {
                                val rawName = part.originalFileName ?: "upload_${System.currentTimeMillis()}"
                                val safeName = File(rawName).name
                                val targetFile = File(context.cacheDir, safeName)

                                part.streamProvider().use { input ->
                                    targetFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }

                                val shared = SharedFile(
                                    uri = Uri.fromFile(targetFile),
                                    name = safeName,
                                    sizeBytes = targetFile.length()
                                )

                                synchronized(uploadedFiles) {
                                    if (uploadedFiles.none { it.name == shared.name }) {
                                        uploadedFiles.add(shared)
                                    }
                                }

                                onFileReceived?.invoke(targetFile)
                            }
                            part.dispose()
                        }
                    }
                    call.respond(HttpStatusCode.OK, "Uploaded")
                }
            }
        }.start(wait = false)
    }

    fun stopServer() {
        server?.stop(gracePeriodMillis = 1000, timeoutMillis = 2000)
        server = null
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}