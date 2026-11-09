# 1.11.0 验证记录

日期：2026-11-09。版本：`versionName 1.11.0`、`versionCode 3`、包名 `com.shopping.app`。

## 构建

- Gradle 8.9、AGP 8.7.3、JDK 21、compileSdk/targetSdk 35、minSdk 26。
- 本地完整构建通过：`assembleDebug`、`testDebugUnitTest`、`lintDebug`、`assembleRelease`。
- 本地环境是 ARM64 Android，Google 的 build-tools/aapt2 只有 x86-64 版本，因此本地构建使用
  `-Pandroid.aapt2FromMavenOverride=$PREFIX/bin/aapt2` 指向 Termux 的 ARM64 aapt2。这只是本机验证手段，
  不写入项目；CI 在 Ubuntu x86-64 上使用标准 SDK，不需要该参数。
- `assembleRelease` 产物 `app-release.apk`：非 debuggable，证书 DN `CN=Shopping APP, OU=Mobile, O=Shopping App, C=CN`，
  证书 SHA-256 `c2a39e9a6c8002e76433cbec1e348ae286de4ca514b6756f59295845cae1b369`。

## 测试

- JVM 单元测试 24 项全部通过：OfferTest 9、PlatformTest 4、PricesTest 6、VersionsTest 5。
- DOM 采集测试 10 项通过（`npm run test:collector`）。
- Android Lint：0 errors、0 warnings（修复了 `ObsoleteSdkInt` 后复跑）。
- 设备测试（9 项旧数据检查 + 5 项聚合检查）需要模拟器/真机，本次未运行；1.1.0 的历史证据仍在
  `docs/test-results/1.1/`。

## 1.11.0 新增：应用内更新

- 检查接口：`https://api.github.com/repos/FZZZZZZZZZ19/Shopping-app/releases/latest`。
- 只选择 Release 附件中名称以 `.apk` 结尾的文件，优先 `shopping-app*`。
- 版本比较在 `Versions` 中完成，纯 Java 单元测试覆盖 `1.11 > 1.9`、`1.0.10 > 1.0.9`、
  `1.11 == 1.11.0`、预发布后缀不计入数字比较。
- 下载后、拉起安装器前校验：包名必须是 `com.shopping.app`，签名证书必须与已安装应用一致。
- 首次安装未知来源应用时，引导到 `ACTION_MANAGE_UNKNOWN_APP_SOURCES`。
- 下载文件保存在应用私有 `files/updates/`，通过内置只读 `ContentProvider`（无 AndroidX）交给系统安装器。

## 未验证 / 限制

- 未在真机上完成“安装 1.11.0 → 发布更高版本 → 应用内下载 → 覆盖安装”的端到端流程；
  这需要先发布一个高于 1.11.0 的测试 Release，或在本地搭一个返回测试 JSON 的端点。
- 1.11.0 换了正式签名，无法覆盖安装 1.1.0；旧版用户需导出备份、卸载、安装 1.11.0、导入备份。
- 未登录 GitHub API 每小时 60 次请求限制；触发后会提示稍后再试。
- 四平台网页采集的限制与 1.1.0 相同，本次未改变。
