package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.PacketEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.IntSetting;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class Velocity extends Module {
    // 模式设置
    private final ModeSetting mode = new ModeSetting("Mode", "Velocity reduction mode", "Normal", List.of("Normal", "JumpReset"));
    
    // Normal模式设置
    private final IntSetting chance = new IntSetting("Chance", "Chance to reduce velocity", 100, 100, 0, 1);
    private final IntSetting horizontalKb = new IntSetting("Horizontal", "Horizontal knockback percentage", 0, 100, 0, 1,() -> mode.is("Normal"));
    private final IntSetting verticalKb = new IntSetting("Vertical", "Vertical knockback percentage", 0, 100, 0, 1,() -> mode.is("Normal"));
    
    // JumpReset模式设置

    private final IntSetting minDelay = new IntSetting("Min Delay", "Minimum delay before jump (ms)", 0, 500, 0, 10,() -> mode.is("JumpReset"));
    private final IntSetting maxDelay = new IntSetting("Max Delay", "Maximum delay before jump (ms)", 0, 500, 0, 10,() -> mode.is("JumpReset"));
    
    // 状态变量
    private boolean shouldJump = false;
    private long jumpTime = 0;
    private final Random random = new Random();
    
    public Velocity() {
        super("Velocity", "Reduces or cancels knockback", Category.COMBAT);
        this.settings.add(mode);
        this.settings.add(chance);
    }
    
    @Override
    public void onEnable() {
        shouldJump = false;
        jumpTime = 0;
    }
    
    @Override
    public void onDisable() {
        shouldJump = false;
        jumpTime = 0;
    }
    
    @Override
    public void onPacket(PacketEvent event) {
        if (mc.player == null || mc.world == null) return;
        
        // 只处理接收的数据包
        if (event.getOrigin() != PacketEvent.TransferOrigin.RECEIVE) return;
        
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            // 检查是否是针对玩家的KB
            if (packet.getEntityId() == mc.player.getId()) {
                String currentMode = mode.getValue();
                
                if (currentMode.equals("Normal")) {
                    // 检查几率
                    if (random.nextInt(100) >= chance.getValue()) {
                        return;
                    }
                    
                    if (horizontalKb.getValue() == 0 && verticalKb.getValue() == 0) {
                        // 完全取消KB
                    }
                } else if (currentMode.equals("JumpReset")) {
                    // 检查几率
                    if (random.nextInt(100) >= chance.getValue()) {
                        return;
                    }
                    
                    // 取消KB数据包
                    event.cancelled = true;
                    
                    // 设置延迟跳跃
                    if (!shouldJump) {
                        shouldJump = true;
                        
                        // 计算随机延迟
                        int delay = minDelay.getValue();
                        if (maxDelay.getValue() > minDelay.getValue()) {
                            delay += random.nextInt(maxDelay.getValue() - minDelay.getValue());
                        }
                        
                        jumpTime = System.currentTimeMillis() + delay;
                    }
                }
            }
        } else if (event.getPacket() instanceof ExplosionS2CPacket packet) {
            String currentMode = mode.getValue();
            
            if (currentMode.equals("Normal")) {
                // 检查几率
                if (random.nextInt(100) >= chance.getValue()) {
                    return;
                }
                
                // 处理爆炸KB
                if (horizontalKb.getValue() == 0 && verticalKb.getValue() == 0) {
                    // 完全取消KB
                    event.cancelled = true;
                } else {
                    // 修改KB值
                    // 获取爆炸击退
                    Optional<Vec3d> knockbackOpt = packet.playerKnockback();
                    if (knockbackOpt.isPresent()) {
                        Vec3d knockback = knockbackOpt.get();
                        
                        // 取消原始数据包
                        event.cancelled = true;
                        
                        double hMultiplier = horizontalKb.getValue() / 100.0;
                        double vMultiplier = verticalKb.getValue() / 100.0;
                        
                        // 应用修改后的击退
                        mc.player.setVelocity(
                            mc.player.getVelocity().x + knockback.x * hMultiplier,
                            mc.player.getVelocity().y + knockback.y * vMultiplier,
                            mc.player.getVelocity().z + knockback.z * hMultiplier
                        );
                    }
                }
            } else if (currentMode.equals("JumpReset")) {
                // 检查几率
                if (random.nextInt(100) >= chance.getValue()) {
                    return;
                }
                
                // 取消KB数据包
                event.cancelled = true;
                
                // 设置延迟跳跃
                if (!shouldJump) {
                    shouldJump = true;
                    
                    // 计算随机延迟
                    int delay = minDelay.getValue();
                    if (maxDelay.getValue() > minDelay.getValue()) {
                        delay += random.nextInt(maxDelay.getValue() - minDelay.getValue());
                    }
                    
                    jumpTime = System.currentTimeMillis() + delay;
                }
            }
        }
    }
    
    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        
        // 处理JumpReset模式的跳跃
        if (shouldJump && System.currentTimeMillis() >= jumpTime) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
                shouldJump = false;
            }
        }
    }

    @Override
    public String getDisplayValue() {
        return mode.getValue();
    }
}