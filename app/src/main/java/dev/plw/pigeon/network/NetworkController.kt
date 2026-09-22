package dev.plw.pigeon.network

import android.content.Context
import dev.plw.pigeon.model.SharedFile
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URLEncoder
import java.util.Collections
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class NetworkController(private val context: Context) {
    private var server: EmbeddedServer<*, *>? = null
    var filesList: List<SharedFile> = emptyList()
    private fun loadAsset(path: String): String {
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }

    fun startServer(port: Int = 8080) {
        if (server != null) return

        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            routing {
                get("/") {
                    if (filesList.isEmpty()) {
                        call.respondText(loadAsset("web/empty.html"), ContentType.Text.Html)
                        return@get
                    }

                    val totalMb = filesList.sumOf { it.sizeBytes } / (1024 * 1024)

                    val itemsHtml = buildString {
                        filesList.forEachIndexed { index, file ->
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

                    val pageHtml = loadAsset("web/index.html")
                        .replace("{{FILES_COUNT}}", filesList.size.toString())
                        .replace("{{TOTAL_SIZE}}", totalMb.toString())
                        .replace("{{ITEMS}}", itemsHtml)

                    call.respondText(pageHtml, ContentType.Text.Html)
                }

                get("/download/{index}") {
                    val index = call.parameters["index"]?.toIntOrNull()
                    val file = index?.let { filesList.getOrNull(it) }

                    if (file == null) {
                        call.respond(HttpStatusCode.NotFound, "File not found")
                        return@get
                    }

                    val inputStream = context.contentResolver.openInputStream(file.uri)
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
                    if (filesList.isEmpty()) {
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

                            filesList.forEach { file ->
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

                                context.contentResolver.openInputStream(file.uri)?.use { input ->
                                    input.copyTo(zipOut)
                                }
                                zipOut.closeEntry()
                            }
                            zipOut.finish()
                        }
                    }
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