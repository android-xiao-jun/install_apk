
import 'dart:async';

import 'package:flutter/services.dart';

class InstallApk {
  static const MethodChannel _channel = const MethodChannel('install_apk');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<String> installApk(String filePath, String appId) async {
    Map<String, String> params = {'filePath': filePath, 'appId': appId};
    return await _channel.invokeMethod('installApk', params);
  }
}
