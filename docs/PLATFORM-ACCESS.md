# 官方平台入口检查

检查日期：2026-11-09；关键词：耳机；移动浏览器 User-Agent。以下是当前云构建环境的 HTTP 入口探测，不等同于真机登录后的搜索、价格或支付流程验证。

| 平台 | 官方搜索入口 | 当前环境结果 |
| --- | --- | --- |
| 淘宝 | `https://s.m.taobao.com/search?q=…` | 云代理 CONNECT 请求返回 403，未验证页面内容 |
| 拼多多 | `https://mobile.yangkeduo.com/search_result.html?search_key=…` | HTTP 200；未验证登录后结果和价格 |
| 京东 | `https://so.m.jd.com/ware/search.action?keyword=…` | HTTP 200，重定向至京东风控页面 |
| 天猫 | `https://list.tmall.com/search_product.htm?q=…` | 云代理 CONNECT 请求返回 403，未验证页面内容 |

云环境拒绝访问不能证明用户手机网络也不可达。HTTP 200 也不能证明平台搜索功能完整可用。应用提供外部打开按钮、加载错误提示和重试，平台仍可能要求登录、验证码或官方应用。

此版本没有获取或缓存任何平台商品价格数据；比价本的数据来源为用户手动录入。
