package cn.advicenext.features.module.impl.misc;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.minecraft.client.TimerUtils;
import cn.advicenext.utility.minecraft.movement.MovementUtils;

import java.util.List;

public class Timer extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Timer mode", "Normal", List.of("Normal", "Balance"));

    // Normal mode settings
    private final DoubleSetting timerSpeed = new DoubleSetting("TimerSpeed", "Set Minecraft timer", 1.0, 5.0, 0.0, 0.1);

    // Balance mode settings
    private final DoubleSetting balanceSpeed = new DoubleSetting("Balance-Speed", "Timer speed when using balance", 2.0, 10.0, 1.0, 0.1, () -> mode.is("Balance"));
    private final DoubleSetting slowTimer = new DoubleSetting("Balance-SlowTimer", "Slow timer speed for charging", 0.0, 1.0, 0.0, 0.01, () -> mode.is("Balance"));
    private final IntSetting maxBalance = new IntSetting("Balance-MaxBalance", "Max balance in ms", 1000, 0, 3000, 10,() -> mode.is("Balance"));
    private final DoubleSetting costMultiplier = new DoubleSetting("Balance-CostMultiplier", "Cost multiplier for timer", 1.0, 5.0, 0.5, 0.05, () -> mode.is("Balance"));
    private final BooleanSetting autoSlow = new BooleanSetting("Balance-AutoSlow", "Auto slow when moving", false,() -> mode.is("Balance"));
    private final BooleanSetting autoDisable = new BooleanSetting("Balance-AutoDisable", "Auto disable when balance empty", true, () -> mode.is("Balance"));

    // Balance state
    private long balance = 0;
    private long startTime = -1;
    private BalanceState balanceState = BalanceState.NONE;

    public Timer() {
        super("Timer", "Allows you to change the speed of the game.", Category.MISC);
        this.settings.add(mode);
        this.settings.add(timerSpeed);
        this.settings.add(balanceSpeed);
        this.settings.add(slowTimer);
        this.settings.add(maxBalance);
        this.settings.add(costMultiplier);
        this.settings.add(autoSlow);
        this.settings.add(autoDisable);
        this.enabled = false;
    }

    @Override
    public void onEnable() {
        if (mode.getValue().equals("Normal")) {
            TimerUtils.setTimerSpeed(timerSpeed.getValue());
        } else {
            resetBalance();
        }
    }

    @Override
    public void onDisable() {
        TimerUtils.setTimerSpeed(1.0);
        resetBalance();
    }

    @Override
    public void onTick(TickEvent event) {
        if (mode.getValue().equals("Normal")) {
            if(!this.settings.contains(timerSpeed)) {
                this.settings.add(timerSpeed);
            }
            TimerUtils.setTimerSpeed(timerSpeed.getValue());
        } else {
            this.settings.remove(timerSpeed);
            updateBalanceTimer();
        }

        if(mode.is("Balance")) {
            if(!this.settings.contains(balanceSpeed)) {
                this.settings.add(balanceSpeed);
            }
            if(!this.settings.contains(slowTimer)) {
                this.settings.add(slowTimer);
            }
            if(!this.settings.contains(maxBalance)) {
                this.settings.add(maxBalance);
            }
            if(!this.settings.contains(costMultiplier)) {
                this.settings.add(costMultiplier);
            }
            if(!this.settings.contains(autoSlow)) {
                this.settings.add(autoSlow);
            }
            if(!this.settings.contains(autoDisable)) {
                this.settings.add(autoDisable);
            }
        } else {
            this.settings.remove(balanceSpeed);
            this.settings.remove(slowTimer);
            this.settings.remove(maxBalance);
            this.settings.remove(costMultiplier);
            this.settings.remove(autoSlow);
            this.settings.remove(autoDisable);
        }
    }

    private void updateBalanceTimer() {
        final long curTime = System.currentTimeMillis();

        switch (balanceState) {
            case NONE:
                startTime = curTime;
                if (autoSlow.getValue() && MovementUtils.isMoving()) break;
                TimerUtils.setTimerSpeed(slowTimer.getValue());
                balanceState = BalanceState.SLOW;
                break;
            case SLOW:
                if (autoSlow.getValue() && MovementUtils.isMoving()) {
                    if (balance > 0) {
                        balanceState = BalanceState.TIMER;
                    } else {
                        balanceState = BalanceState.NONE;
                    }
                    break;
                }
                balance += (long) ((curTime - startTime) * (1 - slowTimer.getValue()));
                if (balance >= maxBalance.getValue()) {
                    balance = maxBalance.getValue();
                    balanceState = BalanceState.TIMER;
                    startTime = curTime;
                } else {
                    startTime = curTime;
                    TimerUtils.setTimerSpeed(slowTimer.getValue());
                }
                break;
            case TIMER:
                balance -= (long) ((curTime - startTime) * balanceSpeed.getValue() * costMultiplier.getValue());
                if (balance <= 0) {
                    resetBalance();
                    if (autoDisable.getValue()) {
                        this.enabled = false;
                    }
                    break;
                }
                startTime = curTime;
                TimerUtils.setTimerSpeed(balanceSpeed.getValue());
                break;
        }
    }

    private void resetBalance() {
        TimerUtils.setTimerSpeed(1.0);
        balance = 0;
        balanceState = BalanceState.NONE;
    }

    @Override
    public String getDisplayValue() {
        if (mode.getValue().equals("Balance")) {
            return balanceState.name() + " | " + balance + "ms";
        }
        return timerSpeed.getValue().toString();
    }

    enum BalanceState {
        NONE,
        SLOW,
        TIMER
    }
}