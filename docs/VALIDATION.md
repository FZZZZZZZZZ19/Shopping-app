# 验证记录

日期：2026-11-09。

| 检查 | 结果 |
| --- | --- |
| Android APK 构建 | `assembleDebug` 成功 |
| 单元测试 | 10 项通过，0 失败、0 跳过 |
| Android Lint | 0 错误、0 警告 |
| APK 签名 | `apksigner verify --verbose` 成功，v2 调试签名 |
| APK 元数据 | `com.shopping.app`，1.0.0，minSdk 26，targetSdk 35 |
| 模拟器安装与首页 | Android 9 / API 28 / x86_64 安装成功，已检查实际首页截图 |
| 设备操作测试 | 9 项检查全部通过：启动、导航、收藏、到手价、第二条报价、排序、统计、持久化、清空 |

单元测试覆盖：精确的整数分转换、运费与优惠、非法金额、过大优惠、四平台链接编码、平台识别、伪装域名拒绝、非 HTTPS 和含账号密码链接拒绝。

设备测试覆盖：启动、页面切换、收藏店铺、到手价计算、第二条报价、报价排序、统计、重新启动后的持久化、清空记录。测试使用一次性模拟器，不使用真实账号或交易。

设备结果：[device-smoke.txt](test-results/device-smoke.txt)。该环境没有 KVM 加速，测试使用软件模拟与渲染；测试在启动应用前连接界面观察服务，最多等待 30 秒获取目标页面。

测试产物见 [test-results](test-results/)，实际界面截图见 [screenshots](screenshots/)。构建使用 Gradle 8.9、Android Gradle Plugin 8.7.3、OpenJDK 21、Android SDK Platform 35。

平台入口探测结果和限制见 [PLATFORM-ACCESS.md](PLATFORM-ACCESS.md)。没有验证真实账号登录、真实结算或全部 Android 设备；当前没有接入四平台授权价格 API。

安装包为可直接安装的调试构建。正式商店发行仍需使用账号所有者自己的发布证书。
