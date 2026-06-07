# HerobrineHUD - Minecraft 电竞导播系统

HerobrineHUD 是一款专为 **Minecraft 电子竞技（E-sports）**、赛事直播与专业导播环境设计的 MOD。无论是大型正式赛事，还是多人小游戏与娱乐对抗，它的目标都是提供一套真正服务于“观赛体验”的系统级导播界面。

我们的理念是：**Minecraft 从来不缺好玩的游戏，但它一直缺少能把这些游戏变得“好看”的系统级展示工具。**

## ✨ 核心功能

HerobrineHUD 目前已经实现了真正意义上的**开箱即用（几乎零配置负担）**。只需要在服务器与希望显示 HUD 的客户端同时安装此 Mod，客户端就可以无缝显示队伍信息与选手状态。原版 `/team` 指令建立出的队伍关系，也能自动同步成导播界面中的队伍分组。


**导播 / 操作热键说明：**
- **按键 `H`**：快速切换 HUD 的显示与隐藏。
- **按键 `K`**：打开 HUD 管理、设置与选项界面。你可以在这里启用或关闭特定元素（如装备栏、药水状态等），并管理左右两侧要展示的玩家。
- **小键盘按键（Numpad 1~9, 0）**：**快速导播切镜**。当你处于旁观者模式（Spectator）时，只需按下对应的数字快捷键，镜头就会直接锁定到屏幕中对应编号的选手视角。

**HerobrineHUD 希望帮助导播清晰呈现的内容包括：**
- 基于队伍的选手卡片布局
- 选手实时血量与状态变化
- 装备、效果与局内信息展示
- 面向旁观与切镜的高效率控制体验

## 📌 当前状态

Fabric 侧的多版本迁移与分版本架构目前已经完成。

当前支持的 Minecraft 版本为：
- **`26.1`**
- **`1.21.11`**

## 🚀 未来计划

这个 Mod 是基于标准 GSI（Game State Integration）理念设计的。它的目标不只是“把游戏里的信息显示出来”，而是逐步为 Minecraft 电竞与直播制作构建一个更完整的展示生态。

长期计划包括：

* **开放更多 GSI 状态字段：** 未来会逐步暴露更多游戏状态数据，允许第三方 Mod 将特殊玩法、规则信息或扩展数据接入 HUD 渲染流程中，最终演化成更开放的 API 体系。
* **实时 HTTP 数据广播：** 服务端未来计划提供将比赛数据以 HTTP 实时服务形式对外广播的能力。这样纯外部程序也可以直接消费比赛数据，并借助 Web 技术构建专属的第三方直播界面，例如直接作为 OBS 浏览器源使用，而不再依赖传统录屏式方案。

## 🗺️ 路线图（Roadmap）

* 引入 **AUI** 框架，在游戏内原生地使用 Web 技术，更灵活地渲染炫酷、可定制性极强的 Web 风格 UI。
* 提供更丰富的 UI 主题框架以及完整的 **Theme Manager** 支持。
* 支持 NeoForge 等更多加载器。

## 🛠️ 开发与调试

Windows PowerShell：

```powershell
.\gradlew.bat build
```

运行客户端：

```powershell
.\gradlew.bat runDebugClient
.\gradlew.bat runDebugClient "-PdebugVersion=26.1"
.\gradlew.bat runDebugClient "-PdebugVersion=1.21.11"
```

运行服务端：

```powershell
.\gradlew.bat runDebugServer
.\gradlew.bat runDebugServer "-PdebugVersion=26.1"
.\gradlew.bat runDebugServer "-PdebugVersion=1.21.11"
```

## 🧱 项目结构

- `common/`：共享数据模型与公共逻辑
- `fabric/`：共享的 Fabric 实现以及版本覆盖代码
- `fabric-remap/`：`1.21.x` 的 remap 构建路径


在 `fabric/src/` 下：
- 共享服务端 / 平台逻辑：`fabric/src/main/`
- 共享客户端逻辑：`fabric/src/client/`
- 版本专属 main 覆盖：`fabric/src/mc<version>/main/`
- 版本专属 client 覆盖：`fabric/src/mc<version>/client/`

