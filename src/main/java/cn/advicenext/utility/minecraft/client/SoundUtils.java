package cn.advicenext.utility.minecraft.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SoundUtils {
    
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void playSound(SoundEvent sound) {
        playSound(sound, 1.0f, 1.0f);
    }
    
    public static void playSound(SoundEvent sound, float volume, float pitch) {
        if (mc.world == null) return;
        
        mc.getSoundManager().play(PositionedSoundInstance.master(sound, pitch, volume));
    }
    
    public static void playSoundAt(SoundEvent sound, Vec3d pos) {
        playSoundAt(sound, pos, 1.0f, 1.0f);
    }
    
    public static void playSoundAt(SoundEvent sound, Vec3d pos, float volume, float pitch) {
        if (mc.world == null) return;
        
        mc.getSoundManager().play(new PositionedSoundInstance(
            sound, SoundCategory.MASTER, volume, pitch,
            SoundInstance.createRandom(), pos.x, pos.y, pos.z
        ));
    }

    public static void playModuleSound(String style,boolean enable) {
        if(style.equals("jello")) {
            if(enable) {
                playCustomSound("jello/activate.wav");
            } else {
                playCustomSound("jello/deactivate.wav");
            }
        }

        if(style.equals("augustus")) {
            if(enable) {
                playCustomSound("augustus/enable.wav");
            } else {
                playCustomSound("augustus/disable.wav");
            }
        }
    }
    
    public static void playClickSound() {
        playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
    }
    
    public static void playSuccessSound() {
        playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
    }
    
    public static void playErrorSound() {
        playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.8f, 0.5f);
    }
    
    public static void playNotificationSound() {
        playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.6f, 1.5f);
    }
    
    public static void playHitSound() {
        playSound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 0.7f, 1.0f);
    }
    
    public static void playBreakSound() {
        playSound(SoundEvents.ENTITY_ITEM_BREAK.value(), 0.8f, 1.0f);
    }
    
    public static void playPopSound() {
        playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.5f, 1.8f);
    }
    
    public static void playWhooshSound() {
        playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.2f);
    }
    
    public static void stopAllSounds() {
        mc.getSoundManager().stopAll();
    }
    
    public static void stopSound(SoundEvent sound) {
        mc.getSoundManager().stopSounds(null, SoundCategory.MASTER);
    }
    
    public static boolean isSoundPlaying(SoundEvent sound) {
        return mc.getSoundManager().isPlaying(new PositionedSoundInstance(
            sound, SoundCategory.MASTER, 1.0f, 1.0f,
            SoundInstance.createRandom(), 0, 0, 0
        ));
    }
    
    public static void setSoundVolume(float volume) {
        mc.options.getSoundVolumeOption(SoundCategory.MASTER).setValue((double) volume);
    }
    
    public static float getSoundVolume() {
        return mc.options.getSoundVolumeOption(SoundCategory.MASTER).getValue().floatValue();
    }
    
    public static void playRandomPitchSound(SoundEvent sound, float volume) {
        float pitch = 0.8f + (float) Math.random() * 0.4f;
        playSound(sound, volume, pitch);
    }
    
    public static void playDelayedSound(SoundEvent sound, int delayTicks) {
        new Thread(() -> {
            try {
                Thread.sleep(delayTicks * 50);
                playSound(sound);
            } catch (InterruptedException ignored) {}
        }).start();
    }
    
    // 外部音频播放
    private static final Map<String, Clip> playingClips = new ConcurrentHashMap<>();
    private static final String AUDIO_PATH = "/assets/advicenext/audio/";
    
    private static String getAudioPath(String fileName) {
        return AUDIO_PATH + fileName;
    }
    
    public static void playCustomSound(String fileName) {
        playCustomSound(fileName, 1.0f);
    }
    
    public static void playCustomSound(String fileName, float volume) {
        new Thread(() -> {
            try {
                InputStream audioStream = SoundUtils.class.getResourceAsStream(getAudioPath(fileName));
                if (audioStream == null) {
                    System.err.println("Audio file not found in resources: " + fileName);
                    return;
                }
                
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioStream);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float gain = 20f * (float) Math.log10(Math.max(0.0001f, volume));
                    gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(gain, gainControl.getMaximum())));
                }
                
                playingClips.put(fileName, clip);
                
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        playingClips.remove(fileName);
                        clip.close();
                    }
                });
                
                clip.start();
                
            } catch (Exception e) {
                System.err.println("Error playing custom audio: " + e.getMessage());
            }
        }).start();
    }
    
    public static void loopCustomSound(String fileName, float volume) {
        new Thread(() -> {
            try {
                InputStream audioStream = SoundUtils.class.getResourceAsStream(getAudioPath(fileName));
                if (audioStream == null) return;
                
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioStream);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float gain = 20f * (float) Math.log10(Math.max(0.0001f, volume));
                    gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(gain, gainControl.getMaximum())));
                }
                
                playingClips.put(fileName, clip);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                
            } catch (Exception e) {
                System.err.println("Error looping custom audio: " + e.getMessage());
            }
        }).start();
    }
    
    public static void stopCustomSound(String fileName) {
        Clip clip = playingClips.get(fileName);
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
            playingClips.remove(fileName);
        }
    }
    
    public static void playAudioFile(String filePath) {
        playAudioFile(filePath, 1.0f);
    }
    
    public static void playAudioFile(String filePath, float volume) {
        new Thread(() -> {
            try {
                File audioFile = new File(filePath);
                if (!audioFile.exists()) {
                    System.err.println("Audio file not found: " + filePath);
                    return;
                }
                
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                
                // 设置音量
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float gain = 20f * (float) Math.log10(Math.max(0.0001f, volume));
                    gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(gain, gainControl.getMaximum())));
                }
                
                playingClips.put(filePath, clip);
                
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        playingClips.remove(filePath);
                        clip.close();
                    }
                });
                
                clip.start();
                
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                System.err.println("Error playing audio file: " + e.getMessage());
            }
        }).start();
    }
    
    public static void stopAudioFile(String filePath) {
        Clip clip = playingClips.get(filePath);
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
            playingClips.remove(filePath);
        }
    }
    
    public static void stopAllAudioFiles() {
        playingClips.values().forEach(clip -> {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.close();
        });
        playingClips.clear();
    }
    
    public static boolean isAudioFilePlaying(String filePath) {
        Clip clip = playingClips.get(filePath);
        return clip != null && clip.isRunning();
    }
    
    public static void loopAudioFile(String filePath, float volume) {
        new Thread(() -> {
            try {
                File audioFile = new File(filePath);
                if (!audioFile.exists()) return;
                
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float gain = 20f * (float) Math.log10(Math.max(0.0001f, volume));
                    gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(gain, gainControl.getMaximum())));
                }
                
                playingClips.put(filePath, clip);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                
            } catch (Exception e) {
                System.err.println("Error looping audio file: " + e.getMessage());
            }
        }).start();
    }
}