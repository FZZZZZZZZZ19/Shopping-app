# Shopping APP 1.1.0

网购商品价格各大平台查询APP安卓。完整 Android 工程源码、构建脚本、测试与文档现已在此仓库提供。

一次搜索，汇总淘宝、拼多多、京东、天猫搜索页上的商品、平台、店铺与价格，并按价格排序。Android 8.0 及以上可用。

## 安装与升级

从 [GitHub Releases](https://github.com/FZZZZZZZZZ19/Shopping-app/releases) 下载 `shopping-app-1.1.0.apk`。包名仍为 `com.shopping.app`，versionCode 从 1 升到 2，使用与 1.0 相同的试用签名，可直接覆盖升级；收藏和旧版手动报价本保留。

## 使用

1. 点击首页的平台登录入口，在本应用内的官方网页完成各平台登录。外部淘宝/拼多多/京东 APP 的登录状态通常不会自动共享给 WebView。
2. 输入商品名称，最好带型号、容量、颜色，例如 `小米 17 256GB 黑色`。点一次搜索，应用分别加载四个平台的官方搜索页。
3. 读取到的商品进入同一个原生列表，显示标题、平台、店铺、页面价格与读取时间。可以选全部/单个平台、价格升序或降序，以及严格关键词/规格筛选。
4. 需要登录、验证码或网络处理时，点击对应平台进入官方页面，由你完成操作。登录跳到首页后，返回汇总并点该平台的“重新搜索”。
5. 仅采集页面已加载的商品。可打开页面滚动加载更多，再点“采集此页”并返回汇总。

## 数据来源与限制

**1.1 使用网页登录后的可见页面采集，不是平台授权 API。不能保证四个平台的网页在每种设备和账号状态下都可识别。**

- 仅读取商品卡片 DOM 与公开 JSON-LD，不调用未公开接口，不读取 Cookie、密码、会话令牌或网页私有 JS 状态，不绕过验证码。
- 访问受限、登录要求、验证码、页面布局变化或没有展示商品字段时，分别提示状态；不生成假价格补齐四个平台。
- 页面未展示店铺名时明确显示“页面未展示店铺名”。没有可识别商品链接时允许回到来源搜索页核对。
- 搜索与标题匹配得到的是**同款候选**，不能证明 SKU 完全一致。严格模式可排除部分型号/容量差异，仍应核对颜色、容量、成色与套餐。
- 排序使用页面展示价，可能是起价或有条件优惠价，不保证为指定 SKU 的结算价；运费、地区、会员和优惠门槛可能不同。
- 只汇总已加载的结果；不是四个平台的全量商品目录。真实四平台登录后端到端结果还需要在用户手机上验证，自动化页面测试不能代替这一验证。

## 源码与发布包

`app/src/main/` 包含全部应用实现，`app/src/test/` 和 `app/src/androidTest/` 包含测试，根目录含 Gradle Wrapper 和构建配置。可以克隆 `main` 分支或下载该分支 ZIP 构建。源码不包含签名私钥、账号登录数据或本机构建缓存。

历史 `v1.0.0`、`v1.1.0` 标签在源码补齐前创建，因此这些标签自动生成的 Source code 压缩包仍是当时的发布文件快照。完整源码请使用 `main` 分支，或下载 [1.1 完整源码 ZIP](https://github.com/FZZZZZZZZZ19/Shopping-app/releases/download/v1.1.0/shopping-app-1.1.0-source.zip)。已发布 APK 保持原文件与签名。自行构建的默认调试证书可能不同，覆盖已安装发布版需要相同签名。

## 本地数据

原 1.0 的店铺收藏、手动比价本、搜索历史、JSON 备份导入导出继续保留。手动报价与网页采集结果分别显示。平台登录由系统 WebView 管理，Cookie 留在本机；“我的”页面可清除登录状态。没有自建数据上传服务器、统计 SDK 或广告。

## 构建与检查

需要 JDK 17/21、Android SDK 35。

```sh
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
scripts/device-smoke.sh
```

设备测试会清空应用本地数据，应仅在一次性模拟器运行。四平台 DOM 到列表的测试使用测试包中的 HTML 测试页面；生产 APK 不包含测试商品或演示价格。

DOM 解析测试的开发依赖已锁定在 `package-lock.json`：

```sh
npm ci --ignore-scripts
npm run test:collector
```

`MainActivity` 为首页/旧版数据功能，`SearchActivity` 为聚合列表，`PageCollector` 管理四个平台的 WebView，`collect-offers.js` 读取页面公开 DOM，`Offer` 负责匹配/去重/排序，`OfferParser` 校验金额和字段。测试证据与实际限制见 [docs/VALIDATION-1.1.md](docs/VALIDATION-1.1.md)。
