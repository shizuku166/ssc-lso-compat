package com.ssclso.compat;

import com.ssclso.api.SscLsoCompatApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 美西螈恒温判定逻辑（0.2-beta）：
 * <ol>
 *   <li>先读取 SSC 当前形态 id（美西螈形态 id 为 1~3，即 axolotl_1/2/3）；</li>
 *   <li>判断玩家是否为美西螈形态；</li>
 *   <li>判断玩家是否处于湿润状态：进水 / 淋雨 / 气泡柱（isInWaterRainOrBubble）。</li>
 * </ol>
 * 三者满足时，主类给予 LSO 内置恒温 buff（TEMPERATURE_IMMUNITY）。
 * <p>v0.2-beta 修复：原判定仅 isInWater，雨天淋湿（不在水中）仍会掉寒冷值，现扩展覆盖淋雨。</p>
 */
public final class ThermoHandler {

    private ThermoHandler() {
    }

    /** 是否应给予 LSO 恒温 buff（美西螈形态 + 进水/淋雨/气泡柱）。 */
    public static boolean shouldApplyAxolotlThermo(Player player) {
        if (player == null) {
            return false;
        }
        // 1. 先读取 SSC 当前形态 id
        ResourceLocation formId = SscFormHelper.getCurrentFormId(player);
        // 2. 判断是否为美西螈形态（axolotl_1/2/3）
        if (!SscLsoCompatApi.isAquaticForm(formId)) {
            return false;
        }
        // 3. 判断玩家是否湿润（进水 / 淋雨 / 气泡柱，美西螈主场为水）
        return player.isInWaterRainOrBubble();
    }
}
