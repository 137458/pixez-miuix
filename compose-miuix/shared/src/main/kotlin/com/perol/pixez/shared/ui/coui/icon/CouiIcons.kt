package com.perol.pixez.shared.ui.coui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * COUI 专属线性图标集合（24dp / 24x24 viewport，Lucide 描边风格：stroke=2f、round cap/join）。
 *
 * 彻底解耦 COUI 皮肤对 MIUIX 图标集的依赖，与 ColorOS 轻盈纤细的界面语言保持一致。
 * [HeartFill] 及底栏选中态图标（`GridFill`、`RankFill`、`RecentFill`、`CompassFill`）采用实心填充，
 * 其余全部为纯描边线性图标。
 */
internal object CouiIcons {

    private inline fun strokeIcon(
        name: String,
        autoMirror: Boolean = false,
        crossinline block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = "CouiIcons.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }.build()

    private inline fun fillIcon(
        name: String,
        pathFillType: PathFillType = PathFillType.NonZero,
        strokeWidth: Float = 0f,
        crossinline block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = "CouiIcons.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            stroke = if (strokeWidth > 0f) SolidColor(Color.Black) else null,
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = pathFillType,
            pathBuilder = block,
        )
    }.build()

    /** `layout-grid` — 推荐 / 宫格 */
    val Grid: ImageVector = strokeIcon("Grid") {
        moveTo(4f, 3f)
        horizontalLineTo(9f)
        arcTo(1f, 1f, 0f, false, true, 10f, 4f)
        verticalLineTo(9f)
        arcTo(1f, 1f, 0f, false, true, 9f, 10f)
        horizontalLineTo(4f)
        arcTo(1f, 1f, 0f, false, true, 3f, 9f)
        verticalLineTo(4f)
        arcTo(1f, 1f, 0f, false, true, 4f, 3f)
        close()
        moveTo(15f, 3f)
        horizontalLineTo(20f)
        arcTo(1f, 1f, 0f, false, true, 21f, 4f)
        verticalLineTo(9f)
        arcTo(1f, 1f, 0f, false, true, 20f, 10f)
        horizontalLineTo(15f)
        arcTo(1f, 1f, 0f, false, true, 14f, 9f)
        verticalLineTo(4f)
        arcTo(1f, 1f, 0f, false, true, 15f, 3f)
        close()
        moveTo(15f, 14f)
        horizontalLineTo(20f)
        arcTo(1f, 1f, 0f, false, true, 21f, 15f)
        verticalLineTo(20f)
        arcTo(1f, 1f, 0f, false, true, 20f, 21f)
        horizontalLineTo(15f)
        arcTo(1f, 1f, 0f, false, true, 14f, 20f)
        verticalLineTo(15f)
        arcTo(1f, 1f, 0f, false, true, 15f, 14f)
        close()
        moveTo(4f, 14f)
        horizontalLineTo(9f)
        arcTo(1f, 1f, 0f, false, true, 10f, 15f)
        verticalLineTo(20f)
        arcTo(1f, 1f, 0f, false, true, 9f, 21f)
        horizontalLineTo(4f)
        arcTo(1f, 1f, 0f, false, true, 3f, 20f)
        verticalLineTo(15f)
        arcTo(1f, 1f, 0f, false, true, 4f, 14f)
        close()
    }

    /** `layout-grid` — 推荐 / 宫格（实心选中态） */
    val GridFill: ImageVector = fillIcon("GridFill", strokeWidth = 1.5f) {
        moveTo(4f, 3f)
        horizontalLineTo(9f)
        arcTo(1f, 1f, 0f, false, true, 10f, 4f)
        verticalLineTo(9f)
        arcTo(1f, 1f, 0f, false, true, 9f, 10f)
        horizontalLineTo(4f)
        arcTo(1f, 1f, 0f, false, true, 3f, 9f)
        verticalLineTo(4f)
        arcTo(1f, 1f, 0f, false, true, 4f, 3f)
        close()
        moveTo(15f, 3f)
        horizontalLineTo(20f)
        arcTo(1f, 1f, 0f, false, true, 21f, 4f)
        verticalLineTo(9f)
        arcTo(1f, 1f, 0f, false, true, 20f, 10f)
        horizontalLineTo(15f)
        arcTo(1f, 1f, 0f, false, true, 14f, 9f)
        verticalLineTo(4f)
        arcTo(1f, 1f, 0f, false, true, 15f, 3f)
        close()
        moveTo(15f, 14f)
        horizontalLineTo(20f)
        arcTo(1f, 1f, 0f, false, true, 21f, 15f)
        verticalLineTo(20f)
        arcTo(1f, 1f, 0f, false, true, 20f, 21f)
        horizontalLineTo(15f)
        arcTo(1f, 1f, 0f, false, true, 14f, 20f)
        verticalLineTo(15f)
        arcTo(1f, 1f, 0f, false, true, 15f, 14f)
        close()
        moveTo(4f, 14f)
        horizontalLineTo(9f)
        arcTo(1f, 1f, 0f, false, true, 10f, 15f)
        verticalLineTo(20f)
        arcTo(1f, 1f, 0f, false, true, 9f, 21f)
        horizontalLineTo(4f)
        arcTo(1f, 1f, 0f, false, true, 3f, 20f)
        verticalLineTo(15f)
        arcTo(1f, 1f, 0f, false, true, 4f, 14f)
        close()
    }

    /** `trophy` — 排行榜 */
    val Rank: ImageVector = strokeIcon("Rank") {
        moveTo(10f, 14.66f)
        verticalLineTo(17f)
        arcTo(1f, 1f, 0f, false, true, 9f, 18f)
        arcTo(2f, 2f, 0f, false, false, 7f, 20f)
        verticalLineTo(22f)
        moveTo(14f, 14.66f)
        verticalLineTo(17f)
        arcTo(1f, 1f, 0f, false, false, 15f, 18f)
        arcTo(2f, 2f, 0f, false, true, 17f, 20f)
        verticalLineTo(22f)
        moveTo(17.916f, 10f)
        horizontalLineTo(19.5f)
        arcTo(2.5f, 2.5f, 0f, false, false, 22f, 7.5f)
        verticalLineTo(5f)
        arcTo(1f, 1f, 0f, false, false, 21f, 4f)
        horizontalLineTo(18f)
        moveTo(4f, 22f)
        horizontalLineTo(20f)
        moveTo(6f, 9f)
        arcTo(6f, 6f, 0f, false, false, 18f, 9f)
        verticalLineTo(3f)
        arcTo(1f, 1f, 0f, false, false, 17f, 2f)
        horizontalLineTo(7f)
        arcTo(1f, 1f, 0f, false, false, 6f, 3f)
        close()
        moveTo(6.084f, 10f)
        horizontalLineTo(4.5f)
        arcTo(2.5f, 2.5f, 0f, false, true, 2f, 7.5f)
        verticalLineTo(5f)
        arcTo(1f, 1f, 0f, false, true, 3f, 4f)
        horizontalLineTo(6f)
    }

    /** `trophy` — 排行榜（实心选中态） */
    val RankFill: ImageVector = ImageVector.Builder(
        name = "CouiIcons.RankFill",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(6f, 9f)
            arcTo(6f, 6f, 0f, false, false, 18f, 9f)
            verticalLineTo(3f)
            arcTo(1f, 1f, 0f, false, false, 17f, 2f)
            horizontalLineTo(7f)
            arcTo(1f, 1f, 0f, false, false, 6f, 3f)
            close()
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(10f, 14.66f)
            verticalLineTo(17f)
            arcTo(1f, 1f, 0f, false, true, 9f, 18f)
            arcTo(2f, 2f, 0f, false, false, 7f, 20f)
            verticalLineTo(22f)
            moveTo(14f, 14.66f)
            verticalLineTo(17f)
            arcTo(1f, 1f, 0f, false, false, 15f, 18f)
            arcTo(2f, 2f, 0f, false, true, 17f, 20f)
            verticalLineTo(22f)
            moveTo(17.916f, 10f)
            horizontalLineTo(19.5f)
            arcTo(2.5f, 2.5f, 0f, false, false, 22f, 7.5f)
            verticalLineTo(5f)
            arcTo(1f, 1f, 0f, false, false, 21f, 4f)
            horizontalLineTo(18f)
            moveTo(4f, 22f)
            horizontalLineTo(20f)
            moveTo(6.084f, 10f)
            horizontalLineTo(4.5f)
            arcTo(2.5f, 2.5f, 0f, false, true, 2f, 7.5f)
            verticalLineTo(5f)
            arcTo(1f, 1f, 0f, false, true, 3f, 4f)
            horizontalLineTo(6f)
        }
    }.build()

    /** `compass` — Spotlight 发现特辑 */
    val Compass: ImageVector = strokeIcon("Compass") {
        moveTo(2f, 12f)
        arcTo(10f, 10f, 0f, true, false, 22f, 12f)
        arcTo(10f, 10f, 0f, true, false, 2f, 12f)
        close()
        moveTo(16.24f, 7.76f)
        lineTo(14.12f, 14.12f)
        lineTo(7.76f, 16.24f)
        lineTo(9.88f, 9.88f)
        close()
    }

    /** `compass` — Spotlight 发现特辑（实心选中态） */
    val CompassFill: ImageVector = fillIcon("CompassFill", pathFillType = PathFillType.EvenOdd) {
        moveTo(2f, 12f)
        arcTo(10f, 10f, 0f, true, false, 22f, 12f)
        arcTo(10f, 10f, 0f, true, false, 2f, 12f)
        close()
        moveTo(16.24f, 7.76f)
        lineTo(14.12f, 14.12f)
        lineTo(7.76f, 16.24f)
        lineTo(9.88f, 9.88f)
        close()
    }

    /** `chevron-left` — 返回 */
    val Back: ImageVector = strokeIcon("Back", autoMirror = true) {
        moveTo(15f, 18f)
        lineTo(9f, 12f)
        lineTo(15f, 6f)
    }

    /** `chevron-right` — 前进 / 展开 */
    val ChevronForward: ImageVector = strokeIcon("ChevronForward", autoMirror = true) {
        moveTo(9f, 18f)
        lineTo(15f, 12f)
        lineTo(9f, 6f)
    }

    /** `chevron-up` — 上移 / 收起 */
    val ExpandLess: ImageVector = strokeIcon("ExpandLess") {
        moveTo(18f, 15f)
        lineTo(12f, 9f)
        lineTo(6f, 15f)
    }

    /** `chevron-down` — 下移 / 展开 */
    val ExpandMore: ImageVector = strokeIcon("ExpandMore") {
        moveTo(6f, 9f)
        lineTo(12f, 15f)
        lineTo(18f, 9f)
    }

    /** `x` — 关闭 / 清空 */
    val Close: ImageVector = strokeIcon("Close") {
        moveTo(18f, 6f)
        lineTo(6f, 18f)
        moveTo(6f, 6f)
        lineTo(18f, 18f)
    }

    /** `plus` — 添加 */
    val Add: ImageVector = strokeIcon("Add") {
        moveTo(5f, 12f)
        horizontalLineTo(19f)
        moveTo(12f, 5f)
        verticalLineTo(19f)
    }

    /** `circle-plus` — 推荐画师 / 圆形添加 */
    val AddCircle: ImageVector = strokeIcon("AddCircle") {
        moveTo(2f, 12f)
        arcTo(10f, 10f, 0f, true, false, 22f, 12f)
        arcTo(10f, 10f, 0f, true, false, 2f, 12f)
        close()
        moveTo(8f, 12f)
        horizontalLineTo(16f)
        moveTo(12f, 8f)
        verticalLineTo(16f)
    }

    /** `minus` — 减少 */
    val Remove: ImageVector = strokeIcon("Remove") {
        moveTo(5f, 12f)
        horizontalLineTo(19f)
    }

    /** `user-round` — 用户 / 登录 */
    val Contacts: ImageVector = strokeIcon("Contacts") {
        moveTo(7f, 8f)
        arcTo(5f, 5f, 0f, true, false, 17f, 8f)
        arcTo(5f, 5f, 0f, true, false, 7f, 8f)
        close()
        moveTo(20f, 21f)
        arcTo(8f, 8f, 0f, false, false, 4f, 21f)
    }

    /** `trash-2` — 删除 */
    val Delete: ImageVector = strokeIcon("Delete") {
        moveTo(3f, 6f)
        horizontalLineTo(21f)
        moveTo(19f, 6f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 17f, 22f)
        horizontalLineTo(7f)
        arcTo(2f, 2f, 0f, false, true, 5f, 20f)
        verticalLineTo(6f)
        moveTo(8f, 6f)
        verticalLineTo(4f)
        arcTo(2f, 2f, 0f, false, true, 10f, 2f)
        horizontalLineTo(14f)
        arcTo(2f, 2f, 0f, false, true, 16f, 4f)
        verticalLineTo(6f)
        moveTo(10f, 11f)
        verticalLineTo(17f)
        moveTo(14f, 11f)
        verticalLineTo(17f)
    }

    /** `download` — 下载 */
    val Download: ImageVector = strokeIcon("Download") {
        moveTo(12f, 15f)
        verticalLineTo(3f)
        moveTo(21f, 15f)
        verticalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 19f, 21f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 19f)
        verticalLineTo(15f)
        moveTo(7f, 10f)
        lineTo(12f, 15f)
        lineTo(17f, 10f)
    }

    /** `heart` — 收藏（描边） */
    val Heart: ImageVector = strokeIcon("Heart") {
        moveTo(2f, 9.5f)
        arcTo(5.5f, 5.5f, 0f, false, true, 11.591f, 5.824f)
        arcTo(0.56f, 0.56f, 0f, false, false, 12.409f, 5.824f)
        arcTo(5.49f, 5.49f, 0f, false, true, 22f, 9.5f)
        curveTo(22f, 11.79f, 20.5f, 13.5f, 19f, 15f)
        lineTo(13.508f, 20.313f)
        arcTo(2f, 2f, 0f, false, true, 10.508f, 20.332f)
        lineTo(5f, 15f)
        curveTo(3.5f, 13.5f, 2f, 11.8f, 2f, 9.5f)
        close()
    }

    /** `heart` — 已收藏（实心填充） */
    val HeartFill: ImageVector = ImageVector.Builder(
        name = "CouiIcons.HeartFill",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(2f, 9.5f)
            arcTo(5.5f, 5.5f, 0f, false, true, 11.591f, 5.824f)
            arcTo(0.56f, 0.56f, 0f, false, false, 12.409f, 5.824f)
            arcTo(5.49f, 5.49f, 0f, false, true, 22f, 9.5f)
            curveTo(22f, 11.79f, 20.5f, 13.5f, 19f, 15f)
            lineTo(13.508f, 20.313f)
            arcTo(2f, 2f, 0f, false, true, 10.508f, 20.332f)
            lineTo(5f, 15f)
            curveTo(3.5f, 13.5f, 2f, 11.8f, 2f, 9.5f)
            close()
        }
    }.build()

    /** `eye` — 浏览 / 显示密码 */
    val Show: ImageVector = strokeIcon("Show") {
        moveTo(2.062f, 12.348f)
        arcTo(1f, 1f, 0f, false, true, 2.062f, 11.652f)
        curveTo(3.5f, 7.6f, 7.36f, 5f, 12f, 5f)
        curveTo(16.64f, 5f, 20.5f, 7.6f, 21.938f, 11.652f)
        arcTo(1f, 1f, 0f, false, true, 21.938f, 12.348f)
        curveTo(20.5f, 16.4f, 16.64f, 19f, 12f, 19f)
        curveTo(7.36f, 19f, 3.5f, 16.4f, 2.062f, 12.348f)
        close()
        moveTo(9f, 12f)
        arcTo(3f, 3f, 0f, true, false, 15f, 12f)
        arcTo(3f, 3f, 0f, true, false, 9f, 12f)
        close()
    }

    /** `eye-off` — 隐藏密码 */
    val Hide: ImageVector = strokeIcon("Hide") {
        moveTo(10.733f, 5.076f)
        arcTo(10.744f, 10.744f, 0f, false, true, 12f, 5f)
        curveTo(16.64f, 5f, 20.5f, 7.6f, 21.938f, 11.652f)
        arcTo(1f, 1f, 0f, false, true, 21.938f, 12.348f)
        arcTo(10.5f, 10.5f, 0f, false, true, 19.7f, 15.8f)
        moveTo(14.084f, 14.158f)
        arcTo(3f, 3f, 0f, false, true, 9.842f, 9.916f)
        moveTo(17.479f, 17.499f)
        arcTo(10.75f, 10.75f, 0f, false, true, 12f, 19f)
        curveTo(7.36f, 19f, 3.5f, 16.4f, 2.062f, 12.348f)
        arcTo(1f, 1f, 0f, false, true, 2.062f, 11.652f)
        arcTo(10.5f, 10.5f, 0f, false, true, 6.51f, 6.51f)
        moveTo(2f, 2f)
        lineTo(22f, 22f)
    }

    /** `message-circle` — 评论 / 消息 */
    val Messages: ImageVector = strokeIcon("Messages") {
        moveTo(2.992f, 16.342f)
        arcTo(2f, 2f, 0f, false, true, 3.086f, 17.509f)
        lineTo(2.021f, 20.799f)
        arcTo(1f, 1f, 0f, false, false, 3.257f, 21.967f)
        lineTo(6.67f, 20.969f)
        arcTo(2f, 2f, 0f, false, true, 7.769f, 21.061f)
        arcTo(10f, 10f, 0f, true, false, 2.992f, 16.342f)
    }

    /** `ellipsis-vertical` — 更多菜单 */
    val More: ImageVector = strokeIcon("More") {
        moveTo(12f, 5f)
        arcTo(1f, 1f, 0f, true, false, 12.01f, 5f)
        close()
        moveTo(12f, 12f)
        arcTo(1f, 1f, 0f, true, false, 12.01f, 12f)
        close()
        moveTo(12f, 19f)
        arcTo(1f, 1f, 0f, true, false, 12.01f, 19f)
        close()
    }

    /** `check` — 确认 / 成功 */
    val Ok: ImageVector = strokeIcon("Ok") {
        moveTo(20f, 6f)
        lineTo(9f, 17f)
        lineTo(4f, 12f)
    }

    /** `clock` — 时间 / 动态 / 日期 */
    val Recent: ImageVector = strokeIcon("Recent") {
        moveTo(2f, 12f)
        arcTo(10f, 10f, 0f, true, false, 22f, 12f)
        arcTo(10f, 10f, 0f, true, false, 2f, 12f)
        close()
        moveTo(12f, 6f)
        verticalLineTo(12f)
        lineTo(16f, 14f)
    }

    /** `clock` — 时间 / 动态（实心选中态） */
    val RecentFill: ImageVector = fillIcon("RecentFill", pathFillType = PathFillType.EvenOdd) {
        moveTo(2f, 12f)
        arcTo(10f, 10f, 0f, true, false, 22f, 12f)
        arcTo(10f, 10f, 0f, true, false, 2f, 12f)
        close()
        moveTo(11f, 6.5f)
        lineTo(13f, 6.5f)
        lineTo(13f, 11.4f)
        lineTo(16.45f, 13.11f)
        lineTo(15.55f, 14.89f)
        lineTo(11f, 12.6f)
        close()
    }

    /** `refresh-cw` — 刷新 */
    val Refresh: ImageVector = strokeIcon("Refresh") {
        moveTo(3f, 12f)
        arcTo(9f, 9f, 0f, false, true, 12f, 3f)
        arcTo(9.75f, 9.75f, 0f, false, true, 18.74f, 5.74f)
        lineTo(21f, 8f)
        moveTo(21f, 3f)
        verticalLineTo(8f)
        horizontalLineTo(16f)
        moveTo(21f, 12f)
        arcTo(9f, 9f, 0f, false, true, 12f, 21f)
        arcTo(9.75f, 9.75f, 0f, false, true, 5.26f, 18.26f)
        lineTo(3f, 16f)
        moveTo(8f, 16f)
        horizontalLineTo(3f)
        verticalLineTo(21f)
    }

    /** `circle-alert` — 警告 / 失败 */
    val Report: ImageVector = strokeIcon("Report") {
        moveTo(2f, 12f)
        arcTo(10f, 10f, 0f, true, false, 22f, 12f)
        arcTo(10f, 10f, 0f, true, false, 2f, 12f)
        close()
        moveTo(12f, 8f)
        verticalLineTo(12f)
        moveTo(12f, 16f)
        horizontalLineTo(12.01f)
    }

    /** `search` — 搜索 */
    val Search: ImageVector = strokeIcon("Search") {
        moveTo(21f, 21f)
        lineTo(16.66f, 16.66f)
        moveTo(3f, 11f)
        arcTo(8f, 8f, 0f, true, false, 19f, 11f)
        arcTo(8f, 8f, 0f, true, false, 3f, 11f)
        close()
    }

    /** `send` — 发送评论 */
    val Send: ImageVector = strokeIcon("Send") {
        moveTo(22f, 2f)
        lineTo(11f, 13f)
        moveTo(22f, 2f)
        lineTo(15f, 22f)
        lineTo(11f, 13f)
        lineTo(2f, 9f)
        close()
    }

    /** `settings` — 设置 */
    val Settings: ImageVector = strokeIcon("Settings") {
        moveTo(9.671f, 4.136f)
        arcTo(2.34f, 2.34f, 0f, false, true, 14.33f, 4.136f)
        arcTo(2.34f, 2.34f, 0f, false, false, 17.649f, 6.051f)
        arcTo(2.34f, 2.34f, 0f, false, true, 19.979f, 10.084f)
        arcTo(2.34f, 2.34f, 0f, false, false, 19.979f, 13.915f)
        arcTo(2.34f, 2.34f, 0f, false, true, 17.649f, 17.948f)
        arcTo(2.34f, 2.34f, 0f, false, false, 14.33f, 19.863f)
        arcTo(2.34f, 2.34f, 0f, false, true, 9.671f, 19.863f)
        arcTo(2.34f, 2.34f, 0f, false, false, 6.351f, 17.948f)
        arcTo(2.34f, 2.34f, 0f, false, true, 4.021f, 13.915f)
        arcTo(2.34f, 2.34f, 0f, false, false, 4.021f, 10.084f)
        arcTo(2.34f, 2.34f, 0f, false, true, 6.35f, 6.051f)
        arcTo(2.34f, 2.34f, 0f, false, false, 9.669f, 4.136f)
        moveTo(9f, 12f)
        arcTo(3f, 3f, 0f, true, false, 15f, 12f)
        arcTo(3f, 3f, 0f, true, false, 9f, 12f)
        close()
    }

    /** `timer` — 等待 / 排队 */
    val Stopwatch: ImageVector = strokeIcon("Stopwatch") {
        moveTo(10f, 2f)
        horizontalLineTo(14f)
        moveTo(12f, 14f)
        lineTo(15f, 11f)
        moveTo(4f, 14f)
        arcTo(8f, 8f, 0f, true, false, 20f, 14f)
        arcTo(8f, 8f, 0f, true, false, 4f, 14f)
        close()
    }

    /** `share` — 分享 */
    val Share: ImageVector = strokeIcon("Share") {
        moveTo(12f, 2f)
        verticalLineTo(15f)
        moveTo(16f, 6f)
        lineTo(12f, 2f)
        lineTo(8f, 6f)
        moveTo(4f, 12f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, false, 6f, 22f)
        horizontalLineTo(18f)
        arcTo(2f, 2f, 0f, false, false, 20f, 20f)
        verticalLineTo(12f)
    }

    /** `copy` — 复制 */
    val Copy: ImageVector = strokeIcon("Copy") {
        moveTo(10f, 8f)
        horizontalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 22f, 10f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 20f, 22f)
        horizontalLineTo(10f)
        arcTo(2f, 2f, 0f, false, true, 8f, 20f)
        verticalLineTo(10f)
        arcTo(2f, 2f, 0f, false, true, 10f, 8f)
        close()
        moveTo(4f, 16f)
        curveTo(2.9f, 16f, 2f, 15.1f, 2f, 14f)
        verticalLineTo(4f)
        curveTo(2f, 2.9f, 2.9f, 2f, 4f, 2f)
        horizontalLineTo(14f)
        curveTo(15.1f, 2f, 16f, 2.9f, 16f, 4f)
    }

    /** `columns-2` — 水平翻页模式 */
    val HorizontalPages: ImageVector = strokeIcon("HorizontalPages") {
        moveTo(5f, 3f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 5f)
        verticalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 19f, 21f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 19f)
        verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 5f, 3f)
        close()
        moveTo(12f, 3f)
        verticalLineTo(21f)
    }

    /** `rows-2` — 垂直卷轴模式 */
    val VerticalScroll: ImageVector = strokeIcon("VerticalScroll") {
        moveTo(5f, 3f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 5f)
        verticalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 19f, 21f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 19f)
        verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 5f, 3f)
        close()
        moveTo(3f, 12f)
        horizontalLineTo(21f)
    }

    /** `calendar` — 排行榜日期 */
    val Calendar: ImageVector = strokeIcon("Calendar") {
        moveTo(8f, 2f)
        verticalLineTo(6f)
        moveTo(16f, 2f)
        verticalLineTo(6f)
        moveTo(5f, 4f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 6f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 19f, 22f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 20f)
        verticalLineTo(6f)
        arcTo(2f, 2f, 0f, false, true, 5f, 4f)
        close()
        moveTo(3f, 10f)
        horizontalLineTo(21f)
    }
}
