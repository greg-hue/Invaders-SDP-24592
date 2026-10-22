package demo;

import java.awt.event.KeyEvent;
import engine.InputManager;

/** One shared edge detector prevents a held key from skipping several scenes. */
public final class DemoControls {
    public enum Command { NONE, HOLD, PREVIOUS, RESTART, NEXT, EXIT }
    private final boolean[] held = new boolean[5];
    private boolean mouseHeld;
    private static final int[] KEYS = { KeyEvent.VK_F5, KeyEvent.VK_F6,
        KeyEvent.VK_F7, KeyEvent.VK_F8, KeyEvent.VK_F10 };

    public Command poll(final InputManager input) {
        Command command = Command.NONE;
        for (int i = 0; i < KEYS.length; i++) {
            boolean down = input.isKeyDown(KEYS[i]);
            if (down && !held[i]) {
                switch (i) {
                case 0: command = Command.HOLD; break;
                case 1: command = Command.PREVIOUS; break;
                case 2: command = Command.RESTART; break;
                case 3: command = Command.NEXT; break;
                case 4: command = Command.EXIT; break;
                default: break;
                }
            }
            held[i] = down;
        }
        return command;
    }

    /** Shared between scenes so one held click cannot advance twice. */
    public boolean consumeClick(final InputManager input) {
        boolean down = input.isMousePressed();
        boolean clicked = down && !mouseHeld;
        mouseHeld = down;
        return clicked;
    }
}
