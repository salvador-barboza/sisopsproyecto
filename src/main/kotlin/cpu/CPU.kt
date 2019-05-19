package cpu

import mem.MemoryPolicies
import mem.Memory
import mem.Page

class CPU(
    val realMemorySize: Int,
    val swapMemorySize: Int,
    val pageSize: Int
) {
    val realMemory = Memory(realMemorySize, pageSize, MemoryPolicies.LIFO)
    val swapMemory = Memory(swapMemorySize, pageSize, MemoryPolicies.LIFO)

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

        var page = realMemory.pages.firstOrNull { it.pid ==  pid && it.processPageIndex == pageNumber }

        if (page == null) {
            val swapPage = realMemory.getSwappingCandidate()
            val swappedPage = swapMemory.pages.first { it.pid == pid && it.processPageIndex == pageNumber }

            swapPage.let {
                swapMemory.allocatePage(
                    pid = realMemory.pages[it].pid,
                    processPageIndex = realMemory.pages[it].processPageIndex)

                realMemory.pages.set(it,
                    Page(pid = swappedPage.pid, pageIndex = it, processPageIndex = swappedPage .processPageIndex))

                page = realMemory.pages[swapPage]
            }
        }

        // just for debug
        // print("${page!!.pageIndex * pageSize + displacement} \n")
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