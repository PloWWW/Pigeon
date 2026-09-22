package dev.plw.pigeon.network

import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class NetworkController {
    private var server: EmbeddedServer<*, *>? = null

    fun startServer(port: Int = 8080) {
        if (server != null) return

        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            routing {
                get("/") {
                    call.respondText("Pigeon Server is running!")
                }
            }
        }.start(wait = false)
    }

    fun stopServer() {
        server?.stop(gracePeriodMillis = 1000, timeoutMillis = 2000)
        server = null
    }
}