package View;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.List;
import java.util.Random;

public class SoundManager {

    private static MediaPlayer backgroundPlayer;
    private static MediaPlayer effectPlayer;
    private static final Random random = new Random();
    private static int lastRandomIndex = -1;
    private static long lastCooldownTime = 0;

    public static void playEffect(String fileName) {

        AudioClip clip = new AudioClip(
                SoundManager.class
                        .getResource("/Sounds/" + fileName)
                        .toExternalForm());

        clip.play();
    }

    public static void playBackground(String fileName) {

        if (backgroundPlayer != null) {
            backgroundPlayer.stop();
            backgroundPlayer.dispose();
        }

        Media media = new Media(
                SoundManager.class
                        .getResource("/Sounds/" + fileName)
                        .toExternalForm());

        backgroundPlayer = new MediaPlayer(media);
        backgroundPlayer.setVolume(0.15);
        backgroundPlayer.setOnEndOfMedia(() -> {
            System.out.println("BACKGROUND FINISHED");
        });

        backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        backgroundPlayer.play();
    }

    public static void stopBackground() {

        if (backgroundPlayer != null) {
            backgroundPlayer.stop();
        }
    }

    public static void playRandomEffect(List<String> soundFiles) {

        if (soundFiles == null || soundFiles.isEmpty()) {
            return;
        }

        int index;

        do {
            index = random.nextInt(soundFiles.size());
        } while (soundFiles.size() > 1 && index == lastRandomIndex);

        lastRandomIndex = index;

        playEffect(soundFiles.get(index));
    }

    public static void playRandomEffectAndThen(List<String> soundFiles,
                                               Runnable onFinished) {

        if (soundFiles == null || soundFiles.isEmpty()) {
            return;
        }

        int index;

        do {
            index = random.nextInt(soundFiles.size());
        } while (soundFiles.size() > 1 && index == lastRandomIndex);

        lastRandomIndex = index;

        Media media = new Media(
                SoundManager.class
                        .getResource("/Sounds/" + soundFiles.get(index))
                        .toExternalForm());

        // אם כבר יש אפקט כזה רץ - עוצרים אותו
        if (effectPlayer != null) {
            effectPlayer.stop();
            effectPlayer.dispose();
        }

        effectPlayer = new MediaPlayer(media);

        effectPlayer.setOnEndOfMedia(() -> {

            effectPlayer.dispose();
            effectPlayer = null;

            if (onFinished != null) {
                onFinished.run();
            }
        });

        effectPlayer.play();
    }

    public static void playEffectAndThen(String fileName,
                                         Runnable onFinished) {

        Media media = new Media(
                SoundManager.class
                        .getResource("/Sounds/" + fileName)
                        .toExternalForm());

        if (effectPlayer != null) {
            effectPlayer.stop();
            effectPlayer.dispose();
        }

        effectPlayer = new MediaPlayer(media);

        effectPlayer.setOnEndOfMedia(() -> {

            effectPlayer.dispose();
            effectPlayer = null;

            if (onFinished != null) {
                onFinished.run();
            }
        });

        effectPlayer.play();
    }

    public static void playRandomEffectWithCooldown(List<String> soundFiles, long cooldownMillis){
        long now = System.currentTimeMillis();

        if (now - lastCooldownTime < cooldownMillis) {
            return;
        }

        lastCooldownTime = now;

        playRandomEffect(soundFiles);
    }

}