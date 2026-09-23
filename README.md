# NEO NEU

NEO NEU 是面向东北大学学生的 Android 原生校园信息查询 App。界面使用 Kotlin、Compose 和 Miuix；数据层读取学校查询接口并保存在本机。学校官方网页用于用户自行认证，项目不部署自建服务器，也不收集学校账号密码。

## 当前状态

前端 `app/`、`core/ui/` 与本地数据层已集成，已完成一轮 Miuix V2 视觉重构及代码审查修复。正式 App 已移除演示模式，只读取真实 Repository；暂无数据时显示明确状态。现有页面包括今日、课表、查询、我的，以及成绩、考试、校园卡与网费、消息、待办、作息与校历等查询入口。`verification/` 是独立的数据验证宿主，不属于正式 App。

**代码可构建不等于所有场景已完成真实验收。** 数据层曾在 Android 模拟器上验证登录、课表、一个历史学期的成绩及详情、两项余额、消息、空考试与空待办，以及缓存、离线和认证失效恢复。非空考试／待办、多校区、实践课程和 API 24 真机仍缺样本或设备验证；正式 App 的完整端到端和 release 真机验收也尚未记录。详情见[验证与发布清单](docs/04-验证与发布清单.md)。

## 构建

需要 JDK 21 和 Android SDK 37。在仓库根目录运行：

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleDebug
```

其他平台用 `./gradlew` 执行相同任务。输出为 `app/build/outputs/apk/debug/app-debug.apk`。调试数据层时可单独运行 `:verification:assembleDebug`。真实登录须由用户在学校官方页面自行完成，演示数据与合成测试不算真实接口验收。

## 文档

| 文档 | 用途 |
| --- | --- |
| [文档索引](docs/README.md) | 当前文档地图和阅读顺序 |
| [验证与发布清单](docs/04-验证与发布清单.md) | 已验证内容、待补场景与发布前检查 |
| [前端实现说明](docs/handoff/frontend.md) | 页面、数据装配和本地配置 |
| [数据层交接](docs/handoff/data.md) | Repository 契约、接口实测范围与限制 |
| [接口与认证研究](docs/03-接口与认证研究.md) | 学校协议证据和只读边界 |

## 仓库结构

```text
app/                正式 App、页面、导航及本地显示配置
core/contract/      共享模型与 Repository 契约
core/ui/            Miuix 主题与通用界面组件
data/               会话、网络、Room 缓存及 Repository 实现
integration/        教务、门户与官方认证适配
verification/       独立 debug 数据验证宿主
docs/               产品设计、接口证据、实现说明和验证清单
research/           公开网页脚本及依赖核验材料，仅供追溯
```

首版只做查询。代码没有支付、选课、挂失、申请提交、课表确认或服务端消息已读入口。仓库不得提交姓名、学号、成绩、余额、密码、Cookie、票据或原始 HAR。
