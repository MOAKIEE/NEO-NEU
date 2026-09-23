# Miuix 技术选型与适配

## 选型结论

采用 Android 原生 Kotlin + Jetpack Compose + Miuix。旧 APK 使用 Flutter 只作为研究背景，不继续采用 Flutter UI，也不在 Flutter 内嵌 Compose 作为主要界面。

全部核心查询界面原生实现。`integration/auth-web` 的范围仅限必要的官方认证与用户主动打开外部页面，不承担课表、成绩、考试、余额或消息的页面展示。接口适配失败不以 WebView 页面替代；数据传输、解析、缓存与 Compose 渲染解耦。

首版是单平台 Android 应用，不需要为了组件库使用 Compose Multiplatform 的所有平台目标。Miuix 官方文档提供 Android Compose 依赖方式，适合当前需求。

## 已核验的版本

研究日期为 2026-09-22。GitHub 最新正式 Release 为 `v0.9.4`，发布时间 2026-09-20。读取源码提交为 `39c40f99844227b853f0049a0933b1f3ae6c00ba`，Maven Central 的 `miuix-ui-android:0.9.4` POM 和 AAR 均成功读取。

| 项目 | 已核验信息 | 对项目的影响 |
| --- | --- | --- |
| 发布版本 | 0.9.4 | 锁定具体版本，不使用 `+` |
| 核心 AAR minSdk | 24 | 不再沿用旧 APK 的 minSdk 21 |
| 核心 AAR minCompileSdk | 37 | 编译 SDK 不能低于 37；不代表只能运行在 API 37 |
| Kotlin 依赖 | POM 指向 2.4.20 | 先采用与上游一致的工具链验证 |
| Compose Foundation | POM 指向 1.12.0 | 不随意覆盖成旧版本 |
| 上游 AGP / Gradle | 9.4.1 / 9.7.1 | 是上游构建配置，不等于已验证的本项目配置 |
| 上游 JDK | 21 | 工程优先以 JDK 21 配置 |
| 模糊模块 | 源码明确 minSdk 33 | 首版不直接依赖，以保留 API 24 构建下限 |
| API 稳定性 | 官方标为实验性 | 包一层项目组件，集中处理升级 |

核心、core、squircle、shader、preference 的 0.9.4 AAR 都核验了 minSdk 24 与 minCompileSdk 37。尚未创建 Android 工程或执行 Gradle 编译，因此“制品存在”不等于“项目已构建通过”。

## 依赖起点

```kotlin
// Android Compose 工程；示意，需结合完整 Gradle 配置编译验证。
dependencies {
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    // 需要扩展图标时再添加：
    // implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")
}
```

仓库使用 Maven Central。`miuix-ui` 已传递包含 core 和 squircle，无需重复添加。首版不加 blur；运行时判断 API 等级不能消除依赖 Manifest 的最低版本约束，也不建议用 overrideLibrary 强行绕过。

`compileSdk` 与 `targetSdk` 分开决定：前者受 AAR 要求约束；后者需要按使用的系统行为逐项测试。不能因为上游示例 targetSdk 为 37 就直接声称本应用已适配。

## 页面与组件映射

| 产品位置 | Miuix 组件或机制 | 项目补充 |
| --- | --- | --- |
| 全局主题 | MiuixTheme、ThemeController | DataStore 保存系统／浅色／深色偏好 |
| 主框架 | Scaffold、TopAppBar | 安全区、返回栈、刷新状态 |
| 四项导航 | NavigationBar、NavigationBarItem | 保留每页滚动位置，始终显示文字 |
| 今日卡片 | Card、Text、Button | 业务加载和过期提示 |
| 查询搜索 | SearchBar、InputField | 本地关键词、分类和取消 |
| 学期及周次筛选 | TabRow、下拉组件 | 以服务器学期 ID 为准 |
| 课程详情 | OverlayBottomSheet | 多周次地点列表 |
| 异常提示 | Snackbar、OverlayDialog | 错误分类与恢复操作 |
| 我的设置 | SwitchPreference、ArrowPreference 等 | 仅改变本地设置 |
| 课表网格 | Compose 自定义 Layout | 冲突布局、节次高度、可访问语义 |

Miuix `NavigationBar` 支持 2—5 项，四项导航符合组件能力。`OverlayBottomSheet` 等 Overlay 组件依赖 Miuix `Scaffold` 提供的 PopupHost；仅使用同名 Material Scaffold 不能假设可替代。

建议在 `core/ui` 中封装 `CampusScaffold`、`QueryCard`、`LoadStatePanel`、`TermSelector`。业务页面不直接散布实验性 API；依赖升级只在单独变更中进行。

## 客户端架构

```text
app                    导航、依赖装配、启动
core/ui                Miuix 封装、主题、状态组件
core/contract          双方共享模型与 Repository 接口
data/session           分域会话、认证恢复、退出
data/network           请求白名单、错误解释、脱敏
data/database          Room 缓存、来源及账号隔离
data/repository        统一契约的真实实现
feature/today          今日聚合
feature/timetable      课表与节次
feature/grades         成绩与官方统计
feature/exams          考试
feature/balances       校园卡与网费
feature/messages       门户消息与教务消息
feature/tasks          待办摘要
feature/settings       本地设置
integration/portal     门户契约适配
integration/academic   教务契约适配
integration/auth-web   官方认证与会话衔接；不承载业务查询页面
```

采用 ViewModel + StateFlow 管理界面，Repository 负责缓存、合并与刷新，网络客户端负责实际传输。OkHttp、Room、DataStore 为建议选型，具体版本在工程初始化时核验并锁定。不要把浏览器页面 DOM、服务器 DTO 或 Cookie 直接暴露给 UI。

## 适配验收

重点验证 Android 系统 WebView 登录、Cookie 持久化、跨域单点登录、进程重建、系统返回手势、边到边布局与大字体。只有本地查询时不请求相机、定位、麦克风和全部文件权限。

最低版本构建支持与设备实际验证分开记录。Miuix 是界面库，使用它不代表需要小米手机、Root、Shizuku 或 Xposed；上述能力也不属于本 App 需求。

## 证据

- [Miuix 仓库](https://github.com/compose-miuix-ui/miuix)
- [0.9.4 Release](https://github.com/compose-miuix-ui/miuix/releases/tag/v0.9.4)
- [Android 接入文档](https://github.com/compose-miuix-ui/miuix/blob/39c40f99844227b853f0049a0933b1f3ae6c00ba/docs/zh_CN/guide/getting-started.md)
- [0.9.4 POM](https://repo.maven.apache.org/maven2/top/yukonga/miuix/kmp/miuix-ui-android/0.9.4/miuix-ui-android-0.9.4.pom)

本地核验材料：`research/public-web/miuix-release.json`、`research/public-web/miuix-0.9.4.pom`、`research/public-web/miuix-aar-metadata.json`。源码通过上方固定提交链接追溯；本地第三方仓库副本不作为本项目开发指南。Miuix 使用 Apache-2.0 许可证；发布时保留适用的许可证和版权声明。

