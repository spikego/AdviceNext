package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.IntSetting;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.List;

public class AutoGap extends Module {

    public static AutoGap INSTANCE;

    private final ModeSetting item = new ModeSetting("Item", "Item to use", "Gapple",
            List.of("Gapple", "Potion"));
    private final BooleanSetting swing = new BooleanSetting("Swing", "Swing hand", false);
    private final BooleanSetting inventory = new BooleanSetting("Inventory", "Work in inventory", false);
    private final BooleanSetting onlyKC = new BooleanSetting("OnlyKC", "Only when KillAura active", false);
    private final BooleanSetting closeAfter = new BooleanSetting("CloseAfter", "Close inventory after eat", true,
            () -> inventory.getValue());
    private final BooleanSetting waitForStop = new BooleanSetting("WaitStop", "Wait for use to stop", false);
    private final IntSetting minHealth = new IntSetting("MinHealth", "Minimum health to eat", 10, 0, 20, 1);
    private final IntSetting maxHealth = new IntSetting("MaxHealth", "Maximum health to eat", 18, 0, 20, 1);
    private final IntSetting minFood = new IntSetting("MinFood", "Minimum food level", 10, 0, 20, 1);
    private final IntSetting delay = new IntSetting("Delay", "Eat delay (ticks)", 6, 0, 20, 1);

    private int eatDelay = 0;
    private boolean eating = false;
    private int prevSlot = -1;

    public AutoGap() {
        super("AutoGap", "Automatically eats golden apples/potions", Category.COMBAT);
        INSTANCE = this;
        this.settings.add(item);
        this.settings.add(swing);
        this.settings.add(inventory);
        this.settings.add(onlyKC);
        this.settings.add(closeAfter);
        this.settings.add(waitForStop);
        this.settings.add(minHealth);
        this.settings.add(maxHealth);
        this.settings.add(minFood);
        this.settings.add(delay);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (eatDelay > 0) {
            eatDelay--;
            return;
        }

        if (onlyKC.getValue()) {
            if (KillAura.INSTANCE == null || !KillAura.INSTANCE.getEnabled()) {
                stopEating();
                return;
            }
        }

        if (mc.player.getHealth() > maxHealth.getValue()) {
            stopEating();
            return;
        }

        if (eating && waitForStop.getValue() && mc.player.isUsingItem()) {
            return;
        }

        if (mc.player.getHealth() >= minHealth.getValue() && mc.player.getHealth() <= maxHealth.getValue()
                && mc.player.getHungerManager().getFoodLevel() >= minFood.getValue()) {

            if (eating && mc.player.getOffHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                return;
            }
            if (eating && mc.player.getOffHandStack().getItem() == Items.GOLDEN_APPLE
                    && item.is("Gapple")) {
                return;
            }

            if (item.is("Gapple")) {
                eatGap();
            } else if (item.is("Potion")) {
                eatPotion();
            }
        } else {
            if (eating) {
                stopEating();
            }
        }
    }

    private void eatGap() {
        if (mc.player == null) return;

        int gapSlot = findGapInHotbar();
        if (gapSlot != -1) {
            if (mc.player.getOffHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE
                    && mc.player.getOffHandStack().getItem() != Items.GOLDEN_APPLE) {
                prevSlot = mc.player.getInventory().getSelectedSlot();
                mc.player.getInventory().setSelectedSlot(gapSlot);
            }
            mc.options.useKey.setPressed(true);
            if (swing.getValue()) mc.player.swingHand(Hand.MAIN_HAND);
            eating = true;
            eatDelay = delay.getValue();
        } else {
            int invGapSlot = findGapInInventory();
            if (invGapSlot != -1 && inventory.getValue()) {
                if (mc.currentScreen instanceof InventoryScreen) {
                    if (invGapSlot > 8) {
                        swapWithHotbar(invGapSlot);
                    }
                } else {
                    mc.player.getInventory().setSelectedSlot(invGapSlot < 9 ? invGapSlot : 0);
                    mc.options.useKey.setPressed(true);
                    eating = true;
                    eatDelay = delay.getValue();
                }
            }
        }
    }

    private void eatPotion() {
        if (mc.player == null) return;

        int potionSlot = findPotionInHotbar();
        if (potionSlot != -1) {
            if (mc.player.getOffHandStack().getItem() != Items.POTION
                    && mc.player.getOffHandStack().getItem() != Items.SPLASH_POTION) {
                prevSlot = mc.player.getInventory().getSelectedSlot();
                mc.player.getInventory().setSelectedSlot(potionSlot);
            }
            mc.options.useKey.setPressed(true);
            if (swing.getValue()) mc.player.swingHand(Hand.MAIN_HAND);
            eating = true;
            eatDelay = delay.getValue();
        }
    }

    private void stopEating() {
        mc.options.useKey.setPressed(false);
        if (eating && prevSlot != -1) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
            prevSlot = -1;
        }
        eating = false;
    }

    private int findGapInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE
                    || (stack.getItem() == Items.GOLDEN_APPLE && item.is("Gapple"))) {
                return i;
            }
        }
        return -1;
    }

    private int findGapInInventory() {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE
                    || (stack.getItem() == Items.GOLDEN_APPLE && item.is("Gapple"))) {
                return i;
            }
        }
        return -1;
    }

    private int findPotionInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.POTION || stack.getItem() == Items.SPLASH_POTION) {
                return i;
            }
        }
        return -1;
    }

    private void swapWithHotbar(int slot) {
        if (mc.player == null) return;
        int hotbarSlot = mc.player.getInventory().getSelectedSlot();
        ItemStack hotbarStack = mc.player.getInventory().getStack(hotbarSlot);
        ItemStack invStack = mc.player.getInventory().getStack(slot);
        mc.player.getInventory().setStack(hotbarSlot, invStack);
        mc.player.getInventory().setStack(slot, hotbarStack);
    }
}