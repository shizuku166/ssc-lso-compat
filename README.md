# SSC LSO Compat

**Shape Shifter Curse（SSC）× Legendary Survival Overhaul（LSO）兼容扩展（Forge 1.20.1）**

让 SSC 的**美西螈形态**在**进水**时获得 LSO 内置的**温度免疫（TEMPERATURE_IMMUNITY）恒温 buff**，不中暑、不冻伤。不再自制水中恒温效果，也不再钳制 LSO 目标温度。

## 特性

- 判定链路（严格按此顺序）：
  1. 读取 SSC 当前形态 id（美西螈形态 id 为 **1~3**，即 `axolotl_1` / `axolotl_2` / `axolotl_3`）；
  2. 判断玩家是否为美西螈形态；
  3. 判断玩家是否**进水**（美西螈进水会获得移速加成，水是其主场）。
- 三者满足时，直接给予 LSO 内置 **TEMPERATURE_IMMUNITY（温度免疫）** 恒温 buff（进水期间持续续期，出水后约 20 秒自然消散）。
- 非美西螈形态、或未进水时，不干预 LSO 任何逻辑。
- 未安装 SSC 时本 mod 静默降级：仅作为空壳加载，不报错、不影响 LSO。
- **仅服务端逻辑**：判定与加 buff 全部在服务端执行，客户端只收到效果同步。

## 实现原理

| 层次 | 方案 |
| --- | --- |
| 形态检测 | 运行时反射读取 SSC 的 CCA 组件（ability 系统 `PLAYER_FORM.getCurrentForm().FormID` 与 new_form_system `COMPONENT.nowForm.getFormID()` 双路兼容），编译期不依赖 SSC / CCA / Apoli / Fabric API |
| 进水判断 | 原生 `Player#isInWater()` |
| 恒温 buff | Forge `PlayerTickEvent` 服务端维护 LSO 内置 `MobEffectRegistry.TEMPERATURE_IMMUNITY`（400 tick 续期，避免误伤玩家自身的温度免疫药水） |
| 扩展性 | `SscLsoCompatApi` 公开水生形态注册表，供以后给 SSC 其他形态扩展 |

## 依赖

| 依赖 | 版本 | 类型 |
| --- | --- | --- |
| Minecraft Forge | 1.20.1（≥ 47.0，编译用 47.2.0） | 必需 |
| Legendary Survival Overhaul | 1.20.1（Modrinth） | 必需 |
| Shape Shifter Curse（connector 分支） | 任意 1.20.1 构建 | 可选（未装则本 mod 空转） |

## 构建

环境要求：JDK 17 + Gradle 8.x。

```bash
gradle build
```

产物：`build/libs/ssc-lso-compat-0.1.jar`

## 安装

1. 把 `ssc-lso-compat-0.1.jar` 放入版本隔离目录的 `mods` 文件夹；
2. 确保同目录已有 **LSO 1.20.1** 与 **SSC connector 分支构建**；
3. 升级前请移除旧版 `ssc-lso-compat-*.jar`（避免同 modId 多版本共存）。

## 扩展 API：给 SSC 其他形态加恒温

以后想给其他形态（如雪狐、龙形态等）也加进水恒温，只需在任意初始化阶段注册其形态 id：

```java
SscLsoCompatApi.registerAquaticForm(new ResourceLocation("shape-shifter-curse", "snow_fox_2"));
```

常用 API：

| 方法 | 说明 |
| --- | --- |
| `registerAquaticForm(ResourceLocation)` | 注册水生形态（幂等） |
| `unregisterAquaticForm(ResourceLocation)` | 取消注册 |
| `isAquaticForm(ResourceLocation)` | 判断是否水生形态 |
| `getAquaticForms()` | 当前全部水生形态（只读） |

> 默认仅注册美西螈 `axolotl_1/2/3`；初始形态 `axolotl_0` 如需恒温请自行注册。

## 许可与免责声明

- 本 mod 以 **MIT** 协议开源，详见 [LICENSE](LICENSE)。
- 本 mod **仅调用** Legendary Survival Overhaul（LSO）的**公开 API** 与 Shape Shifter Curse（SSC）的运行时反射互操作，**不含** LSO / SSC 的任何代码、资源或数据。
- LSO 本身为 **All Rights Reserved**，版权归原作者 sfiomn 所有；SSC 为 **MIT** 协议，版权归原团队所有。
