package cpu

import mem.FIFOPolicy
import mem.Memory

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
            val swappedPages = candidates.map { realMemory.pages.get(it) }

//            candidates.forEach { realMemory.pages[it] = realMemory.pages[it].copy(pid = null) }
//
//            swappedPages.forEach {
//                realMemory.pages.add
//            }

        }


        (0 until requiredPages).forEach {
            realMemory.allocatePage(
                pid = pid,
                processPageIndex = it
            )
        }
    }

    fun accessProccess(pid: Int, virtualAddress: Int, modify: Boolean = false) {
//        if (realMemory.contains(pid = pid, virtualAddress: pid))
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