package View;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.List;
import java.util.Random;

/**
 * Utility class responsible for playing background music
 * and sound effects throughout the game.
 */
public class SoundManager {

    private static MediaPlayer backgroundPlayer;
    private static MediaPlayer effectPlayer;

    private static final Random random = new Random();

    private static int lastRandomIndex = -1;
    private static long lastCooldownTime = 0;
    private static long lastRandomEffectTime = 0;

    /**
     * Plays a short sound effect.
     *
     * @param fileName sound file name
     */
    public static void playEffect(String fileName) {
        AudioClip clip = new AudioClip(SoundManager.class.getResource("/Sounds/" + fileName).toExternalForm());
        clip.play();
    }

    /**
     * Starts looping background music.
     * Stops any previously playing background track.
     *
     * @param fileName background music file
     */
    public static void playBackground(String fileName) {
        if (backgroundPlayer != null) {
            backgroundPlayer.stop();
            backgroundPlayer.dispose();
        }

        Media media = new Media(SoundManager.class.getResource("/Sounds/" + fileName).toExternalForm());

        backgroundPlayer = new MediaPlayer(media);
        backgroundPlayer.setVolume(0.15);

        backgroundPlayer.setOnEndOfMedia(() -> {
            System.out.println("BACKGROUND FINISHED");
        });

        backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        backgroundPlayer.play();
    }

    /**
     * Stops the currently playing background music.
     */
    public static void stopBackground() {
        if (backgroundPlayer != null) {
            backgroundPlayer.stop();
        }
    }

    /**
     * Plays a random sound effect from the given list.
     * Prevents playing the same sound twice in a row
     * and limits how frequently sounds can be played.
     *
     * @param soundFiles available sound effects
     */
    public static void playRandomEffect(List<String> soundFiles) {
        long now = System.currentTimeMillis();

        if (now - lastRandomEffectTime < 300) {
            return;
        }

        lastRandomEffectTime = now;

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

    /**
     * Plays a random sound effect and executes
     * the given action after the sound finishes.
     *
     * @param soundFiles available sound effects
     * @param onFinished action to execute after wards
     */
    public static void playRandomEffectAndThen(List<String> soundFiles, Runnable onFinished) {
        if (soundFiles == null || soundFiles.isEmpty()) {
            return;
        }

        int index;

        do {
            index = random.nextInt(soundFiles.size());
        } while (soundFiles.size() > 1 && index == lastRandomIndex);

        lastRandomIndex = index;

        Media media = new Media(SoundManager.class.getResource("/Sounds/" + soundFiles.get(index)).toExternalForm());

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

    /**
     * Plays a random sound effect only if the
     * specified cooldown period has elapsed.
     *
     * @param soundFiles available sound effects
     * @param cooldownMillis minimum time between plays
     */
    public static void playRandomEffectWithCooldown(List<String> soundFiles, long cooldownMillis) {

        long now = System.currentTimeMillis();

        if (now - lastCooldownTime < cooldownMillis) {
            return;
        }

        lastCooldownTime = now;

        playRandomEffect(soundFiles);
    }
}