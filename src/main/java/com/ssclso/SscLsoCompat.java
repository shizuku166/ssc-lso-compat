package com.ssclso;

import com.ssclso.api.SscLsoCompatApi;
import com.ssclso.compat.FileLogger;
import com.ssclso.compat.ThermoHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import sfiomn.legendarysurvivaloverhaul.registry.MobEffectRegistry;

/**
 * SSC（Shape Shifter Curse）× LSO（Legendary Survival Overhaul）兼容 mod。
 *
 * 核心行为（1.1.0 重构）：玩家处于 SSC 美西螈形态（axolotl_1/2/3）且进水时，
 * 直接给予 LSO 内置的“温度免疫”恒温 buff（TEMPERATURE_IMMUNITY），
 * 不再自制水中恒温效果、不再用 mixin 钳制 LSO 目标温度。
 */
@Mod(SscLsoCompatApi.MOD_ID)
public class SscLsoCompat {

    /** LSO 恒温 buff 单次时长（tick）：进水期间持续续期，出水后约 20 秒自然消散。 */
    private static final int TEMP_IMMUNE_DURATION = 400;
    /** 剩余时长低于该值（tick）时视为即将到期，需要重新补充。 */
    private static final int TEMP_IMMUNE_REFRESH_THRESHOLD = 300;

    private static final Logger LOGGER = LogUtils.getLogger();
    /** 调试日志计数：每 100 tick（5 秒）打印一次判定过程。 */
    private int debugTickCounter = 0;

    public SscLsoCompat() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 每 tick 维护 LSO 恒温 buff：
     * 美西螈形态（1~3）+ 进水 → 给予/续期 TEMPERATURE_IMMUNITY；
     * 出水后不主动移除，buff 在 400 tick（约 20 秒）内自然到期，避免误伤玩家自身的温度免疫药水效果。
     * 仅服务端处理，客户端通过效果同步显示。
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

        // 调试：每 100 tick 打印一次判定过程
        debugTickCounter++;
        boolean debug = debugTickCounter % 100 == 0;
        ResourceLocation debugFormId = debug ? com.ssclso.compat.SscFormHelper.getCurrentFormId(player) : null;
        boolean debugInWater = debug && player.isInWaterRainOrBubble();
        boolean shouldApply = ThermoHandler.shouldApplyAxolotlThermo(player);
        if (debug) {
            String line = String.format("[SscLsoCompat] debug formId=%s inWater=%s aquatic=%s shouldApply=%s diag=[%s]",
                    debugFormId, debugInWater,
                    debugFormId != null && SscLsoCompatApi.isAquaticForm(debugFormId),
                    shouldApply,
                    com.ssclso.compat.SscFormHelper.getDiagnostics());
            LOGGER.info(line);
            FileLogger.log(line);
        }

        if (!shouldApply) {
            return;
        }

        MobEffectInstance current = player.getEffect(MobEffectRegistry.TEMPERATURE_IMMUNITY.get());
        if (current == null || current.getDuration() < TEMP_IMMUNE_REFRESH_THRESHOLD) {
            player.addEffect(new MobEffectInstance(
                    MobEffectRegistry.TEMPERATURE_IMMUNITY.get(),
                    TEMP_IMMUNE_DURATION,
                    0, false, false, true));
            String granted = "[SscLsoCompat] granted TEMPERATURE_IMMUNITY to " + player.getName().getString();
            LOGGER.info(granted);
            FileLogger.log(granted);
        }
    }
}
