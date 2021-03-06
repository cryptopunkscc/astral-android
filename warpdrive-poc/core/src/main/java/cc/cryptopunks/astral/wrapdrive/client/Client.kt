package cc.cryptopunks.astral.wrapdrive.client

import android.util.Log
import cc.cryptopunks.astral.binary.bytes
import cc.cryptopunks.astral.ext.byte
import cc.cryptopunks.astral.ext.read
import cc.cryptopunks.astral.net.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

const val List: Byte = 1
const val SendStream: Byte = 3
const val Port = "wdrive-local"
private const val R_OK: Byte = 0

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun Network.listPeers(): List<String> = withContext(Dispatchers.IO) {
    query(Port).run {
        write(List.bytes)
        input.run {
            (0 until read()).map {
                String(read(read()))
            }
        }.also { close() }
    }
}


@Suppress("BlockingMethodInNonBlockingContext")
fun Network.sendFile(
    nodeId: String,
    fileName: String,
    inputStream: InputStream,
) = channelFlow<Long> {
    query(Port).run {
        write(SendStream.bytes)
        nodeId.toByteArray().run {
            write(size.toByte().bytes)
            write(this)
        }
        fileName.toByteArray().run {
            write(size.toShort().bytes)
            write(this)
        }
        Log.d("Client", "reading response")
        if (byte == R_OK) {
            Log.d("Client", "sending stream")
            copy(inputStream, output, progress = channel)
            Log.d("Client", "sending finish")
        } else {
            Log.d("Client", "sending rejected")
        }
        close()
    }
}

fun copy(
    input: InputStream,
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    progress: SendChannel<Long>? = null,
) {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = input.read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        progress?.trySend(bytesCopied)
        bytes = input.read(buffer)
    }
}
