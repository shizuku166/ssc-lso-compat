package com.ssclso;

import com.ssclso.api.SscLsoCompatApi;
import com.ssclso.compat.SscFormHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil;
import sfiomn.legendarysurvivaloverhaul.registry.MobEffectRegistry;

/**
 * SSC（Shape Shifter Curse）× LSO（Legendary Survival Overhaul）兼容 mod。
 *
 * 核心行为（0.2）：
 * <ul>
 *   <li>美西螈形态（axolotl_1/2/3）且进水/淋雨时，给予 LSO 内置御寒 buff
 *       （COLD_IMMUNITY），单次 6 秒、剩余不足 1 秒时续期；出水/雨停不主动
 *       移除，buff 自然消散（最长 6 秒），气泡柱不再触发。</li>
 *   <li>口渴分级：完全体（axolotl_3）停用口渴系统不显示口渴条；幼体
 *       （axolotl_0/1/2）保留口渴条但免疫 LSO 干渴 debuff（THIRST 效果）；
 *       其他形态维持正常口渴。复用 LSO 公开 API。</li>
 * </ul>
 */
@Mod(SscLsoCompatApi.MOD_ID)
public class SscLsoCompat {

    /** LSO 御寒 buff 单次时长（tick，120=6 秒）：进水/淋雨期间持续续期。 */
    private static final int COLD_IMMUNE_DURATION = 120;
    /** 剩余时长低于该值（tick，20=1 秒）时视为即将到期，需要重新补充。 */
    private static final int COLD_IMMUNE_REFRESH_THRESHOLD = 20;

    public SscLsoCompat() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 每 tick 维护御寒 buff 与口渴开关（仅服务端处理，客户端通过效果同步显示）：
     * <ol>
     *   <li>美西螈形态 + 进水(isInWater)或淋雨(isRainingAt) → 给予/续期 COLD_IMMUNITY；</li>
     *   <li>出水/雨停：不主动取消 buff，由效果自然消散（最长 6 秒），避免误伤
     *       玩家自用御寒药水或命令给予的同款 buff；非美西螈形态不动玩家身上的效果；</li>
     *   <li>SSC 可用时口渴分级：完全体（axolotl_3）deactivateThirst；幼体
     *       （axolotl_0/1/2）与其他形态 activateThirst，且幼体每 tick 移除 LSO
     *       THIRST 干渴效果；调用前先 isThirstActive 判断避免每 tick 重复调用；
     *       SSC 未加载跳过。</li>
     * </ol>
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }

        // 先取一次形态 id，供御寒判定与口渴开关共用
        boolean sscAvailable = SscFormHelper.isSscAvailable();
        ResourceLocation formId = sscAvailable ? SscFormHelper.getCurrentFormId(player) : null;
        boolean aquatic = SscLsoCompatApi.isAquaticForm(formId);

        // —— 1. 御寒 buff：美西螈形态 + 进水/淋雨 ——
        if (aquatic) {
            if (player.isInWater() || player.level().isRainingAt(player.blockPosition())) {
                MobEffectInstance current = player.getEffect(MobEffectRegistry.COLD_IMMUNITY.get());
                if (current == null || current.getDuration() < COLD_IMMUNE_REFRESH_THRESHOLD) {
                    player.addEffect(new MobEffectInstance(
                            MobEffectRegistry.COLD_IMMUNITY.get(),
                            COLD_IMMUNE_DURATION,
                            0, false, false, true));
                }
            }
            // 出水/雨停：不主动取消 buff，由效果自然消散（最长 6 秒），
            // 避免误伤玩家自用御寒药水或命令给予的同款 buff
        }
        // 非美西螈形态：不动玩家身上的效果（避免误伤自用御寒药水）

        // —— 2. 口渴分级处理：SSC 未加载时跳过 ——
        if (!sscAvailable) {
            return;
        }
        // 完全体（3阶段）vs 幼体（0~2阶段）
        boolean isAxolotl3 = isAxolotlStage(formId, "axolotl_3");
        boolean isAxolotl0to2 = isAxolotlStage(formId, "axolotl_0")
                || isAxolotlStage(formId, "axolotl_1")
                || isAxolotlStage(formId, "axolotl_2");

        boolean thirstActive = ThirstUtil.isThirstActive(player);
        if (isAxolotl3 && thirstActive) {
            // 完全体（3阶段）：停用口渴系统，不显示口渴条
            ThirstUtil.deactivateThirst(player);
        } else if (!isAxolotl3 && !thirstActive) {
            // 幼体（0~2）与其他非美西螈形态：保持口渴系统激活
            ThirstUtil.activateThirst(player);
        }
        if (isAxolotl0to2) {
            // 幼体（0~2阶段）：口渴条正常消耗，但免疫 LSO 干渴 debuff（THIRST 效果）
            player.removeEffect(MobEffectRegistry.THIRST.get());
        }
    }

    /** 判断形态 id 是否是指定阶段的 axolotl（namespace 须为 shape-shifter-curse）。 */
    private static boolean isAxolotlStage(ResourceLocation formId, String path) {
        return formId != null
                && "shape-shifter-curse".equals(formId.getNamespace())
                && path.equals(formId.getPath());
    }
}
