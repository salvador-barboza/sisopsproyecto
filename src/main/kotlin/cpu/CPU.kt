package cpu

import mem.FIFOPolicy
import mem.Memory
import mem.Page

class CPU(
    val realMemorySize: Int,
    val swapMemorySize: Int,
    val pageSize: Int
) {
    val realMemory = Memory(realMemorySize, pageSize, FIFOPolicy())
    val swapMemory = Memory(swapMemorySize, pageSize, FIFOPolicy())

    private val modificationHistoryQueue = mutableListOf<Int>() //NO SE SI ESTO ES DEL CPU O DE MEMOIRA

    fun spawnProcess(size: Int, pid: Int) {
        val pagesAvailable = realMemory.getAvailablePages()
        val requiredPages = size / pageSize

        if (pagesAvailable < requiredPages) {
            val candidates = realMemory.getSwappingCandidates(requiredPages)
            val swappedPages = candidates.map {
                val page = realMemory.pages.get(it)
                realMemory.pages.set(it, Page(pid = null, pageIndex = it, processPageIndex = null))

                return@map page
            }

            swappedPages.forEach {
                swapMemory.allocatePage(
                    pid = it.pid,
                    processPageIndex = it.processPageIndex)
            }
        }

        (0 until requiredPages).forEach {
            realMemory.allocatePage(
                pid = pid,
                processPageIndex = it
            )
        }
    }

    fun accessProccess(pid: Int, virtualAddress: Int, modify: Boolean = false) {
        val pageNumber = virtualAddress / pageSize
        val displacement = virtualAddress % pageSize

        val page = realMemory.pages.firstOrNull { it.pid ==  pid && it.processPageIndex == pageNumber }

        if (page == null) {
            //swap
        } else {
            print("${page.pageIndex * pageSize + displacement} \n")
        }
    }

    fun getMemoryAllocationStatus() = Pair<Any, Any>(
        realMemory.getCurrentAllocationSnapshot(),
        swapMemory.getCurrentAllocationSnapshot()
    )

    fun clearProcess(pid: Int) {
        realMemory.clearProcess(pid)
        swapMemory.clearProcess(pid)
    }
}