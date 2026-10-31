# Objection! — NeoForge 1.21.1 移植说明

本仓库是 [GlcDeamino/Objection-](https://github.com/GlcDeamino/Objection-) 的 fork，
固定在 **Minecraft 1.21.1 + NeoForge 21.1.x** 上，并用 GitHub Actions 持续产出可安装的 jar。

**功能上已裁剪：只保留「按 O 键喊异议」（音效 + 头顶粒子 + 聊天广播），
其余上游内容（两个物品、创造模式物品栏、Objection Timer 效果、屏幕中央贴图）全部移除。**

## 结论：上游 main 分支本身已经是 1.21.1 NeoForge 工程

克隆上游 main（`2598dba`）后逐项核对，工作区并不是 1.20.x 的 Forge 工程，而是
MCreator 2025.1 生成的 1.21.1 NeoForge 工程。证据：

| 检查项 | 实际值 |
| --- | --- |
| `build.gradle` | `id 'net.neoforged.moddev' version '2.0.95'`，`neoForge { version = "21.1.176" }` |
| Java 工具链 | `JavaLanguageVersion.of(21)` |
| Gradle wrapper | `gradle-8.14.2-bin` |
| 元数据文件 | `src/main/resources/META-INF/neoforge.mods.toml`（1.20.5+ 的 NeoForge 元数据） |
| 依赖声明 | `neoforge [21.1.176,)`、`minecraft [1.21.1]` |
| 网络 API | `RegisterPayloadHandlersEvent` / `PayloadRegistrar` / `StreamCodec`（1.20.5+ 新网络栈） |
| 注册 API | `DeferredRegister` / `DeferredHolder` / `DeferredItem`（NeoForge 风格） |
| 混淆工具方法 | `ResourceLocation.fromNamespaceAndPath/parse`（1.21 起替代构造器） |
| 已提交的 class 文件 | major version 65（= Java 21） |

上游历史上的旧版本（`.mcreator/FullBackup202300110610.zip`）才是 1.20.4 工程：
那一份用的是 `IPayloadRegistrar`、`RegisterPayloadHandlerEvent`、`PlayPayloadContext`、
`TickEvent.ServerTickEvent`、`mods.toml` + `neoforge [20.4.223,)`。
也就是说 **1.20.4 → 1.21.1 的移植上游作者已经做过**，本 fork 不需要重复搬运代码。

## 功能裁剪明细（v1.1.0 起）

保留：

| 保留项 | 位置 |
| --- | --- |
| O 键触发（默认键位可在「控制」里改） | `init/ObjectionModKeyMappings.java` |
| 网络包 `objection:key_objection_trigger` | `network/ObjectionTriggerMessage.java` |
| 服务端执行：粒子 + 音效 + 聊天广播 | `procedures/ObjectionTriggerEventProcedure.java` |
| `objection:objection` 音效与 `objection` 粒子 | `init/ObjectionModSounds.java`、`init/ObjectionModParticleTypes.java` |
| 头顶粒子渲染 | `client/particle/ObjectionParticle.java` |

移除（连同 MCreator 元素定义一起删掉，避免重新生成时回潮）：

- 物品：`AttorneysBadge`（律师徽章）、`Indictment`（起诉状）及其模型、贴图、语言键
- 创造模式物品栏 `ObjectionTab`
- 状态效果 `ObjectionTimer` 及 `potion/ObjectionTimerMobEffect.java`、`textures/mob_effect/*`
- 屏幕中央贴图：`ObjectionOverlay`、`OOA1/2/3` 四个 overlay 类、4 个配套 procedure、
  `textures/screens/objection.png`
- 触发流程里「32 格内实体施加 Objection Timer」的循环（overlay 的唯一数据来源），
  `elements/ObjectionTriggerEvent.mod.json` 里的 Blockly 过程也同步删掉了那个块
- 因此触发时不再有屏幕中央的大图，只剩玩家头顶的粒子 + 音效 + 聊天栏

## 本 fork 做了什么

1. **CI 构建**：`.github/workflows/build.yml` 在 `ubuntu-latest` 上用 Temurin JDK 21
   跑 `./gradlew build`，把 `build/libs/*.jar` 作为 artifact 上传；打 `v*` tag 时
   自动附加到 Release。任何人（包括作者）不需要本地 JDK 即可验证工程是否还能编译。
2. **构建参数**：`org.gradle.jvmargs` 从 `-Xmx1G` 提到 `-Xmx3G`（NeoForm 反编译/重编译
   阶段 1G 容易 OOM）；关闭 `org.gradle.configuration-cache`，避免 moddev 插件在
   配置缓存复用时的偶发失败。
3. **产物命名**：`build.gradle` 的 `version` 从 `1.0` 改成 `1.0.0`（与
   `neoforge.mods.toml` 中的 `version="1.0.0"` 对齐），`base.archivesName` 从
   `modid` 改成 `objection-neoforge-1.21.1`，产物一眼可辨。
4. **`gradlew` 权限位**：git 里原本是 `100644`，Linux runner 上 `./gradlew` 会
   "Permission denied"，已提交为 `100755`，并在 workflow 里再 `chmod +x` 兜底。

5. **冒烟测试**：`.github/workflows/smoke-test.yml`（手动触发）用 `./gradlew runServer`
   起一个 1.21.1 专用服务器，等日志出现 `Done (`，并断言模组自己的初始化标记
   `[Objection] initialised`，从而证明「能加载」，而不只是「能编译」。
6. **初始化日志**：`ObjectionMod` 构造函数末尾加了一行
   `LOGGER.info("[Objection] initialised: …")`，既方便玩家排查，也作为冒烟测试的断言锚点。
   这是本 fork 对 `src/**` 的唯一改动。

## CI 产物

[![Build](https://github.com/Youzix-Star/Objection-/actions/workflows/build.yml/badge.svg)](https://github.com/Youzix-Star/Objection-/actions/workflows/build.yml)

- 构建：<https://github.com/Youzix-Star/Objection-/actions/workflows/build.yml>
- 发布（每次打 `v*` tag，CI 会自动把 jar 挂到 Release）：
  <https://github.com/Youzix-Star/Objection-/releases>
- 最新产物：`objection-neoforge-1.21.1-1.1.0.jar`（裁剪版，385 KB），位于
  [v1.1.0 Release](https://github.com/Youzix-Star/Objection-/releases/tag/v1.1.0)；
  历史版本 [v1.0.0](https://github.com/Youzix-Star/Objection-/releases/tag/v1.0.0)
  是未裁剪的上游行为。
  两者元数据均为 `neoforge [21.1.176,)` + `minecraft [1.21.1]`，class 版本 65（Java 21）。


## 本地构建（可选）

```bash
git clone https://github.com/Youzix-Star/Objection-.git
cd Objection-
./gradlew build          # 需要 JDK 21；首次会下载 NeoForge/Minecraft 依赖
# 产物: build/libs/objection-neoforge-1.21.1-1.1.0.jar
```

## 已知遗留缺陷

- 裁剪后 `pack.mcmeta` 的 `pack_format` 仍是 48（1.21.1 资源包为 34，数据包为 48）。
  NeoForge 只提示不致命，未改动。
- `ObjectionTriggerMessage` 里 `context.enqueueWork(...)` 的异常分支会用到
  `context.connection()`，1.21.1 上标记为 deprecated，编译只有提示。
