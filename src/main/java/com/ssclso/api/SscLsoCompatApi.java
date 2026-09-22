package com.ssclso.api;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SSC × LSO 兼容扩展的公开 API。
 *
 * 默认已注册 SSC 美西螈的三个变身形态：axolotl_1 / axolotl_2 / axolotl_3
 * （对应形态 id 1~3，axolotl_0 是初始形态）。这些形态进水时会被给予 LSO 内置恒温 buff。
 */
public final class SscLsoCompatApi {

    public static final String MOD_ID = "ssc_lso_compat";

    private static final Set<ResourceLocation> AQUATIC_FORMS = new HashSet<>();

    static {
        registerAquaticForm(new ResourceLocation("shape-shifter-curse", "axolotl_1"));
        registerAquaticForm(new ResourceLocation("shape-shifter-curse", "axolotl_2"));
        registerAquaticForm(new ResourceLocation("shape-shifter-curse", "axolotl_3"));
    }

    private SscLsoCompatApi() {
    }

    /** 注册一个 SSC 形态 id 为“水生形态”（进水恒温）。可随时调用，幂等。 */
    public static void registerAquaticForm(ResourceLocation formId) {
        if (formId != null) {
            AQUATIC_FORMS.add(formId);
        }
    }

    /** 取消注册某个水生形态。 */
    public static void unregisterAquaticForm(ResourceLocation formId) {
        if (formId != null) {
            AQUATIC_FORMS.remove(formId);
        }
    }

    /** 判断某个形态 id 是否已注册为水生形态（美西螈）。 */
    public static boolean isAquaticForm(ResourceLocation formId) {
        return formId != null && AQUATIC_FORMS.contains(formId);
    }

    /** 返回当前已注册的全部水生形态 id（只读视图）。 */
    public static Set<ResourceLocation> getAquaticForms() {
        return Collections.unmodifiableSet(AQUATIC_FORMS);
    }
}
