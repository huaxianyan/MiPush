# MiPushFramework的分应用模块

**本模块需要配合[MiPushFramework](https://github.com/NihilityT/MiPushFramework)使用**

`MiPushFramework`是一个能让不用MIUI的用户也能用上小米的系统推送服务的开源项目

默认情况下，其发送的通知是以“推送服务”的身份发送。

该模块借助 [LSPosed](https://github.com/LSPosed/LSPosed) 为 `MiPushFramework` 提供通知以目标应用发送的能力，
同时支持将应用运行环境伪装成小米设备，以此来实现无后台系统级别的推送通道。

### 安装步骤：
- 从[这里](https://github.com/NihilityT/MiPushFramework/releases/latest)下载并安装`MiPushFramework`，按照指引完成其初始化。

- 下载本模块的最新版本并安装，在 LSPosed 中启用 MiPush 模块，并勾选 「系统框架」、「推送服务」作用域，然后重启设备，[下载地址](https://github.com/NihilityT/MiPush/releases/latest)

- LSPosed 里 MiPush 模块里勾选你需要支持推送的目标应用（这一步目的是将应用环境伪装成小米设备，如果你使用了其他方式伪装设备，可以不进行这一步），然后重启一到两次目标应用使其注册上推送通道

- 杀掉应用测试推送是否生效（可以使用QQ、酷安测试）
　　
### 注意：
- 并不是所有应用都支持推送，目前测试已支持大部分应用，比如 QQ、酷安等

- **微信不支持**

- 请保证 `MiPushFramework` 在后台运行，不要禁用其自启权限和访问目标推送应用的权限

- 反馈问题或交流讨论可加入 [Telegram 群组](https://t.me/+SXl7v8t-lOa9KCAp)、[QQ群](https://jq.qq.com/?_wv=1027&k=P0EQCaUz)

- 通过GitHub反馈 `MiPushFramework` 的问题时请到[这里](https://github.com/NihilityT/MiPushFramework/issues)反馈

- 提建议时不要操之过急，否则会有反作用。

- 不要在交流群里挑起对立，因挑起对立导致大佬退群的，自己面壁思过看怎么挽回
### 反馈
[Github Issues](https://github.com/NihilityT/MiPush/issues)、[Telegram Group](https://t.me/+SXl7v8t-lOa9KCAp)、[QQ群](https://jq.qq.com/?_wv=1027&k=P0EQCaUz)

通过 GitHub 反馈 MiPushFramework 的问题时请到[这里](https://github.com/NihilityT/MiPushFramework/issues)反馈

### QQ 通知图标适配

本分支为 `com.tencent.mobileqq` 内置了符合 Android 通知规范的单色小图标。MiPush 代理发布的 QQ 通知会自动使用该兜底图标，不需要开启“自定义通知图标”；开启该选项后，用户配置的自定义图标仍具有更高优先级。Android 16 上还会通过平台支持的 `android.app.preferSmallIcon` 通知标记，让联系人头像右下角继续显示该通知小图标而不是 QQ 彩色启动器图标；联系人或群头像本身不会被覆盖。

### GitHub Actions 构建

在仓库的 **Actions → Build installable APK → Run workflow** 中可以手动构建。构建成功后，在运行详情页的 **Artifacts** 区域下载 `MiPush-*-debug`，解压后即可获得可安装的 Debug APK。构建产物保留 30 天。

未配置签名 Secrets 时，Actions 会使用临时 Debug 签名，通常不能直接覆盖官方签名版本，需要先卸载原应用。若需要让多次构建保持同一签名，可以在仓库 Secrets 中配置 `SIGNING_KEY`（Base64 格式）、`KEY_STORE_PASSWORD`、`ALIAS` 和 `KEY_PASSWORD`。

### License

本分支包含从 [MiPushFaker](https://github.com/yin-ol/MiPushFaker) 移植并修改的系统属性 Hook。MiPushFaker 使用 [GNU Affero General Public License v3](LICENSE.MiPushFaker-AGPL-3.0)，因此本组合分发版本遵循仓库根目录中的 [GNU AGPL v3](LICENSE)。

原 MiPush/HMSPush 代码仍保留其 [GNU General Public License v3](LICENSE.MiPush-GPL-3.0) 授权。详细来源、文件范围和署名参见 [NOTICE](NOTICE)。GPLv3 与 AGPLv3 的组合依据 GPLv3 第 13 节进行分发。

有些狗不遵守开源协议（非本项目），请**务必**遵守开源协议 **（此话来自MiPushFramework的README.md）**
