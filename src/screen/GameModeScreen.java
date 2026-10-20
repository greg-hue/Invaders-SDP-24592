package screen;

import java.awt.event.KeyEvent;

import audio.AudioManager;
import engine.Cooldown;
import engine.Core;

public class GameModeScreen extends Screen { // Implementation of the game mode selection screen.

    private static final int SELECTION_TIME = 200;
    private static final int NUM_CHOICES = 3; // Number of game mode choices (0: Infinite, 1: Campaign, 2: Back)

    private int selected;
    private Cooldown selectionCooldown;
    private boolean wasMousePressed;

    public GameModeScreen(final int width, final int height, final int fps) {
        super(width, height, fps);
        this.selected = 0; // Se positionne par défaut sur le premier mode
        this.returnCode = 1; // Par défaut : retour au menu principal
        this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
        this.selectionCooldown.reset();
    }

    public final int run() {
        super.run();
        return this.returnCode;
    }

    protected final void update() {
        super.update();

        if (this.inputDelay.checkFinished()) {
            int mouseOption = getMouseOption();
            if (mouseOption != -1 && mouseOption != this.selected) {
                this.selected = mouseOption;
                AudioManager.playSFX("audio/sfx/menu_move.wav");
            }

            boolean mousePressed = inputManager.isMousePressed();
            boolean newMouseClick = mousePressed && !wasMousePressed;
            wasMousePressed = mousePressed;
            if (newMouseClick && mouseOption != -1) {
                chooseOption();
            }
        } else {
            wasMousePressed = inputManager.isMousePressed();
        }

        if (this.selectionCooldown.checkFinished()
                && this.inputDelay.checkFinished()) {
            if (inputManager.isKeyDown(KeyEvent.VK_UP)
                    || inputManager.isKeyDown(KeyEvent.VK_W)) {
                this.selected = this.selected == 0 ? NUM_CHOICES - 1
                        : this.selected - 1;
                this.selectionCooldown.reset();
                AudioManager.playSFX("audio/sfx/menu_move.wav");
            } else if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
                    || inputManager.isKeyDown(KeyEvent.VK_S)) {
                this.selected = (this.selected + 1) % NUM_CHOICES;
                this.selectionCooldown.reset();
                AudioManager.playSFX("audio/sfx/menu_move.wav");
            } else if (inputManager.isKeyDown(KeyEvent.VK_SPACE)
                    || inputManager.isKeyDown(KeyEvent.VK_ENTER)) {
                chooseOption();
            } else if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
                goBack();
            }
        }
        draw();
    }

    /** Returns the menu row under the mouse, or -1 when outside the rows. */
    private int getMouseOption() {
        int mouseX = inputManager.getMouseX();
        int mouseY = inputManager.getMouseY();
        int centerX = this.getWidth() / 2;
        int firstY = this.getHeight() / 3 + 60;
        for (int i = 0; i < NUM_CHOICES; i++) {
            int rowY = firstY + i * 40;
            if (mouseX >= centerX - 180 && mouseX <= centerX + 180
                    && mouseY >= rowY - 22 && mouseY <= rowY + 12) {
                return i;
            }
        }
        return -1;
    }

    private void chooseOption() {
        if (this.selected == 2) {
            goBack();
        } else {
            this.returnCode = this.selected == 0 ? 8 : 2;
            AudioManager.playSFX("audio/sfx/menu_select.wav");
            this.isRunning = false;
        }
    }

    private void goBack() {
        this.returnCode = 1;
        AudioManager.playSFX("audio/sfx/menu_back.wav");
        this.isRunning = false;
    }

    public final int getSelectedGameMode() {
        return this.selected;
    }

    private void draw() {
        drawManager.initDrawing(this);
        drawManager.drawGameModeMenu(this, this.selected);
        drawManager.completeDrawing(this);
    }
}