package View;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class SoundManager {

    private static MediaPlayer backgroundPlayer;

    public static void playBackgroundMusic() {
        if (backgroundPlayer != null)
            return;

        Media media = new Media(
                SoundManager.class.getResource("/Sounds/background.mp3").toExternalForm());

        backgroundPlayer = new MediaPlayer(media);
        backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        backgroundPlayer.setVolume(0.3);
        backgroundPlayer.play();
    }

    public static void stopBackgroundMusic() {
        if (backgroundPlayer != null)
            backgroundPlayer.stop();
    }

    public static void playSound(String fileName) {
        Media media = new Media(
                SoundManager.class.getResource("/Sounds/" + fileName).toExternalForm());

        MediaPlayer player = new MediaPlayer(media);
        player.play();
    }
}