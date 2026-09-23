# NEO NEU 前端实现说明

更新日期：2026-09-23。记录 Miuix 界面重构（含本轮界面与动效重构）及审查修复后的当前实现。构建、真实样本与待验收事项统一维护在 [验证清单](../04-验证与发布清单.md)；设计令牌、组件与动效约定见[界面设计系统与动效](../05-界面设计系统与动效.md)。旧布局、重构方案及单独审查记录已移除，历史版本可通过 Git 查阅。

## 界面维护约束

UI 库固定使用 Miuix，基础控件、导航和弹层优先复用项目封装；课表、时间轴等业务组件可用 Compose 自定义布局。保持今日、课表、查询、我的四栏结构，颜色集中在 CampusColors/ThemeManager，避免另起一套 Material UI 或散落硬编码配色。深浅色、大字体、数据加载和失败状态应与功能一同维护。

页面内不直接写圆角、间距与动画曲线：分别取自 `CampusShapes`、`CampusSpacing`、`CampusMotion`。图标统一使用 Miuix 图标库（`MiuixIcons.Regular.*` 与 `MiuixIcons.Basic.*`），不引入 `androidx.compose.material.icons`。点击反馈统一用 `Modifier.tapScale`，圆角通过其 `clipShape` 传入。

视觉参考：[THU Info 仓库](https://github.com/thu-info-community/thu-info-app)。仅参考功能组织、课程配色和信息层级，不复制品牌素材、源码或学校业务能力；本项目保持 Kotlin + Compose + Miuix。

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

## 代码与数据装配

下表中的页面文件位于 `app/src/main/java/edu/neu/campus/app/`；共享 UI 位于 `core/ui/src/main/java/edu/neu/campus/ui/`。

| 位置 | 当前职责 |
| --- | --- |
| MainActivity / MainScreen / navigation | 官方认证入口、四栏导航、详情路由；所有页面位于 Miuix Scaffold 下，共用弹层宿主。MainScreen 负责标签与二级路由转场 |
| CampusDataProvider | 只装配真实 CampusData；演示仓库和切换入口已删除 |
| registry/FeatureRegistry | 稳定功能 ID、分类、名称/别名/拼音搜索 |
| config/HomeLayoutConfig | 首页快捷入口、模块显示和顺序，本机 SharedPreferences |
| theme/CampusColors、ThemeManager | 品牌色、功能色、课程色、系统/浅色/深色主题；补全 Miuix Colors 全部语义槽位 |
| theme/CampusMotion | 动效时长、缓动、弹簧预设与交错延迟；CampusShapes/CampusSpacing 提供圆角与间距 |
| components/CampusCard、CampusGroup、CampusSection、CampusRow | 卡片、分组容器与列表项，页面不再自绘卡片 |
| components/CampusChips、CampusSegmented | 筛选胶囊、抽屉选择行与分段控件 |
| components/CampusMotion（Modifier.tapScale、StaggeredAppear、CampusPageEnter、ShimmerLine） | 按压反馈、交错入场、整页入场与骨架屏 |
| components/HeroCourseCard、CourseTimeline | 首页品牌主课程卡与今日课程时间轴 |
| components/LoadStatePanel、SafeDataTag、QueryCard、Controls | 状态面板、来源标注、信息卡与开关 |
| components/CoursePresentation、SchoolClock | 可测试的课程时间判断、学校时区分钟时钟 |
| components/AnimatedNumber | 余额与绩点的数字滚动呈现 |
| timetable/TimetableGrid | 动态节次、跨节课程块、真正重叠的安排集合；不硬编码作息时间 |
| feature/* | 首页、查询与业务页面；余额隐私、消息本机阅读与主题偏好保存在本机 |

页面通过 `CampusDataProvider.academic`、`.portal`、`.session` 使用既有契约，不直接处理 Cookie 或学校业务 DTO。本轮没有变更共享 Repository 契约、学校查询端点或 Room schema。

## 页面实际行为

- 今日只以学校标记的当前学期/教学周展示课程，不再用第一学期/第一周冒充当前值。周次取得后独立加载对应周课表；学校时区时钟按分钟更新，跨日重新读取周次。尚未取得数据、成功无课、失败、旧缓存分别呈现。
- 课程状态只使用可解析且完整的起止时间，下课边界不继续显示“正在上课”。存在不完整时间或旧缓存时显示待确认，不推断已全部结束或冲突。
- 课表以返回节次及课程实际覆盖范围生成坐标轴，不限制为固定 12 节；连续课程显示为跨节块，相邻单节课程不算冲突。日期高亮比较实际日期。大字体或全部校区含多个校区时降级为列表；列表带校区名称并按校区排序。
- 校历仅展示学校返回的教学周及日期。作息按返回校区列出节次；缺少时间时明确说明，不补造通用作息、选退课窗口、考试周或假期。
- 成绩保留原始成绩与官方 GPA；缺失字段不默认初修/正常通过。统计与详情失败提供重试及旧缓存说明。
- 考试详情路由携带所选学期和选中记录，避免按课程名在当前学期误匹配。当前契约没有稳定考试 ID，记录已变化/不可用时要求返回列表重新选择；不提供深链接持久身份保证。
- 消息首次进入自动获取；按学校已读状态发起查询，30 条一页，提供上一页/下一页。详情使用同一页码/状态快照。本机阅读只在取得正文记录后标记；未提供学校状态时不默认未读。当前只接入门户来源，不展示虚构的教务独立来源。
- 待办切分类会发起查询；非空记录仍受现有适配器的 SCHEMA_CHANGED 限制。
- 余额展示数值、遮罩及同步信息，不展示无来源的卡状态、在线状态、套餐或充值途径。服务目录仅保留项目已记录的 HTTPS 门户/本科教务入口，由用户主动打开浏览器。
- 我的显示通用“学校账号”，不推断身份、学历。当前没有独立“只清缓存”API，操作明确命名为“退出并清除本地数据”，说明会话和当前账号缓存都会清除；Miuix OverlayDialog 承担确认。

## 页面状态与适配

`SaveableStateHolder` 按主标签/详情路由隔离保存状态，筛选、查询词、学期 ID、周次、校区及列表模式使用可保存状态；滚动组件参与 Compose 保存机制。账号作用域变化时重建页面保存容器，避免旧作用域状态沿用。官方登录返回后复验连接，已连接的教务/门户触发对应数据恢复。

主框架对标签与二级路由分别提供转场动画；二级页面统一用 `CampusPageEnter` 做挂载入场，列表首屏用 `StaggeredAppear` 逐项出现。骨架屏仅在真实加载态出现，空结果与失败不渲染为占位形状。

首页快捷区和余额区、查询重点入口与工具面板根据窄屏/字体放大调整列数；课表大字体切列表。详情补充系统底部安全区及键盘避让。此处记录实现机制，尚不是旋转、进程恢复、所有大字体及真机手势验收通过。

## 验证与继续工作

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`。核心课程逻辑新增 JUnit 回归测试，覆盖时间缺失/边界/过期数据、相邻与重叠节次及跨节分组；测试不依赖真实个人数据。

当前 adb 没有连接设备。本轮只验证了编译与单元测试通过，**未在真机或模拟器上核对视觉与动效**：标签与路由转场、按压反馈、骨架屏、数字滚动、交错入场的实际观感均待设备确认。同样未完成正式 App 逐页截图、官方登录端到端、TalkBack、后台预览隐私和 release 真机性能验收。未实现或仍需完善的界面事项包括：独立欢迎页、标题折叠、外观缩略预览、后台预览隐私设置及完整宽屏排版。当前实现不代表全部界面需求已经验收。

新增入口继续使用 FeatureRegistry 与稳定路由；新增首页模块默认关闭，保留用户配置。真实数据能力变更须同步更新契约、适配、测试和数据交接；界面新增与调整遵循[界面设计系统与动效](../05-界面设计系统与动效.md)。
