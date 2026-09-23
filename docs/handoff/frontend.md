# NEO NEU 前端实现说明

更新日期：2026-09-23。本文记录已经集成到正式 `:app` 的原生界面和装配位置。页面目标以[UI 设计](../06-UI页面布局设计.md)为准；真实数据验证范围以[数据层交接](data.md)和[验证清单](../04-验证与发布清单.md)为准。

## 工程配置

| 项目 | 当前配置 |
| --- | --- |
| Android Gradle Plugin / Gradle Wrapper | 9.4.1 / 9.7.1 |
| Kotlin Compose 插件 | 2.2.10 |
| Miuix | 0.9.4，依赖位于 `core/ui/build.gradle.kts` |
| SDK | `compileSdk 37`、`minSdk 24`、`targetSdk 36` |
| applicationId | `edu.neu.campus.neoneu` |
| JDK | 21 |

根构建配置启用了 `-Xskip-metadata-version-check`，用于当前 Kotlin 与 Miuix 制品元数据的兼容。升级 Kotlin、Miuix 或 AGP 时应重新编译和运行测试，不能把该参数当成长期兼容保证。

## 代码位置

| 位置 | 职责 |
| --- | --- |
| `app/MainActivity.kt`、`app/MainScreen.kt`、`app/navigation/` | 正式入口、四项底部导航、详情页切换与官方登录启动 |
| `app/CampusDataProvider.kt` | 真实 `CampusData` 与显式演示模式的数据源选择 |
| `app/feature/` | 今日、课表、成绩、考试、余额、消息、待办、作息与校历、查询、服务和设置页面 |
| `app/registry/FeatureRegistry.kt` | 查询功能标识、分类、搜索别名与入口状态 |
| `app/config/HomeLayoutConfig.kt` | 首页快捷入口、摘要显示与顺序；使用本机 `SharedPreferences` |
| `app/demo/DemoDataRepository.kt` | 虚构样本与演示模式提示，不作为接口验证 |
| `core/ui/` | Miuix 主题、通用状态组件、课表网格和课程详情抽屉 |

数据查询经 `CampusDataProvider.academic`、`.portal`、`.session` 使用 `core/contract` 契约；正式页面不应自行拼学校 URL、处理 Cookie 或复制接口 DTO。`OfficialLogin.intent(this)` 仅启动官方认证页；返回后 `session.verify()` 复验会话。

## 页面范围

四个主入口是**今日、课表、查询、我的**。详情入口在 `MainActivity` 中按 `AppDestination` 路由；主要页面已有原生 Compose 实现。首页快捷项和摘要模块由本机配置管理；余额隐私、消息本机阅读状态和主题偏好分别由对应管理器保存到本机。

演示模式使用 `DemoDataRepository`，并在主页面与详情页顶部显示提示。正常模式从真实 Repository 读取，查询失败不会自动切换为演示样本。非空考试、非空待办、多校区和实践课程等场景仍须真实数据复验，不能只凭页面代码或演示样本宣布完成。

## 构建与继续验证

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleDebug
```

Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。截至本次整理，上述命令通过；正式 App 的逐页真机、release 性能及不同 Android 版本验收尚未记录，后续按[验证与发布清单](../04-验证与发布清单.md)补齐。

新增查询入口时，先在 `FeatureRegistry` 增加稳定 ID、分类和搜索别名，再接到 `AppDestination` 与对应页面。需要首页快捷入口时加入配置候选；需要首页摘要时在 `HomeLayoutConfigManager` 注册模块，新增摘要默认关闭。数据层新增字段或状态需同步更新 `core/contract`、适配实现和使用它的页面。
