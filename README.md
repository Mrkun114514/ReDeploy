# ReDeploy —— 战术重生（COD 风格死亡动画）

> 只改 GUI。用 Call of Duty 风格的「你已死亡 / REDEPLOY」覆盖原版死亡界面。
> 死亡后**长按【重新部署】**读条重生，长按过程中播放**滴答确认音效**，完成时播放确认音。

- **MC 版本**：1.21.8
- **加载器**：NeoForge（`neo_version` = 21.8.53）
- **作者**：Mrkun114514
- **协议**：MIT
- **只改 GUI，无任何玩法改动**，服务端/客户端均可加载（GUI 逻辑仅在客户端生效）。

---

## ✨ 特性

- 覆盖原版 `DeathScreen`，替换为深色全屏遮罩 + 红色大字「你已死亡」+「REDEPLOY」副标题。
- **长按重生**：必须**持续按住**「重新部署」按钮约 3 秒（读条填充），松手或移出按钮即重置。
- **滴答确认音效**：长按过程中按节奏播放滴答声，音高随进度升高（紧张感）。
- **确认音**：读条满时播放一声确认音，随后自动重生并关闭界面。
- 打开后约 2 秒进入「可部署」状态（提示「正在定位重生点…」）。
- 按 ESC 无效，必须完成长按部署才能重生。
- 原版音效使用 MC 自带音（`block.comparator.click` / `entity.player.levelup`），**开箱即用，无需任何二进制音频资源**。

---

## 🛠 构建

需要 **JDK 21**。

```bash
# 构建产出 jar -> build/libs/redeploy-1.0.0.jar
./gradlew build

# 在本机客户端测试（会自动生成 run/ 配置并启动 MC）
./gradlew runClient
```

首次构建会下载 Minecraft 与 NeoForge 并反编译，可能耗时数分钟，请耐心等待。
将生成的 `build/libs/redeploy-1.0.0.jar` 放入 `.minecraft/mods/` 即可。

---

## 📁 项目结构

```
redeploy/
├── build.gradle / settings.gradle / gradle.properties   # 基于官方 MDK-1.21.8-NeoGradle
├── gradlew / gradlew.bat / gradle/wrapper/            # Gradle 9.2.1 wrapper（开箱即用）
├── src/main/java/com/mrkun114514/redeploy/
│   ├── Redeploy.java                       # @Mod 入口
│   └── client/
│       ├── ClientEvents.java             # ScreenEvent.Opening 拦截并替换死亡界面
│       └── RedeployDeathScreen.java     # COD 风格死亡界面（长按部署 + 音效）
└── src/main/resources/
    ├── META-INF/neoforge.mods.toml
    └── assets/redeploy/lang/{zh_cn,en_us}.json
```

---

## 🔊 想换成自定义滴答音效？

默认用 MC 自带音，零资源即可运行。若要换成自己的「滴答 / 确认」音：

1. 准备两个 `.ogg` 文件（Vorbis 编码），例如
   `src/main/resources/assets/redeploy/sounds/redeploy_tick.ogg`
   `src/main/resources/assets/redeploy/sounds/redeploy_confirm.ogg`
2. 新建 `src/main/resources/assets/redeploy/sounds.json`：
   ```json
   {
     "redeploy:redeploy_tick":   { "sounds": [ "redeploy:sounds/redeploy_tick" ] },
     "redeploy:redeploy_confirm": { "sounds": [ "redeploy:sounds/redeploy_confirm" ] }
   }
   ```
3. 在 `RedeployDeathScreen` 中用
   `ResourceLocation.parse("redeploy:redeploy_tick")` / `...redeploy_confirm"`
   替换 `TICK_SOUND` / `CONFIRM_SOUND` 即可。

（也可改用 `DeferredRegister<SoundEvent>` 正式注册音效事件；当前用 `BuiltInRegistries.SOUND_EVENT.get(...)` 直接取资源，足够轻量。）

---

## 📤 上传到 GitHub

```bash
cd redeploy
git init
git add .
git commit -m "feat: ReDeploy - COD style death/respawn GUI (NeoForge 1.21.8)"
# 在 GitHub 新建仓库后：
git branch -M main
git remote add origin https://github.com/<你的用户名>/redeploy.git
git push -u origin main
```

仓库已包含 `.github/workflows/build.yml`，推送后会在 Actions 中自动用 JDK 21 跑 `./gradlew build` 并上传 jar 产物。

---

© 2026 Mrkun114514 · Released under the MIT License.
