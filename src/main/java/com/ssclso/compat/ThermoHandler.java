package com.ssclso.compat;

import com.ssclso.api.SscLsoCompatApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 美西螈御寒判定逻辑（0.3）：
 * <ol>
 *   <li>先读取 SSC 当前形态 id（美西螈形态 id 为 1~3，即 axolotl_1/2/3）；</li>
 *   <li>判断玩家是否为美西螈形态；</li>
 *   <li>判断玩家是否处于进水（isInWater）或淋雨（isInRain）状态；气泡柱不再触发。</li>
 * </ol>
 * 三者满足时，主类给予 LSO 内置御寒 buff（COLD_IMMUNITY）。
 */
public final class ThermoHandler {

    private ThermoHandler() {
    }

    /** 是否应给予 LSO 御寒 buff（美西螈形态 + 进水/淋雨）。 */
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
        // 3. 判断玩家是否进水或淋雨（气泡柱不再触发）
        return player.isInWater() || player.level().isRainingAt(player.blockPosition());
    }
}
