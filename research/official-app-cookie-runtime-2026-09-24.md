# 官方 App 教务 WebView 会话补充核验

日期：2026-09-24。样本为用户提供的“智慧东大”3.1.8 `base.apk`，SHA-256 `84b9b5f80d3107f92663f1bacea7186f1b71c2492f2865b0705d69f8899df406`。测试设备为已由用户登录的 Android 模拟器。本次只观察学校门户和教务域的请求路径、状态、Cookie 名称与属性、JSON 字段名；不记录 Cookie 值、账号值、密码、票据、查询参数或响应正文。

## APK 与页面结构

- APK 为 Flutter 容器，包含 `flutter_inappwebview` 和注入的 `assets/flutter_assets/assets/bridge.js`。脚本提供网页到 Flutter 的桥接方法，包括获取 token 和 token 失效通知；脚本中的模拟 token 文本不能视为真实凭据。
- `libapp.so` 可见 `saveCookiesToLocal`、`saveInAppSyncCookie2Dio`、`PersistCookieJar`、`_scheduleAutoLoginIfStillNeeded`、`neu_auto_login_retried`，以及门户 `/manage/common/app_login/check`。这些是整个多模块 App 的字符串，尚不能据此断言教务 WebView 使用了某一特定持久化或刷新函数。
- 官方首页是 Flutter 页面；点“日程”打开门户网页，点“教务系统（新）”打开教务网页。前次运行时记录和本次新采集均看到网页业务请求，确认教务页面不是静态截图。

## 教务入口与 Cookie

一次进入教务的脱敏请求顺序：`GET /jwapp/sys/homeapp/index.do` 返回 302，随后同一路径返回 200。302 响应设置 `JSESSIONID`，属性为 `Path=/jwapp`、`HttpOnly`，没有 `Max-Age` 或 `Expires`。成功加载的页面随后请求 `currentUser.do`、学期、周次、课程等首页接口。本次未解密 `pass.neu.edu.cn`，因此不能从本轮记录还原 CAS 中间跳转；前次元数据采集曾在教务 302 后观察到连接认证域。

教务业务请求携带的 Cookie **名称**有 `CASTGC`、`CK_LC`、`CK_VL`、`JSESSIONID`、`_WEU` 和 `route`。首次短时采集有 34 个教务响应设置 `_WEU`，其 `Path=/jwapp/`，均无 `Max-Age` 或 `Expires`；另一次采集中连续可比较的 32 次设置，值每次都与上一条不同。这里只比较内存中的值是否变化，没有保存或输出值。频繁更新 `_WEU` 证明网页请求会刷新这个 Cookie，**不能证明它延长 `JSESSIONID`、CAS 或服务器会话的绝对有效期**。

再次进入教务时，已有效的网页会话直接取得首页和业务 HTTP 200，没有新的教务入口认证 302。HTTP 200 仅证明请求完成，不替代业务码和内容核验。

## 账号标识候选

门户 `/portal/personal/frontend/data/info` 的 `d.info` 含 `xgh`，教务 `/jwapp/sys/homeapp/api/home/currentUser.do` 的 `datas` 含 `userId`。采集插件只在内存中比较，在本次同一测试账号下二者相等；`uid` 和 `identity_id` 与教务 `userId` 不等。未输出、保存或提交任何标识值。`xgh` ↔ `userId` 可作为同账号核对候选，但目前只有一个账号样本，不能直接用于跨账号缓存迁移或自动换号判断。

## 对本项目的含义

1. 原生请求收到教务认证 302 后，官方 WebView 的入口加载能够经学校网页流程取得新业务会话；本项目不应删除可能仍有效的 CAS 和门户 Cookie。此前 `c9ab203` 已修正补登时的无条件清除。
2. 本项目 `SchoolHttp` 已按响应 URL 接受 `Set-Cookie` 并回写同一个 Android `CookieManager`，这是必要的，因为 `_WEU` 在正常网页业务响应中反复更新。不能把一次登录时的 Cookie 当作永远不变的值缓存。
3. 下一步可在**前台且教务明确过期**时尝试一次学校 WebView 入口恢复；若跳到需输入密码、验证码或二次认证的页面，必须转为用户可见操作。要在原有缓存作用域自动重放查询，先需用门户 `xgh` 与教务 `userId` 做更充分的同账号核验。

## 采集与清理

采集脚本为 [`tools/cookie_metadata_proxy.py`](tools/cookie_metadata_proxy.py)。mitmproxy 未使用 `-w`，仅解密 `personal.neu.edu.cn`、`jwxt.neu.edu.cn`；其他域透传，流式转发非目标响应正文。结束后已删除全局 HTTP 代理、ADB reverse 和设备临时证书文件，停止代理进程，并在系统“用户凭据”页卸载临时 CA，确认该页显示没有用户凭据。临时截图及代理输出不进仓库。
