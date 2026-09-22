package com.ssclso.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * SSC（Shape Shifter Curse）运行时访问辅助。
 *
 * SSC 1.9.2 存在两套形态组件系统，这里双路反射兼容：
 * <ul>
 *   <li>ability 系统（主）：{@code RegPlayerFormComponent.PLAYER_FORM.get(player).getCurrentForm().FormID}</li>
 *   <li>new_form_system：{@code PlayerFormComponent.COMPONENT.get(player).nowForm.getFormID()}</li>
 * </ul>
 * 反射访问，因此本 mod 编译期不依赖 SSC / CCA / Apoli / Fabric API；
 * SSC 未安装时返回 null 且不报错。
 */
public final class SscFormHelper {

    private static final String SSC_MOD_ID = "shape-shifter-curse";

    /** ability 系统组件注册类：持有 PLAYER_FORM ComponentKey。 */
    private static final String ABILITY_REG_CLASS =
            "net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent";
    /** ability 系统组件实例类。 */
    private static final String ABILITY_COMP_CLASS =
            "net.onixary.shapeShifterCurseFabric.player_form.ability.PlayerFormComponent";
    /** ability 系统表单基类：public 字段 FormID。 */
    private static final String FORM_BASE_CLASS =
            "net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBase";

    /** new_form_system 组件类：COMPONENT ComponentKey + nowForm(IForm) 字段。 */
    private static final String NFS_COMP_CLASS =
            "net.onixary.shapeShifterCurseFabric.player_form.new_form_system.PlayerFormComponent";
    /** new_form_system 表单接口：getFormID() 方法。 */
    private static final String I_FORM_CLASS =
            "net.onixary.shapeShifterCurseFabric.player_form.new_form_system.IForm";

    private static boolean resolved = false;
    private static boolean available = false;

    private static String lastResolveError = "not-resolved";

    // ability 系统
    private static Object abilityComponentKey;         // ComponentKey 实例（get 的 receiver）
    private static Method abilityKeyGetMethod;         // ComponentKey#get(provider) 实例方法
    private static Method abilityGetCurrentFormMethod; // PlayerFormComponent#getCurrentForm()
    private static Field formBaseFormIdField;          // PlayerFormBase.FormID

    // new_form_system
    private static Object nfsComponentKey;             // ComponentKey 实例（get 的 receiver）
    private static Method nfsKeyGetMethod;             // ComponentKey#get(provider) 实例方法
    private static Field nfsNowFormField;              // PlayerFormComponent.nowForm
    private static Method iFormGetFormIdMethod;        // IForm#getFormID()

    private SscFormHelper() {
    }

    /** SSC 是否已加载且至少一套 CCA 组件可访问。 */
    public static boolean isSscAvailable() {
        if (!resolved) {
            resolved = true;
            try {
                if (!isSscModLoaded()) {
                    available = false;
                    lastResolveError = "mod-not-loaded";
                    return false;
                }
                resolveAbilitySystem();
                resolveNewFormSystem();
                available = abilityComponentKey != null || nfsComponentKey != null;
                lastResolveError = available ? "ok"
                        : "abilityKey=" + (abilityComponentKey != null)
                        + " nfsKey=" + (nfsComponentKey != null);
                if (!available) {
                    // 日志刷屏治理：不再向服务器后台打印，仅写文件供排查
                    FileLogger.log("[SscFormHelper] resolve failed: " + lastResolveError);
                }
            } catch (Throwable t) {
                available = false;
                lastResolveError = "resolve-throw: " + t;
                // 日志刷屏治理：不再向服务器后台打印，仅写文件供排查
                FileLogger.log("[SscFormHelper] resolve threw: " + t, t);
            }
        }
        return available;
    }

    /** 返回可读诊断串（用于调试日志）。 */
    public static String getDiagnostics() {
        if (!resolved) {
            isSscAvailable();
        }
        return "available=" + available
                + " abilityKey=" + (abilityComponentKey != null)
                + " abilityKeyGetMethod=" + (abilityKeyGetMethod != null)
                + " abilityGetMethod=" + (abilityGetCurrentFormMethod != null)
                + " formIdField=" + (formBaseFormIdField != null)
                + " nfsKey=" + (nfsComponentKey != null)
                + " nfsGetMethod=" + (nfsKeyGetMethod != null)
                + " nfsNowFormField=" + (nfsNowFormField != null)
                + " nfsFormIdMethod=" + (iFormGetFormIdMethod != null)
                + " lastError=" + lastResolveError;
    }

    private static boolean isSscModLoaded() {
        try {
            ModList modList = ModList.get();
            if (modList != null && modList.isLoaded(SSC_MOD_ID)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        // Sinytra Connector 场景下 Forge ModList 可能查不到 fabric mod，改由类加载兜底判断
        try {
            Class.forName(ABILITY_REG_CLASS);
            return true;
        } catch (Throwable t) {
            try {
                Class.forName(NFS_COMP_CLASS);
                return true;
            } catch (Throwable t2) {
                return false;
            }
        }
    }

    private static void resolveAbilitySystem() {
        try {
            Class<?> regClass = Class.forName(ABILITY_REG_CLASS);
            Field keyField = regClass.getField("PLAYER_FORM");
            abilityComponentKey = keyField.get(null);
            abilityKeyGetMethod = findGetMethod(abilityComponentKey);
            Class<?> compClass = Class.forName(ABILITY_COMP_CLASS);
            abilityGetCurrentFormMethod = compClass.getMethod("getCurrentForm");
            formBaseFormIdField = Class.forName(FORM_BASE_CLASS).getField("FormID");
            if (abilityComponentKey == null || abilityKeyGetMethod == null
                    || abilityGetCurrentFormMethod == null || formBaseFormIdField == null) {
                abilityComponentKey = null;
            }
        } catch (Throwable t) {
            abilityComponentKey = null;
        }
    }

    private static void resolveNewFormSystem() {
        try {
            Class<?> compClass = Class.forName(NFS_COMP_CLASS);
            Field keyField = compClass.getField("COMPONENT");
            nfsComponentKey = keyField.get(null);
            nfsKeyGetMethod = findGetMethod(nfsComponentKey);
            nfsNowFormField = compClass.getField("nowForm");
            iFormGetFormIdMethod = Class.forName(I_FORM_CLASS).getMethod("getFormID");
            if (nfsComponentKey == null || nfsKeyGetMethod == null || nfsNowFormField == null || iFormGetFormIdMethod == null) {
                nfsComponentKey = null;
                nfsKeyGetMethod = null;
            }
        } catch (Throwable t) {
            // SSC 的 new_form_system 组件与 ability 组件注册了相同 CCA ID，触发其
            // 静态初始化会抛 ExceptionInInitializerError；SSC 1.9.2 实际变身走 ability
            // 系统，此路仅作兜底，失败不再静默，记录以便排查。
            nfsComponentKey = null;
            nfsKeyGetMethod = null;
            // 日志刷屏治理：不再向服务器后台打印，仅写文件供排查
            FileLogger.log("[SscFormHelper] nfs resolve threw: " + t, t);
        }
    }

    /** ComponentKey 上的 get(provider) 方法（泛型擦除后遍历首个单参 get）。 */
    private static Method findGetMethod(Object componentKey) {
        for (Method m : componentKey.getClass().getMethods()) {
            if ("get".equals(m.getName()) && m.getParameterCount() == 1) {
                return m;
            }
        }
        return null;
    }

    /**
     * 获取玩家当前的 SSC 形态 id（如 shape-shifter-curse:axolotl_2）。
     * SSC 未加载 / 组件不可用 / 玩家无形态时返回 null。
     */
    @Nullable
    public static ResourceLocation getCurrentFormId(Player player) {
        if (!isSscAvailable() || player == null) {
            return null;
        }
        ResourceLocation id = tryAbilitySystem(player);
        if (id != null) {
            return id;
        }
        return tryNewFormSystem(player);
    }

    @Nullable
    private static ResourceLocation tryAbilitySystem(Player player) {
        if (abilityKeyGetMethod == null) {
            return null;
        }
        try {
            Object component = abilityKeyGetMethod.invoke(abilityComponentKey, player);
            if (component == null) {
                return null;
            }
            Object form = abilityGetCurrentFormMethod.invoke(component);
            if (form == null) {
                return null;
            }
            Object id = formBaseFormIdField.get(form);
            if (id == null) {
                return null;
            }
            return ResourceLocation.tryParse(id.toString());
        } catch (Throwable t) {
            // 日志刷屏治理：不再向服务器后台打印，仅写文件供排查
            FileLogger.log("[SscFormHelper] ability read threw: " + t, t);
            return null;
        }
    }

    @Nullable
    private static ResourceLocation tryNewFormSystem(Player player) {
        if (nfsKeyGetMethod == null) {
            return null;
        }
        try {
            Object component = nfsKeyGetMethod.invoke(nfsComponentKey, player);
            if (component == null) {
                return null;
            }
            Object form = nfsNowFormField.get(component);
            if (form == null) {
                return null;
            }
            Object id = iFormGetFormIdMethod.invoke(form);
            if (id == null) {
                return null;
            }
            return ResourceLocation.tryParse(id.toString());
        } catch (Throwable t) {
            // 日志刷屏治理：不再向服务器后台打印，仅写文件供排查
            FileLogger.log("[SscFormHelper] nfs read threw: " + t, t);
            return null;
        }
    }
}
