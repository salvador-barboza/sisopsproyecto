import cpu.CPU
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import java.net.InetSocketAddress
import kotlinx.coroutines.*
import kotlinx.coroutines.io.readUTF8Line
import kotlinx.coroutines.io.writeStringUtf8
import mem.Memory
import mem.MemoryPolicies

//Test que viene en el doc
fun main() {

    val realMemory = Memory(2*Sizes.KB, 16, MemoryPolicies.FIFO)
    val swapMemory = Memory(4*Sizes.KB, 16, MemoryPolicies.FIFO)

    val cpu = CPU(
        realMemory = realMemory,
        swapMemory = swapMemory,
        pageSize = 16
    )

    // P 2048 1
    cpu.spawnProcess(size = 2048, pid = 1)

    // A 1 1 0
    cpu.accessProccess(pid = 1, virtualAddress = 1)

    // A 33 1  1
    cpu.accessProccess(pid = 1, virtualAddress = 33, modify = true)

    // P 32 2
    cpu.spawnProcess(size = 32, pid = 2)

    // A 15 2 0
    cpu.accessProccess(pid = 2, virtualAddress = 15)

    // A 82 1 0
    cpu.accessProccess(pid = 1, virtualAddress = 82)

    // L 2
    cpu.clearProcess(pid = 2)

    // P 32 3
    cpu.spawnProcess(size = 32, pid = 3)

    // L 1
    cpu.clearProcess(pid = 1)

    cpu.realMemory.pages.forEach {
        print("${it.pageIndex} - ${it.pid ?: ""} ${it.processPageIndex ?: 'L'} \n")
    }
}