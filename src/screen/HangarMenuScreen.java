
package screen;

import java.awt.event.KeyEvent;

import audio.AudioManager;
import engine.Core;
import engine.Cooldown;

/** Menu for choosing a Hangar action. */
public class HangarMenuScreen extends Screen {

    public static final int RETURN_TO_MAIN_MENU = 1;
    public static final int OPEN_SHIP_SELECTION = 12;
    public static final int OPEN_COLOR_SELECTION = 13;

    private static final int SELECTION_TIME = 200;
    private static final int OPTION_COUNT = 3;

    private final Cooldown selectionCooldown;
    private int selectedOption;
    private boolean wasMousePressed = true;
    private int lastHoveredOption = -1;

    public HangarMenuScreen(final int width, final int height, final int fps) {
        super(width, height, fps);

        this.returnCode = RETURN_TO_MAIN_MENU;
        this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
        this.selectionCooldown.reset();
        this.selectedOption = 0;
    }

    @Override
    public final int run() {
        super.run();
        return this.returnCode;
    }

    @Override
    protected void update() {
        super.update();

        boolean mousePressed = inputManager.isMousePressed();
        boolean newMouseClick = mousePressed && !wasMousePressed;
        wasMousePressed = mousePressed;

        if (this.inputDelay.checkFinished()) {
            int mouseOption = getMouseOption();

            // Play the movement sound only when entering a different option.
            if (mouseOption != lastHoveredOption) {
                lastHoveredOption = mouseOption;

                if (mouseOption != -1 && mouseOption != selectedOption) {
                    selectedOption = mouseOption;
                    AudioManager.playSFX("audio/sfx/menu_move.wav");
                }
            }

            if (newMouseClick && mouseOption != -1) {
                selectedOption = mouseOption;
                selectOption();
                draw();
                return;
            }
        }

        if (this.inputDelay.checkFinished()
                && this.selectionCooldown.checkFinished()) {

            if (inputManager.isKeyDown(KeyEvent.VK_UP)
                    || inputManager.isKeyDown(KeyEvent.VK_W)) {
                selectedOption =
                        (selectedOption - 1 + OPTION_COUNT) % OPTION_COUNT;
                selectionCooldown.reset();
                AudioManager.playSFX("audio/sfx/menu_move.wav");

            } else if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
                    || inputManager.isKeyDown(KeyEvent.VK_S)) {
                selectedOption = (selectedOption + 1) % OPTION_COUNT;
                selectionCooldown.reset();
                AudioManager.playSFX("audio/sfx/menu_move.wav");

            } else if (inputManager.isKeyDown(KeyEvent.VK_SPACE)
                    || inputManager.isKeyDown(KeyEvent.VK_ENTER)) {
                selectOption();

            } else if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
                returnToMainMenu();
            }
        }

        draw();
    }

    /**
     * Returns the option currently under the mouse.
     * The vertical positions follow the layout in DrawManager.drawHangarMenu().
     */
    private int getMouseOption() {
        int mouseX = inputManager.getMouseX();
        int mouseY = inputManager.getMouseY();

        // The labels are centered at half the screen height, then spaced
        // according to the Hangar menu layout.
        int firstY = getHeight() / 2;
        int spacing = getHeight() / 7;
        int halfWidth = 180;
        int halfHeight = 16;

        if (spacing <= 0) {
            return -1;
        }

        for (int i = 0; i < OPTION_COUNT; i++) {
            int rowY = firstY + i * spacing;

            if (mouseX >= getWidth() / 2 - halfWidth
                    && mouseX <= getWidth() / 2 + halfWidth
                    && mouseY >= rowY - halfHeight
                    && mouseY <= rowY + halfHeight) {
                return i;
            }
        }

        return -1;
    }

    private void selectOption() {
        if (selectedOption == 0) {
            returnCode = OPEN_SHIP_SELECTION;
            AudioManager.playSFX("audio/sfx/menu_select.wav");

        } else if (selectedOption == 1) {
            returnCode = OPEN_COLOR_SELECTION;
            AudioManager.playSFX("audio/sfx/menu_select.wav");

        } else {
            returnToMainMenu();
            return;
        }

        isRunning = false;
    }

    private void returnToMainMenu() {
        returnCode = RETURN_TO_MAIN_MENU;
        isRunning = false;
        AudioManager.playSFX("audio/sfx/menu_back.wav");
    }

    private void draw() {
        drawManager.initDrawing(this);
        drawManager.drawHangarMenu(this, selectedOption);
        drawManager.completeDrawing(this);
    }
}
