# 源码与 APK 发布

仓库：<https://github.com/FZZZZZZZZZ19/Shopping-app>。

`main` 分支维护完整 Android 应用源码、Gradle 构建配置、测试、文档和 APK 发布文件。签名私钥、登录数据、构建缓存不属于公开源码。

## 构建与检查

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

DOM 采集测试和设备测试的运行方式见 README。CI 会构建 APK、运行单元测试与 Lint，保存 APK 和报告作为工作流产物。自行构建的默认调试签名不保证与已发布 APK 相同。

## 发布 APK

本地通过检查后，把 APK 复制到 `release-assets/`，更新 SHA256SUMS、发布说明及 `.github/workflows/publish-apk.yml` 中的版本号。该工作流使用仓库自身的 Token 上传 Release 附件。只修改普通源码不会重新发布已有版本。

1.1.0 使用 versionCode 2，发布 APK 保持与 1.0 相同的签名证书。发布后下载附件，校验 SHA-256 并与本地安装包逐字节比较。保留旧版本 Release，不覆盖旧安装包。

## 历史标签

`v1.0.0` 与 `v1.1.0` 在完整源码上传前创建，标签指向原始发布提交，自动生成的 Source code 压缩包仍只包含当时的文件。完整源码请使用 `main` 分支、明确的源码提交，或 Release 附件 `shopping-app-1.1.0-source.zip`。`.github/workflows/source-archive.yml` 用于附加完整源码与校验文件，不修改原始标签或 APK。后续版本应在完整源码提交上创建新标签。
