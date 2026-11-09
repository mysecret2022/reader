package io.legado.app.model.webBook

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.http.StrResponse
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.webBook.BookChapterList
import io.legado.app.model.webBook.BookContent
import io.legado.app.model.webBook.BookInfo
import io.legado.app.model.webBook.BookList
import io.legado.app.model.Debug
import io.legado.app.model.DebugLog
import mu.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

class WebBook(val bookSource: BookSource, val debugLog: Boolean = true, var debugLogger: DebugLog? = null) {

    constructor(bookSourceString: String, debugLog: Boolean = true) : this(BookSource.fromJson(bookSourceString).getOrNull() ?: BookSource(), debugLog)

    val sourceUrl: String
        get() = bookSource.bookSourceUrl

    val debugger: DebugLog?
        get() {
            if (debugLogger != null) {
                return debugLogger
            }
            if (debugLog) {
                return Debug
            }
            return null
        }

    /**
     * 搜索
     */
    suspend fun searchBook(
        key: String,
        page: Int? = 1
    ): List<SearchBook> {
        val searchUrl = bookSource.searchUrl
        if (searchUrl.isNullOrBlank()) {
            throw IllegalArgumentException("书源未配置搜索规则 (searchUrl 为空)")
        }
        val variableBook = SearchBook()
        val analyzeUrl = AnalyzeUrl(
            mUrl = searchUrl,
            key = key,
            page = page,
            baseUrl = bookSource.bookSourceUrl,
            source = bookSource,
            ruleData = variableBook,
            headerMapF = bookSource.getHeaderMap(true),
        )
        val res = analyzeUrl.getStrResponseAwait(debugLog = debugger)
        val code = res.code()
        if (code != 200) {
            throw IllegalStateException("搜索 HTTP 状态码异常: $code")
        }
        val body = res.body ?: ""
        val bodyLower = body.lowercase()
        // 反劫持与软 404 / 停靠页指纹校验
        if (body.toByteArray().size < 200) {
            throw IllegalStateException("搜索响应内容过短 (< 200 字节)，疑似空白或已失效")
        }
        if (bodyLower.contains("domain for sale") ||
            bodyLower.contains("domain is for sale") ||
            bodyLower.contains("parking") ||
            bodyLower.contains("域名出售") ||
            bodyLower.contains("站点已关闭") ||
            bodyLower.contains("网站已关闭") ||
            bodyLower.contains("5秒后自动跳转") ||
            (bodyLower.contains("cloudflare") && bodyLower.contains("attention required"))
        ) {
            throw IllegalStateException("命中域名停靠/防爬阻断特征页")
        }
        var processedRes = res
        //检测书源是否已登录
        bookSource.loginCheckJs?.let { checkJs ->
            if (checkJs.isNotBlank()) {
                processedRes = analyzeUrl.evalJS(checkJs, res) as StrResponse
            }
        }
        return BookList.analyzeBookList(
            processedRes.body,
            bookSource,
            analyzeUrl,
            processedRes.url,
            variableBook,
            true,
            debugLog = debugger
        ).map {
            it.tocHtml = ""
            it.infoHtml = ""
            it
        }
    }

    /**
     * 发现
     */
    suspend fun exploreBook(
        url: String,
        page: Int? = 1
    ): List<SearchBook> {
        val variableBook = SearchBook()
        val analyzeUrl = AnalyzeUrl(
            mUrl = url,
            page = page,
            baseUrl = bookSource.bookSourceUrl,
            source = bookSource,
            ruleData = variableBook,
            headerMapF = bookSource.getHeaderMap(true)
        )
        val res = analyzeUrl.getStrResponseAwait(debugLog = debugger)
        val code = res.code()
        if (code != 200) {
            throw IllegalStateException("发现页 HTTP 状态码异常: $code")
        }
        val body = res.body ?: ""
        val bodyLower = body.lowercase()
        if (body.toByteArray().size < 200) {
            throw IllegalStateException("发现页响应内容过短 (< 200 字节)，疑似空白或已失效")
        }
        if (bodyLower.contains("domain for sale") ||
            bodyLower.contains("domain is for sale") ||
            bodyLower.contains("parking") ||
            bodyLower.contains("域名出售") ||
            bodyLower.contains("站点已关闭") ||
            bodyLower.contains("网站已关闭") ||
            bodyLower.contains("5秒后自动跳转") ||
            (bodyLower.contains("cloudflare") && bodyLower.contains("attention required"))
        ) {
            throw IllegalStateException("发现页命中域名停靠/防爬阻断特征页")
        }
        var processedRes = res
        //检测书源是否已登录
        bookSource.loginCheckJs?.let { checkJs ->
            if (checkJs.isNotBlank()) {
                processedRes = analyzeUrl.evalJS(checkJs, result = res) as StrResponse
            }
        }
        return BookList.analyzeBookList(
            processedRes.body,
            bookSource,
            analyzeUrl,
            processedRes.url,
            variableBook,
            false,
            debugLog = debugger
        )
    }

    /**
     * 书籍信息
     */
    suspend fun getBookInfo(book: Book, canReName: Boolean = true): Book {
        book.type = bookSource.bookSourceType
        if (!book.infoHtml.isNullOrEmpty()) {
            BookInfo.analyzeBookInfo(
                book,
                book.infoHtml,
                bookSource,
                book.bookUrl,
                book.bookUrl,
                canReName
            )
            return book
        } else {
            return getBookInfo(book.bookUrl, canReName)
        }
    }

    /**
     * 书籍信息
     */
    suspend fun getBookInfo(bookUrl: String, canReName: Boolean = true): Book {
        val book = Book()
        book.bookUrl = bookUrl
        book.origin = bookSource.bookSourceUrl
        book.originName = bookSource.bookSourceName
        book.originOrder = bookSource.customOrder
        book.type = bookSource.bookSourceType
        val analyzeUrl = AnalyzeUrl(
            mUrl = book.bookUrl,
            baseUrl = bookSource.bookSourceUrl,
            source = bookSource,
            ruleData = book,
            headerMapF = bookSource.getHeaderMap(true)
        )
        var res = analyzeUrl.getStrResponseAwait(debugLog = debugger)
        //检测书源是否已登录
        bookSource.loginCheckJs?.let { checkJs ->
            if (checkJs.isNotBlank()) {
                res = analyzeUrl.evalJS(checkJs, result = res) as StrResponse
            }
        }

        BookInfo.analyzeBookInfo(book, res.body, bookSource, book.bookUrl, res.url, canReName, debugLog = debugger)
        book.tocHtml = null
        return book
    }

    /**
     * 目录
     */
    suspend fun getChapterList(
        book: Book
    ): List<BookChapter> {
        book.type = bookSource.bookSourceType
        return if (book.bookUrl == book.tocUrl && !book.tocHtml.isNullOrEmpty()) {
            BookChapterList.analyzeChapterList(
                book,
                book.tocHtml,
                bookSource,
                book.tocUrl,
                book.tocUrl
            )
        } else {
            val analyzeUrl = AnalyzeUrl(
                mUrl = book.tocUrl,
                baseUrl = book.bookUrl,
                source = bookSource,
                ruleData = book,
                headerMapF = bookSource.getHeaderMap(true)
            )
            var res = analyzeUrl.getStrResponseAwait(debugLog = debugger)
            //检测书源是否已登录
            bookSource.loginCheckJs?.let { checkJs ->
                if (checkJs.isNotBlank()) {
                    res = analyzeUrl.evalJS(checkJs, result = res) as StrResponse
                }
            }
            return BookChapterList.analyzeChapterList(book, res.body, bookSource, book.tocUrl, res.url, debugLog = debugger)
        }
    }

    /**
     * 章节内容
     */
    suspend fun getBookContent(
       book: Book,
       bookChapter: BookChapter,
        // bookChapterUrl:String,
        nextChapterUrl: String? = null
    ): String {
       if (bookSource.getContentRule().content.isNullOrEmpty()) {
            debugger?.log(bookSource.bookSourceUrl, "⇒正文规则为空,使用章节链接: ${bookChapter.url}")
            return bookChapter.url
       }
       if (bookChapter.isVolume && bookChapter.url.startsWith(bookChapter.title)) {
            debugger?.log(bookSource.bookSourceUrl, "⇒一级目录正文不解析规则")
            return bookChapter.tag ?: ""
        }
//        val body = if (book != null && bookChapter.url == book.bookUrl && !book.tocHtml.isNullOrEmpty()) {
//            book.tocHtml
//        } else {
        logger.info("bookChapterUrl: {}", bookChapter.url, bookChapter.getAbsoluteURL())
        val analyzeUrl = AnalyzeUrl(
            mUrl = bookChapter.getAbsoluteURL(),
            baseUrl = book.tocUrl,
            source = bookSource,
            ruleData = book,
            chapter = bookChapter,
            headerMapF = bookSource.getHeaderMap(true)
        )
        var res = analyzeUrl.getStrResponseAwait(
            jsStr = bookSource.getContentRule().webJs,
            sourceRegex = bookSource.getContentRule().sourceRegex,
            debugLog = debugger
        )
        return BookContent.analyzeContent(
            res.body,
            book,
            bookChapter,
            bookSource,
            bookChapter.url,
            res.url,
            nextChapterUrl,
            debugLog = debugger
        )
    }
}