# Objection! — NeoForge 1.21.1 移植说明

本仓库是 [GlcDeamino/Objection-](https://github.com/GlcDeamino/Objection-) 的 fork，
目标是把它固定在 **Minecraft 1.21.1 + NeoForge 21.1.x** 上，并用 GitHub Actions
持续产出可安装的 jar。

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

源代码（`src/**`）**一行未改**：上游代码在 21.1.176 上可以编译通过。

## 本地构建（可选）

```bash
git clone https://github.com/Youzix-Star/Objection-.git
cd Objection-
./gradlew build          # 需要 JDK 21；首次会下载 NeoForge/Minecraft 依赖
# 产物: build/libs/objection-neoforge-1.21.1-1.0.0.jar
```

## 已知遗留缺陷（上游自带，未修）

- `assets/objection/models/item/indictment.json` 的 `layer0` 指向
  `objection:item/attorneys_badge`，且 `textures/item/` 下没有 `indictment.png`，
  所以「起诉状」物品显示的是律师徽章贴图。
- `lang/zh_cn.json` 的 `item_group.objection.objection_tab` 是 `"Objection!"`，
  创造模式物品栏标题在中文下会显示英文；`itemGroup.tab_objection_tab` 是 1.19
  时代的旧键，已不再被读取。
- `pack.mcmeta` 的 `pack_format` 为 48（1.21.1 的资源包格式是 34，数据包为 48）。
  NeoForge 只提示不致命，故未改动。
- 四个 overlay 类里有 MCreator 误加的 `import org.checkerframework.checker.units.qual.h;`
  （checker-qual 在编译类路径上，所以能过编译），无实际作用。
- `AttorneysBadgeItem` 覆写的 `hasCraftingRemainingItem/getCraftingRemainingItem`
  在 1.21.1 已标记 `@Deprecated`（1.21.2 起被移除），编译只会产生 deprecation 提示。
