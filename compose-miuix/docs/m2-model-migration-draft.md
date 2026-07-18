# M2 Model Migration Draft

## 顶层说明与警告

- **已迁移、不要再重复生成的文件**：`compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/data/model` 下已有 `Account.kt`、`Illust.kt`、`UserDetail.kt`。本草案会直接引用其中的类型，不会再为 `account.dart` 的 `Account/AccountResponse/User/ProfileImageUrls`、`illust.dart` 的全部实体、`user_detail.dart` 的全部实体生成重复代码。
- **命名冲突处理**：多个 Dart 文件里都有局部定义的 `User`、`Tags`、`ProfileImageUrls`、`Body`、`ImageUrls` 等类。为在 Kotlin 侧避免冲突，本草案采用“文件名/场景前缀”重新命名，例如 `CommentUser`、`NovelUser`、`BookmarkDetailTag`、`CreateUserBody` 等。
- **不确定类型**：
  - `DateTime` 字段在 Dart 侧由 `json_serializable` 解析为 ISO-8601 字符串；本草案统一映射为 `String`，如需 `kotlinx.datetime.Instant` 可在实现阶段替换。
  - `dynamic` / `Object?` / 未知结构统一映射为 `kotlinx.serialization.json.JsonElement`。
  - `novel_web_response.dart` 中的 `NovelUrls` 字段名（`the240Mw` 等）没有 `@JsonKey`，实际 JSON key 可能是 `240mw`、`480mw` 等，需在联调时确认并补充 `@SerialName`。
  - `NovelWatchListModel`、`WatchlistMangaModel` 在 Dart 侧写了容错 `tryParse`，直接 `@Serializable` 会在元素格式异常时抛异常；如需要同样的容错行为，需要手写 `KSerializer` 或在仓库层先过滤。
- **Provider 类**：`AccountProvider`、`BanTagProvider`、`TaskPersistProvider` 等负责 `sqflite` 操作，不属于 JSON 模型，本草案不迁移。

---

## 1. 已迁移模型（仅引用，不重新生成）

| Dart 文件 | 已存在的 Kotlin 文件 | 已覆盖的类 |
|---|---|---|
| `account.dart` | `Account.kt` | `Account`, `AccountResponse`, `OAuthUser`, `OAuthProfileImageUrls` |
| `illust.dart` | `Illust.kt` | `Illust`, `ImageUrls`, `IllustUser`, `IllustProfileImageUrls`, `IllustTag`, `MetaSinglePage`, `MetaPage`, `MetaPageImageUrls`, `IllustSeries` |
| `user_detail.dart` | `UserDetail.kt` | `UserDetail`, `Profile`, `ProfilePublicity`, `Workspace` |

> 注意：`account.dart` 中的 `AccountPersist` 不在 `Account.kt` 中，会在“持久化模型”章节补充。

---

## 2. 网络响应根对象

### 2.1 `account_edit_response.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `AccountEditResponse` | `error` | `bool` | 否 | `error` |
| | `message` | `String` | 否 | `message` |
| | `body` | `Body` | 否 | `body` |
| `Body` | `isSucceed` | `bool` | 否 | `is_succeed` |
| | `validationErrors` | `ValidationErrors` | 否 | `validation_errors` |
| `ValidationErrors` | - | - | - | - |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountEditResponse(
    val error: Boolean,
    val message: String,
    val body: AccountEditBody,
)

@Serializable
data class AccountEditBody(
    @SerialName("is_succeed") val isSucceed: Boolean,
    @SerialName("validation_errors") val validationErrors: ValidationErrors,
)

@Serializable
data class ValidationErrors(
    // 空对象，对应 Dart 中无字段的 ValidationErrors
)
```

### 2.2 `bookmark.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `BookmarkRsp` | `bookmark_detail` | `Bookmark_detail` | 否 | `bookmark_detail` |
| `Bookmark_detail` | `is_bookmarked` | `bool` | 否 | `is_bookmarked` |
| | `tags` | `List<Tags>` | 否 | `tags` |
| | `restrict` | `String` | 否 | `restrict` |
| `Tags` | `name` | `String` | 否 | `name` |
| | `is_registered` | `bool` | 否 | `is_registered` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookmarkResponse(
    @SerialName("bookmark_detail") val bookmarkDetail: BookmarkDetailBody,
)

@Serializable
data class BookmarkDetailBody(
    @SerialName("is_bookmarked") val isBookmarked: Boolean,
    val tags: List<BookmarkTagEntry>,
    val restrict: String,
)

@Serializable
data class BookmarkTagEntry(
    val name: String,
    @SerialName("is_registered") val isRegistered: Boolean,
)
```

### 2.3 `bookmark_detail.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `BookMarkDetailResponse` | `bookmark_detail` | `BookmarkDetail` | 否 | `bookmark_detail` |
| `BookmarkDetail` | `is_bookmarked` | `bool` | 否 | `is_bookmarked` |
| | `tags` | `List<TagsR>` | 否 | `tags` |
| | `restrict` | `String` | 否 | `restrict` |
| `TagsR` | `name` | `String` | 否 | `name` |
| | `is_registered` | `bool` | 否 | `is_registered` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookmarkDetailResponse(
    @SerialName("bookmark_detail") val bookmarkDetail: BookmarkDetail,
)

@Serializable
data class BookmarkDetail(
    @SerialName("is_bookmarked") val isBookmarked: Boolean,
    val tags: List<BookmarkDetailTag>,
    val restrict: String,
)

@Serializable
data class BookmarkDetailTag(
    val name: String,
    @SerialName("is_registered") val isRegistered: Boolean,
)
```

### 2.4 `comment_response.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `CommentResponse` | `total_comments` | `int` | 是 | `total_comments` |
| | `comments` | `List<Comment>` | 否 | `comments` |
| | `next_url` | `String` | 是 | `next_url` |
| `Comment` | `id` | `int` | 是 | `id` |
| | `comment` | `String` | 是 | `comment` |
| | `date` | `DateTime` | 是 | `date` |
| | `user` | `User` | 是 | `user` |
| | `parent_comment` | `Comment` | 是 | `parent_comment` |
| | `has_replies` | `bool` | 是 | `has_replies` |
| | `stamp` | `Stamp` | 是 | `stamp` |
| `User` | `id` | `int` | 是 | `id` |
| | `name` | `String` | 否 | `name` |
| | `account` | `String` | 否 | `account` |
| | `profile_image_urls` | `ProfileImageUrls` | 否 | `profile_image_urls` |
| `ProfileImageUrls` | `medium` | `String` | 否 | `medium` |
| `Stamp` | `stamp_id` | `int` | 是 | `stamp_id` |
| | `stamp_url` | `String` | 是 | `stamp_url` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentResponse(
    @SerialName("total_comments") val totalComments: Int? = null,
    val comments: List<Comment>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class Comment(
    val id: Int? = null,
    val comment: String? = null,
    val date: String? = null,
    val user: CommentUser? = null,
    @SerialName("parent_comment") val parentComment: Comment? = null,
    @SerialName("has_replies") val hasReplies: Boolean? = null,
    val stamp: CommentStamp? = null,
)

@Serializable
data class CommentUser(
    val id: Int? = null,
    val name: String,
    val account: String,
    @SerialName("profile_image_urls") val profileImageUrls: CommentProfileImageUrls,
)

@Serializable
data class CommentProfileImageUrls(
    val medium: String,
)

@Serializable
data class CommentStamp(
    @SerialName("stamp_id") val stampId: Int? = null,
    @SerialName("stamp_url") val stampUrl: String? = null,
)
```

### 2.5 `create_user_response.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `CreateUserResponse` | `error` | `bool` | 否 | `error` |
| | `message` | `String` | 否 | `message` |
| | `body` | `Body` | 否 | `body` |
| `Body` | `userAccount` | `String` | 否 | `user_account` |
| | `password` | `String` | 否 | `password` |
| | `deviceToken` | `String` | 否 | `device_token` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserResponse(
    val error: Boolean,
    val message: String,
    val body: CreateUserBody,
)

@Serializable
data class CreateUserBody(
    @SerialName("user_account") val userAccount: String,
    val password: String,
    @SerialName("device_token") val deviceToken: String,
)
```

### 2.6 `error_message.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `ErrorMessage` | `error` | `Error` | 否 | `error` |
| `Error` | `user_message` | `String` | 是 | `user_message` |
| | `message` | `String` | 是 | `message` |
| | `reason` | `String` | 是 | `reason` |
| | `user_message_details` | `Object?` | 是 | `user_message_details` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ErrorMessage(
    val error: ApiError,
)

@Serializable
data class ApiError(
    @SerialName("user_message") val userMessage: String? = null,
    val message: String? = null,
    val reason: String? = null,
    @SerialName("user_message_details") val userMessageDetails: JsonElement? = null,
)
```

### 2.7 `follow_detail.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `FollowDetail` | `is_followed` | `bool` | 否 | `is_followed` |
| | `restrict` | `String` | 否 | `restrict` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FollowDetail(
    @SerialName("is_followed") val isFollowed: Boolean,
    val restrict: String,
)
```

### 2.8 `illust_bookmark_tags_response.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `IllustBookmarkTagsResponse` | `bookmark_tags` | `List<BookmarkTag>` | 否 | `bookmark_tags` |
| | `next_url` | `String` | 是 | `next_url` |
| `BookmarkTag` | `name` | `String` | 否 | `name` |
| | `count` | `int` | 否 | `count` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IllustBookmarkTagsResponse(
    @SerialName("bookmark_tags") val bookmarkTags: List<IllustBookmarkTag>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class IllustBookmarkTag(
    val name: String,
    val count: Int,
)
```

### 2.9 `illust_series_detail.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `IllustSeriesDetailResponse` | `illust_series_context` | `IllustSeriesContext` | 是 | `illust_series_context` |
| | `illust_series_detail` | `IllustSeriesDetail` | 是 | `illust_series_detail` |
| `IllustSeriesContext` | `content_order` | `int` | 是 | `content_order` |
| | `next` | `Illusts` | 是 | `next` |
| | `prev` | `Illusts` | 是 | `prev` |
| `IllustSeriesDetail` | `height` | `int` | 否 | `height` |
| | `series_work_count` | `int` | 否 | `series_work_count` |
| | `id` | `int` | 否 | `id` |
| | `create_date` | `String` | 否 | `create_date` |
| | `title` | `String` | 否 | `title` |
| | `width` | `int` | 否 | `width` |
| | `cover_image_urls` | `CoverImageUrls` | 否 | `cover_image_urls` |
| | `watchlist_added` | `bool` | 否 | `watchlist_added` |
| | `caption` | `String` | 否 | `caption` |
| | `user` | `IllustSeriesUser` | 是 | `user` |
| `IllustSeriesUser` | `id` | `int` | 否 | `id` |
| | `account` | `String` | 否 | `account` |
| | `name` | `String` | 否 | `name` |
| | `profile_image_urls` | `IllustSeriesProfileImageUrls` | 是 | `profile_image_urls` |
| | `is_followed` | `bool` | 否 | `is_followed` |
| `IllustSeriesProfileImageUrls` | `medium` | `String` | 是 | `medium` |
| `CoverImageUrls` | `medium` | `String` | 是 | `medium` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IllustSeriesDetailResponse(
    @SerialName("illust_series_context") val illustSeriesContext: IllustSeriesContext? = null,
    @SerialName("illust_series_detail") val illustSeriesDetail: IllustSeriesDetail? = null,
)

@Serializable
data class IllustSeriesContext(
    @SerialName("content_order") val contentOrder: Int? = null,
    val next: Illust? = null,
    val prev: Illust? = null,
)

@Serializable
data class IllustSeriesDetail(
    val height: Int,
    @SerialName("series_work_count") val seriesWorkCount: Int,
    val id: Int,
    @SerialName("create_date") val createDate: String,
    val title: String,
    val width: Int,
    @SerialName("cover_image_urls") val coverImageUrls: CoverImageUrls,
    @SerialName("watchlist_added") val watchlistAdded: Boolean,
    val caption: String,
    val user: IllustSeriesUser? = null,
)

@Serializable
data class IllustSeriesUser(
    val id: Int,
    val account: String,
    val name: String,
    @SerialName("profile_image_urls") val profileImageUrls: IllustSeriesProfileImageUrls? = null,
    @SerialName("is_followed") val isFollowed: Boolean,
)

@Serializable
data class IllustSeriesProfileImageUrls(
    val medium: String? = null,
)

@Serializable
data class CoverImageUrls(
    val medium: String? = null,
)
```

### 2.10 `illust_series_with_id_model.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `IllustSeriesWithIdModel` | `illust_series_detail` | `IllustSeriesDetail` | 是 | `illust_series_detail` |
| | `illust_series_first_illust` | `Illusts` | 是 | `illust_series_first_illust` |
| | `illusts` | `List<Illusts>` | 是 | `illusts` |
| | `next_url` | `String` | 是 | `next_url` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IllustSeriesWithIdModel(
    @SerialName("illust_series_detail") val illustSeriesDetail: IllustSeriesDetail? = null,
    @SerialName("illust_series_first_illust") val illustSeriesFirstIllust: Illust? = null,
    val illusts: List<Illust>? = null,
    @SerialName("next_url") val nextUrl: String? = null,
)
```

### 2.11 `login_error_response.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `LoginErrorResponse` | `hasError` | `bool` | 否 | `has_error` |
| | `errors` | `Errors` | 否 | `errors` |
| `Errors` | `system` | `System` | 否 | `system` |
| `System` | `message` | `String` | 否 | `message` |
| | `code` | `int` | 否 | `code` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginErrorResponse(
    @SerialName("has_error") val hasError: Boolean,
    val errors: LoginErrors,
)

@Serializable
data class LoginErrors(
    val system: LoginErrorSystem,
)

@Serializable
data class LoginErrorSystem(
    val message: String,
    val code: Int,
)
```

### 2.12 `novel_recom_response.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `NovelRecomResponse` | `novels` | `List<Novel>` | 否 | `novels` |
| | `next_url` | `String` | 是 | `next_url` |
| `Novel` | 大量字段 | - | - | - |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NovelRecomResponse(
    val novels: List<Novel>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class Novel(
    val id: Int,
    val title: String,
    val caption: String,
    val restrict: Int,
    @SerialName("x_restrict") val xRestrict: Int,
    @SerialName("is_original") val isOriginal: Boolean,
    @SerialName("image_urls") val imageUrls: NovelImageUrls,
    @SerialName("create_date") val createDate: String,
    val tags: List<NovelTag>,
    @SerialName("page_count") val pageCount: Int,
    @SerialName("text_length") val textLength: Int,
    val user: NovelUser,
    val series: NovelSeries,
    @SerialName("is_bookmarked") val isBookmarked: Boolean,
    @SerialName("total_bookmarks") val totalBookmarks: Int,
    @SerialName("total_view") val totalView: Int,
    val visible: Boolean,
    @SerialName("total_comments") val totalComments: Int,
    @SerialName("is_muted") val isMuted: Boolean,
    @SerialName("is_mypixiv_only") val isMypixivOnly: Boolean,
    @SerialName("is_x_restricted") val isXRestricted: Boolean,
    @SerialName("novel_ai_type") val novelAIType: Int,
)

@Serializable
data class NovelImageUrls(
    @SerialName("square_medium") val squareMedium: String,
    val medium: String,
    val large: String,
)

@Serializable
data class NovelSeries(
    val id: Int? = null,
    val title: String? = null,
)

@Serializable
data class NovelTag(
    val name: String,
    @SerialName("translated_name") val translatedName: String? = null,
    @SerialName("added_by_uploaded_user") val addedByUploadedUser: Boolean,
)

@Serializable
data class NovelUser(
    val id: Int,
    val name: String,
    val account: String,
    @SerialName("profile_image_urls") val profileImageUrls: NovelProfileImageUrls,
    @SerialName("is_followed") val isFollowed: Boolean,
)

@Serializable
data class NovelProfileImageUrls(
    val medium: String,
)
```

### 2.13 `novel_series_detail.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `NovelSeriesSeries` | `id` | `int` | 否 | `id` |
| | `title` | `String` | 否 | `title` |
| `NovelSeriesNovelTag` | `name` | `String` | 否 | `name` |
| | `translated_name` | `String` | 是 | `translated_name` |
| | `added_by_uploaded_user` | `bool` | 否 | `added_by_uploaded_user` |
| `NovelSeriesNovel` | 见代码 | - | - | - |
| `NovelSeriesDetail` | 见代码 | - | - | - |
| `NovelSeriesUser` | 见代码 | - | - | - |
| `NovelSeriesProfileImageUrls` | `medium` | `String` | 否 | `medium` |
| `NovelSeriesFirstNovel` | 见代码 | - | - | - |
| `NovelSeriesImageUrls` | `square_medium` / `medium` / `large` | `String` | 否 | - |
| `NovelSeriesResponse` | `novel_series_detail` / `novel_series_first_novel` / `novel_series_latest_novel` / `novels` / `next_url` | - | - | - |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NovelSeriesSeries(
    val id: Int,
    val title: String,
)

@Serializable
data class NovelSeriesNovelTag(
    val name: String,
    @SerialName("translated_name") val translatedName: String? = null,
    @SerialName("added_by_uploaded_user") val addedByUploadedUser: Boolean,
)

@Serializable
data class NovelSeriesNovel(
    val id: Int,
    val title: String,
    val caption: String? = null,
    val restrict: Int,
    @SerialName("x_restrict") val xRestrict: Int,
    @SerialName("is_original") val isOriginal: Boolean? = null,
    @SerialName("image_urls") val imageUrls: NovelSeriesImageUrls,
    @SerialName("create_date") val createDate: String,
    val tags: List<NovelSeriesNovelTag>,
    @SerialName("page_count") val pageCount: Int,
    @SerialName("text_length") val textLength: Int,
    val user: NovelSeriesUser,
    val series: NovelSeriesSeries,
    @SerialName("is_bookmarked") val isBookmarked: Boolean,
    @SerialName("total_bookmarks") val totalBookmarks: Int,
    @SerialName("total_view") val totalView: Int,
    val visible: Boolean,
    @SerialName("total_comments") val totalComments: Int,
    @SerialName("is_muted") val isMuted: Boolean,
    @SerialName("is_mypixiv_only") val isMypixivOnly: Boolean,
    @SerialName("is_x_restricted") val isXRestricted: Boolean,
    @SerialName("novel_ai_type") val novelAiType: Int,
)

@Serializable
data class NovelSeriesDetail(
    val id: Int,
    val title: String,
    val caption: String? = null,
    @SerialName("is_original") val isOriginal: Boolean,
    @SerialName("is_concluded") val isConcluded: Boolean,
    @SerialName("content_count") val contentCount: Int,
    @SerialName("total_character_count") val totalCharacterCount: Int,
    val user: NovelSeriesUser,
    @SerialName("display_text") val displayText: String,
    @SerialName("novel_ai_type") val novelAiType: Int,
    @SerialName("watchlist_added") val watchlistAdded: Boolean? = null,
)

@Serializable
data class NovelSeriesUser(
    val id: Int,
    val name: String,
    val account: String,
    @SerialName("profile_image_urls") val profileImageUrls: NovelSeriesProfileImageUrls,
    @SerialName("is_followed") val isFollowed: Boolean,
    @SerialName("is_access_blocking_user") val isAccessBlockingUser: Boolean,
)

@Serializable
data class NovelSeriesProfileImageUrls(
    val medium: String,
)

@Serializable
data class NovelSeriesFirstNovel(
    val id: Int,
    val title: String,
    val caption: String,
    val restrict: Int,
    @SerialName("x_restrict") val xRestrict: Int,
    @SerialName("is_original") val isOriginal: Boolean,
    @SerialName("image_urls") val imageUrls: NovelSeriesImageUrls,
    @SerialName("create_date") val createDate: String,
    val tags: List<NovelSeriesNovelTag>,
    @SerialName("page_count") val pageCount: Int,
    @SerialName("text_length") val textLength: Int,
    val user: NovelSeriesUser,
    val series: NovelSeriesSeries,
    @SerialName("is_bookmarked") val isBookmarked: Boolean,
    @SerialName("total_bookmarks") val totalBookmarks: Int,
    @SerialName("total_view") val totalView: Int,
    val visible: Boolean,
    @SerialName("total_comments") val totalComments: Int,
    @SerialName("is_muted") val isMuted: Boolean? = null,
    @SerialName("is_my_pixiv_only") val isMypixivOnly: Boolean? = null,
    @SerialName("is_X_restricted") val isXRestricted: Boolean? = null,
    @SerialName("novel_ai_type") val novelAiType: Int,
)

@Serializable
data class NovelSeriesImageUrls(
    @SerialName("square_medium") val squareMedium: String,
    val medium: String,
    val large: String,
)

@Serializable
data class NovelSeriesResponse(
    @SerialName("novel_series_detail") val novelSeriesDetail: NovelSeriesDetail,
    @SerialName("novel_series_first_novel") val novelSeriesFirstNovel: NovelSeriesFirstNovel,
    @SerialName("novel_series_latest_novel") val novelSeriesLatestNovel: NovelSeriesFirstNovel? = null,
    val novels: List<Novel>,
    @SerialName("next_url") val nextUrl: String? = null,
)
```

### 2.14 `novel_text_response.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `NovelTextResponse` | `novel_marker` | `NovelMarker` | 否 | `novel_marker` |
| | `novel_text` | `String` | 否 | `novel_text` |
| | `series_prev` | `TextNovel` | 是 | `series_prev` |
| | `series_next` | `TextNovel` | 是 | `series_next` |
| `NovelMarker` | `page` | `int` | 是 | `page` |
| `TextNovel` | `id` | `int` | 是 | `id` |
| | `title` | `String` | 是 | `title` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NovelTextResponse(
    @SerialName("novel_marker") val novelMarker: NovelMarker,
    @SerialName("novel_text") val novelText: String,
    @SerialName("series_prev") val seriesPrev: TextNovel? = null,
    @SerialName("series_next") val seriesNext: TextNovel? = null,
)

@Serializable
data class NovelMarker(
    val page: Int? = null,
)

@Serializable
data class TextNovel(
    val id: Int? = null,
    val title: String? = null,
)
```

### 2.15 `novel_watch_list_model.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `NovelWatchListModel` | `series` | `List<NovelSeriesModel>` | 否 | `series` |
| | `next_url` | `String` | 是 | `next_url` |
| `NovelSeriesModel` | 见代码 | - | - | - |
| `NovelSeriesUser` | 见代码 | - | - | - |
| `NovelSeriesUserProfileImageUrls` | `medium` | `String` | 是 | `medium` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NovelWatchListModel(
    val series: List<NovelWatchListSeries>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class NovelWatchListSeries(
    val id: Int,
    val title: String,
    val url: String? = null,
    @SerialName("mask_text") val maskText: String? = null,
    @SerialName("published_content_count") val publishedContentCount: Int,
    @SerialName("last_published_content_datetime") val lastPublishedContentDatetime: String,
    @SerialName("latest_content_id") val latestContentId: Int,
    val user: NovelWatchListSeriesUser? = null,
)

@Serializable
data class NovelWatchListSeriesUser(
    val id: Int,
    val name: String,
    val account: String,
    @SerialName("profile_image_urls") val profileImageUrls: NovelWatchListSeriesProfileImageUrls? = null,
    @SerialName("is_accept_request") val isAcceptRequest: Boolean,
)

@Serializable
data class NovelWatchListSeriesProfileImageUrls(
    val medium: String? = null,
)
```

### 2.16 `novel_web_response.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `NovelWebResponse` | 大量字段，含 `dynamic` / `Map` / `List<dynamic>` | - | - | - |
| `NovelIllusts` | `illust` | `NovelIllust` | 否 | `illust` |
| `NovelIllust` | `images` | `NovelIllustImages` | 否 | `images` |
| `NovelIllustImages` | `small` / `medium` / `original` | `String` | 是 | - |
| `NovelRating` | `like` / `bookmark` / `view` | `int` | 否 | - |
| `SeriesNavigation` | `nextNovel` / `prevNovel` | `PrevNovel` | 是 | - |
| `PrevNovel` | `id` / `viewable` / `contentOrder` / `title` / `coverUrl` | - | - | - |
| `NovelImage` | `novelImageId` / `sl` / `urls` | - | - | - |
| `NovelUrls` | `the240Mw` 等 | `String` | 是 | 不确定 |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NovelWebResponse(
    val id: String,
    val title: String,
    val seriesId: JsonElement? = null,
    val seriesTitle: JsonElement? = null,
    val seriesIsWatched: JsonElement? = null,
    val userId: String,
    val coverUrl: String,
    val tags: List<String>,
    val caption: String,
    val cdate: String,
    val rating: NovelRating,
    val text: String,
    val marker: JsonElement? = null,
    val seriesNavigation: NovelSeriesNavigation? = null,
    val glossaryItems: List<JsonElement>? = null,
    val replaceableItemIds: List<JsonElement>? = null,
    val images: Map<String, NovelWebImage>? = null,
    val illusts: Map<String, NovelWebIllusts?>? = null,
    val aiType: Int? = null,
    val isOriginal: Boolean? = null,
)

@Serializable
data class NovelWebIllusts(
    val illust: NovelWebIllust,
)

@Serializable
data class NovelWebIllust(
    val images: NovelWebIllustImages,
)

@Serializable
data class NovelWebIllustImages(
    val small: String? = null,
    val medium: String? = null,
    val original: String? = null,
)

@Serializable
data class NovelRating(
    val like: Int,
    val bookmark: Int,
    val view: Int,
)

@Serializable
data class NovelSeriesNavigation(
    val nextNovel: NovelPrevNext? = null,
    val prevNovel: NovelPrevNext? = null,
)

@Serializable
data class NovelPrevNext(
    val id: Int,
    val viewable: Boolean,
    val contentOrder: String,
    val title: String? = null,
    val coverUrl: String? = null,
)

@Serializable
data class NovelWebImage(
    val novelImageId: String? = null,
    val sl: String,
    val urls: NovelImageUrlsWeb,
)

@Serializable
data class NovelImageUrlsWeb(
    // TODO: 联调确认真实 key，可能是 @SerialName("240mw") 等
    val the240Mw: String? = null,
    val the480Mw: String? = null,
    val the1200X1200: String? = null,
    val the128X128: String? = null,
    val original: String? = null,
)
```

### 2.17 `onezero_response.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `OnezeroResponse` | `answer` | `List<OnezeroAnswer>` | 否 | `Answer` |
| `OnezeroAnswer` | `name` | `String` | 否 | `name` |
| | `type` | `int` | 否 | `type` |
| | `data` | `String` | 否 | `data` |
| | `ttl` | `int` | 否 | `TTL` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OnezeroResponse(
    @SerialName("Answer") val answer: List<OnezeroAnswer>,
)

@Serializable
data class OnezeroAnswer(
    val name: String,
    val type: Int,
    val data: String,
    @SerialName("TTL") val ttl: Int,
)
```

### 2.18 `recommend.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `Recommend` | `illusts` | `List<Illusts>` | 否 | `illusts` |
| | `ranking_illusts` | `List<Illusts>` | 是 | `ranking_illusts` |
| | `contest_exists` | `bool` | 是 | `contest_exists` |
| | `privacy_policy` | `PrivacyPolicy` | 是 | `privacy_policy` |
| | `next_url` | `String` | 是 | `next_url` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recommend(
    val illusts: List<Illust>,
    @SerialName("ranking_illusts") val rankingIllusts: List<Illust>? = null,
    @SerialName("contest_exists") val contestExists: Boolean? = null,
    @SerialName("privacy_policy") val privacyPolicy: PrivacyPolicy? = null,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class PrivacyPolicy(
    // Dart 侧为空对象
)
```

### 2.19 `show_ai_response.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `ShowAIResponse` | `showAI` | `bool` | 否 | `show_ai` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShowAIResponse(
    @SerialName("show_ai") val showAI: Boolean,
)
```

### 2.20 `spotlight_response.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `SpotlightResponse` | `spotlight_articles` | `List<SpotlightArticle>` | 否 | `spotlight_articles` |
| | `next_url` | `String` | 是 | `next_url` |
| `SpotlightArticle` | `id` | `int` | 否 | `id` |
| | `title` | `String` | 否 | `title` |
| | `pure_title` | `String` | 否 | `pure_title` |
| | `thumbnail` | `String` | 否 | `thumbnail` |
| | `article_url` | `String` | 否 | `article_url` |
| | `publish_date` | `DateTime` | 否 | `publish_date` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotlightResponse(
    @SerialName("spotlight_articles") val spotlightArticles: List<SpotlightArticle>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class SpotlightArticle(
    val id: Int,
    val title: String,
    @SerialName("pure_title") val pureTitle: String,
    val thumbnail: String,
    @SerialName("article_url") val articleUrl: String,
    @SerialName("publish_date") val publishDate: String,
)
```

### 2.21 `trend_tags.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `TrendingTag` | `trend_tags` | `List<TrendTags>` | 否 | `trend_tags` |
| `TrendTags` | `tag` | `String` | 否 | `tag` |
| | `translated_name` | `String` | 是 | `translated_name` |
| | `illust` | `TrendTagsIllust` | 否 | `illust` |
| `TrendTagsIllust` | `id` | `int` | 否 | `id` |
| | `image_urls` | `ImageUrls` | 否 | `image_urls` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrendingTag(
    @SerialName("trend_tags") val trendTags: List<TrendTag>,
)

@Serializable
data class TrendTag(
    val tag: String,
    @SerialName("translated_name") val translatedName: String? = null,
    val illust: TrendTagIllust,
)

@Serializable
data class TrendTagIllust(
    val id: Int,
    @SerialName("image_urls") val imageUrls: ImageUrls,
)
```

### 2.22 `ugoira_metadata_response.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `UgoiraMetadataResponse` | `ugoira_metadata` | `UgoiraMetadata` | 否 | `ugoira_metadata` |
| `UgoiraMetadata` | `zip_urls` | `ZipUrls` | 否 | `zip_urls` |
| | `frames` | `List<Frame>` | 否 | `frames` |
| `Frame` | `file` | `String` | 否 | `file` |
| | `delay` | `int` | 否 | `delay` |
| `ZipUrls` | `medium` | `String` | 否 | `medium` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UgoiraMetadataResponse(
    @SerialName("ugoira_metadata") val ugoiraMetadata: UgoiraMetadata,
)

@Serializable
data class UgoiraMetadata(
    @SerialName("zip_urls") val zipUrls: UgoiraZipUrls,
    val frames: List<UgoiraFrame>,
)

@Serializable
data class UgoiraFrame(
    val file: String,
    val delay: Int,
)

@Serializable
data class UgoiraZipUrls(
    val medium: String,
)
```

### 2.23 `user_preview.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `UserPreviewsResponse` | `user_previews` | `List<UserPreviews>` | 否 | `user_previews` |
| | `next_url` | `String` | 是 | `next_url` |
| `UserPreviews` | `user` | `User` | 否 | `user` |
| | `illusts` | `List<Illusts>` | 否 | `illusts` |
| | `novels` | `List<UserPreviewsNovel>` | 否 | `novels` |
| | `is_muted` | `bool` | 否 | `is_muted` |
| `UserPreviewsNovel` | `id` | `int` | 否 | `id` |
| | `title` | `String` | 否 | `title` |
| | `caption` | `String` | 是 | `caption` |
| | `image_urls` | `ImageUrls` | 否 | `image_urls` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPreviewsResponse(
    @SerialName("user_previews") val userPreviews: List<UserPreview>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class UserPreview(
    val user: IllustUser,
    val illusts: List<Illust>,
    val novels: List<UserPreviewNovel>,
    @SerialName("is_muted") val isMuted: Boolean,
)

@Serializable
data class UserPreviewNovel(
    val id: Int,
    val title: String,
    val caption: String? = null,
    @SerialName("image_urls") val imageUrls: ImageUrls,
)
```

### 2.24 `watchlist_manga_model.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `WatchlistMangaModel` | `series` | `List<MangaSeriesModel>` | 否 | `series` |
| | `next_url` | `String` | 是 | `next_url` |
| `MangaSeriesModel` | 见代码 | - | - | - |
| `MangaSeriesUser` | 见代码 | - | - | - |
| `MangaSeriesUserProfileImageUrls` | `medium` | `String` | 是 | `medium` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchlistMangaModel(
    val series: List<WatchlistMangaSeries>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class WatchlistMangaSeries(
    @SerialName("mask_text") val maskText: String? = null,
    @SerialName("latest_content_id") val latestContentId: Int,
    val id: Int,
    val user: WatchlistMangaSeriesUser? = null,
    val title: String,
    @SerialName("last_published_content_datetime") val lastPublishedContentDatetime: String? = null,
    @SerialName("published_content_count") val publishedContentCount: Int,
    val url: String? = null,
)

@Serializable
data class WatchlistMangaSeriesUser(
    val id: Int,
    val account: String? = null,
    val name: String? = null,
    val profileImageUrls: WatchlistMangaSeriesProfileImageUrls? = null,
)

@Serializable
data class WatchlistMangaSeriesProfileImageUrls(
    val medium: String? = null,
)
```

---

## 3. 业务实体

### 3.1 `amwork.dart`

| 类 | 字段 | Dart 类型 | 可空 |
|---|---|---|---|
| `AmWork` | `title` | `String` | 是 |
| | `user` | `String` | 是 |
| | `arworkLink` | `String` | 是 |
| | `userLink` | `String` | 是 |
| | `userImage` | `String` | 是 |
| | `showImage` | `String` | 是 |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AmWork(
    val title: String? = null,
    val user: String? = null,
    val arworkLink: String? = null,
    val userLink: String? = null,
    val userImage: String? = null,
    val showImage: String? = null,
)
```

### 3.2 `board_info.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `BoardInfo` | `title` | `String` | 否 | `title` |
| | `content` | `String` | 否 | `content` |
| | `startDate` | `String` | 否 | `startDate` |
| | `endDate` | `String` | 是 | `endDate` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BoardInfo(
    val title: String,
    val content: String,
    val startDate: String,
    val endDate: String? = null,
)
```

### 3.3 `tags.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `AutoWords` | `tags` | `List<Tags>` | 否 | `tags` |
| `Tags` | `name` | `String` | 否 | `name` |
| | `translated_name` | `String` | 是 | `translated_name` |
| `TagsPersist` | `_id` | `int` | 是 | `_id` |
| | `name` | `String` | 否 | `name` |
| | `translated_name` | `String` | 否 | `translated_name` |
| | `type` | `int` | 是 | `type` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AutoWords(
    val tags: List<SearchTag>,
)

@Serializable
data class SearchTag(
    val name: String,
    @SerialName("translated_name") val translatedName: String? = null,
)

@Serializable
data class TagPersist(
    @SerialName("_id") val id: Int? = null,
    val name: String,
    @SerialName("translated_name") val translatedName: String,
    val type: Int? = 0,
)
```

---

## 4. 持久化 / 本地数据库模型

### 4.1 `account.dart` -> `AccountPersist`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `AccountPersist` | `id` | `int` | 是 | `id` |
| | `user_id` | `String` | 否 | `user_id` |
| | `user_image` | `String` | 否 | `user_image` |
| | `access_token` | `String` | 否 | `access_token` |
| | `refresh_token` | `String` | 否 | `refresh_token` |
| | `device_token` | `String` | 否 | `device_token` |
| | `name` | `String` | 否 | `name` |
| | `account` | `String` | 否 | `account` |
| | `mail_address` | `String` | 否 | `mail_address` |
| | `password` | `String` | 否 | `password` |
| | `is_premium` | `int` | 否 | `is_premium` |
| | `x_restrict` | `int` | 否 | `x_restrict` |
| | `is_mail_authorized` | `int` | 否 | `is_mail_authorized` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountPersist(
    val id: Int? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("user_image") val userImage: String,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("device_token") val deviceToken: String,
    val name: String,
    val account: String,
    @SerialName("mail_address") val mailAddress: String,
    @SerialName("password") val passWord: String,
    @SerialName("is_premium") val isPremium: Int,
    @SerialName("x_restrict") val xRestrict: Int,
    @SerialName("is_mail_authorized") val isMailAuthorized: Int,
)
```

### 4.2 `ban_comment_persist.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `BanCommentPersist` | `comment_id` | `String` | 否 | `comment_id` |
| | `name` | `String` | 否 | `name` |
| | `id` | `int` | 是 | `id` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BanCommentPersist(
    @SerialName("comment_id") val commentId: String,
    val name: String,
    val id: Int? = null,
)
```

### 4.3 `ban_illust_id.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `BanIllustIdPersist` | `illust_id` | `String` | 否 | `illust_id` |
| | `name` | `String` | 否 | `name` |
| | `id` | `int` | 是 | `id` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BanIllustIdPersist(
    @SerialName("illust_id") val illustId: String,
    val name: String,
    val id: Int? = null,
)
```

### 4.4 `ban_tag.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `BanTagPersist` | `id` | `int` | 是 | `id` |
| | `name` | `String` | 否 | `name` |
| | `translate_name` | `String` | 否 | `translate_name` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BanTagPersist(
    val id: Int? = null,
    val name: String,
    @SerialName("translate_name") val translateName: String,
)
```

### 4.5 `ban_user_id.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `BanUserIdPersist` | `user_id` | `String` | 是 | `user_id` |
| | `id` | `int` | 是 | `id` |
| | `name` | `String` | 是 | `name` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BanUserIdPersist(
    @SerialName("user_id") val userId: String? = null,
    val id: Int? = null,
    val name: String? = null,
)
```

### 4.6 `glance_illust_persist.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `GlanceIllustPersist` | `id` | `int` | 是 | `id` |
| | `illust_id` | `int` | 否 | `illust_id` |
| | `user_id` | `int` | 否 | `user_id` |
| | `picture_url` | `String` | 否 | `picture_url` |
| | `original_url` | `String` | 是 | `original_url` |
| | `large_url` | `String` | 是 | `large_url` |
| | `user_name` | `String` | 是 | `user_name` |
| | `title` | `String` | 是 | `title` |
| | `type` | `String` | 否 | `type` |
| | `time` | `int` | 否 | `time` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GlanceIllustPersist(
    val id: Int? = null,
    @SerialName("illust_id") val illustId: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("picture_url") val pictureUrl: String,
    @SerialName("original_url") val originalUrl: String? = null,
    @SerialName("large_url") val largeUrl: String? = null,
    @SerialName("user_name") val userName: String? = null,
    val title: String? = null,
    val type: String,
    val time: Int,
)
```

### 4.7 `illust_persist.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `IllustPersist` | `id` | `int` | 是 | `id` |
| | `illust_id` | `int` | 否 | `illust_id` |
| | `user_id` | `int` | 否 | `user_id` |
| | `picture_url` | `String` | 否 | `picture_url` |
| | `user_name` | `String` | 是 | `user_name` |
| | `title` | `String` | 是 | `title` |
| | `time` | `int` | 否 | `time` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IllustPersist(
    val id: Int? = null,
    @SerialName("illust_id") val illustId: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("picture_url") val pictureUrl: String,
    @SerialName("user_name") val userName: String? = null,
    val title: String? = null,
    val time: Int,
)
```

### 4.8 `key_value_pair.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `KVPair` | `key` | `String` | 否 | `key` |
| | `value` | `String` | 否 | `value` |
| | `expire_time` | `int` | 否 | `expire_time` |
| | `date_time` | `int` | 否 | `date_time` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KVPair(
    val key: String,
    val value: String,
    @SerialName("expire_time") val expireTime: Int,
    @SerialName("date_time") val dateTime: Int,
)
```

### 4.9 `novel_persist.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `NovelPersist` | `id` | `int` | 是 | `id` |
| | `novel_id` | `int` | 否 | `novel_id` |
| | `user_id` | `int` | 否 | `user_id` |
| | `picture_url` | `String` | 否 | `picture_url` |
| | `time` | `int` | 否 | `time` |
| | `title` | `String` | 否 | `title` |
| | `user_name` | `String` | 否 | `user_name` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NovelPersist(
    val id: Int? = null,
    @SerialName("novel_id") val novelId: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("picture_url") val pictureUrl: String,
    val time: Int,
    val title: String,
    @SerialName("user_name") val userName: String,
)
```

### 4.10 `novel_viewer_persist.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `NovelViewerPersist` | `id` | `int` | 是 | `id` |
| | `novel_id` | `int` | 否 | `novel_id` |
| | `offset` | `double` | 否 | `offset` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NovelViewerPersist(
    val id: Int? = null,
    @SerialName("novel_id") val novelId: Int,
    val offset: Double,
)
```

### 4.11 `tags.dart` -> `TagPersist`

已在“业务实体”章节定义 `TagPersist`，此处不再重复。

### 4.12 `task_persist.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `TaskPersist` | `id` | `int` | 是 | `id` |
| | `user_name` | `String` | 否 | `user_name` |
| | `file_name` | `String` | 否 | `file_name` |
| | `title` | `String` | 否 | `title` |
| | `url` | `String` | 否 | `url` |
| | `medium` | `String` | 是 | `medium` |
| | `user_id` | `int` | 否 | `user_id` |
| | `illust_id` | `int` | 否 | `illust_id` |
| | `sanity_level` | `int` | 否 | `sanity_level` |
| | `status` | `int` | 否 | `status` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskPersist(
    val id: Int? = null,
    @SerialName("user_name") val userName: String,
    @SerialName("file_name") val fileName: String,
    val title: String,
    val url: String,
    val medium: String? = null,
    @SerialName("user_id") val userId: Int,
    @SerialName("illust_id") val illustId: Int,
    @SerialName("sanity_level") val sanityLevel: Int,
    val status: Int,
)
```

### 4.13 `export_tag_history_data.dart`

| 类 | 字段 | Dart 类型 | 可空 | JSON key |
|---|---|---|---|---|
| `ExportData` | `tagHisotry` | `List<TagsPersist>` | 是 | `tagHisotry` |
| | `bookTags` | `List<String>` | 是 | `bookTags` |

```kotlin
package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagExportData(
    @SerialName("tagHisotry") val tagHistory: List<TagPersist>? = null,
    val bookTags: List<String>? = null,
)
```

---

## 5. 空文件 / 无模型类

### 5.1 `ranking.dart`

该文件仅包含版权头注释，没有公共类，无需迁移。

---

## 6. 汇总表

| Dart 文件 | 主要生成的 Kotlin 类 | 备注 |
|---|---|---|
| `account.dart` | `AccountPersist` | 其余类已存在 |
| `account_edit_response.dart` | `AccountEditResponse`, `AccountEditBody`, `ValidationErrors` | |
| `amwork.dart` | `AmWork` | 非 JSON 模型 |
| `ban_comment_persist.dart` | `BanCommentPersist` | |
| `ban_illust_id.dart` | `BanIllustIdPersist` | |
| `ban_tag.dart` | `BanTagPersist` | |
| `ban_user_id.dart` | `BanUserIdPersist` | |
| `board_info.dart` | `BoardInfo` | |
| `bookmark.dart` | `BookmarkResponse`, `BookmarkDetailBody`, `BookmarkTagEntry` | |
| `bookmark_detail.dart` | `BookmarkDetailResponse`, `BookmarkDetail`, `BookmarkDetailTag` | |
| `comment_response.dart` | `CommentResponse`, `Comment`, `CommentUser`, `CommentProfileImageUrls`, `CommentStamp` | |
| `create_user_response.dart` | `CreateUserResponse`, `CreateUserBody` | |
| `error_message.dart` | `ErrorMessage`, `ApiError` | |
| `export_tag_history_data.dart` | `TagExportData` | 复用 `TagPersist` |
| `follow_detail.dart` | `FollowDetail` | |
| `glance_illust_persist.dart` | `GlanceIllustPersist` | |
| `illust.dart` | - | 已存在 |
| `illust_bookmark_tags_response.dart` | `IllustBookmarkTagsResponse`, `IllustBookmarkTag` | |
| `illust_persist.dart` | `IllustPersist` | |
| `illust_series_detail.dart` | `IllustSeriesDetailResponse`, `IllustSeriesContext`, `IllustSeriesDetail`, `IllustSeriesUser`, `IllustSeriesProfileImageUrls`, `CoverImageUrls` | |
| `illust_series_with_id_model.dart` | `IllustSeriesWithIdModel` | 复用 `IllustSeriesDetail`, `Illust` |
| `key_value_pair.dart` | `KVPair` | |
| `login_error_response.dart` | `LoginErrorResponse`, `LoginErrors`, `LoginErrorSystem` | |
| `novel_persist.dart` | `NovelPersist` | |
| `novel_recom_response.dart` | `NovelRecomResponse`, `Novel`, `NovelImageUrls`, `NovelSeries`, `NovelTag`, `NovelUser`, `NovelProfileImageUrls` | |
| `novel_series_detail.dart` | `NovelSeriesResponse` 及全部嵌套类 | |
| `novel_text_response.dart` | `NovelTextResponse`, `NovelMarker`, `TextNovel` | |
| `novel_viewer_persist.dart` | `NovelViewerPersist` | |
| `novel_watch_list_model.dart` | `NovelWatchListModel`, `NovelWatchListSeries`, `NovelWatchListSeriesUser`, `NovelWatchListSeriesProfileImageUrls` | 容错解析需额外处理 |
| `novel_web_response.dart` | `NovelWebResponse` 及全部嵌套类 | `NovelUrls` key 待确认 |
| `onezero_response.dart` | `OnezeroResponse`, `OnezeroAnswer` | |
| `ranking.dart` | - | 空文件 |
| `recommend.dart` | `Recommend`, `PrivacyPolicy` | 复用 `Illust` |
| `show_ai_response.dart` | `ShowAIResponse` | |
| `spotlight_response.dart` | `SpotlightResponse`, `SpotlightArticle` | |
| `tags.dart` | `AutoWords`, `SearchTag`, `TagPersist` | |
| `task_persist.dart` | `TaskPersist` | |
| `trend_tags.dart` | `TrendingTag`, `TrendTag`, `TrendTagIllust` | 复用 `ImageUrls` |
| `ugoira_metadata_response.dart` | `UgoiraMetadataResponse`, `UgoiraMetadata`, `UgoiraFrame`, `UgoiraZipUrls` | |
| `user_detail.dart` | - | 已存在 |
| `user_preview.dart` | `UserPreviewsResponse`, `UserPreview`, `UserPreviewNovel` | 复用 `Illust`, `IllustUser`, `ImageUrls` |
| `watchlist_manga_model.dart` | `WatchlistMangaModel`, `WatchlistMangaSeries`, `WatchlistMangaSeriesUser`, `WatchlistMangaSeriesProfileImageUrls` | 容错解析需额外处理 |
