package com.perol.pixez.shared.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 应用本地化多语言字符串字典定义。
 */
interface AppStrings {
    // 底部主标签与核心导航
    val tabRecommend: String
    val tabRanking: String
    val tabNew: String
    val tabSearch: String
    val tabSpotlight: String
    val tabSettings: String

    // 通用操作与状态
    val back: String
    val cancel: String
    val confirm: String
    val complete: String
    val refresh: String
    val retry: String
    val share: String
    val download: String
    val bookmark: String
    val bookmarked: String
    val follow: String
    val followed: String
    val copy: String
    val copiedToClipboard: String
    val openInBrowser: String
    val loading: String
    val loadFailed: String
    val noData: String

    // Spotlight 特辑
    val spotlightTitle: String
    val categoryAll: String
    val categoryIllust: String
    val categoryManga: String
    val categoryNovel: String
    val spotlightLead: String
    val includedWorks: String
    val includedArticles: String
    val viewArtworkDetail: String
    val viewOriginalArticle: String

    // 初次启动引导流程
    val guideTitle: String
    val guideStepLanguage: String
    val guideStepLanguageDesc: String
    val guideStepNetwork: String
    val guideStepNetworkDesc: String
    val guideStepWelcome: String
    val guideStepWelcomeDesc: String
    val guideLoggedInStatus: String
    val guideSwitchAccount: String
    val guideNotLoggedIn: String
    val guideLoginNow: String
    val guideSkipLogin: String
    val guideStartJourney: String
    val guideNext: String
    val guidePrev: String
    val guideFinish: String

    // 设置项
    val settingsTitle: String
    val settingLanguage: String
    val settingTheme: String
    val settingNetwork: String
    val settingDownload: String
    val settingSave: String
    val settingLayout: String
    val settingCrossAdapter: String
    val settingShield: String
    val settingAbout: String
    val settingThanks: String
    val settingGuide: String
    val settingGeneral: String get() = "General Settings"
    val settingPlatform: String get() = "Platform Settings"
    val settingQuality: String get() = "Quality Settings"
    val settingDataExport: String get() = "Data Backup & Export"
    val settingUpdate: String get() = "Check for Updates"
    val settingPrivacy: String get() = "Privacy Settings"
    val settingFeed: String get() = "Feed Settings"
    val settingWelcomePage: String get() = "Welcome Page"
    val settingCopyText: String get() = "Copy Text Settings"
    val settingInteraction: String get() = "Interactions & Gestures"
    val settingUserShowAI: String get() = "AI Work Visibility"
    val settingWidgetRecommend: String get() = "Widget Settings"

    // 弹窗与通用对话框
    val dialogNeedLogin: String get() = "Need Login"
    val dialogNeedLoginSummary: String get() = "You are currently not logged in. Log in to use full features."
    val btnGoLogin: String get() = "Log In"
    val btnCancelLogin: String get() = "Not Now"
    val menuMoreActions: String get() = "More Actions"
    val menuCopyInfo: String get() = "Copy Info"
    val menuCopyLink: String get() = "Copy Link"
    val menuShareLink: String get() = "Share Link"
    val menuBanWork: String get() = "Block Artwork"
    val dialogAddTag: String get() = "Add Bookmark Tag"
    val dialogAddTagSummary: String get() = "Enter tag name to bookmark"
    val dialogDeleteConfirm: String get() = "Confirm Deletion"
    val dialogDeleteConfirmSummary: String get() = "Are you sure you want to delete? This action cannot be undone."
    val btnDelete: String get() = "Delete"
    val btnDeleting: String get() = "Deleting..."
    val btnAdd: String get() = "Add"
    val btnAdding: String get() = "Adding..."
    val dialogSavePath: String get() = "Save Path"
    val dialogSaveMode: String get() = "Save Mode"
    val dialogSaveFormat: String get() = "Save Format"
    val dialogTaskCount: String get() = "Concurrent Download Tasks"
    val dialogPickSeedColor: String get() = "Select Seed Color"
    val dialogPickSeedColorSummary: String get() = "Click preset color or enter custom HEX value"
    val dialogPaletteStyle: String get() = "Palette Style"
    val dialogPaletteStyleSummary: String get() = "Select Monet dynamic palette style"
    val dialogDisplayMode: String get() = "Display Mode"
    val dialogNewVersionFound: String get() = "New Version Found"
    val dialogNewVersionSummary: String get() = "A new version of PixEz is available."
    val btnUpdate: String get() = "Update"
    val btnIgnore: String get() = "Ignore"
    val batchActions: String get() = "Batch Actions"
    val retryFailedTasks: String get() = "Retry Failed Tasks"
    val clearCompletedTasks: String get() = "Clear Completed Tasks"

    // 搜索与筛选
    val searchFilter: String get() = "Search Filter"
    val matchTarget: String get() = "Match Target"
    val filterAi: String get() = "AI Work Filter"
    val filterBookmark: String get() = "Bookmark Threshold"
    val filterUgoira: String get() = "Animation Filter"
    val filterDateRange: String get() = "Date Range"
    val filterStartDate: String get() = "Start Date"
    val filterEndDate: String get() = "End Date"

    // 作品详情与互动
    val views: String
    val bookmarks: String
    val publishDate: String
    val tags: String
    val author: String
    val comments: String
    val relatedIllusts: String
    val noComments: String

    companion object {
        fun fromLanguageNum(num: Int): AppStrings = when (num) {
            1 -> ZhCnStrings
            2 -> ZhTwStrings
            3 -> JaStrings
            4 -> KoStrings
            5 -> RuStrings
            6 -> EsStrings
            7 -> TrStrings
            8 -> IdStrings
            9 -> FilStrings
            10 -> DeStrings
            else -> EnStrings // 0 或默认 -> en-US
        }
    }
}

/**
 * 简体中文 (zh-CN)
 */
object ZhCnStrings : AppStrings {
    override val tabRecommend = "推荐"
    override val tabRanking = "排行"
    override val tabNew = "动态"
    override val tabSearch = "搜索"
    override val tabSpotlight = "特辑"
    override val tabSettings = "设置"

    override val back = "返回"
    override val cancel = "取消"
    override val confirm = "确定"
    override val complete = "完成"
    override val refresh = "刷新"
    override val retry = "重试"
    override val share = "分享"
    override val download = "下载"
    override val bookmark = "收藏"
    override val bookmarked = "已收藏"
    override val follow = "关注"
    override val followed = "已关注"
    override val copy = "复制"
    override val copiedToClipboard = "已复制到剪切板"
    override val openInBrowser = "在浏览器中打开"
    override val loading = "加载中…"
    override val loadFailed = "加载失败"
    override val noData = "暂无数据"

    override val spotlightTitle = "Pixivision 特辑"
    override val categoryAll = "全部"
    override val categoryIllust = "插画"
    override val categoryManga = "漫画"
    override val categoryNovel = "小说"
    override val spotlightLead = "导语"
    override val includedWorks = "收录作品"
    override val includedArticles = "收录特辑"
    override val viewArtworkDetail = "查看画作详情"
    override val viewOriginalArticle = "在浏览器中查看 Pixivision 原文"

    override val guideTitle = "PixEz 引导向导"
    override val guideStepLanguage = "选择语言"
    override val guideStepLanguageDesc = "选择您习惯的语言与界面文本"
    override val guideStepNetwork = "网络与镜像加速"
    override val guideStepNetworkDesc = "根据网络环境选择图片加速源或直连"
    override val guideStepWelcome = "欢迎与账号"
    override val guideStepWelcomeDesc = "登录 Pixiv 账号以同步收藏与关注"
    override val guideLoggedInStatus = "已检测到 Pixiv 登录账号"
    override val guideSwitchAccount = "切换账号或重新登录"
    override val guideNotLoggedIn = "尚未登录 Pixiv 账号"
    override val guideLoginNow = "立即登录 Pixiv 账号"
    override val guideSkipLogin = "稍后登录，直接体验"
    override val guideStartJourney = "开启 PixEz 之旅"
    override val guideNext = "下一步"
    override val guidePrev = "上一步"
    override val guideFinish = "完成"

    override val settingsTitle = "设置"
    override val settingLanguage = "语言设置"
    override val settingTheme = "主题与色彩"
    override val settingNetwork = "网络与镜像源"
    override val settingDownload = "下载设置"
    override val settingSave = "保存路径与格式"
    override val settingLayout = "布局与列数"
    override val settingCrossAdapter = "横屏与自适应"
    override val settingShield = "屏蔽与过滤"
    override val settingAbout = "关于 PixEz"
    override val settingThanks = "致谢与支持"
    override val settingGuide = "启动引导向导"
    override val settingGeneral = "通用设置"
    override val settingPlatform = "平台专属设置"
    override val settingQuality = "画质设置"
    override val settingDataExport = "数据备份与导出"
    override val settingUpdate = "检查更新"
    override val settingPrivacy = "隐私设置"
    override val settingFeed = "Feed 设置"
    override val settingWelcomePage = "启动页设置"
    override val settingCopyText = "复制文本设置"
    override val settingInteraction = "交互与手势"
    override val settingUserShowAI = "AI 作品可见性"
    override val settingWidgetRecommend = "小组件推荐设置"

    override val dialogNeedLogin = "需要登录"
    override val dialogNeedLoginSummary = "当前未登录，登录后可使用完整功能"
    override val btnGoLogin = "去登录"
    override val btnCancelLogin = "暂不登录"
    override val menuMoreActions = "更多操作"
    override val menuCopyInfo = "复制信息"
    override val menuCopyLink = "复制链接"
    override val menuShareLink = "分享链接"
    override val menuBanWork = "屏蔽作品"
    override val dialogAddTag = "添加收藏标签"
    override val dialogAddTagSummary = "输入要收藏的标签名称"
    override val dialogDeleteConfirm = "删除确认"
    override val dialogDeleteConfirmSummary = "确定要删除吗？此操作无法撤销。"
    override val btnDelete = "删除"
    override val btnDeleting = "删除中…"
    override val btnAdd = "添加"
    override val btnAdding = "添加中…"
    override val dialogSavePath = "保存路径"
    override val dialogSaveMode = "保存模式"
    override val dialogSaveFormat = "保存格式"
    override val dialogTaskCount = "同时下载任务数"
    override val dialogPickSeedColor = "选择种子色"
    override val dialogPickSeedColorSummary = "点击预设颜色快速选择，或输入自定义 HEX 色值"
    override val dialogPaletteStyle = "调色板风格"
    override val dialogPaletteStyleSummary = "选择 Monet 动态取色的调色板风格"
    override val dialogDisplayMode = "显示模式"
    override val dialogNewVersionFound = "发现新版本"
    override val dialogNewVersionSummary = "检测到 PixEz 存在新版本发布"
    override val btnUpdate = "更新"
    override val btnIgnore = "忽略"
    override val batchActions = "批量操作"
    override val retryFailedTasks = "重试失败任务"
    override val clearCompletedTasks = "清空已完成任务"

    override val searchFilter = "搜索筛选"
    override val matchTarget = "匹配目标"
    override val filterAi = "AI 作品筛选"
    override val filterBookmark = "收藏数下限"
    override val filterUgoira = "动图筛选"
    override val filterDateRange = "日期范围"
    override val filterStartDate = "起始日期"
    override val filterEndDate = "结束日期"

    override val views = "浏览"
    override val bookmarks = "收藏"
    override val publishDate = "发布时间"
    override val tags = "标签"
    override val author = "画师"
    override val comments = "查看评论"
    override val relatedIllusts = "相关作品"
    override val noComments = "暂无评论"
}

/**
 * 繁體中文 (zh-TW)
 */
object ZhTwStrings : AppStrings {
    override val tabRecommend = "推薦"
    override val tabRanking = "排行"
    override val tabNew = "動態"
    override val tabSearch = "搜尋"
    override val tabSpotlight = "特輯"
    override val tabSettings = "設定"

    override val back = "返回"
    override val cancel = "取消"
    override val confirm = "確定"
    override val complete = "完成"
    override val refresh = "重新整理"
    override val retry = "重試"
    override val share = "分享"
    override val download = "下載"
    override val bookmark = "收藏"
    override val bookmarked = "已收藏"
    override val follow = "關注"
    override val followed = "已關注"
    override val copy = "複製"
    override val copiedToClipboard = "已複製到剪貼簿"
    override val openInBrowser = "在瀏覽器中開啟"
    override val loading = "載入中…"
    override val loadFailed = "載入失敗"
    override val noData = "暫無資料"

    override val spotlightTitle = "Pixivision 特輯"
    override val categoryAll = "全部"
    override val categoryIllust = "插畫"
    override val categoryManga = "漫畫"
    override val categoryNovel = "小說"
    override val spotlightLead = "導語"
    override val includedWorks = "收錄作品"
    override val includedArticles = "收錄特輯"
    override val viewArtworkDetail = "檢視作品詳情"
    override val viewOriginalArticle = "在瀏覽器中檢視 Pixivision 原文"

    override val guideTitle = "PixEz 引導精靈"
    override val guideStepLanguage = "選擇語言"
    override val guideStepLanguageDesc = "選擇您習慣的語言與介面文本"
    override val guideStepNetwork = "網路與鏡像加速"
    override val guideStepNetworkDesc = "根據連線環境選擇圖片加速源或直連"
    override val guideStepWelcome = "歡迎與帳號"
    override val guideStepWelcomeDesc = "登入 Pixiv 帳號以同步收藏與關注"
    override val guideLoggedInStatus = "已偵測到 Pixiv 登入帳號"
    override val guideSwitchAccount = "切換帳號或重新登入"
    override val guideNotLoggedIn = "尚未登入 Pixiv 帳號"
    override val guideLoginNow = "立即登入 Pixiv 帳號"
    override val guideSkipLogin = "稍後登入，直接體驗"
    override val guideStartJourney = "開啟 PixEz 之旅"
    override val guideNext = "下一步"
    override val guidePrev = "上一步"
    override val guideFinish = "完成"

    override val settingsTitle = "設定"
    override val settingLanguage = "語言設定"
    override val settingTheme = "主題與色彩"
    override val settingNetwork = "網路與鏡像源"
    override val settingDownload = "下載設定"
    override val settingSave = "儲存路徑與格式"
    override val settingLayout = "版面與欄數"
    override val settingCrossAdapter = "橫向與自適應"
    override val settingShield = "封鎖與過濾"
    override val settingAbout = "關於 PixEz"
    override val settingThanks = "致謝與贊助"
    override val settingGuide = "啟動引導精靈"
    override val settingGeneral = "通用設定"
    override val settingPlatform = "平台專屬設定"
    override val settingQuality = "畫質設定"
    override val settingDataExport = "資料備份與匯出"
    override val settingUpdate = "檢查更新"
    override val settingPrivacy = "隱私設定"
    override val settingFeed = "動態設定"
    override val settingWelcomePage = "啟動頁設定"
    override val settingCopyText = "複製文字設定"
    override val settingInteraction = "互動與手勢"
    override val settingUserShowAI = "AI 作品可見度"
    override val settingWidgetRecommend = "小工具推薦設定"

    override val dialogNeedLogin = "需要登入"
    override val dialogNeedLoginSummary = "目前未登入，登入後可使用完整功能"
    override val btnGoLogin = "去登入"
    override val btnCancelLogin = "暫不登入"
    override val menuMoreActions = "更多操作"
    override val menuCopyInfo = "複製資訊"
    override val menuCopyLink = "複製連結"
    override val menuShareLink = "分享連結"
    override val menuBanWork = "屏蔽作品"
    override val dialogAddTag = "新增收藏標籤"
    override val dialogAddTagSummary = "輸入要收藏的標籤名稱"
    override val dialogDeleteConfirm = "刪除確認"
    override val dialogDeleteConfirmSummary = "確定要刪除嗎？此操作無法復原。"
    override val btnDelete = "刪除"
    override val btnDeleting = "刪除中…"
    override val btnAdd = "新增"
    override val btnAdding = "新增中…"
    override val dialogSavePath = "儲存路徑"
    override val dialogSaveMode = "儲存模式"
    override val dialogSaveFormat = "儲存格式"
    override val dialogTaskCount = "同時下載任務數"
    override val dialogPickSeedColor = "選擇種子色"
    override val dialogPickSeedColorSummary = "點選預設顏色快速選擇，或輸入自訂 HEX 色值"
    override val dialogPaletteStyle = "調色盤風格"
    override val dialogPaletteStyleSummary = "選擇 Monet 動態取色的調色盤風格"
    override val dialogDisplayMode = "顯示模式"
    override val dialogNewVersionFound = "發現新版本"
    override val dialogNewVersionSummary = "偵測到 PixEz 有新版本發布"
    override val btnUpdate = "更新"
    override val btnIgnore = "忽略"
    override val batchActions = "批次操作"
    override val retryFailedTasks = "重試失敗任務"
    override val clearCompletedTasks = "清空已完成任務"

    override val searchFilter = "搜尋篩選"
    override val matchTarget = "符合目標"
    override val filterAi = "AI 作品篩選"
    override val filterBookmark = "收藏數下限"
    override val filterUgoira = "動圖篩選"
    override val filterDateRange = "日期範圍"
    override val filterStartDate = "起始日期"
    override val filterEndDate = "結束日期"

    override val views = "瀏覽"
    override val bookmarks = "收藏"
    override val publishDate = "發布時間"
    override val tags = "標籤"
    override val author = "繪師"
    override val comments = "檢視評論"
    override val relatedIllusts = "相關作品"
    override val noComments = "暫無評論"
}

/**
 * 英语 (en-US)
 */
object EnStrings : AppStrings {
    override val tabRecommend = "Recommend"
    override val tabRanking = "Ranking"
    override val tabNew = "Feeds"
    override val tabSearch = "Search"
    override val tabSpotlight = "Spotlight"
    override val tabSettings = "Settings"

    override val back = "Back"
    override val cancel = "Cancel"
    override val confirm = "OK"
    override val complete = "Complete"
    override val refresh = "Refresh"
    override val retry = "Retry"
    override val share = "Share"
    override val download = "Download"
    override val bookmark = "Bookmark"
    override val bookmarked = "Bookmarked"
    override val follow = "Follow"
    override val followed = "Followed"
    override val copy = "Copy"
    override val copiedToClipboard = "Copied to clipboard"
    override val openInBrowser = "Open in browser"
    override val loading = "Loading..."
    override val loadFailed = "Failed to load"
    override val noData = "No data"

    override val spotlightTitle = "Pixivision Spotlight"
    override val categoryAll = "All"
    override val categoryIllust = "Illust"
    override val categoryManga = "Manga"
    override val categoryNovel = "Novel"
    override val spotlightLead = "Introduction"
    override val includedWorks = "Featured Artworks"
    override val includedArticles = "Featured Articles"
    override val viewArtworkDetail = "View Artwork Details"
    override val viewOriginalArticle = "View original article on Pixivision"

    override val guideTitle = "PixEz Onboarding"
    override val guideStepLanguage = "Language"
    override val guideStepLanguageDesc = "Choose your preferred interface language"
    override val guideStepNetwork = "Network & Mirror"
    override val guideStepNetworkDesc = "Configure image proxy or direct connection"
    override val guideStepWelcome = "Welcome & Account"
    override val guideStepWelcomeDesc = "Log in with your Pixiv account to sync bookmarks"
    override val guideLoggedInStatus = "Active Pixiv Account Detected"
    override val guideSwitchAccount = "Switch Account / Re-login"
    override val guideNotLoggedIn = "Not Logged in to Pixiv"
    override val guideLoginNow = "Log in to Pixiv"
    override val guideSkipLogin = "Skip for now"
    override val guideStartJourney = "Start Exploring PixEz"
    override val guideNext = "Next"
    override val guidePrev = "Previous"
    override val guideFinish = "Finish"

    override val settingsTitle = "Settings"
    override val settingLanguage = "Language"
    override val settingTheme = "Theme & Color"
    override val settingNetwork = "Network & Proxy"
    override val settingDownload = "Downloads"
    override val settingSave = "Save Path & Format"
    override val settingLayout = "Layout & Columns"
    override val settingCrossAdapter = "Cross Adapter"
    override val settingShield = "Block & Filter"
    override val settingAbout = "About PixEz"
    override val settingThanks = "Thanks & Donors"
    override val settingGuide = "Launch Onboarding Wizard"

    override val views = "Views"
    override val bookmarks = "Bookmarks"
    override val publishDate = "Date"
    override val tags = "Tags"
    override val author = "Artist"
    override val comments = "Comments"
    override val relatedIllusts = "Related Artworks"
    override val noComments = "No comments yet"
}

/**
 * 日语 (ja)
 */
object JaStrings : AppStrings {
    override val tabRecommend = "おすすめ"
    override val tabRanking = "ランキング"
    override val tabNew = "フォロー新着"
    override val tabSearch = "検索"
    override val tabSpotlight = "特集"
    override val tabSettings = "設定"

    override val back = "戻る"
    override val cancel = "キャンセル"
    override val confirm = "確定"
    override val complete = "完了"
    override val refresh = "更新"
    override val retry = "再試行"
    override val share = "共有"
    override val download = "ダウンロード"
    override val bookmark = "ブックマーク"
    override val bookmarked = "ブックマーク済み"
    override val follow = "フォロー"
    override val followed = "フォロー中"
    override val copy = "コピー"
    override val copiedToClipboard = "クリップボードにコピーしました"
    override val openInBrowser = "ブラウザで開く"
    override val loading = "読み込み中…"
    override val loadFailed = "読み込み失敗"
    override val noData = "データがありません"

    override val spotlightTitle = "Pixivision 特集"
    override val categoryAll = "すべて"
    override val categoryIllust = "イラスト"
    override val categoryManga = "マンガ"
    override val categoryNovel = "小説"
    override val spotlightLead = "紹介文"
    override val includedWorks = "掲載作品"
    override val includedArticles = "掲載特集"
    override val viewArtworkDetail = "作品詳細を見る"
    override val viewOriginalArticle = "Pixivisionで元の記事を見る"

    override val guideTitle = "PixEz 初期設定ガイド"
    override val guideStepLanguage = "言語設定"
    override val guideStepLanguageDesc = "利用する言語を選択してください"
    override val guideStepNetwork = "ネットワーク設定"
    override val guideStepNetworkDesc = "画像プロキシまたはダイレクト接続を選択"
    override val guideStepWelcome = "アカウント設定"
    override val guideStepWelcomeDesc = "Pixivアカウントにログインして同期"
    override val guideLoggedInStatus = "ログイン済みのアカウントが見つかりました"
    override val guideSwitchAccount = "アカウント切り替え / 再ログイン"
    override val guideNotLoggedIn = "Pixivアカウント未ログイン"
    override val guideLoginNow = "Pixivアカウントにログイン"
    override val guideSkipLogin = "ログインせずに始める"
    override val guideStartJourney = "PixEzを始める"
    override val guideNext = "次へ"
    override val guidePrev = "前へ"
    override val guideFinish = "完了"

    override val settingsTitle = "設定"
    override val settingLanguage = "言語設定"
    override val settingTheme = "テーマとカラー"
    override val settingNetwork = "ネットワーク設定"
    override val settingDownload = "ダウンロード設定"
    override val settingSave = "保存パスとフォーマット"
    override val settingLayout = "レイアウト設定"
    override val settingCrossAdapter = "アダプティブ設定"
    override val settingShield = "フィルターとブロック"
    override val settingAbout = "PixEzについて"
    override val settingThanks = "謝辞・寄付"
    override val settingGuide = "初期設定ガイドを起動"

    override val views = "閲覧数"
    override val bookmarks = "ブックマーク数"
    override val publishDate = "投稿日時"
    override val tags = "タグ"
    override val author = "作者"
    override val comments = "コメント一覧"
    override val relatedIllusts = "関連作品"
    override val noComments = "コメントはありません"
}

/**
 * 韩语 (ko)
 */
object KoStrings : AppStrings {
    override val tabRecommend = "추천"
    override val tabRanking = "랭킹"
    override val tabNew = "피드"
    override val tabSearch = "검색"
    override val tabSpotlight = "특집"
    override val tabSettings = "설정"

    override val back = "뒤로"
    override val cancel = "취소"
    override val confirm = "확인"
    override val complete = "완료"
    override val refresh = "새로고침"
    override val retry = "다시 시도"
    override val share = "공유"
    override val download = "다운로드"
    override val bookmark = "북마크"
    override val bookmarked = "북마크됨"
    override val follow = "팔로우"
    override val followed = "팔로잉"
    override val copy = "복사"
    override val copiedToClipboard = "클립보드에 복사되었습니다"
    override val openInBrowser = "브라우저에서 열기"
    override val loading = "로딩 중…"
    override val loadFailed = "로드 실패"
    override val noData = "데이터 없음"

    override val spotlightTitle = "Pixivision 특집"
    override val categoryAll = "전체"
    override val categoryIllust = "일러스트"
    override val categoryManga = "만화"
    override val categoryNovel = "소설"
    override val spotlightLead = "소개"
    override val includedWorks = "수록 작품"
    override val includedArticles = "수록 특집"
    override val viewArtworkDetail = "작품 상세 보기"
    override val viewOriginalArticle = "Pixivision에서 원문 보기"

    override val guideTitle = "PixEz 초기 설정 가이드"
    override val guideStepLanguage = "언어 선택"
    override val guideStepLanguageDesc = "선호하는 인터페이스 언어를 선택하세요"
    override val guideStepNetwork = "네트워크 & 프록시"
    override val guideStepNetworkDesc = "이미지 가속 소스 또는 직접 연결 설정"
    override val guideStepWelcome = "환영 & 계정"
    override val guideStepWelcomeDesc = "북마크 동기화를 위해 Pixiv 계정으로 로그인하세요"
    override val guideLoggedInStatus = "로그인된 Pixiv 계정이 감지되었습니다"
    override val guideSwitchAccount = "계정 전환 / 다시 로그인"
    override val guideNotLoggedIn = "Pixiv 계정 미로그인"
    override val guideLoginNow = "Pixiv 계정 로그인"
    override val guideSkipLogin = "나중에 로그인하고 시작"
    override val guideStartJourney = "PixEz 시작하기"
    override val guideNext = "다음"
    override val guidePrev = "이전"
    override val guideFinish = "완료"

    override val settingsTitle = "설정"
    override val settingLanguage = "언어 설정"
    override val settingTheme = "테마 및 색상"
    override val settingNetwork = "네트워크 및 프록시"
    override val settingDownload = "다운로드 설정"
    override val settingSave = "저장 경로 및 형식"
    override val settingLayout = "레이아웃 및 열 수"
    override val settingCrossAdapter = "화면 적응 설정"
    override val settingShield = "차단 및 필터"
    override val settingAbout = "PixEz 정보"
    override val settingThanks = "감사의 글 및 후원"
    override val settingGuide = "초기 설정 가이드 실행"

    override val views = "조회수"
    override val bookmarks = "북마크"
    override val publishDate = "게시일"
    override val tags = "태그"
    override val author = "작가"
    override val comments = "댓글 보기"
    override val relatedIllusts = "관련 작품"
    override val noComments = "댓글이 없습니다"
}

/**
 * 俄语 (ru)
 */
object RuStrings : AppStrings by EnStrings {
    override val tabRecommend = "Рекомендации"
    override val tabRanking = "Рейтинг"
    override val tabNew = "Лента"
    override val tabSearch = "Поиск"
    override val tabSpotlight = "Подборки"
    override val tabSettings = "Настройки"
    override val back = "Назад"
    override val cancel = "Отмена"
    override val confirm = "ОК"
    override val complete = "Готово"
    override val refresh = "Обновить"
    override val retry = "Повторить"
    override val share = "Поделиться"
    override val download = "Скачать"
    override val bookmark = "В закладки"
    override val bookmarked = "В закладках"
    override val follow = "Подписаться"
    override val followed = "Вы подписаны"
}

/**
 * 西班牙语 (es)
 */
object EsStrings : AppStrings by EnStrings {
    override val tabRecommend = "Recomendados"
    override val tabRanking = "Clasificación"
    override val tabNew = "Novedades"
    override val tabSearch = "Buscar"
    override val tabSpotlight = "Especiales"
    override val tabSettings = "Ajustes"
    override val back = "Atrás"
    override val cancel = "Cancelar"
    override val confirm = "Aceptar"
    override val complete = "Completado"
    override val refresh = "Actualizar"
    override val retry = "Reintentar"
    override val share = "Compartir"
    override val download = "Descargar"
    override val bookmark = "Guardar"
    override val bookmarked = "Guardado"
}

/**
 * 土耳其语 (tr)
 */
object TrStrings : AppStrings by EnStrings {
    override val tabRecommend = "Önerilenler"
    override val tabRanking = "Sıralama"
    override val tabNew = "Akış"
    override val tabSearch = "Ara"
    override val tabSpotlight = "Öne Çıkanlar"
    override val tabSettings = "Ayarlar"
}

/**
 * 印尼语 (id)
 */
object IdStrings : AppStrings by EnStrings {
    override val tabRecommend = "Rekomendasi"
    override val tabRanking = "Peringkat"
    override val tabNew = "Terbaru"
    override val tabSearch = "Cari"
    override val tabSpotlight = "Sorotan"
    override val tabSettings = "Pengaturan"
}

/**
 * 菲律宾语 (fil)
 */
object FilStrings : AppStrings by EnStrings {
    override val tabRecommend = "Rekomendado"
    override val tabRanking = "Ranggo"
    override val tabNew = "Mga Bago"
    override val tabSearch = "Maghanap"
    override val tabSpotlight = "Tampok"
    override val tabSettings = "Mga Setting"
}

/**
 * 德语 (de)
 */
object DeStrings : AppStrings by EnStrings {
    override val tabRecommend = "Empfohlen"
    override val tabRanking = "Rangliste"
    override val tabNew = "Neuigkeiten"
    override val tabSearch = "Suche"
    override val tabSpotlight = "Spotlight"
    override val tabSettings = "Einstellungen"
}

val LocalStrings = staticCompositionLocalOf<AppStrings> { ZhCnStrings }
