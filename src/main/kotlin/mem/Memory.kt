package mem


interface MemoryPolicy {
    fun getSwappingCandidates(
        requiredPages: Int,
        pages: List<Page>,
        modifyQueue: List<Int>,
        accessQueue: List<Int>,
        insertionQueue: List<Int>
    ): List<Int>
}

class FIFOPolicy: MemoryPolicy {
    override fun getSwappingCandidates(
        requiredPages: Int,
        pages: List<Page>,
        modifyQueue: List<Int>,
        accessQueue: List<Int>,
        insertionQueue: List<Int>
    ): List<Int> {
        return insertionQueue.subList(0, requiredPages)
    }
}

class Memory(
    val size: Int,
    val pageSize: Int,
    val policy: MemoryPolicy
) {
    private val modifyQueue = mutableListOf<Int>()
    private val accessQueue = mutableListOf<Int>()
    private val insertionQueue = mutableListOf<Int>()

    val pages: MutableList<Page> = (1..size/pageSize).map { Page(
            pageIndex = it,
            pid = null,
            processPageIndex = null
        ) }.toMutableList()


    private fun availablePages(): List<Page> =
        pages.filter { it.pid == null }


    fun getProcessPageIndex(pid: Int, pageIndex: Int) =
            pages.firstOrNull {  it.pid == pid && it.processPageIndex== pageIndex }

    fun isRequiredSpaceAvailable(requiredSize: Int): Boolean =
        pages.count { it.pid == null } >= requiredSize / pageSize


    fun clearProcess(pid: Int) {
        val newPages = pages.mapIndexed { index, page ->
            if (page.pid == pid) Page(pageSize, null, index) else page
        }

        pages.clear()
        pages.addAll(newPages)
    }

    fun allocatePage(pid: Int, processPageIndex: Int) {
//        if (!isRequiredSpaceAvailable(size)) {
//            throw Throwable("Not enough space left in memory")
//        }

        pages.indexOfFirst { it.pid == null }
            .let {
                insertionQueue.add(it)
                pages[it] = Page(
                    pageIndex =  it,
                    pid = pid,
                    processPageIndex = processPageIndex
                )
            }
    }

    fun getSwappingCandidates(requiredPages: Int): List<Int> =
        policy.getSwappingCandidates(requiredPages, pages, modifyQueue, accessQueue, insertionQueue)


    fun getAvailablePages() = pages.count { it.pid == null }


    fun getCurrentAllocationSnapshot(): Triple<Int, Int, Int> = Triple(0,0,0)
}
