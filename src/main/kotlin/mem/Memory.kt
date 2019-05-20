package mem

//import jdk.internal.org.objectweb.asm.tree.InnerClassNode


interface MemoryPolicy {
    fun getSwappingCandidates(i: Int): List<Int>
}

class Memory(
        val size: Int,
        val pageSize: Int,
        val policy: MemoryPolicies
) {
    val actualPolicy: MemoryPolicy = when(policy) {
        MemoryPolicies.FIFO -> FIFOPolicy()
        MemoryPolicies.LRU -> FIFOPolicy()
        MemoryPolicies.MFU -> FIFOPolicy()
        MemoryPolicies.LIFO -> LIFOPolicy()
    }
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
            if (page.pid == pid) Page(index, null, null) else page
        }

        pages.clear()
        pages.addAll(newPages)
    }

    fun allocatePage(pid: Int?, processPageIndex: Int?) {
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

    fun getSwappingCandidates(requiredPages: Int): List<Int> = actualPolicy.getSwappingCandidates(requiredPages)

    fun getSwappingCandidate(): Int = actualPolicy.getSwappingCandidates(1).first()


    fun getAvailablePages() = pages.count { it.pid == null }


    fun getCurrentAllocationSnapshot(): Triple<Int, Int, Int> = Triple(0,0,0)


    inner class FIFOPolicy: MemoryPolicy {
        override fun getSwappingCandidates(i: Int): List<Int> {
            var res = insertionQueue.subList(fromIndex = 0, toIndex = i)
            return res
        }
    }

    inner class LIFOPolicy: MemoryPolicy {
        override fun getSwappingCandidates(i: Int): List<Int> {
            var res = insertionQueue.subList(fromIndex = insertionQueue.size - i, toIndex = insertionQueue.size)
            return res
        }
    }
}