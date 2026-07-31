package cn.advicenext.gui.hud.widgets;

import cn.advicenext.features.module.impl.world.Scaffold;
import cn.advicenext.gui.hud.widget.Widget;
import cn.advicenext.utility.client.render.KawaseBlur;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockCounterWidget extends Widget {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final int MAX_ENTRIES = 8;
    private static final float ENTRY_HEIGHT = 20f;
    private static final float PADDING = 8f;
    private static final float RADIUS = 8f;
    private static final float ITEM_ICON_SIZE = 16f;
    private static final float BLUR_RADIUS = 8f;

    private final List<BlockEntry> entries = new ArrayList<>();

    public BlockCounterWidget() {
        super("blockcounter", 10, 50, 140, 100);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible || mc.player == null || mc.world == null) return;
        if (!Scaffold.isActive()) return;

        updateEntries();

        if (entries.isEmpty()) return;

        int visibleCount = Math.min(entries.size(), MAX_ENTRIES);
        float totalHeight = visibleCount * ENTRY_HEIGHT + PADDING * 2 + ENTRY_HEIGHT + 4;
        width = 140;
        height = totalHeight;

        KawaseBlur.renderBlurRegion(BLUR_RADIUS, x, y, width, height);

        SkijaUIRenderer.drawRoundedRect(x, y, width, height, RADIUS, 0x40FFFFFF);
        SkijaUIRenderer.drawRoundedRect(x, y, width, totalHeight, RADIUS, 0x50FFFFFF);

        for (int i = 0; i < visibleCount; i++) {
            BlockEntry entry = entries.get(i);
            float entryY = y + PADDING + i * ENTRY_HEIGHT;

            ItemStack stack = entry.getItemStack();
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x + PADDING, entryY + (ENTRY_HEIGHT - ITEM_ICON_SIZE) / 2);
            context.drawItem(stack, 0, 0);
            context.getMatrices().popMatrix();

            String name = stack.getName().getString();
            float nameX = x + PADDING + ITEM_ICON_SIZE + 6;
            float nameY = entryY + (ENTRY_HEIGHT - 16) / 2 + 2;
            Fonts.robotoMedium.get(12).drawString(name, nameX, nameY, 0xFFFFFFCC);

            String countText = "x" + entry.getCount();
            float countWidth = Fonts.robotoMedium.get(12).getStringWidth(countText);
            float countX = x + width - PADDING - countWidth;
            Fonts.robotoMedium.get(12).drawString(countText, countX, nameY, 0xFFFFFFCC);
        }

        int totalBlocks = entries.stream().mapToInt(BlockEntry::getCount).sum();
        float totalY = y + PADDING + visibleCount * ENTRY_HEIGHT + 4;
        String totalText = "Total: " + totalBlocks + " blocks";
        float totalTextWidth = Fonts.robotoMedium.get(12).getStringWidth(totalText);
        Fonts.robotoMedium.get(12).drawString(totalText, x + (width - totalTextWidth) / 2, totalY, 0xFFCCCCCC);
    }

    private void updateEntries() {
        entries.clear();
        if (mc.player == null) return;

        Map<String, BlockEntry> blockMap = new HashMap<>();

        int invSize = mc.player.getInventory().size();
        for (int i = 0; i < invSize; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;

            String key = stack.getItem().getTranslationKey();
            BlockEntry existing = blockMap.get(key);
            if (existing != null) {
                existing.addCount(stack.getCount());
            } else {
                blockMap.put(key, new BlockEntry(stack.copy(), stack.getCount()));
            }
        }

        entries.addAll(blockMap.values());
        entries.sort(Comparator.comparingInt(BlockEntry::getCount).reversed());
    }

    private static class BlockEntry {
        private final ItemStack stack;
        private int count;

        BlockEntry(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }

        ItemStack getItemStack() {
            return stack;
        }

        int getCount() {
            return count;
        }

        void addCount(int amount) {
            this.count += amount;
        }
    }
}