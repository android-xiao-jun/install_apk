package com.example.install_apk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * InstallApkPlugin
 */
public class InstallApkPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler {

    private MethodChannel channel;

    private File apkFile = null;
    private String appId = null;
    private static final int installRequestCode = 1234;
    private Activity activity;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "install_apk");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if ("installApk".equals(call.method)) {
            String filePath = call.argument("filePath");
            String appId = call.argument("appId");
            Log.d("android plugin", "installApk  "+filePath+appId);
            try {
                installApk(filePath, appId);
                result.success("Success");
            } catch (Throwable e) {
                result.error(e.getClass().getSimpleName(), e.getMessage(), null);
            }
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    private void installApk(String filePath, String appId) {
        if (filePath == null) {
            throw new NullPointerException("fillPath is null!");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new NullPointerException(filePath+" is not exist! or check permission");
        }
        this.apkFile = file;
        this.appId = appId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (canRequestPackageInstalls(activity)) {
                install24(activity, file, appId);

            } else {
                showSettingPackageInstall(activity);
            }
        } else {
            installBelow24(activity, file);
        }
    }


    private void showSettingPackageInstall(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("SettingPackageInstall", ">= Build.VERSION_CODES.O");
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, installRequestCode);
        } else {
            throw new RuntimeException("VERSION.SDK_INT < O");
        }

    }

    private boolean canRequestPackageInstalls(Context activity) {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.O || activity.getPackageManager().canRequestPackageInstalls();
    }

    private void installBelow24(Context context, File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    /**
     * android24及以上安装需要通过 ContentProvider 获取文件Uri，
     * 需在应用中的AndroidManifest.xml 文件添加 provider 标签，
     * 并新增文件路径配置文件 res/xml/provider_path.xml
     * 在android 6.0 以上如果没有动态申请文件读写权限，会导致文件读取失败，你将会收到一个异常。
     * 插件中不封装申请权限逻辑，是为了使模块功能单一，调用者可以引入独立的权限申请插件
     */
    private void install24(Context context, File file, String appId) {
        if (context == null) {
            throw new NullPointerException("context is null!");
        }
        if (file == null) {
            throw new NullPointerException("file is null!");
        }
        if (appId == null) {
            throw new NullPointerException("appId is null!");
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Uri uri = FileProvider.getUriForFile(context, appId+".fileProvider.install2", file);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(new PluginRegistry.ActivityResultListener() {
            @Override
            public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
                if (resultCode == Activity.RESULT_OK && requestCode == installRequestCode) {
                    install24(activity, apkFile, appId);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }
}
