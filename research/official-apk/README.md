# 官方 APK 参考

项目根目录的 `base.apk` 是官方“智慧东大”客户端安装包，保留用于模拟器安装和功能调查。包名 `com.sunyt.testdemo`，版本 `3.1.8`，SHA-256：`84B9B5F80D3107F92663F1BACEA7186F1B71C2492F2865B0705D69F8899DF406`。

- `unpacked/`：从 APK 直接展开的原始内容，含二进制 Manifest、资源、DEX 和原生库；这是本机参考目录，已排除 Git 跟踪。
- `badging.txt`：Android SDK `aapt dump badging` 输出，仅保存在本机。
- `manifest-tree.txt`：Android SDK `aapt dump xmltree` 的可读 Manifest 结构，仅保存在本机。

需要重新展开时，在项目根目录运行：

```powershell
python -c "import zipfile; zipfile.ZipFile('base.apk').extractall('research/official-apk/unpacked')"
```

如需重新生成可读信息，使用 Android SDK Build Tools 中的 `aapt` 对 `base.apk` 执行 `dump badging` 与 `dump xmltree base.apk AndroidManifest.xml`。

模拟器安装可使用 `adb install -r .\base.apk`。该 APK 是参考样本，不是 NEO NEU 的构建产物；`unpacked/` 中的资源和 DEX 未反编译成源码。
