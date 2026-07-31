package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.features.module.impl.combat.AntiBot;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.utility.client.anticheat.AntiCheatManager;
import cn.advicenext.utility.client.anticheat.AntiCheatPlayerData;
import cn.advicenext.utility.client.render.Render3DEngine;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.List;

/**
 * Nametags：自定义玩家名牌。
 *
 * 模式：
 * <ul>
 *   <li><b>Classic</b> — 取代原版名牌，可穿墙，显示更多信息（血量、盔甲、距离等）。</li>
 *   <li><b>New</b> — 留空待设计。</li>
 * </ul>
 */
public class Nametags extends Module {

    public static Nametags INSTANCE;

    private final ModeSetting mode = new ModeSetting("Mode", "Nametags mode", "Classic", List.of("Classic", "New"));
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", "Show nametags through walls", true);
    private final BooleanSetting showHealth = new BooleanSetting("Health", "Show health bar", true);
    private final BooleanSetting showArmor = new BooleanSetting("Armor", "Show equipped armor", true);
    private final BooleanSetting showDistance = new BooleanSetting("Distance", "Show distance", true);
    private final BooleanSetting showPing = new BooleanSetting("Ping", "Show player ping", true);
    private final BooleanSetting showGameMode = new BooleanSetting("GameMode", "Show game mode", true);
    private final BooleanSetting showItems = new BooleanSetting("Items", "Show held item", true);
    private final BooleanSetting showHealthBar = new BooleanSetting("Health Bar", "Show health bar below name", true);
    private final BooleanSetting showFlag = new BooleanSetting("Flag", "Show anti-cheat flag VL", true);
    private final BooleanSetting self = new BooleanSetting("Self", "Show own nametag", false);
    private final DoubleSetting scale = new DoubleSetting("Scale", "Nametag scale", 1.0, 2.0, 0.5, 0.05);

    public Nametags() {
        super("Nametags", "Custom player nametags", Category.RENDER);
        INSTANCE = this;
        this.settings.add(mode);
        this.settings.add(throughWalls);
        this.settings.add(showHealth);
        this.settings.add(showArmor);
        this.settings.add(showDistance);
        this.settings.add(showPing);
        this.settings.add(showGameMode);
        this.settings.add(showItems);
        this.settings.add(showHealthBar);
        this.settings.add(showFlag);
        this.settings.add(self);
        this.settings.add(scale);
    }

    @Override
    public String getDisplayValue() {
        return mode.getValue();
    }

    public boolean shouldCancelVanilla() {
        return getEnabled() && mode.is("Classic");
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null) return;

        if (mode.is("New")) {
            return;
        }

        if (mode.is("Classic")) {
            renderClassicNametags(event);
        }
    }

    private void renderClassicNametags(Render2DEvent event) {
        DrawContext ctx = event.getContext();
        float tickDelta = event.getTickCounter().getDynamicDeltaTicks();

        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRenderNametag(entity)) continue;

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

            double eyeY = y + entity.getHeight() + 0.5;
            Vec3d projected = Render3DEngine.worldToScreen(x, eyeY, z);
            if (projected == null) continue;

            float screenX = (float) projected.x;
            float screenY = (float) projected.y;

            LivingEntity living = (LivingEntity) entity;
            PlayerEntity player = entity instanceof PlayerEntity ? (PlayerEntity) entity : null;

            String name = entity.getName().getString();
            int nameWidth = mc.textRenderer.getWidth(name);
            int bgWidth = Math.max(nameWidth, 60) + 8;
            int bgHeight = 14;

            StringBuilder info = new StringBuilder();
            if (showHealth.getValue()) {
                info.append(String.format("%.0fHP ", living.getHealth()));
            }
            if (showDistance.getValue()) {
                info.append(String.format("%.1fm ", mc.player.distanceTo(entity)));
            }
            if (showPing.getValue() && player != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                if (entry != null) info.append(entry.getLatency()).append("ms ");
            }
            if (showGameMode.getValue() && player != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                if (entry != null) {
                    GameMode gm = entry.getGameMode();
                    if (gm == GameMode.CREATIVE) info.append("[C] ");
                    else if (gm == GameMode.SPECTATOR) info.append("[Sp] ");
                    else if (gm == GameMode.ADVENTURE) info.append("[A] ");
                }
            }
            if (showItems.getValue() && living.getMainHandStack() != null && !living.getMainHandStack().isEmpty()) {
                info.append(living.getMainHandStack().getName().getString());
            }

            int infoLines = info.length() > 0 ? 1 : 0;
            int armorLines = (showArmor.getValue() && player != null && hasArmor(player)) ? 1 : 0;
            int healthBarLine = showHealthBar.getValue() ? 1 : 0;
            int flagLine = (showFlag.getValue() && player != null) ? 1 : 0;
            int totalExtraLines = infoLines + armorLines + healthBarLine + flagLine;

            int totalHeight = bgHeight + totalExtraLines * 10;

            float bgX = screenX - bgWidth / 2f;
            float bgY = screenY - totalHeight / 2f;

            int bgColor = (160 << 24) | 0x111111;
            ctx.fill((int) bgX, (int) bgY, (int) (bgX + bgWidth), (int) (bgY + totalHeight), bgColor);

            int nameColor = getNametagColor(player);
            ctx.drawText(mc.textRenderer, name, (int) (screenX - nameWidth / 2f), (int) bgY + 2, nameColor, true);

            int lineY = (int) bgY + bgHeight;

            if (info.length() > 0) {
                String infoStr = info.toString().trim();
                int infoWidth = mc.textRenderer.getWidth(infoStr);
                ctx.drawText(mc.textRenderer, infoStr, (int) (screenX - infoWidth / 2f), lineY, 0xFFCCCCCC, true);
                lineY += 10;
            }

            if (showArmor.getValue() && player != null) {
                StringBuilder armorStr = new StringBuilder();
                EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                for (EquipmentSlot slot : armorSlots) {
                    ItemStack armor = player.getEquippedStack(slot);
                    if (armor != null && !armor.isEmpty()) {
                        String armorName = armor.getName().getString();
                        armorStr.append(armorName, 0, Math.min(4, armorName.length())).append(" ");
                    }
                }
                if (armorStr.length() > 0) {
                    int armorWidth = mc.textRenderer.getWidth(armorStr.toString().trim());
                    ctx.drawText(mc.textRenderer, armorStr.toString().trim(), (int) (screenX - armorWidth / 2f), lineY, 0xFFAAAAAA, true);
                    lineY += 10;
                }
            }

            if (showHealthBar.getValue()) {
                float healthPercent = MathHelper.clamp(living.getHealth() / living.getMaxHealth(), 0, 1);
                float barX = bgX + 2;
                float barW = bgWidth - 4;
                float barH = 2;
                ctx.fill((int) barX, lineY, (int) (barX + barW), lineY + (int) barH, 0xFF202020);
                int healthColor = getHealthColor(healthPercent);
                ctx.fill((int) barX, lineY, (int) (barX + barW * healthPercent), lineY + (int) barH, healthColor);
                lineY += 4;
            }

            if (showFlag.getValue() && player != null) {
                String flagStr = getFlagString(player);
                if (flagStr != null) {
                    int flagWidth = mc.textRenderer.getWidth(flagStr);
                    ctx.drawText(mc.textRenderer, flagStr, (int) (screenX - flagWidth / 2f), lineY, 0xFFFF5555, true);
                }
            }
        }
    }

    private boolean hasArmor(PlayerEntity player) {
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack != null && !stack.isEmpty()) return true;
        }
        return false;
    }

    private boolean shouldRenderNametag(Entity entity) {
        if (entity == mc.player && !self.getValue()) return false;
        if (!(entity instanceof LivingEntity)) return false;
        if (entity.isInvisible()) return false;
        if (entity instanceof PlayerEntity p && AntiBot.isBotStatic(p)) return false;
        return true;
    }

    private int getNametagColor(PlayerEntity player) {
        if (player == null) return 0xFFFFFFFF;
        if (player.isSneaking()) return 0xFFFFAA00;
        return 0xFFFFFFFF;
    }

    private int getHealthColor(float healthPercent) {
        if (healthPercent > 0.8f) return 0xFF4CAF50;
        if (healthPercent > 0.6f) return 0xFF8BC34A;
        if (healthPercent > 0.4f) return 0xFFFFC107;
        if (healthPercent > 0.2f) return 0xFFFF9800;
        return 0xFFF44336;
    }

    private String getFlagString(PlayerEntity player) {
        AntiCheatPlayerData data = AntiCheatManager.getInstance().getPlayerData(player.getUuid().toString());
        if (data == null) return null;

        int totalVL = data.noFallVL + data.flyVL + data.speedVL + data.omniSprintVL
                + data.timerVL + data.auraVL + data.reachVL + data.autoClickerVL
                + data.blinkVL + data.stepVL + data.highJumpVL + data.jesusVL
                + data.scaffoldVL + data.invalidVL;

        if (totalVL == 0) return null;

        String highest = getHighestVLName(data);
        if (highest != null && !highest.isEmpty()) {
            return "Flag: " + totalVL + " VL (" + highest + ")";
        }
        return "Flag: " + totalVL + " VL";
    }

    private String getHighestVLName(AntiCheatPlayerData data) {
        int max = 0;
        String name = "";
        int v;

        if ((v = data.flyVL) > max) { max = v; name = "Fly"; }
        if ((v = data.speedVL) > max) { max = v; name = "Speed"; }
        if ((v = data.omniSprintVL) > max) { max = v; name = "OmniSprint"; }
        if ((v = data.noFallVL) > max) { max = v; name = "NoFall"; }
        if ((v = data.timerVL) > max) { max = v; name = "Timer"; }
        if ((v = data.reachVL) > max) { max = v; name = "Reach"; }
        if ((v = data.auraVL) > max) { max = v; name = "KillAura"; }
        if ((v = data.autoClickerVL) > max) { max = v; name = "AutoClicker"; }
        if ((v = data.blinkVL) > max) { max = v; name = "Blink"; }
        if ((v = data.stepVL) > max) { max = v; name = "Step"; }
        if ((v = data.highJumpVL) > max) { max = v; name = "HighJump"; }
        if ((v = data.jesusVL) > max) { max = v; name = "Jesus"; }
        if ((v = data.scaffoldVL) > max) { max = v; name = "Scaffold"; }
        if ((v = data.invalidVL) > max) { max = v; name = "Invalid"; }

        return name;
    }
}