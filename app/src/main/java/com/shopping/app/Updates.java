package com.shopping.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Checks GitHub Releases, downloads the latest APK and hands it to the system installer. */
final class Updates {
    static final String OWNER = "FZZZZZZZZZ19";
    static final String REPO = "Shopping-app";
    static final String API = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest";
    static final String RELEASES_PAGE = "https://github.com/" + OWNER + "/" + REPO + "/releases/latest";
    private static final int MAX_JSON = 512 * 1024;
    private static final int MAX_APK = 200 * 1024 * 1024;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);

    interface CheckCallback { void done(Release release, String error); }
    interface ProgressCallback { void progress(int percent, long downloaded, long total); }
    interface DownloadCallback { void done(File file, String error); }

    static final class Release {
        final String tag, name, body, page, apkName, apkUrl;
        final long apkSize;
        Release(String tag, String name, String body, String page, String apkName, String apkUrl, long apkSize) {
            this.tag = tag; this.name = name; this.body = body; this.page = page;
            this.apkName = apkName; this.apkUrl = apkUrl; this.apkSize = apkSize;
        }
    }

    private Updates() {}

    static void check(final CheckCallback callback) {
        IO.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(API).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(20000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
                connection.setRequestProperty("User-Agent", "ShoppingApp/" + BuildConfig.VERSION_NAME);
                int code = connection.getResponseCode();
                if (code == 403 || code == 429) { fail(callback, "GitHub 请求受限，请稍后再试或打开 Release 页面"); return; }
                if (code != 200) { fail(callback, "GitHub 返回 HTTP " + code); return; }
                Release release = parse(new JSONObject(readText(connection.getInputStream(), MAX_JSON)));
                if (release == null) { fail(callback, "最新 Release 没有可下载的 APK"); return; }
                MAIN.post(() -> callback.done(release, null));
            } catch (Exception e) {
                fail(callback, friendly(e));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    static Release parse(JSONObject json) {
        String tag = json.optString("tag_name", "").trim();
        if (tag.isEmpty()) return null;
        String name = json.optString("name", tag);
        String body = json.optString("body", "");
        String page = json.optString("html_url", RELEASES_PAGE);
        JSONArray assets = json.optJSONArray("assets");
        String bestName = null, bestUrl = null;
        long bestSize = 0;
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset == null) continue;
                String assetName = asset.optString("name", "");
                String assetUrl = asset.optString("browser_download_url", "");
                if (!assetName.toLowerCase(Locale.ROOT).endsWith(".apk") || assetUrl.isEmpty()) continue;
                if (bestUrl == null || score(assetName) > score(bestName)) {
                    bestName = assetName;
                    bestUrl = assetUrl;
                    bestSize = asset.optLong("size", 0);
                }
            }
        }
        if (bestUrl == null) return null;
        return new Release(tag, name, body, page, bestName, bestUrl, bestSize);
    }

    private static int score(String name) {
        if (name == null) return 0;
        int score = 0;
        if (name.startsWith("shopping-app")) score += 2;
        if (name.toLowerCase(Locale.ROOT).contains("release")) score += 1;
        return score;
    }

    static void download(final Release release, final File target, final ProgressCallback progress, final DownloadCallback callback) {
        IO.execute(() -> {
            HttpURLConnection connection = null;
            File partial = new File(target.getPath() + ".part");
            try {
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("无法创建下载目录");
                if (target.isFile() && target.length() > 0 && (release.apkSize <= 0 || target.length() == release.apkSize)) {
                    MAIN.post(() -> callback.done(target, null));
                    return;
                }
                connection = (HttpURLConnection) new URL(release.apkUrl).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "ShoppingApp/" + BuildConfig.VERSION_NAME);
                int code = connection.getResponseCode();
                if (code != 200) throw new IOException("下载返回 HTTP " + code);
                long total = release.apkSize > 0 ? release.apkSize : connection.getContentLengthLong();
                if (total > MAX_APK) throw new IOException("安装包超过大小限制");
                long written = 0;
                byte[] buffer = new byte[16384];
                try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(partial)) {
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        written += read;
                        if (written > MAX_APK) throw new IOException("安装包超过大小限制");
                        out.write(buffer, 0, read);
                        final long current = written, size = total;
                        if (progress != null) MAIN.post(() -> progress.progress(size > 0 ? (int) (current * 100 / size) : -1, current, size));
                    }
                }
                if (release.apkSize > 0 && partial.length() != release.apkSize) throw new IOException("下载大小与 Release 不一致");
                if (target.exists() && !target.delete()) throw new IOException("无法覆盖旧安装包");
                if (!partial.renameTo(target)) throw new IOException("无法保存安装包");
                MAIN.post(() -> callback.done(target, null));
            } catch (Exception e) {
                partial.delete();
                MAIN.post(() -> callback.done(null, friendly(e)));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    static void install(final Activity activity, final File apk) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        String problem = checkApk(activity, apk);
        if (problem != null) {
            new AlertDialog.Builder(activity)
                    .setTitle("无法安装这个更新包")
                    .setMessage(problem + "\n\n请从 GitHub Release 页面下载与当前应用同签名的 APK。")
                    .setPositiveButton("打开 Release 页面", (dialog, which) -> Ui.external(activity, RELEASES_PAGE))
                    .setNegativeButton("关闭", null)
                    .show();
            return;
        }
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity)
                    .setTitle("需要安装权限")
                    .setMessage("请在系统设置中允许“Shopping APP”安装未知应用，然后返回重试。")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        try {
                            activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName())));
                        } catch (Exception e) {
                            Ui.toast(activity, "无法打开安装权限设置");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }
        Uri uri = ApkProvider.uriFor(activity, apk);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(intent);
        } catch (Exception e) {
            Ui.toast(activity, "无法启动系统安装器");
        }
    }

    /** Returns null when the APK has the same package name and signing certificate as this app. */
    private static String checkApk(Activity activity, File apk) {
        try {
            PackageManager pm = activity.getPackageManager();
            PackageInfo archive, installed;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archive = pm.getPackageArchiveInfo(apk.getPath(), PackageManager.GET_SIGNING_CERTIFICATES);
                installed = pm.getPackageInfo(activity.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            } else {
                archive = pm.getPackageArchiveInfo(apk.getPath(), PackageManager.GET_SIGNATURES);
                installed = pm.getPackageInfo(activity.getPackageName(), PackageManager.GET_SIGNATURES);
            }
            if (archive == null) return "安装包无法读取。";
            if (!activity.getPackageName().equals(archive.packageName)) return "安装包包名与当前应用不一致。";
            byte[] a = firstSignature(installed), b = firstSignature(archive);
            if (a == null || b == null || !java.util.Arrays.equals(a, b)) return "安装包签名与当前应用不一致，无法覆盖升级。";
            return null;
        } catch (Exception e) {
            return "安装包校验失败：" + friendly(e);
        }
    }

    private static byte[] firstSignature(PackageInfo info) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.signingInfo != null) {
            android.content.pm.Signature[] signers = info.signingInfo.getApkContentsSigners();
            return signers != null && signers.length > 0 ? signers[0].toByteArray() : null;
        }
        return info.signatures != null && info.signatures.length > 0 ? info.signatures[0].toByteArray() : null;
    }

    private static String readText(InputStream in, int max) throws IOException {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (out.size() > max) throw new IOException("响应过大");
            }
            return out.toString("UTF-8");
        }
    }

    private static void fail(CheckCallback callback, String error) {
        MAIN.post(() -> callback.done(null, error));
    }

    private static String friendly(Exception e) {
        if (e instanceof java.net.UnknownHostException) return "无法连接 GitHub，请检查网络";
        if (e instanceof java.net.SocketTimeoutException) return "连接 GitHub 超时，请重试";
        if (e instanceof javax.net.ssl.SSLException) return "GitHub HTTPS 连接失败";
        if (e instanceof org.json.JSONException) return "GitHub 返回数据格式异常";
        String message = e.getMessage();
        return message == null || message.trim().isEmpty() ? "更新失败，请重试" : message;
    }
}
