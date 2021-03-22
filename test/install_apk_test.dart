import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:install_apk/install_apk.dart';

void main() {
  const MethodChannel channel = MethodChannel('install_apk');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await InstallApk.platformVersion, '42');
  });
}
