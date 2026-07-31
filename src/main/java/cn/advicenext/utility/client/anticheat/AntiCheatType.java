package cn.advicenext.utility.client.anticheat;

public enum AntiCheatType {
    FLY_A("Fly (A)", "No ground, not falling, no levitation"),
    FLY_B("Fly (B)", "Slow falling / anti-gravity pattern"),
    SPEED_A("Speed (A)", "Ground speed exceeds vanilla limit"),
    SPEED_B("Speed (B)", "Acceleration / strafe anomaly"),
    OMNI_SPRINT("Omni Sprint", "Sprinting in non-forward direction"),
    NO_FALL("NoFall", "No fall damage from lethal height"),
    TIMER("Timer", "Actions faster than normal tick rate"),
    REACH("Reach", "Hit distance exceeds vanilla limit"),
    KILL_AURA("KillAura", "Rotation snap / multi-target / no-swing"),
    AUTO_CLICKER("AutoClicker", "Abnormal CPS consistency"),
    BLINK("Blink", "Lag spike / position desync pattern"),
    STEP("Step", "Instant Y-teleport without jumping"),
    HIGH_JUMP("HighJump", "Jump height exceeds vanilla limit"),
    JESUS("Jesus", "Walking on liquid surface"),
    SCAFFOLD("Scaffold", "Auto-place / silent swap pattern"),
    INVALID("Invalid", "Invalid movement / rotation data");

    private final String displayName;
    private final String description;

    AntiCheatType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}