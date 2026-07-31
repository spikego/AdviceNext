package cn.advicenext.cloudmusic;

import cn.advicenext.cloudmusic.MusicModels.Song;
import cn.advicenext.cloudmusic.MusicModels.LyricResult;
import cn.advicenext.cloudmusic.MusicModels.LyricLine;
import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MusicPlayerEngine {
    private static MusicPlayerEngine instance;
    private static boolean mp3SpiChecked;

    static {
        try {
            javax.sound.sampled.AudioSystem.getAudioFileFormat(new java.io.File("nul"));
        } catch (Exception ignored) {}
        try {
            Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFileReader");
            mp3SpiChecked = true;
        } catch (ClassNotFoundException e) {
            System.err.println("[MusicPlayer] MP3 SPI not available: " + e.getMessage());
        }
    }
    private SourceDataLine line;
    private volatile boolean playing;
    private volatile boolean paused;
    private volatile long position;
    private AudioInputStream audioStream;
    private Song currentSong;
    private LyricResult currentLyric;
    private float volume = 0.8f;
    private Consumer<Long> progressCallback;
    private Consumer<LyricLine> lyricCallback;
    private Consumer<Boolean> stateCallback;
    private float[] spectrumData = new float[64];

    public static MusicPlayerEngine get() { if (instance == null) instance = new MusicPlayerEngine(); return instance; }

    public Song getCurrentSong() { return currentSong; }
    public LyricResult getCurrentLyric() { return currentLyric; }
    public boolean isPlaying() { return playing; }
    public boolean isPaused() { return paused; }
    public long getPosition() { return position; }
    public float getVolume() { return volume; }
    public float[] getSpectrumData() { return spectrumData; }

    public void setVolume(float v) { this.volume = Math.max(0, Math.min(1, v)); if (line != null) setLineVolume(); }
    public void adjustVolume(float delta) { setVolume(volume + delta); }
    public void setProgressCallback(Consumer<Long> cb) { this.progressCallback = cb; }
    public void setLyricCallback(Consumer<LyricLine> cb) { this.lyricCallback = cb; }
    public void setStateCallback(Consumer<Boolean> cb) { this.stateCallback = cb; }

    private void setLineVolume() {
        try {
            if (line != null && line.isOpen()) {
                FloatControl fc = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                fc.setValue(fc.getMinimum() * (1 - volume));
            }
        } catch (Exception ignored) {}
    }

    public void play(Song song) {
        stop();
        currentSong = song;
        if (song == null || song.url == null || song.url.isEmpty()) return;
        CompletableFuture.runAsync(() -> {
            try {
                URL u = new URL(song.url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36");
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("Referer", "https://music.163.com/");
                String cookie = MusicApi.get().getCookie();
                if (cookie != null && !cookie.isEmpty()) {
                    conn.setRequestProperty("Cookie", "appver=2.7.1.198277; os=pc; " + cookie);
                }
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                AudioInputStream in;
                try {
                    in = AudioSystem.getAudioInputStream(bis);
                } catch (javax.sound.sampled.UnsupportedAudioFileException uafe) {
                    if (mp3SpiChecked) {
                        try {
                            javazoom.spi.mpeg.sampled.file.MpegAudioFileReader reader =
                                new javazoom.spi.mpeg.sampled.file.MpegAudioFileReader();
                            in = reader.getAudioInputStream(bis);
                        } catch (Exception e2) {
                            throw uafe;
                        }
                    } else {
                        throw uafe;
                    }
                }
                AudioFormat baseFormat = in.getFormat();
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
                    baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                audioStream = AudioSystem.getAudioInputStream(decodedFormat, in);
                AudioFormat pcmFormat = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcmFormat);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(pcmFormat);
                setLineVolume();
                line.start();
                playing = true;
                paused = false;
                if (stateCallback != null) stateCallback.accept(true);
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytes = 0;
                float sampleRate = pcmFormat.getSampleRate();
                int frameSize = pcmFormat.getFrameSize();
                while (playing && (bytesRead = audioStream.read(buffer, 0, buffer.length)) != -1) {
                    while (paused && playing) {
                        try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                    }
                    if (!playing) break;
                    line.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                    position = (long) ((totalBytes / (double) frameSize / sampleRate) * 1000);
                    updateSpectrum(buffer, bytesRead);
                    if (progressCallback != null) progressCallback.accept(position);
                    if (lyricCallback != null && currentLyric != null) {
                        for (int i = currentLyric.lines.size() - 1; i >= 0; i--) {
                            if (currentLyric.lines.get(i).time <= position) {
                                lyricCallback.accept(currentLyric.lines.get(i));
                                break;
                            }
                        }
                    }
                }
                if (playing) {
                    position = currentSong.duration;
                    if (progressCallback != null) progressCallback.accept(position);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cleanup();
                if (stateCallback != null) stateCallback.accept(false);
            }
        });
    }

    private void updateSpectrum(byte[] buffer, int len) {
        int samples = len / 2;
        for (int i = 0; i < 64; i++) {
            float sum = 0;
            int start = i * samples / 64;
            int end = (i + 1) * samples / 64;
            for (int j = start; j < end && j * 2 + 1 < len; j++) {
                short sample = (short) ((buffer[j * 2 + 1] << 8) | (buffer[j * 2] & 0xFF));
                sum += Math.abs(sample) / 32768f;
            }
            int count = end - start;
            float val = count > 0 ? sum / count : 0;
            spectrumData[i] = spectrumData[i] * 0.7f + val * 0.3f;
        }
    }

    public void pause() { paused = true; if (stateCallback != null) stateCallback.accept(false); }
    public void resume() { paused = false; if (stateCallback != null) stateCallback.accept(true); }
    public void togglePause() { if (paused) resume(); else pause(); }

    public void stop() {
        playing = false;
        paused = false;
        cleanup();
        position = 0;
        currentSong = null;
        currentLyric = null;
        if (stateCallback != null) stateCallback.accept(false);
    }

    public void loadLyric(Song song) {
        if (song == null) return;
        MusicApi.get().getLyric(song.id, lr -> { currentLyric = lr; });
    }

    private void cleanup() {
        playing = false;
        try { if (line != null) { line.stop(); line.close(); } } catch (Exception ignored) {}
        try { if (audioStream != null) audioStream.close(); } catch (Exception ignored) {}
        line = null;
        audioStream = null;
    }
}