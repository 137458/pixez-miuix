package com.perol.pixez.shared.ui.navigation

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MetaSinglePage
import com.perol.pixez.shared.ui.state.IllustFeedSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 高-5 Main child 跨页恢复接线断言：consume/register 必须在组件初始化（createChild）执行，
 * 快照经 stateKeeper 序列化往返；并固化 Essenty 2.5.0「同 key 重复 register 抛
 * IllegalStateException」的行为——即接线禁止放入组合期 remember{}（栈动画换层重建组合即崩）。
 */
class MainFeedRestoreTest {

    private fun illust(id: Long): Illust = Illust(
        id = id,
        title = "Illust $id",
        type = "illust",
        imageUrls = ImageUrls(squareMedium = "", medium = "", large = ""),
        caption = "",
        restrict = 0,
        user = IllustUser(
            id = 1L,
            name = "Artist",
            account = "artist",
            profileImageUrls = IllustProfileImageUrls(medium = ""),
        ),
        tags = emptyList(),
        tools = emptyList(),
        createDate = "2026-10-11T00:00:00+09:00",
        pageCount = 1,
        width = 1000,
        height = 1000,
        sanityLevel = 2,
        xRestrict = 0,
        metaSinglePage = MetaSinglePage(originalImageUrl = ""),
        metaPages = emptyList(),
        totalView = 0,
        totalBookmarks = 0,
        isBookmarked = false,
        visible = true,
        isMuted = false,
        totalComments = 0,
        illustAIType = 0,
        illustBookStyle = 0,
    )

    @Test
    fun snapshotsRoundTripThroughStateKeeper() {
        val liveDispatcher = StateKeeperDispatcher()
        val liveRestore = createMainFeedRestore(liveDispatcher, InstanceKeeperDispatcher())
        liveRestore.hello.update(listOf(illust(1L), illust(2L)), "helloNext")
        liveRestore.ranking.update("day_", listOf(illust(3L)), null)

        // container 为一次性消费体（SerializableContainer.consume 清空 data），
        // 只能交给一个恢复侧 dispatcher，对齐生产中父级 save() 产物只被子级反序列化一次的行为。
        val container = liveDispatcher.save()

        // 模拟进程死亡恢复：新 dispatcher 以保存态构造，InstanceKeeper 同样为空
        val restored = createMainFeedRestore(StateKeeperDispatcher(container), InstanceKeeperDispatcher())

        assertEquals(listOf(1L, 2L), restored.hello.snapshot?.illustIds, "进程死亡恢复：推荐快照应经 stateKeeper 往返")
        assertEquals("helloNext", restored.hello.snapshot?.nextUrl)
        assertEquals(listOf(3L), restored.ranking.get("day_")?.illustIds, "进程死亡恢复：排行快照应经 stateKeeper 往返")
    }

    @Test
    fun savedSnapshotReflectsLatestWrites() {
        val dispatcher = StateKeeperDispatcher()
        val restore = createMainFeedRestore(dispatcher, InstanceKeeperDispatcher())

        restore.hello.update(listOf(illust(1L)), "first")
        dispatcher.save()
        // 二次写入后再次保存：register 的 supplier 必须每次保存读取最新快照
        restore.hello.update(listOf(illust(9L)), null)
        val latest = dispatcher.save()

        val restored = createMainFeedRestore(StateKeeperDispatcher(latest), InstanceKeeperDispatcher())
        assertEquals(listOf(9L), restored.hello.snapshot?.illustIds, "保存时必须导出最新快照而非注册时的旧值")
        assertEquals(null, restored.hello.snapshot?.nextUrl)
    }

    @Test
    fun essentyRawRoundTrip() {
        // 最小往返：隔离 Essenty 序列化容器与本项目快照类型，定位往返失败所在层。
        val d1 = StateKeeperDispatcher()
        d1.register("k", IllustFeedSnapshot.serializer()) { IllustFeedSnapshot(illustIds = listOf(5L), nextUrl = "n") }

        val saved = d1.save()
        val restored = StateKeeperDispatcher(saved).consume("k", IllustFeedSnapshot.serializer())

        assertEquals(listOf(5L), restored?.illustIds, "Essenty stateKeeper 基本往返必须工作")
    }

    @Test
    fun duplicateRegisterOnSameKeyThrows() {
        // 固化 Essenty 2.5.0 DefaultStateKeeperDispatcher.register 的行为：
        // 同 key 二次注册抛 IllegalStateException——接线因此必须放在组件初始化只执行一次。
        val dispatcher = StateKeeperDispatcher()
        dispatcher.register("key_feed_test", IllustFeedSnapshot.serializer()) { null }

        assertFailsWith<IllegalStateException> {
            dispatcher.register("key_feed_test", IllustFeedSnapshot.serializer()) { null }
        }
    }
}
