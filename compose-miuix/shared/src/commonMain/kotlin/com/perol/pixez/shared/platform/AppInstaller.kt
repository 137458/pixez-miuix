package com.perol.pixez.shared.platform

/**
 * 跨平台应用安装与本地更新存储管理器。
 */
expect class AppInstaller() {
    /**
     * 打开并安装指定路径的应用安装包。
     *
     * @param filePath 安装包本地绝对路径。
     * @return 是否成功调起系统安装器或打开文件。
     */
    fun install(filePath: String): Boolean

    /**
     * 获取用于存放下载更新安装包的本地缓存目录路径。
     */
    fun getUpdateSaveDir(): String
}
