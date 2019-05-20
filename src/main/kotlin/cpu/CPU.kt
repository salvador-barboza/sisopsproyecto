package cpu

import mem.MemoryPolicies
import mem.Memory
import mem.Page

class CPU(
        val realMemory: Memory,
        val swapMemory: Memory,
        val pageSize: Int
) {

    fun spawnProcess(size: Int, pid: Int) {
        val pagesAvailable = realMemory.getAvailablePages()
        val requiredPages = (Math.ceil(size.toDouble() / pageSize)).toInt()



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

    fun accessProccess(pid: Int, virtualAddress: Int, modify: Boolean = false):Int {
        var pageNumber = virtualAddress / pageSize
        var displacement = virtualAddress % pageSize
        if(displacement==0 && virtualAddress!=0){
            pageNumber--
            displacement=pageSize
        }

        var page = realMemory.pages.firstOrNull { it.pid ==  pid && it.processPageIndex == pageNumber }

        if (page == null) {
            val swapPage = realMemory.getSwappingCandidate()
            val swappedPage = swapMemory.pages.first { it.pid == pid && it.processPageIndex == pageNumber }

            swapPage.let {
                swapMemory.allocatePage(
                        pid = realMemory.pages[it].pid,
                        processPageIndex = realMemory.pages[it].processPageIndex)

                realMemory.pages.set(it,
                        Page(pid = swappedPage?.pid, pageIndex = it, processPageIndex = swappedPage?.processPageIndex))

                page = realMemory.pages[swapPage]
            }
        }
        return page!!.pageIndex*pageSize+displacement
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