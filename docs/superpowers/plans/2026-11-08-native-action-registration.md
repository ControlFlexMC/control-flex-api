# 原生动作注册实现计划（0.14.0）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让桥接模组直接向 ControlFlex 声明动作与默认绑定，不再注册 MC `KeyMapping`，并把 cobblemon 的 18 个战斗动作迁到这套机制。

**Architecture:** 已注册动作进 `BindingRegistry.getActionIds()`，解析统一走 `getEffectiveBinding`；Screen/Overlay 绑定存在新的 `screen_action_bindings.json`，默认值只在内存兜底；回调挂在组合事件汇的边沿，不随每 tick 重复；序列化显式跳过已注册动作，保证它们不进全局映射表。

**Tech Stack:** Java 8（API 模块）/ Java 21（核心与桥）、Gradle、JUnit 5、Stonecutter（1.21.1-neoforge / fabric）。

**Spec:** `control-flex-api/docs/specs/2026-11-08-native-action-registration-design.md`

## Global Constraints

- API 模块必须保持 Java 8 兼容（`--release 8`，不能用 `List.of`、record、switch 表达式）。
- 注册回调 `onRegisterActions` 必须早于 `bindingRegistry.loadFromJson`。
- Screen/Overlay 动作不得写入 `guiActionBindings` / `overlayActionBindings`。
- 默认值不落盘；`""` 表示永久清空，解析时不得回落到默认。
- 回调按边沿发出；HOLD / TOGGLE 的重复 tick 不得重复回调。
- 版本：API `0.14.0`；核心 `cfx_api_version=0.14.0`。

## Review Focus

- **注册窗口关闭后仍调用 registrar** → 应抛 `IllegalStateException`，不能静默返回 no-op。
- **用户把动作清空后重进游戏** → store 里是 `""`，不得被默认值重新种回。
- **HOLD 绑定的屏幕动作** → 每个 tick 都参与派发，但 `onActionTriggered` 只能一次。
- **同一动作同时出现在 Screen 与 Overlay 前台** → 只回调一次，且 Screen 优先。
- **已注册动作被序列化** → profile 里不能出现它的层映射条目。

---

### Task 1: API 类型（Java 8）

**Files:**
- Create: `control-flex-api/src/main/java/com/ifels/controlflex/api/ActionLayer.java`
- Create: `.../DefaultBinding.java`
- Create: `.../ActionRegistration.java`
- Create: `.../IActionRegistrar.java`
- Create: `.../IControlFlexAction.java`
- Create: `.../ActionTrigger.java`
- Modify: `.../IControlFlexPlugin.java`（三个 default 方法）
- Modify: `control-flex-api/gradle.properties`（`api_version=0.14.0`）

**Interfaces:**
- `ActionLayer { IN_GAME, SCREEN, OVERLAY }`
- `DefaultBinding.of(String button, TriggerMode mode)`
- `ActionRegistration.builder(String).nameKey/groupModId/layer/targetClass/defaultBinding/build()`
- `IActionRegistrar.register(ActionRegistration) -> IControlFlexAction`
- `IControlFlexAction { String getActionId(); boolean isActive(); }`
- `ActionTrigger { String actionId(); TriggerMode mode(); ActionLayer layer(); String targetClassName(); }`
- `IControlFlexPlugin.onRegisterActions(IActionRegistrar)` / `onActionTriggered(ActionTrigger)` / `onActionReleased(String)`

- [ ] Step 1: 按上述签名创建接口与值对象；Builder 内部保存 `layers`（空 = 三层）、`classes`、`defaultBinding`，`build()` 校验 id 非空。
- [ ] Step 2: `./gradlew build`（API 仓库），确认 Java 8 编译通过。
- [ ] Step 3: Commit `feat(api): native action registration types (0.14.0)`.

### Task 2: 注册表与元数据

**Files:**
- Create: `control-flex-1.21.1/src/main/java/com/ifels/controlflex/compat/RegisteredActionRegistry.java`
- Modify: `binding/BindingRegistry.java`（`getActionIds`、`isActionRelevantForLayer`、`getActionDisplayName`、`getActionLocalizedName`、新增 `getActionGroupModId`、缓存失效）
- Test: `src/test/java/com/ifels/controlflex/compat/RegisteredActionRegistryTest.java`

**Interfaces:**
- `RegisteredActionRegistry.getInstance()`
- `void openRegistration()` / `void closeRegistration()`
- `IControlFlexAction register(ActionRegistration, String ownerModId)`
- `boolean isRegistered(String)` / `RegisteredActionDef definition(String)`
- `Set<String> registeredFor(ActionLayer, String className)`
- `void clearForTests()`

- [ ] Step 1: 写失败测试：非法 id 返回 no-op handle；重复 id 幂等；窗口关闭后注册抛异常；`getActionIds()` 含注册 id；SCREEN 动作对 `shift1` 不相关、对 `gui` 相关。
- [ ] Step 2: 跑测试确认失败。
- [ ] Step 3: 实现注册表与 `BindingRegistry` 分支（`isActionRelevantForLayer` 的注册分支必须排在“含冒号 = 全层”之前）。
- [ ] Step 4: 跑测试通过；`./gradlew :1.21.1-neoforge:test`。
- [ ] Step 5: Commit `feat(compat): registered action registry and metadata`.

### Task 3: 每屏 store 与解析

**Files:**
- Create: `compat/ScreenActionBindingStore.java`
- Create: `compat/RegisteredActionResolver.java`
- Modify: `binding/BindingRegistry.getEffectiveBinding`
- Test: `src/test/java/com/ifels/controlflex/compat/ScreenActionBindingStoreTest.java`

**Interfaces:**
- `ScreenActionBindingStore(Supplier<Path> file)`
- `boolean hasValue(boolean overlay, String className, String actionId)`
- `ActionBinding resolve(boolean overlay, String className, String actionId, ActionBinding fallback)`
- `void put(boolean overlay, String className, String actionId, ActionBinding binding)`
- `void remove(boolean overlay, String className, String actionId)`

- [ ] Step 1: 测试：无值走默认；`""` 返回 unbound；用户值覆盖默认；文件损坏当空；`hasValue` 区分“清空”与“未配置”。
- [ ] Step 2: 实现 store（Gson，路径 `config/controlflex/screen_action_bindings.json`）与 resolver。
- [ ] Step 3: `getEffectiveBinding` 增加注册分支（IN_GAME 走层映射；SCREEN/OVERLAY 走 resolver）。
- [ ] Step 4: 全量测试；Commit `feat(compat): per-screen store for registered actions`.

### Task 4: 序列化与 UI 写入口

**Files:**
- Modify: `binding/BindingSerializer.java`（层加载 / 导出跳过注册 id）
- Modify: `binding/BindingRegistry.setActionBinding` / `resetActionBinding`
- Test: `src/test/java/com/ifels/controlflex/binding/RegisteredActionSerializationTest.java`

- [ ] Step 1: 测试：`exportToJson()` 的 `guiActionBindings` 不含注册的 SCREEN 动作；`setActionBinding(id, binding, GUI)` 写 store 而不写 map；`resetActionBinding` 删 store 键。
- [ ] Step 2: 实现跳过与写分支。
- [ ] Step 3: 全量测试；Commit `feat(compat): keep registered actions out of layer maps`.

### Task 5: 派发与回调

**Files:**
- Modify: `binding/BindingMapper.java`（GUI 候选追加注册动作）
- Modify: `binding/ActionCandidacy.java`（注册分支）
- Modify: `gui/GuiActionHandler.java`（`handleGUIAction` / `handleGUIActionRelease` 分支）
- Modify: `binding/ActionDispatcher.java`（IN_GAME 注册动作分支）
- Modify: `compat/ControlFlexApiAdapter.java`（`onRegisterActions` 调用）
- Create: `compat/RegisteredActionDispatcher.java`（事件 → 回调映射、活跃集、`isActive`）
- Test: `src/test/java/com/ifels/controlflex/compat/RegisteredActionDispatcherTest.java`

- [ ] Step 1: 测试：PRESS 一次；HOLD 触发一次 + 释放一次；TOGGLE 翻转两次各一次；异常隔离；`isActive` 对一次性模式仅触发 tick。
- [ ] Step 2: 实现 dispatcher 与各分支。
- [ ] Step 3: 测试 GUI 候选包含当前 Screen 的注册动作、不包含其他 Screen 的。
- [ ] Step 4: 全量测试；Commit `feat(compat): dispatch registered actions through callbacks`.

### Task 6: cobblemon 迁移

**Files:**
- Modify: `cfx-compat-cobblemon/common/.../CfxCobblemonPlugin.java`（实现 `onRegisterActions` + `onActionTriggered`）
- Create: `common/.../BattleActions.java`（18 个动作的注册与默认值）
- Delete: `BattleKeys.java`、`BattleAction` 的 KeyMapping 注册、compat JSON 的 `screenKeys`
- Modify: `BattleTicker.java`（去掉按键消费，保留页面同步 / 光标停靠）
- Modify: `common/src/test/.../BattleActionTest`（若存在）

- [ ] Step 1: 注册 18 个动作：`layers={SCREEN}`、`targetClass=BattleGUI`；默认仅 prev/next=LB/RB、log_up/down=右摇杆上下。
- [ ] Step 2: 回调按 actionId 分派到 `BattleScreen.click` / 高亮移动 / 日志滚动。
- [ ] Step 3: 删除旧路径（伪 KeyMapping、screenKeys、candidateIds、ticker 消费）。
- [ ] Step 4: `:neoforge:test` 与 `:fabric:compileJava` 通过。
- [ ] Step 5: Commit `feat: declare battle actions natively`.

### Task 7: 版本、构建与部署

- [ ] Step 1: API `publishToMavenLocal`（0.14.0）；核心 `cfx_api_version=0.14.0`；桥接 `controlflex_api_version=0.14.0`。
- [ ] Step 2: 核心全量测试；两个实例部署。
- [ ] Step 3: 桥接构建并部署两个实例；核对产物无 `KeyMapping` 注册、无 `screenKeys`。
- [ ] Step 4: 记录到 memory；输出实机验证清单（4 个默认键、清空后不回落、HOLD 不重复）。
