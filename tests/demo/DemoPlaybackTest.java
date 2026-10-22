package demo;

import java.awt.event.KeyEvent;
import java.awt.Canvas;
import engine.Core;
import engine.InputManager;

/** Timing and presenter navigation must not depend on real sleeps. */
public final class DemoPlaybackTest {
    private static int checks;
    private static final class FakeClock implements DemoPlayback.Clock {
        long nanos;
        public long nanoTime() { return nanos; }
        void millis(long value) { nanos = value * 1000000; }
    }
    public static void main(String[] args) {
        FakeClock clock = new FakeClock();
        DemoControls controls = new DemoControls();
        DemoPlayback playback = new DemoPlayback("Scene", "Note", 1, 2, 10000, controls, clock);
        int[] fired = {0};
        playback.at(2000, screen -> fired[0]++);
        check(playback.advance(DemoControls.Command.NONE, null), "starts");
        clock.millis(1999);
        playback.advance(DemoControls.Command.NONE, null);
        check(fired[0] == 0, "cue must wait");
        clock.millis(2000);
        playback.advance(DemoControls.Command.NONE, null);
        playback.advance(DemoControls.Command.NONE, null);
        check(fired[0] == 1, "cue fires once");
        clock.millis(3000);
        playback.advance(DemoControls.Command.HOLD, null);
        clock.millis(30000);
        check(playback.advance(DemoControls.Command.NONE, null), "hold prevents timeout");
        check(playback.getElapsedMillis() == 3000, "held time excluded");
        playback.advance(DemoControls.Command.HOLD, null);
        clock.millis(36999);
        check(playback.advance(DemoControls.Command.NONE, null), "resume keeps remaining time");
        clock.millis(37000);
        check(playback.advance(DemoControls.Command.NONE, null), "deadline must not advance a manual scene");
        clock.millis(900000);
        check(playback.advance(DemoControls.Command.NONE, null), "manual scene has no time limit");
        check(fired[0] == 1, "no repeated cues");
        screen.Screen screen = new screen.Screen(448, 520, 60);
        java.awt.Rectangle next = DemoPlayback.buttonBounds(screen, 2);
        check(DemoPlayback.commandAt(screen, next.x + 1, next.y + 1) == DemoControls.Command.NEXT,
                "next button hit test");
        check(DemoPlayback.commandAt(screen, 200, 400) == DemoControls.Command.NONE,
                "gameplay clicks do not navigate");
        check(next.y + next.height < screen.getHeight() + DemoPlayback.FOOTER_HEIGHT,
                "button fits within footer");
        DemoPlayback automatic = new DemoPlayback("", "", 1, 1, 1000, controls, clock, true);
        automatic.advance(DemoControls.Command.NONE, null);
        clock.millis(901000);
        check(!automatic.advance(DemoControls.Command.NONE, null), "opt-in smoke test timer");
        for (DemoControls.Command command : new DemoControls.Command[] {
                DemoControls.Command.PREVIOUS, DemoControls.Command.RESTART,
                DemoControls.Command.NEXT, DemoControls.Command.EXIT }) {
            DemoPlayback scene = new DemoPlayback("", "", 1, 1, 1000, controls, clock);
            check(!scene.advance(command, null) && scene.getNavigation() == command,
                    "navigation " + command);
        }
        InputManager input = Core.getInputManager();
        Canvas source = new Canvas();
        input.keyPressed(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_F8, KeyEvent.CHAR_UNDEFINED));
        check(controls.poll(input) == DemoControls.Command.NEXT, "F8 next");
        check(controls.poll(input) == DemoControls.Command.NONE, "held F8 cannot skip again");
        input.keyReleased(new KeyEvent(source, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_F8, KeyEvent.CHAR_UNDEFINED));
        controls.poll(input);
        input.keyPressed(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_SPACE, ' '));
        check(controls.poll(input) == DemoControls.Command.NONE && input.isKeyDown(KeyEvent.VK_SPACE),
                "gameplay input stays available");
        input.keyReleased(new KeyEvent(source, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_SPACE, ' '));
        input.mouseMoved(new java.awt.event.MouseEvent(source, java.awt.event.MouseEvent.MOUSE_MOVED,
                0, 0, next.x + 2, next.y + 2, 0, false));
        input.mousePressed(new java.awt.event.MouseEvent(source, java.awt.event.MouseEvent.MOUSE_PRESSED,
                0, 0, next.x + 2, next.y + 2, 1, false, java.awt.event.MouseEvent.BUTTON1));
        DemoPlayback clicked = new DemoPlayback("", "", 1, 2, 1, controls, clock);
        check(!clicked.beforeFrame(screen) && clicked.getNavigation() == DemoControls.Command.NEXT,
                "click advances the actual presentation input path");
        DemoPlayback following = new DemoPlayback("", "", 2, 2, 1, controls, clock);
        check(following.beforeFrame(screen), "holding a click cannot skip the next scene");
        input.mouseReleased(new java.awt.event.MouseEvent(source, java.awt.event.MouseEvent.MOUSE_RELEASED,
                0, 0, next.x + 2, next.y + 2, 1, false, java.awt.event.MouseEvent.BUTTON1));
        controls.consumeClick(input);
        int[] updates = {0};
        screen.Screen completed = new screen.Screen(448, 520, 60) {
            protected void update() { updates[0]++; isRunning = false; }
        };
        DemoPlayback.Clock ticking = new DemoPlayback.Clock() {
            long nanos;
            public long nanoTime() { return nanos += 1000000L; }
        };
        DemoPlayback waiting = new DemoPlayback("", "", 1, 1, 1, controls, ticking);
        completed.setPresentation(waiting);
        completed.run();
        check(updates[0] == 1 && waiting.getNavigation() == DemoControls.Command.NONE,
                "normal screen completion returns to game navigation");
        input.keyReleased(new KeyEvent(source, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_F8, KeyEvent.CHAR_UNDEFINED));
        System.out.println("Demo playback: " + checks + " checks passed.");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
