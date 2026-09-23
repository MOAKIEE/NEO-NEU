# NEO NEU · 数据层 v1 交接

日期：2026-09-23。`core/contract` v1 接口及数据层历史验证记录保留于此；前端现已集成到 `:app`。本文件列明实测范围和仍缺少样本的功能，当前整体状态见[验证与发布清单](../04-验证与发布清单.md)。

## 本轮 UI 修复说明

V2 重构与审查修复没有改变本文件的 Repository 契约、查询端点、Room schema 或历史真实样本范围。正式 App 已删除演示仓库，只使用真实 CampusData；新增的课程逻辑测试是虚构样本的 JVM 回归测试。校历/作息改为只显示现有契约返回的内容；非空待办、考试等限制仍按下表保留。当前前端行为见 [前端交接](frontend.md)，验证与剩余限制见 [验证清单](../04-验证与发布清单.md)。

## 构建和验证

要求 JDK 21、Android SDK 37、Gradle Wrapper 9.7.1。Windows 在仓库根目录运行：

```powershell
.\gradlew.bat :core:contract:testDebugUnitTest :data:network:testDebugUnitTest :integration:academic:testDebugUnitTest :integration:portal:testDebugUnitTest :verification:assembleDebug
adb install -r verification\build\outputs\apk\debug\verification-debug.apk
adb shell am start -n edu.neu.campus.verification/.VerificationActivity
```

本机以上构建、解析／状态单元测试通过；验证宿主在 Android 模拟器安装、启动。合成单元测试覆盖 HTML 登录页、错误包、空值、成绩／课表结构与缓存失败保留，不能替代真实样本。验证宿主只显示条目数、状态和字段名／类型；真实个人值、密码、Cookie、票据不写入仓库。

已使用宿主完成官方网页登录、两域会话探针、真实查询、进程重启、Room 缓存迁移、离线刷新、无 Cookie 的认证失效探针及恢复：学期课表 24 条旧缓存分别在 `NETWORK` 和 `AUTH_REQUIRED` 后保留，正常刷新恢复 `READY`。房间数据库旧版 SQLite 文件已一次迁移到 `query_cache_room.db`；导出的 Room schema v1 位于 `data/database/schemas/`。以后版本须写显式 Room migration，不能配置 destructive fallback。

## 前端装配

```kotlin
val campus = CampusData.get(applicationContext) // 应用进程中保持单实例
startActivity(OfficialLogin.intent(this))       // 用户在学校官方网页自行登录

lifecycleScope.launch {
    campus.session.verify()                       // 进程重启后复验门户和教务
    campus.academic.refreshTerms()
    val term = campus.academic.terms().value.data?.firstOrNull { it.isCurrent } ?: return@launch
    campus.academic.refreshWeeks(term.id)
    campus.academic.refreshCampuses(term.id)
    campus.academic.refreshTimetable(term.id, null) // 学期视图，遍历所有返回校区
    val table = campus.academic.timetable(term.id, null).value
    campus.portal.refreshBalance(BalanceKind.CAMPUS_CARD)
    val card = campus.portal.balance(BalanceKind.CAMPUS_CARD).value
    campus.portal.refreshMessages(page = 1, pageSize = 20)
    val messages = campus.portal.messages(page = 1, pageSize = 20).value
    // 观察 Flow<QuerySnapshot<T>>；FAILED 时可能保留旧 data
}
```

认证入口由 `integration/auth-web` 提供；业务查询使用 `CampusData.academic`、`.portal`、`.session` 的 `core/contract` 接口。正式业务页面不得直接用 `CampusData.localSession`、`SchoolHttp`、Cookie 或学校 URL。验证宿主不进入正式发行包。App Manifest 应保留 `INTERNET`、禁用明文传输及备份；使用 Miuix 0.9.4 时须保留 compileSdk 37、minSdk 24，并完成正式 UI 的 targetSdk 适配。

`NEO NEU` 是面向用户和 Gradle 根工程的名称；现有 `edu.neu.campus.*` 是 Kotlin 命名空间及 debug 宿主 ID，不应为改名批量替换。正式 `app` 当前的 `applicationId` 是 `edu.neu.campus.neoneu`，发布前仍需核对。

`QuerySnapshot.data == null` 表示尚无成功数据；成功空列表是非 null 空集合。刷新失败可保留旧 `data`、原 `lastSuccessEpochMillis` 和 `isStale=true`。Room 只在完整解析成功后写入；账号作用域为本机随机 ID，退出清除当前范围与 WebView Cookie。若重新开始认证会生成新作用域，以免未经账号身份确认时展示旧账号数据。进程重启先标 `UNVERIFIED`，由 `session.verify()` 更新两域状态。

## 实测状态与限制

| 模块 | 当前真实验证 | 未解决限制 |
| --- | --- | --- |
| `data/session`, `integration/auth-web` | 用户在官方页面登录；P01/A01 双探针 `READY`，重启可复验；显式退出曾清除本地作用域 | 账号切换身份字段未核验，不复用旧作用域；WebVPN／校外路径未测。 |
| `data/network` | 固定主机、路径、方法、参数名白名单；教务 POST 表单、门户 GET 在当前环境被实际响应接受；匿名 302、无 Cookie 302 已观察 | 真正服务器 HTTP 200 登录 HTML 未遇到，仅合成测试覆盖。 |
| `data/database`, `data/repository` | Room v1 缓存迁移、进程重启先读旧课表和两项余额、断网／认证失效保留旧数据及恢复通过 | 仅本机 Android 模拟器测试；API 24 真机尚未安装验收。应用私有缓存未额外加密。 |
| `integration/academic` 学期／课表 | 学期 78 项、教学周 26 项、校区 1 项、学期课表 24 条、当前周 11 条；非空未排课程有样本 | 多校区、实践课程非空、单双周与临时变化未核对。节次响应无起止时间；教师／周次组合描述未拆分。A03 星期字典未接入。 |
| `integration/academic` 成绩 | G01 6 学期；一个历史学期 G02 17 门；G03 一门详情 3 个组成项；G05 官方 GPA 字段；本学期成功为空 | G04 未接入；历史成绩只核对一个学期，未与官方页面逐项人工比对。 |
| `integration/academic` 考试 | E01 当前学期成功为空，`arranged/notArranged/showkeys` 包裹层已核对 | 无非空考试样本；时间、地点、座位及提醒时间不可宣称已验证。 |
| `integration/portal` 余额 | P02 动态 key `card.balance`／`net.balance` 与 P03 详情两项均成功；数值与遮罩分开 | 本账号本次未出现遮罩值；遮罩解析只经合成样本。余额未与学校页面数额逐项人工比对。 |
| `integration/portal` 消息 | P10 第 1 页返回 1 条，`content` 为数组、已读为布尔、`log_id` 为数字；无 PUT 已读调用 | 翻页、来源及已读筛选其他组合未核对。正文应按纯文本渲染。 |
| `integration/portal` 待办 | P08 待办、已办及 P09 我的申请均成功为空 | 非空记录字段未知；适配器对非空记录报 `SCHEMA_CHANGED`，不能在前端当作已完成列表。 |

`ServiceRepository`、门户日程和旧一卡通不在这次已完成契约内。支付、选课、挂失、申请提交、课表确认、承诺书确认和服务端标记已读没有调用入口。G03 详情仅允许从已查出的本账号成绩 `sourceId` 读取，不猜 WID。缓存的成功时间是客户端时间，不冒充学校更新时间。

## 契约冻结说明

冻结范围是当前 `core/contract` 的 `SessionRepository`、`AcademicRepository`、`PortalRepository`、模型、错误与状态语义。`Grade.sourceId` 仅是调用详情的本地不透明句柄；UI 不解析其内容。若前端提出必要字段变更，须修改契约、实现和测试并记录迁移。未取得非空考试／待办、跨校区或实践样本时，UI 应显示“当前账户数据样本未验证”或对应错误状态，不以演示数据和业务 WebView 代替。

研究细节及每个操作的实际字段证据见 [接口与认证研究](../03-接口与认证研究.md)。
