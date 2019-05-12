package mem

data class Page(
    val pageIndex: Int,
    val pid: Int?,
    val processPageIndex: Int?
)
