package cn.advicenext.features.module.impl.player;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.Listener;
import cn.advicenext.event.impl.AttackEvent;
import cn.advicenext.event.impl.Render3DEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.client.render.Render3DEngine;
import cn.advicenext.utility.minecraft.network.LagUtils;
import cn.advicenext.utility.minecraft.network.lag.LagDirection;
import cn.advicenext.utility.minecraft.network.lag.LagRequest;
import cn.advicenext.utility.minecraft.network.lag.ModuleTimeout;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Set;

public class Blink extends Module {

    private static final String[] MODE_LABELS = {"Inbound", "Outbound", "Both"};

    private final ModeSetting mode = new ModeSetting("Mode", "Packet delay direction", "Outbound",
        List.of("Inbound", "Outbound", "Both"));
    private final BooleanSetting maxDuration = new BooleanSetting("MaxDuration", "Auto-disable after time", false);
    private final IntSetting disableAfterMs = new IntSetting("DisableAfter", "Auto-disable after (ms)", 500, 50, 20000, 50,
        () -> maxDuration.getValue());
    private final BooleanSetting disableOnAttack = new BooleanSetting("DisableOnAttack", "Disable on attack", false);
    private final BooleanSetting showInitialPos = new BooleanSetting("ShowInitialPos", "Show initial position", true);

    private Vec3d initialPos;
    private int blinkTicks;
    private long enableTime;

    public Blink() {
        super("Blink", "Stores packets and sends them later", Category.PLAYER);
        this.settings.add(mode);
        this.settings.add(maxDuration);
        this.settings.add(disableAfterMs);
        this.settings.add(disableOnAttack);
        this.settings.add(showInitialPos);
    }

    @Override
    public void onEnable() {
        EventBus.register(this);
        initialPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        blinkTicks = 0;
        enableTime = System.currentTimeMillis();
        LagUtils.requestLag(new LagRequest(lagDirectionsForMode(), new ModuleTimeout(this)));
    }

    @Override
    public void onDisable() {
        EventBus.unregister(this);
        initialPos = null;
    }

    @Override
    public void onTick(TickEvent event) {
        blinkTicks++;
        if (!maxDuration.getValue()) return;
        long elapsed = System.currentTimeMillis() - enableTime;
        if (elapsed >= disableAfterMs.getValue()) {
            this.disable();
        }
    }

    @Listener
    public void onAttack(AttackEvent event) {
        if (!this.enabled || !disableOnAttack.getValue()) return;
        this.disable();
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!showInitialPos.getValue() || initialPos == null) return;
        if (mc.player == null || mc.world == null) return;

        Vec3d trackedPos = initialPos;
        double x = trackedPos.x - mc.player.getX();
        double y = trackedPos.y - mc.player.getY();
        double z = trackedPos.z - mc.player.getZ();

        VertexConsumer vertexConsumer = event.getVertexConsumers().getBuffer(RenderLayers.lines());
        int lineColor = 0xDC00FF00;

        Render3DEngine.drawBox3D(event.getMatrices(), vertexConsumer, event.getCameraRenderState(),
            mc.player.getBoundingBox().offset(x, y, z), lineColor, 1.5F);
    }

    private Set<LagDirection> lagDirectionsForMode() {
        switch (mode.getValue()) {
            case "Inbound":
                return LagDirection.ONLY_INBOUND;
            case "Both":
                return LagDirection.BIDIRECTIONAL;
            case "Outbound":
            default:
                return LagDirection.ONLY_OUTBOUND;
        }
    }

    public boolean delaysInboundPackets() {
        String m = mode.getValue();
        return m.equals("Inbound") || m.equals("Both");
    }

    @Override
    public String getDisplayValue() {
        return String.valueOf(blinkTicks);
    }
}