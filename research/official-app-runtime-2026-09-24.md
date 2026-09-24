# 官方 App 网络行为实测

日期：2026-09-24。官方 App：智慧东大 `com.sunyt.testdemo`；设备：TestDevice Android 模拟器。用户本人完成登录。目标是验证页面取数和认证跳转，不修改业务数据。

## 方法及证据边界

通过本机 mitmproxy、ADB reverse 和临时用户 CA 观察门户及教务 HTTPS 请求。只解密 `personal.neu.edu.cn`、`jwxt.neu.edu.cn`；其他域透传。插件流式转发正文，不保存正文、请求参数、请求头、Cookie、票据或原始 HAR。学校 CONNECT 仅记录域名。路径长标识符已替换为 `{id}`，部分带哈希的静态资源名也会被保守脱敏。

可复查的精选元数据：[official-app-request-evidence.jsonl](official-app-request-evidence.jsonl)；采集插件：[metadata_proxy.py](tools/metadata_proxy.py)。`t_s` 是各采集阶段内的相对秒数，v1 与 v2 不能相减。插件热加载会重新开始计时，正式比较仅采用未再热加载的 v2。

首次使用否定式 ignore-hosts 过滤时漏掉了请求（规则也可能匹配 IP）；该阶段只保留明确捕获到的事件，不统计缺失请求。改用 allow-hosts 后，日程和教务请求均可见。采集工具没有覆盖 Flutter 可能绕过系统代理的连接，因此未捕获不能证明未发生。

## 已观察事实

| 场景 | 证据 | 结论边界 |
| --- | --- | --- |
| 本次启动模拟器 | App 显示登录页，用户重新登录 | 与此前仅杀 App 进程后仍已登录不同；没有控制间隔、模拟器状态等变量，不能断言是重启导致会话失效 |
| 首次进入教务（v1） | `GET /jwapp/sys/homeapp/index.do` 为 302，随后 CONNECT `pass.neu.edu.cn`，教务页面显示内容 | 符合入口跳转统一认证的行为；未读取 Location、CAS 响应或 Cookie，不能证明具体票据交换及续期机制 |
| 日程第一次（v2） | t≈10–12 秒，页面及 `/portal/personal/frontend/data/info`、`/portal/schedule/frontend/calendar/calendar`、`/portal/schedule/frontend/default/index` 均 HTTP 200 | 真实网络请求已发生；未读正文，不断言每项业务码成功 |
| 返回首页后再次进入日程 | t≈26–28 秒，上述业务接口再次请求并返回 200 | 约 16 秒内重新进入也会重新取数；不能据此推导所有页面或长期轮询周期 |
| 再次进入教务（v2） | t≈43 秒，教务入口 HTTP 200；随后首页用户、学期、周次、成绩、教学安排、消息等 API 返回 200 | WebView 确实使用教务网页首页 API；不是仅显示本地静态快照 |
| 教务切到后台再返回 | t≈109.5–109.6 秒出现 `currentUser.do`、`announcement.do`、`artificialMessage.do`、`messages_pc.do` HTTP 200 | 返回前台附近确有部分请求；未控制网页定时器，不能判定必然由 onResume 触发，也没有证明全量刷新 |

日程页面还自动发送 `/portal/frontend/site/log` POST。这是网页自身行为，不应当复制到本项目的只读白名单。教务首页也有重复配置、成绩等请求；本项目应提取必要查询并合并重复请求，不照搬整页资源请求。

## 对设计的修正

1. 每次真实页面进入、冷启动、回到前台都应刷新可见业务数据。缓存用于立即展示和失败兜底，不能因 3–5 分钟 TTL 而直接跳过进入时查询。同一轮 UI 事件与正在执行的相同查询仍需合并。
2. 门户网页 `data/info` 路线得到运行时支持。教务首页 API 与项目课表/成绩详情 API 是不同接口族；没有参数和业务响应验证前不替换现有查询白名单。
3. 会话恢复仍采用分域复用 Cookie、官方 HTTPS 入口尝试 SSO、复验、原查询最多重放一次。此次 302→认证域连接支持该方向，不能把它描述成已实现或已验证长期无感续登。
4. 原生 HTTP 层目前将所有 3xx 和 HTML 都当作认证过期；实现时需要区分已知认证跳转、普通跳转、维护/错误 HTML。尤其未来加入条件请求后，304 不能被误判成登录失效。
5. 仍需验证 Cookie 写入一致性、业务域单独过期、CAS 失效、验证码、跨账号隔离和长时间闲置。未测得 refresh token 接口、固定登录有效期或后台保活收益。

## 环境恢复

已删除全局 HTTP 代理设置、移除 ADB 的 8899 reverse、停止本次代理进程；已在系统用户凭据页卸载 mitmproxy CA，并确认列表不再显示该证书。模拟器中的证书安装文件和临时 UI XML 已删除。官方 App 已重新置于前台；没有清除其登录状态。
