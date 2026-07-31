package cn.advicenext.features.module.impl.movement;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;

public class NoSlowDown extends Module {

    public static NoSlowDown INSTANCE;
    public static boolean slowDownCancelled = false;

    private final BooleanSetting sprint = new BooleanSetting("Sprint", "Keep sprinting while using items", true);
    private final BooleanSetting food = new BooleanSetting("Food", "Cancel slowdown while eating", true);
    private final BooleanSetting potion = new BooleanSetting("Potion", "Cancel slowdown while drinking potions", true);
    private final BooleanSetting sword = new BooleanSetting("Sword", "Cancel slowdown while blocking with sword", true);
    private final BooleanSetting bow = new BooleanSetting("Bow", "Cancel slowdown while drawing bow", true);
    private final BooleanSetting shield = new BooleanSetting("Shield", "Cancel slowdown while blocking with shield", true);

    public NoSlowDown() {
        super("NoSlowDown", "Prevents slowdown when using items", Category.MOVEMENT);
        INSTANCE = this;
        this.settings.add(sprint);
        this.settings.add(food);
        this.settings.add(potion);
        this.settings.add(sword);
        this.settings.add(bow);
        this.settings.add(shield);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (!mc.player.isUsingItem()) {
            slowDownCancelled = false;
            return;
        }

        ItemStack activeStack = mc.player.getActiveItem();
        Item item = activeStack.getItem();
        boolean shouldCancel = false;

        if (food.getValue() && activeStack.get(DataComponentTypes.FOOD) != null) {
            shouldCancel = true;
        }
        if (item instanceof PotionItem && potion.getValue()) {
            shouldCancel = true;
        }
        if (activeStack.isIn(ItemTags.SWORDS) && sword.getValue()) {
            shouldCancel = true;
        }
        if (item instanceof BowItem && bow.getValue()) {
            shouldCancel = true;
        }
        if (item instanceof ShieldItem && shield.getValue()) {
            shouldCancel = true;
        }

        slowDownCancelled = shouldCancel;

        if (sprint.getValue() && shouldCancel) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    public void onDisable() {
        slowDownCancelled = false;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.enabled;
    }
}