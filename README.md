# MiPushFramework的分应用模块

**本模块需要配合[MiPushFramework](https://github.com/NihilityT/MiPushFramework)使用**

`MiPushFramework`是一个能让不用MIUI的用户也能用上小米的系统推送服务的开源项目

默认情况下，其发送的通知是以“推送服务”的身份发送。

该模块借助 [LSPosed](https://github.com/LSPosed/LSPosed) 为 `MiPushFramework` 提供通知以目标应用发送的能力，
同时支持将应用运行环境伪装成小米设备，以此来实现无后台系统级别的推送通道。

### 安装步骤：
- 从[这里](https://github.com/NihilityT/MiPushFramework/releases/latest)下载并安装`MiPushFramework`，按照指引完成其初始化。

- 本通用版使用 Modern Xposed API 102，需要支持 API 102 的 LSPosed。下载并安装[本仓库最新 Release](https://github.com/huaxianyan/MiPush/releases/latest)，在 LSPosed 中启用 MiPush 模块。

- 保留推荐作用域中的「系统框架」、「系统界面（SystemUI）」和「推送服务（com.xiaomi.xmsf）」，然后重启设备。SystemUI 作用域用于兼容 HyperOS 的焦点通知策略；标准 Android 的基础推送与通知发布不依赖该视觉策略 Hook。

- LSPosed 里 MiPush 模块中勾选你需要支持推送的目标应用（这一步目的是将应用环境伪装成小米设备，如果你使用了其他方式伪装设备，可以不进行这一步），然后重启一到两次目标应用使其注册上推送通道

- 杀掉应用测试推送是否生效（可以使用QQ、酷安测试）

### 版本选择

正式 Release 同时提供两个 Modern Xposed API 102 APK，二选一安装并启用：

#### 通用版

- 包名：`com.neko7ina.mipush`
- 面向一般 Android、MIUI/HyperOS 环境。
- 推荐作用域：系统框架、SystemUI、XMSF，以及需要属性伪装的目标应用。
- SystemUI 代码用于通用的焦点通知策略兼容；标准 Android 的基础推送链路不依赖该视觉策略 Hook。

#### Pixel Android 16 适配版

- 包名：`com.neko7ina.mipush.pixel`
- 仅面向 Pixel Android 16；Pixel 分支不包含任何 MIUI/HyperOS SystemUI 插件代码。
- 在通用 API 102 推送能力之外，针对 QQ MiPush 会话通知：
  - 保留联系人头像、群头像、`Person.icon` 和 `android.largeIcon`；
  - 右下角角标使用 MiPushFramework 已提供的单色 `smallIcon`，不内置或自动启用 QQ fallback 图标；
  - 仅将 Android 16 的 40dp 会话模板恢复为 48dp 头像、20dp 角标、30dp 角标位置和 4dp 内边距；
  - 严格过滤 `packageName=com.tencent.mobileqq` 与 `tag=mipush_com.tencent.mobileqq`，接口不匹配时安全跳过。
- Pixel 视觉适配依赖 `com.android.systemui` 作用域。

两个 Modern APK 沿用对应 Legacy 版的包名和长期签名，可以分别原位更新 Legacy 通用版和 Legacy Pixel 版。不要在同一组 LSPosed 作用域中同时启用多个版本。
　　
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

### 版本分支

- `master`：Modern Xposed API 102 通用发行版。
- `modern-api-102-pixel-android16`：Modern Xposed API 102 Pixel Android 16 适配版。
- `legacy-api-82-general`：冻结的 Legacy 通用版，保留用于查阅和回退分析。
- `pixel-android-16-notification-badge`：冻结的 Pixel Android 16 Legacy 视觉适配版。

不要在相同 LSPosed 作用域中同时启用通用版、Modern Pixel 版或 Pixel Legacy 版。

### GitHub Actions 构建

在仓库的 **Actions → Build installable APK → Run workflow** 中可以手动构建。构建成功后，在运行详情页的 **Artifacts** 区域下载 `MiPush-*-release`，解压后即可获得可安装 APK。构建产物保留 30 天。

未配置签名 Secrets 时，Actions 会使用临时 Debug 签名，不能直接覆盖正式 Release。仓库正式构建使用私有 Secrets 中的长期签名，并在 CI 中校验证书、Modern API 102 模块声明、包名以及 APK 中不存在 Legacy Xposed API。

### License

本分支包含从 [MiPushFaker](https://github.com/yin-ol/MiPushFaker) 移植并修改的系统属性 Hook。MiPushFaker 使用 [GNU Affero General Public License v3](LICENSE.MiPushFaker-AGPL-3.0)，因此本组合分发版本遵循仓库根目录中的 [GNU AGPL v3](LICENSE)。

原 MiPush/HMSPush 代码仍保留其 [GNU General Public License v3](LICENSE.MiPush-GPL-3.0) 授权。详细来源、文件范围和署名参见 [NOTICE](NOTICE)。GPLv3 与 AGPLv3 的组合依据 GPLv3 第 13 节进行分发。

有些狗不遵守开源协议（非本项目），请**务必**遵守开源协议 **（此话来自MiPushFramework的README.md）**
