# ReDeploy —— 战术重生（COD 风格死亡界面）

> 只改 GUI。用 Call of Duty 风格的「你已死亡 / REDEPLOY」覆盖原版死亡界面。
> 死亡后**长按【重新部署】**读条重生，长按过程中播放**滴答确认音效**，完成时播放确认音。
> 极限（硬核）模式提供 **脱离机体 / 退出战区** 两个独立长按选项，样式与普通模式区分。

- **作者**：Mrkun114514 ｜ **协议**：MIT ｜ **只改 GUI，无任何玩法改动**
- 客户端 mod（GUI 逻辑仅在客户端生效），可与纯净服务端共存。

---

## 📦 版本矩阵（每个里程碑 = 一个独立构建）

`1.20.1 → 1.21.11` 横跨多代 Minecraft API（尤其 **1.21.6** 把 GUI 渲染从 `PoseStack` 换成
`Matrix3x2fStack`），无法用一个 jar 通吃。因此按「API 代」拆成多个里程碑，每个里程碑
**向上兼容它之后、下一里程碑之前的小版本**。

| 里程碑目录 | 覆盖 MC 版本 | 加载器 | GUI 渲染 API | JDK 目标 | 状态 |
|---|---|---|---|---|---|
| `neoforge-1.21.8/` | 1.21.6 – 1.21.11 | NeoForge | Matrix3x2fStack | 21 | ✅ CI 已构建 |
| `fabric-1.21.8/` | 1.21.6 – 1.21.11 | Fabric | Matrix3x2fStack | 21 | ✅ CI 已构建 |
| `neoforge-1.21.1/` | 1.21 – 1.21.1 | NeoForge | PoseStack | 21 | ✅ CI 已构建 |
| `fabric-1.21.1/` | 1.21 – 1.21.1 | Fabric | PoseStack | 21 | ✅ CI 已构建 |
| `fabric-1.20.1/` | 1.20.1 | Fabric | PoseStack | 17 | ✅ CI 已构建 |
| `neoforge-1.20.1/` | 1.20.1 | NeoForge (NeoGradle) | PoseStack | 17 | ✅ CI 已构建 |

> 说明：1.21.8 里程碑与 1.21.11 共用同一套渲染 API，理论上直接兼容 1.21.11；
> 若后续 1.21.11 出现破坏性改动，会再单独出 `*-1.21.11/` 目录。

---

## 🛠 构建

每个里程碑是**独立的 Gradle 工程**，进入对应目录构建即可。

- **NeoForge 1.21.x / Fabric 1.21.x / Fabric 1.20.1**：用 **JDK 21** 即可（1.20.1 的 Fabric 版虽面向 Java 17 运行时，但用 JDK 21 编译并以 `--release 17` 产出 17 字节码，可正常跑在 1.20.1）。
- **NeoForge 1.20.1（NeoGradle 6.x / ForgeGradle 代）**：**必须用 JDK 17 运行 Gradle**（NeoGradle 6.x 在 JDK 21 上无法启动），CI 已为该矩阵项单独装 JDK 17。本机同理：`sdk/jenv` 切到 17 后再 `./gradlew build`。

```bash
# 例：构建 NeoForge 1.21.8
cd neoforge-1.21.8
./gradlew build            # 产出 build/libs/redeploy-1.0.0.jar

# 例：构建 Fabric 1.21.8
cd fabric-1.21.8
./gradlew build            # 产出 build/libs/redeploy-fabric-1.0.0.jar

# 本机客户端测试
./gradlew runClient
```

首次构建会下载并反编译 Minecraft，耗时数分钟。把生成的 jar 放入 `.minecraft/mods/`
（Fabric 版还需安装 **Fabric API**）。

---

## ✨ 特性

- 覆盖原版 `DeathScreen`，深色全屏遮罩 + 大字标题 + 副标题。
- **开屏动画**：先一瞬柔和红闪，再淡入静置遮罩与 COD 覆盖层（红色不刺眼）。
- **长按重生**：持续按住「重新部署」约 3 秒（整条按钮进度填充），松手/移出即重置。
- **滴答音效**：长按时按节奏播放滴答声，音高随进度升高；读条满播放确认音。
- **极限模式**：
  - 样式区分——近黑底 + 冷白标题（普通模式为红/海军蓝）。
  - 两个等宽等高长按按钮：**脱离机体**（重生为旁观）/ **退出战区**（返回标题界面，**不删档**）。
- **ESC 后门**：按 ESC 打开原版暂停菜单（可进设置/退出）；死亡状态下「返回游戏」会重新显示死亡界面（无法「未死」）。
- **服务器自动重生检测**：若服务端开启立即重生/几秒后自动重生，检测到玩家复活时主动关闭 GUI。
- 音效使用 MC 自带音（`block.comparator.click` / `entity.player.levelup`），**零二进制资源开箱即用**。

---

## 📁 加载器差异（实现要点）

核心界面 `RedeployDeathScreen.java` **只用原版 `net.minecraft.*` 类**，在同一渲染代内
可被两种加载器**逐字复用**；差异只在「如何替换死亡界面」和模组入口：

| | NeoForge | Fabric |
|---|---|---|
| 替换机制 | `ScreenEvent.Opening` 事件拦截 | `ClientTickEvents` 检测到 `DeathScreen` 时换屏 |
| 入口 | `@Mod` 主类 + 手动注册事件总线 | `ClientModInitializer`（`fabric.mod.json` client 入口）|
| 映射 | 官方 Mojang 映射 | 官方 Mojang 映射（与 NeoForge 同名，便于共享源码）|

---

## 🔊 换成自定义滴答/确认音效？

默认零资源即可运行。若要换成自己的音：

1. 准备两个 `.ogg`（Vorbis），如 `assets/redeploy/sounds/redeploy_tick.ogg`、`..._confirm.ogg`。
2. 新建 `assets/redeploy/sounds.json`：
   ```json
   {
     "redeploy:redeploy_tick":    { "sounds": [ "redeploy:sounds/redeploy_tick" ] },
     "redeploy:redeploy_confirm": { "sounds": [ "redeploy:sounds/redeploy_confirm" ] }
   }
   ```
3. 在 `RedeployDeathScreen` 中把 `TICK_SOUND` / `CONFIRM_SOUND` 换成
   `ResourceLocation.parse("redeploy:redeploy_tick")` / `..._confirm`。

---

## 🤖 持续集成

`.github/workflows/build.yml` 用矩阵在干净的 JDK 21 环境中构建每个里程碑模块，
推送后可在 Actions 里下载各版本 jar 产物。

---

© 2026 Mrkun114514 · Released under the MIT License.
