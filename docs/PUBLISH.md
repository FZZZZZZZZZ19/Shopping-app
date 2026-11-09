# 源码与 APK 发布

仓库：<https://github.com/FZZZZZZZZZ19/Shopping-app>。

`main` 分支维护完整 Android 应用源码、Gradle 构建配置、测试、文档和发布文件。签名私钥、登录数据、构建缓存不属于公开源码。

## 构建与检查

```sh
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
```

DOM 采集测试和设备测试的运行方式见 README。CI 会构建 debug APK、运行单元测试与 Lint，保存 APK 和报告作为工作流产物。

## 正式签名

1.11.0 起使用正式 release keystore。本地签名材料放在仓库根目录的 `keystore.properties`（已被 `.gitignore` 排除）：

```properties
storeFile=keystore/shopping-release.jks
storePassword=…
keyAlias=shopping
keyPassword=…
```

也可以通过环境变量提供：`SHOPPING_KEYSTORE_PATH`、`SHOPPING_KEYSTORE_PASSWORD`、`SHOPPING_KEY_ALIAS`、`SHOPPING_KEY_PASSWORD`。

密钥库与密码必须离线备份。丢失后无法再发布能覆盖升级的 APK，只能换签名并要求用户重装。GitHub Actions 使用以下 Secrets：

| Secret | 内容 |
| --- | --- |
| `SHOPPING_KEYSTORE_BASE64` | `.jks` 文件的 base64 |
| `SHOPPING_KEYSTORE_PASSWORD` | 密钥库密码 |
| `SHOPPING_KEY_ALIAS` | `shopping` |
| `SHOPPING_KEY_PASSWORD` | 密钥密码 |

## 发布 APK

发布由 `.github/workflows/publish-apk.yml` 完成，不再提交预构建 APK：

1. 更新 `app/build.gradle` 的 `versionCode` / `versionName`；
2. 在 `docs/RELEASE-NOTES-<versionName>.md` 写发布说明；
3. 提交并推送 `main`；
4. 推送标签：`git tag v<versionName> && git push origin v<versionName>`。

工作流会运行 DOM 测试、单元测试与 Lint，构建并签名 `assembleRelease`，校验证书 DN 为 `CN=Shopping APP`，然后创建 Release 并上传 `shopping-app-<versionName>.apk` 与 `SHA256SUMS.txt`。`.github/workflows/source-archive.yml` 会在同一标签上附加完整源码 ZIP 与校验文件。

应用内更新读取的是最新 Release，因此发布说明会直接显示在更新弹窗里；请写清楚兼容性、签名变更和数据迁移要求。

## 签名变更记录

- 1.0.0 / 1.1.0：Android Debug 证书（试用签名）。
- 1.11.0 起：正式 release 证书。由于签名不同，1.11.0 无法覆盖安装 1.1.0；旧版用户需导出备份、卸载、安装 1.11.0 后导入。
