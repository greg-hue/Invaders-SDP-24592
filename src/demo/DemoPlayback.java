package demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import engine.Core;
import screen.Screen;

/** Presentation clock and one-shot cues; gameplay input remains untouched. */
public final class DemoPlayback {
    public static final int FOOTER_HEIGHT = 180;
    public interface Clock { long nanoTime(); }
    public interface Action { void apply(Screen screen); }
    private static final class Cue {
        final long atMillis;
        final Action action;
        boolean fired;
        Cue(long atMillis, Action action) {
            this.atMillis = atMillis;
            this.action = action;
        }
    }
    private final String title;
    private final String note;
    private final int number;
    private final int count;
    private final long durationMillis;
    private final DemoControls controls;
    private final Clock clock;
    private final boolean autoAdvance;
    private final List<Cue> cues = new ArrayList<Cue>();
    private long previousNanos;
    private long elapsedNanos;
    private boolean started;
    private boolean held;
    private DemoControls.Command navigation = DemoControls.Command.NONE;
    private java.util.function.Supplier<String> liveStatus;

    public void setLiveStatus(java.util.function.Supplier<String> status) { liveStatus = status; }

    public DemoPlayback(String title, String note, int number, int count,
            long durationMillis, DemoControls controls) {
        this(title, note, number, count, durationMillis, controls, System::nanoTime);
    }

    public DemoPlayback(String title, String note, int number, int count,
            long durationMillis, DemoControls controls, Clock clock) {
        this(title, note, number, count, durationMillis, controls, clock, false);
    }

    public DemoPlayback(String title, String note, int number, int count,
            long durationMillis, DemoControls controls, Clock clock, boolean autoAdvance) {
        if (durationMillis <= 0 || clock == null || controls == null)
            throw new IllegalArgumentException("A demo scene needs a positive duration and a clock.");
        this.title = title;
        this.note = note;
        this.number = number;
        this.count = count;
        this.durationMillis = durationMillis;
        this.controls = controls;
        this.clock = clock;
        this.autoAdvance = autoAdvance;
    }

    public void at(long millis, Action action) {
        if (millis < 0 || action == null)
            throw new IllegalArgumentException("Invalid demo cue.");
        cues.add(new Cue(millis, action));
    }

    public boolean beforeFrame(Screen screen) {
        engine.InputManager input = Core.getInputManager();
        DemoControls.Command command = controls.poll(input);
        if (controls.consumeClick(input)) {
            DemoControls.Command clicked = commandAt(screen, input.getMouseX(), input.getMouseY());
            if (clicked != DemoControls.Command.NONE) command = clicked;
        }
        return advance(command, screen);
    }

    /** Also usable with a fake clock, without opening a window. */
    public boolean advance(DemoControls.Command command, Screen screen) {
        long now = clock.nanoTime();
        if (started && !held)
            elapsedNanos += Math.max(0, now - previousNanos);
        started = true;
        previousNanos = now;
        if (command == DemoControls.Command.HOLD) held = !held;
        else if (command != DemoControls.Command.NONE) {
            navigation = command;
            return false;
        }
        if (autoAdvance && getElapsedMillis() >= durationMillis) return false;
        if (!held) {
            for (Cue cue : cues) {
                if (!cue.fired && getElapsedMillis() >= cue.atMillis) {
                    cue.fired = true;
                    cue.action.apply(screen);
                }
            }
        }
        return true;
    }

    public long getElapsedMillis() { return elapsedNanos / 1000000L; }
    public DemoControls.Command getNavigation() { return navigation; }
    public boolean isComplete() {
        return navigation != DemoControls.Command.NONE
                || (autoAdvance && getElapsedMillis() >= durationMillis);
    }

    public void drawFooter(Graphics graphics, Screen screen) {
        int y = screen.getHeight();
        int width = screen.getWidth();
        graphics.setColor(new Color(15, 23, 42));
        graphics.fillRect(0, y, width, FOOTER_HEIGHT);
        graphics.setColor(new Color(72, 220, 120));
        graphics.fillRect(0, y, width, 3);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 13));
        graphics.setColor(Color.WHITE);
        drawWrapped(graphics, number + "/" + count + "  " + title, 10, y + 21, width - 20);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 12));
        drawWrapped(graphics, note, 10, y + 57, width - 20);
        drawButton(graphics, buttonBounds(screen, 0), "Back");
        drawButton(graphics, buttonBounds(screen, 1), "Restart");
        drawButton(graphics, buttonBounds(screen, 2), number == count ? "Finish" : "Next >");
        graphics.setColor(new Color(160, 190, 220));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 11));
        graphics.drawString(liveStatus != null ? liveStatus.get() : held ? "Prepared events held. Gameplay continues."
                : "Click Next when you are ready. No time limit.", 10, y + 108);
        graphics.drawString("F6 Back  |  F7 Restart  |  F8 Next  |  F10 Exit", 10, y + 166);
    }

    public static Rectangle buttonBounds(Screen screen, int index) {
        int gap = 8;
        int buttonWidth = (screen.getWidth() - 20 - gap * 2) / 3;
        return new Rectangle(10 + index * (buttonWidth + gap), screen.getHeight() + 118,
                buttonWidth, 30);
    }

    public static DemoControls.Command commandAt(Screen screen, int x, int y) {
        for (int i = 0; i < 3; i++) {
            if (buttonBounds(screen, i).contains(x, y))
                return new DemoControls.Command[] { DemoControls.Command.PREVIOUS,
                    DemoControls.Command.RESTART, DemoControls.Command.NEXT }[i];
        }
        return DemoControls.Command.NONE;
    }

    private static void drawButton(Graphics graphics, Rectangle bounds, String label) {
        graphics.setColor(new Color(36, 64, 87));
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 13));
        graphics.drawString(label, bounds.x + (bounds.width - graphics.getFontMetrics().stringWidth(label)) / 2,
                bounds.y + 20);
    }

    private static void drawWrapped(Graphics graphics, String text, int x, int y, int width) {
        String line = "";
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && graphics.getFontMetrics().stringWidth(candidate) > width) {
                graphics.drawString(line, x, y);
                y += 15;
                line = word;
            } else line = candidate;
        }
        graphics.drawString(line, x, y);
    }
}
