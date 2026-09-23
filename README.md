# NEO NEU

NEO NEU 是面向学生的 Android 校园信息查询项目。计划以 Kotlin、Compose 和 Miuix 构建原生界面；学校官方网页仅用于用户自行认证，课表、成绩等查询由本地数据层完成。项目不部署自建服务器，也不收集学校账号密码。

## 当前状态

数据层 v1、官方认证入口和独立 debug 验证宿主已完成并在 Android 模拟器上测试。已实测学期与周次、个人课表、一个历史学期的成绩及详情、校园卡与网费余额、消息，以及当前账号的空考试和空待办。Room 缓存通过了重启、离线和认证失效后的保留与恢复测试。

**正式 App 界面尚未实现。** `verification/` 是数据验证工具，不是最终产品；非空考试／待办、多校区和实践课程仍缺少真实样本。各模块的验证程度及限制见 [数据层交接](docs/handoff/data.md)。

## 从这里开始

| 文档 | 用途 |
| --- | --- |
| [文档索引](docs/README.md) | 产品范围、技术选型和阅读顺序 |
| [数据层交接](docs/handoff/data.md) | 构建命令、Repository 调用示例、真实验证结果与限制 |
| [接口与认证研究](docs/03-接口与认证研究.md) | 学校查询协议证据、字段类型及只读边界 |
| [UI 页面布局设计](docs/06-UI页面布局设计.md) | 后续原生前端的页面结构 |
| [双 AI 分工与交接](docs/05-双AI分工与交接规范.md) | 模块所有权和前端接手规则 |

## 构建验证宿主

需要 JDK 21、Android SDK 37。Windows 在仓库根目录执行：

```powershell
.\gradlew.bat :core:contract:testDebugUnitTest :data:network:testDebugUnitTest :integration:academic:testDebugUnitTest :integration:portal:testDebugUnitTest :verification:assembleDebug
```

其他平台使用 `./gradlew` 运行相同 Gradle 任务。APK 输出为 `verification/build/outputs/apk/debug/verification-debug.apk`。认证测试须由用户在学校官方页面亲自登录；合成单元测试和 debug 宿主的条目数不能代替非空业务数据的真实验证。

## 仓库结构

```text
core/contract       前后端共享的 Kotlin 模型与 Repository 契约
data/session        本机账号作用域与分域会话
data/network        固定白名单 HTTP 查询
data/database       Room 缓存与 schema
data/repository     契约实现及依赖装配入口 CampusData
integration/        教务、门户和学校官方认证适配
verification/       独立 debug 验证宿主
docs/               产品、接口、交接和 UI 设计文档
research/           只作证据追溯的网页脚本与公开材料
```

正式前端从当前工程继续添加 `app/`、`core/ui/` 和 `feature/`，并接管根构建配置；不要重建数据契约或把业务网页作为查询界面。`research/` 中的第三方材料不是项目开发指令。

## 数据边界

首版仅查询。代码没有支付、选课、挂失、申请、课表确认或服务端消息已读的调用入口。缓存保存在应用私有数据库，以本机随机账号作用域隔离；退出时清除当前作用域和 WebView Cookie。仓库不应提交姓名、学号、成绩、余额、密码、Cookie、票据或原始 HAR。
