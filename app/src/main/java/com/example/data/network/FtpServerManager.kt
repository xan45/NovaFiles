package com.example.data.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FtpServerConfig(
    val port: Int = 2121,
    val username: String = "admin",
    val password: String = "novafiles",
    val isAnonymousAllowed: Boolean = true,
    val rootPath: String = android.os.Environment.getExternalStorageDirectory().absolutePath,
    val isReadOnly: Boolean = false
)

data class FtpServerState(
    val isRunning: Boolean = false,
    val ipAddress: String = "0.0.0.0",
    val port: Int = 2121,
    val activeClientsCount: Int = 0,
    val logs: List<String> = emptyList()
)

class FtpServerManager(private val context: Context) {

    private val _serverState = MutableStateFlow(FtpServerState())
    val serverState: StateFlow<FtpServerState> = _serverState.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val activeClients = mutableListOf<Socket>()

    fun startServer(config: FtpServerConfig, scope: CoroutineScope) {
        if (_serverState.value.isRunning) return

        val ip = getLocalIpAddress()
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket(config.port)
                serverSocket = server
                addLog("FTP Server started on ftp://$ip:${config.port}")
                _serverState.value = _serverState.value.copy(
                    isRunning = true,
                    ipAddress = ip,
                    port = config.port
                )

                while (isActive && !server.isClosed) {
                    try {
                        val clientSocket = server.accept()
                        synchronized(activeClients) { activeClients.add(clientSocket) }
                        _serverState.value = _serverState.value.copy(activeClientsCount = activeClients.size)
                        addLog("Client connected: ${clientSocket.inetAddress.hostAddress}")

                        scope.launch(Dispatchers.IO) {
                            handleClient(clientSocket, config)
                            synchronized(activeClients) { activeClients.remove(clientSocket) }
                            _serverState.value = _serverState.value.copy(activeClientsCount = activeClients.size)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                addLog("Error starting FTP server: ${e.message}")
                stopServer()
            }
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
            serverSocket = null
            synchronized(activeClients) {
                activeClients.forEach { runCatching { it.close() } }
                activeClients.clear()
            }
            serverJob?.cancel()
            serverJob = null
            addLog("FTP Server stopped")
            _serverState.value = _serverState.value.copy(
                isRunning = false,
                activeClientsCount = 0
            )
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$time] $msg"
        val updated = (_serverState.value.logs + entry).takeLast(50)
        _serverState.value = _serverState.value.copy(logs = updated)
    }

    private fun handleClient(clientSocket: Socket, config: FtpServerConfig) {
        var currentDir = File(config.rootPath)
        var isAuthenticated = config.isAnonymousAllowed
        var currentUsername = ""
        var passiveDataServer: ServerSocket? = null

        try {
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val writer = PrintWriter(OutputStreamWriter(clientSocket.getOutputStream()), true)

            writer.println("220 NovaFiles FTP Server Ready")

            while (!clientSocket.isClosed) {
                val line = reader.readLine() ?: break
                val parts = line.trim().split(" ", limit = 2)
                val cmd = parts[0].uppercase(Locale.ROOT)
                val arg = if (parts.size > 1) parts[1] else ""

                when (cmd) {
                    "USER" -> {
                        currentUsername = arg
                        if (config.isAnonymousAllowed && arg.equals("anonymous", ignoreCase = true)) {
                            isAuthenticated = true
                            writer.println("230 Anonymous user logged in")
                        } else {
                            writer.println("331 Password required for $arg")
                        }
                    }
                    "PASS" -> {
                        if (currentUsername == config.username && arg == config.password) {
                            isAuthenticated = true
                            writer.println("230 User logged in successfully")
                        } else if (config.isAnonymousAllowed && currentUsername.equals("anonymous", ignoreCase = true)) {
                            isAuthenticated = true
                            writer.println("230 Anonymous user logged in")
                        } else {
                            writer.println("530 Login incorrect")
                        }
                    }
                    "SYST" -> writer.println("215 UNIX Type: L8")
                    "FEAT" -> {
                        writer.println("211-Features:")
                        writer.println(" UTF8")
                        writer.println(" SIZE")
                        writer.println("211 End")
                    }
                    "PWD" -> writer.println("257 \"${currentDir.absolutePath}\" is current directory")
                    "TYPE" -> writer.println("200 Type set to $arg")
                    "PASV" -> {
                        passiveDataServer?.close()
                        passiveDataServer = ServerSocket(0)
                        val port = passiveDataServer.localPort
                        val ipParts = getLocalIpAddress().split(".")
                        val p1 = port / 256
                        val p2 = port % 256
                        val pasvIp = ipParts.joinToString(",")
                        writer.println("227 Entering Passive Mode ($pasvIp,$p1,$p2)")
                    }
                    "EPSV" -> {
                        passiveDataServer?.close()
                        passiveDataServer = ServerSocket(0)
                        val port = passiveDataServer.localPort
                        writer.println("229 Entering Extended Passive Mode (|||$port|)")
                    }
                    "LIST" -> {
                        writer.println("150 Opening ASCII mode data connection")
                        val dataSocket = passiveDataServer?.accept()
                        dataSocket?.use { ds ->
                            val dataOut = PrintWriter(OutputStreamWriter(ds.getOutputStream()), true)
                            val files = currentDir.listFiles() ?: emptyArray()
                            val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.US)
                            for (f in files) {
                                val perms = if (f.isDirectory) "drwxr-xr-x" else "-rw-r--r--"
                                val size = if (f.isDirectory) 4096 else f.length()
                                val dateStr = sdf.format(Date(f.lastModified()))
                                dataOut.println("$perms 1 owner group $size $dateStr ${f.name}")
                            }
                        }
                        passiveDataServer?.close()
                        passiveDataServer = null
                        writer.println("226 Transfer complete")
                    }
                    "CWD" -> {
                        val target = if (arg.startsWith("/")) File(arg) else File(currentDir, arg)
                        if (target.exists() && target.isDirectory) {
                            currentDir = target
                            writer.println("250 Directory changed to ${currentDir.absolutePath}")
                        } else {
                            writer.println("550 Failed to change directory")
                        }
                    }
                    "CDUP" -> {
                        val parent = currentDir.parentFile
                        if (parent != null && parent.exists()) {
                            currentDir = parent
                            writer.println("200 Directory changed to ${currentDir.absolutePath}")
                        } else {
                            writer.println("550 Cannot go above root")
                        }
                    }
                    "SIZE" -> {
                        val file = File(currentDir, arg)
                        if (file.exists() && file.isFile) {
                            writer.println("213 ${file.length()}")
                        } else {
                            writer.println("550 File not found")
                        }
                    }
                    "RETR" -> {
                        val file = File(currentDir, arg)
                        if (file.exists() && file.isFile) {
                            writer.println("150 Opening binary mode data connection")
                            val dataSocket = passiveDataServer?.accept()
                            dataSocket?.use { ds ->
                                FileInputStream(file).use { fis ->
                                    val buffer = ByteArray(8192)
                                    var read: Int
                                    while (fis.read(buffer).also { read = it } != -1) {
                                        ds.getOutputStream().write(buffer, 0, read)
                                    }
                                }
                            }
                            passiveDataServer?.close()
                            passiveDataServer = null
                            writer.println("226 Transfer complete")
                        } else {
                            writer.println("550 File not found")
                        }
                    }
                    "STOR" -> {
                        if (config.isReadOnly) {
                            writer.println("550 Server is in read-only mode")
                        } else {
                            val file = File(currentDir, arg)
                            writer.println("150 Opening binary mode data connection")
                            val dataSocket = passiveDataServer?.accept()
                            dataSocket?.use { ds ->
                                FileOutputStream(file).use { fos ->
                                    val buffer = ByteArray(8192)
                                    var read: Int
                                    while (ds.getInputStream().read(buffer).also { read = it } != -1) {
                                        fos.write(buffer, 0, read)
                                    }
                                }
                            }
                            passiveDataServer?.close()
                            passiveDataServer = null
                            writer.println("226 Transfer complete")
                        }
                    }
                    "DELE" -> {
                        if (config.isReadOnly) {
                            writer.println("550 Read-only mode")
                        } else {
                            val f = File(currentDir, arg)
                            if (f.delete()) writer.println("250 File deleted")
                            else writer.println("550 Could not delete file")
                        }
                    }
                    "MKD" -> {
                        if (config.isReadOnly) {
                            writer.println("550 Read-only mode")
                        } else {
                            val newDir = File(currentDir, arg)
                            if (newDir.mkdirs()) writer.println("257 \"$arg\" created")
                            else writer.println("550 Failed to create directory")
                        }
                    }
                    "RMD" -> {
                        if (config.isReadOnly) {
                            writer.println("550 Read-only mode")
                        } else {
                            val dir = File(currentDir, arg)
                            if (dir.deleteRecursively()) writer.println("250 Directory removed")
                            else writer.println("550 Failed to delete directory")
                        }
                    }
                    "NOOP" -> writer.println("200 OK")
                    "QUIT" -> {
                        writer.println("221 Goodbye")
                        break
                    }
                    else -> writer.println("502 Command not implemented")
                }
            }
        } catch (e: Exception) {
            // client disconnected
        } finally {
            runCatching { passiveDataServer?.close() }
            runCatching { clientSocket.close() }
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return "127.0.0.1"
    }
}
