# 项目说明

NEO NEU 是面向东北大学学生的第三方 Android 教务与校园信息查询应用，并非学校官方客户端。使用 Kotlin、Jetpack Compose 和 **Miuix 组件库**构建原生界面；首版只做查询，认证由用户在学校官方网页自行完成，不接管账号密码。

## 工程结构

- `app/`：正式应用、页面与导航。
- `core/contract/`：共享数据模型和 Repository 契约；`core/ui/`：Miuix 主题、通用组件与课表 UI。
- `data/`：会话、网络、Room 缓存与数据仓库；`integration/`：教务、门户和官方网页登录适配。
- `verification/`：独立的数据验证宿主，不属于正式应用。
- `docs/`：开发、界面、接口和验证说明；`research/`：研究材料。

## 开发约定

- 新增或修改界面时使用项目的 Miuix 主题与 `core/ui` 组件，先看 `docs/界面规范.md`。Miuix 版本固定在 `core/ui/build.gradle.kts`，避免页面各自引入另一套视觉组件。
- 界面文案只保留帮助用户理解数据或完成操作的信息。标题、筛选项和内容已表达的意思不要再用副标题、小字或页脚重复说明；单一来源页面不常驻展示来源与同步时间。来源可能混淆时标明来源；登录失效、离线、失败、空结果和旧缓存等状态仍需明确提示，旧缓存应显示最近成功同步时间。
- 页面通过 `core/contract` 的 Repository 读取真实数据，不直接处理 Cookie、学校接口 DTO 或业务 URL。登录、失效、离线、空结果和失败应分别呈现，不用演示数据冒充真实查询。
- 仅实现只读功能；不提交选课、支付、挂失、申请或消息已读等学校端写操作。不要把姓名、学号、成绩、密码、Cookie、票据及原始会话记录提交到仓库。
- 官方客户端 `base.apk` 保留在项目根目录，供模拟器安装调查；`research/official-apk/unpacked/` 是其解包参考材料，`badging.txt` 和 `manifest-tree.txt` 提供可读的包信息。官方客户端只作为行为与界面参考，不属于本项目源码或构建输入。
- 变更前按需阅读 `docs/开发指南.md`、`docs/接口参考.md` 和 `docs/验证清单.md`。构建与测试：`.\gradlew.bat testDebugUnitTest :app:assembleDebug`（JDK 21、Android SDK 37）。
