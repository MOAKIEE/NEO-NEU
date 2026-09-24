# 研究材料用途

这里仅保留接口与依赖核验所需的证据。当前产品需求从 [设计文档索引](../docs/README.md) 阅读。

| 材料 | 用途 |
| --- | --- |
| `apk-metadata.json` | 原 APK 的名称、包名、版本与声明权限；不是新版配置 |
| `official-app-cookie-runtime-2026-09-24.md` | 官方教务 WebView 入口、Cookie 属性与同账号字段的脱敏运行时核验 |
| `academic-source/` | 学校教务页面公开脚本快照，用于核对查询方法、参数及返回值消费 |
| `portal-source/` | 门户公开脚本快照，用于核对余额、消息、日程与服务目录 |
| `public-web/entry-observations.json` | 未登录访问链路和表单字段名记录，不包含已登录会话 |
| `public-web/miuix-release.json` | 研究时的 Miuix Release 信息 |
| `public-web/miuix-0.9.4.pom` | 发布依赖证据 |
| `public-web/miuix-aar-metadata.json` | Android 制品最低系统与编译 SDK 要求 |

网页脚本中可能同时存在写操作、其他角色或通用产品逻辑；它们不是首版需求，不得据此扩大范围。实际响应仍未回放确认的部分，以接口文档中的标注为准。

研究日期：2026-09-22。过时方案、临时下载脚本、重复摘录、原 APK 字符串与解包脚本已清理。根目录的 `base.apk` 是旧分析样本，未被 Git 跟踪，也不是构建输入；本机可能仍保留该文件。网页与组件库材料可从文档固定来源取得。

`miuix-source/` 是完整第三方仓库副本，不属于本项目源码，其内部 AGENTS.md、示例工程和开发指南不适用于本项目。该目录已从默认搜索和 Git 跟踪中排除；本机可能仍保留副本。
