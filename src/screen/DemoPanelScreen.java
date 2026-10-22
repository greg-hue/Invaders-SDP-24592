package screen;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** Clickable presenter controls for real services which have no production UI yet. */
public final class DemoPanelScreen extends Screen {
    private final String title;
    private final Supplier<List<String>> lines;
    private final List<String> buttons;
    private final Runnable[] actions;
    private boolean mouseHeld = true;
    private boolean confirmHeld = true;
    private int selected;

    public DemoPanelScreen(int width, int height, int fps, String title,
            Supplier<List<String>> lines, String[] buttons, Runnable... actions) {
        super(width, height, fps);
        if (buttons.length != actions.length) throw new IllegalArgumentException("Buttons/actions mismatch");
        this.title = title; this.lines = lines; this.buttons = Arrays.asList(buttons); this.actions = actions;
    }

    public static Rectangle buttonBounds(Screen screen, int count, int index) {
        int columns = count > 3 ? 2 : 1;
        int rows = (count + columns - 1) / columns;
        int width = (screen.getWidth() - 32 - (columns - 1) * 8) / columns;
        return new Rectangle(16 + (index % columns) * (width + 8),
                screen.getHeight() - rows * 36 - 12 + (index / columns) * 36, width, 30);
    }

    @Override protected void update() {
        boolean mouse = inputManager.isMousePressed();
        boolean confirm = inputManager.isKeyDown(KeyEvent.VK_ENTER);
        if (mouse && !mouseHeld && inputDelay.checkFinished()) {
            for (int i = 0; i < actions.length; i++) {
                if (buttonBounds(this, actions.length, i).contains(inputManager.getMouseX(), inputManager.getMouseY())) {
                    selected = i;
                    actions[i].run();
                    break;
                }
            }
        }
        if (confirm && !confirmHeld && inputDelay.checkFinished() && actions.length > 0) actions[selected].run();
        mouseHeld = mouse; confirmHeld = confirm;
        drawManager.initDrawing(this);
        drawManager.drawPresentationPanel(this, title, lines.get(), buttons, selected);
        drawManager.completeDrawing(this);
    }
}
