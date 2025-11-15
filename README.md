# MinecartLoader

MinecartLoader 是一个基于 WildLoaders 的辅助插件，用于在 Paper 1.21+ 服务器上为矿车自动创建临时区块加载器。矿车在远处移动时，可自动保持所在区块加载；当矿车停止且超时后，插件会自动卸载对应的加载器，避免长期占用资源。

## 依赖

- Paper 1.21+（或兼容的分支）
- [WildLoaders](https://www.spigotmc.org/resources/69992/) 主插件

## 构建

1. 在 `MinecartLoader` 目录下使用 Gradle Wrapper：

   ```bash
   cd MinecartLoader
   ./gradlew build
   ```

2. 构建成功后，产物位于 `build/libs/`，文件名通常是 `MinecartLoader-1.0.0.jar`。

## 安装

1. 从项目的 [Release 页面](https://github.com/LingNc/WildLoaders/releases) 下载最新版的 `MinecartLoader` 插件 jar 和对应版本的 WildLoaders。
2. 将两个插件的 jar 放进你的 Paper 1.21.x 服务器的 `plugins/` 目录（WildLoaders 需先加载）。
3. 启动服务器，确认控制台无错误后即可运行，插件会自动在 `plugins/MinecartLoader/` 生成 `config.yml`。

## 配置

插件的主配置文件为 `config.yml`：

- `minecart-loader-enabled`：是否启用矿车加载逻辑，默认 `true`。
- `timeout-seconds`：每个由本插件创建的加载器基础存活时间（单位：秒），例如 `60` ≈ 1 分钟。
- `static-threshold-seconds`：矿车静止时额外允许运行的时间（单位：秒），用于避免短暂停顿就立即卸载，`0` 表示不开启。
- `chunk-radius`：加载范围的块半径，`0` 表示仅当前 chunk，该值会设置到 WildLoaders 的 `LoaderData`。
- `scan-on-startup`：服务器启动时是否扫描现有矿车并为它们建立加载记录，默认 `true`。
- `display-item`：在 WildLoaders 中用于展示/掉落的物品类型，请填写合法的 Material 名称（例如 `CLOCK`、`EMERALD_BLOCK`）。

## 命令与权限

- 命令：`/minecartloader <on|off|freeze|unfreeze|status|reload>`
- 权限：`minecartloader.admin`（默认 OP）

子命令说明：

- `on`：启用矿车区块加载逻辑。
- `off`：关闭矿车区块加载逻辑，不再创建或续期加载器，但不立即删除现有加载器。
- `freeze`：关闭逻辑并移除所有由本插件创建的临时加载器。
- `unfreeze`：重新开启逻辑，可通过移动矿车重新建立加载器。
- `status`：查看当前启用状态以及活跃 chunk 计数。
- `reload`：从磁盘重新加载 `config.yml` 并应用新的时间/半径配置（无需重启服务器）。

## 开发与构建

- 本项目使用 Java 21 和 Gradle 构建。
- 若你的环境没有全局 `gradle`，确保该目录中可执行的 Gradle Wrapper 存在（`./gradlew build`）。
