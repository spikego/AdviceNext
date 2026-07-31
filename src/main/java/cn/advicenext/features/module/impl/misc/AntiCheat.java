package cn.advicenext.features.module.impl.misc;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.notification.NotificationManager;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.client.anticheat.AntiCheatFlag;
import cn.advicenext.utility.client.anticheat.AntiCheatManager;
import cn.advicenext.utility.client.anticheat.AntiCheatType;
import net.minecraft.text.Text;

import java.util.List;

public class AntiCheat extends Module {

    private final ModeSetting alertMode = new ModeSetting("AlertMode", "How to display alerts",
            "Notification", List.of("Notification", "Chat", "Both"));
    private final IntSetting minVL = new IntSetting("MinVL", "Minimum VL to alert", 5, 1, 100, 1);
    private final BooleanSetting autoBan = new BooleanSetting("AutoBan", "Auto-ban detected cheaters", false);
    private final DoubleSetting strictness = new DoubleSetting("Strictness", "Detection strictness (1.0=normal)", 1.0, 0.1, 3.0, 0.1);

    private final BooleanSetting checkFlyA = new BooleanSetting("FlyA", "Detect Fly (A)", true);
    private final BooleanSetting checkFlyB = new BooleanSetting("FlyB", "Detect Fly (B)", true);
    private final BooleanSetting checkSpeedA = new BooleanSetting("SpeedA", "Detect Speed (A)", true);
    private final BooleanSetting checkSpeedB = new BooleanSetting("SpeedB", "Detect Speed (B)", true);
    private final BooleanSetting checkOmniSprint = new BooleanSetting("OmniSprint", "Detect Omni Sprint", true);
    private final BooleanSetting checkNoFall = new BooleanSetting("NoFall", "Detect NoFall", true);
    private final BooleanSetting checkInvalid = new BooleanSetting("Invalid", "Detect Invalid", true);
    private final BooleanSetting checkBlink = new BooleanSetting("Blink", "Detect Blink", true);
    private final BooleanSetting checkStep = new BooleanSetting("Step", "Detect Step", true);
    private final BooleanSetting checkHighJump = new BooleanSetting("HighJump", "Detect HighJump", true);
    private final BooleanSetting checkJesus = new BooleanSetting("Jesus", "Detect Jesus", true);
    private final BooleanSetting checkScaffold = new BooleanSetting("Scaffold", "Detect Scaffold", true);
    private final BooleanSetting checkReach = new BooleanSetting("Reach", "Detect Reach", true);
    private final BooleanSetting checkKillAura = new BooleanSetting("KillAura", "Detect KillAura", true);
    private final BooleanSetting checkAutoClicker = new BooleanSetting("AutoClicker", "Detect AutoClicker", true);

    public AntiCheat() {
        super("AntiCheat", "Detects cheaters on the server", Category.MISC);
        this.settings.add(alertMode);
        this.settings.add(minVL);
        this.settings.add(autoBan);
        this.settings.add(strictness);
        this.settings.add(checkFlyA);
        this.settings.add(checkFlyB);
        this.settings.add(checkSpeedA);
        this.settings.add(checkSpeedB);
        this.settings.add(checkOmniSprint);
        this.settings.add(checkNoFall);
        this.settings.add(checkInvalid);
        this.settings.add(checkBlink);
        this.settings.add(checkStep);
        this.settings.add(checkHighJump);
        this.settings.add(checkJesus);
        this.settings.add(checkScaffold);
        this.settings.add(checkReach);
        this.settings.add(checkKillAura);
        this.settings.add(checkAutoClicker);
    }

    @Override
    public void onEnable() {
        AntiCheatManager.getInstance().setEnabled(true);
        AntiCheatManager.getInstance().setAlertCallback(this::onFlag);
        syncCheckToggles();
    }

    @Override
    public void onDisable() {
        AntiCheatManager.getInstance().setEnabled(false);
        AntiCheatManager.getInstance().setAlertCallback(null);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        syncCheckToggles();
        AntiCheatManager.getInstance().onTick();
    }

    private void syncCheckToggles() {
        AntiCheatManager.getInstance().setStrictness(strictness.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.FLY_A, checkFlyA.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.FLY_B, checkFlyB.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.SPEED_A, checkSpeedA.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.SPEED_B, checkSpeedB.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.OMNI_SPRINT, checkOmniSprint.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.NO_FALL, checkNoFall.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.INVALID, checkInvalid.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.BLINK, checkBlink.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.STEP, checkStep.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.HIGH_JUMP, checkHighJump.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.JESUS, checkJesus.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.SCAFFOLD, checkScaffold.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.REACH, checkReach.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.KILL_AURA, checkKillAura.getValue());
        AntiCheatManager.getInstance().setCheckEnabled(AntiCheatType.AUTO_CLICKER, checkAutoClicker.getValue());
    }

    private void onFlag(AntiCheatFlag flag) {
        if (flag.level < minVL.getValue()) return;

        String message = String.format("§c[AC] §f%s §7flagged §c%s §7(VL: §c%d§7)",
                flag.playerName, flag.type.getDisplayName(), flag.level);

        switch (alertMode.getValue()) {
            case "Chat" -> {
                if (mc.player != null) {
                    mc.player.sendMessage(Text.of(message), false);
                }
            }
            case "Both" -> {
                if (mc.player != null) {
                    mc.player.sendMessage(Text.of(message), false);
                }
                NotificationManager.getInstance().addNotification(
                        "AntiCheat",
                        flag.playerName + " flagged " + flag.type.getDisplayName(),
                        NotificationManager.NotificationType.WARNING,
                        5000
                );
            }
            default -> {
                NotificationManager.getInstance().addNotification(
                        "AntiCheat",
                        flag.playerName + " flagged " + flag.type.getDisplayName(),
                        NotificationManager.NotificationType.WARNING,
                        5000
                );
            }
        }
    }
}