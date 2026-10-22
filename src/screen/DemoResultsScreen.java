package screen;

import engine.GameState;
import audio.AudioManager;
import java.util.List;
import records.Achievement;

/** Uses the real results renderer without writing the player's high-score file. */
public final class DemoResultsScreen extends Screen {
    private final GameState state;
    private final List<Achievement> unlocks;

    public DemoResultsScreen(int width, int height, int fps, GameState state,
            List<Achievement> unlocks) {
        super(width, height, fps);
        this.state = state;
        this.unlocks = unlocks;
        AudioManager.playBGM(state.getLivesRemaining() > 0
                ? "audio/bgm/music_end_victory.wav" : "audio/bgm/music_end_defeat.wav");
    }

    @Override
    protected void update() {
        drawManager.initDrawing(this);
        float accuracy = state.getBulletsShot() == 0 ? 0f
                : (float) state.getShipsDestroyed() / state.getBulletsShot();
        if (unlocks.isEmpty()) {
            drawManager.drawCenteredBigString(this, "Demo results", 70);
            drawManager.drawResults(this, state.getScore(), state.getLivesRemaining(),
                    state.getShipsDestroyed(), accuracy, false);
        } else {
            drawManager.drawAchievementResults(this, state.getScore(), state.getLivesRemaining(),
                    state.getShipsDestroyed(), accuracy, false, "AAA".toCharArray(),
                    0, unlocks, false, 0, false);
        }
        drawManager.completeDrawing(this);
    }
}
