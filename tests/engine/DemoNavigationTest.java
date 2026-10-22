package engine;

import demo.DemoControls;
import demo.DemoPlayback;
import java.awt.Canvas;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import screen.*;

/** The presentation footer must not intercept normal game menu navigation. */
public final class DemoNavigationTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.prefs.PreferencesFactory", "demo.DemoPreferencesFactory");
        Constructor<DemoRunner> constructor = DemoRunner.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        DemoRunner runner = constructor.newInstance();
        try {
            check(clickMenu(runner, 2) instanceof GameModeScreen, "Play opens mode selection");
            check(clickMenu(runner, 10) instanceof ShopScreen, "Shop click opens shop");
            check(clickMenu(runner, 11) instanceof HangarMenuScreen, "Hangar click opens hangar");
            check(clickMenu(runner, 4) instanceof SettingsScreen, "Settings click opens settings");
            Screen mode = clickMenu(runner, 2);
            Screen campaign = follow(runner, mode, 2);
            check(campaign instanceof GameScreen, "mode confirmation starts the real game");
            check(follow(runner, mode, 8) instanceof InfiniteGameScreen, "infinite mode starts");
            Screen settings = clickMenu(runner, 4);
            Screen audio = follow(runner, settings, 5);
            check(audio instanceof AudioControlScreen, "audio settings navigation");
            check(follow(runner, audio, 4) instanceof SettingsScreen, "audio back navigation");
            Screen hangar = clickMenu(runner, 11);
            Screen ships = follow(runner, hangar, 12);
            check(ships instanceof ShipSelectionScreen, "ship selection navigation");
            check(follow(runner, ships, ShipSelectionScreen.SHIP_SELECTED) instanceof HangarMenuScreen,
                    "selected ship returns to hangar");
            Screen colors = follow(runner, hangar, 13);
            check(follow(runner, colors, 1) instanceof HangarMenuScreen, "color screen returns to hangar");
            Path scores = FileManager.resolveStoragePath("scores");
            check(scores.getParent().getFileName().toString().startsWith("invaders-demo-"),
                    "ordinary results write only inside demo storage");
            Core.getFileManager().saveHighScores(java.util.Collections.singletonList(new Score("DEM", 123)));
            check(Files.exists(scores) && Core.getFileManager().loadHighScores().get(0).getScore() == 123,
                    "normal high-score save/load works in the isolated demo");
            check(clickMenu(runner, 0) == null, "normal Exit closes the game");
            stationaryMouseKeepsKeyboardSelection();
            System.out.println("Demo navigation: " + checks + " checks passed.");
        } finally {
            Core.getRecordsService().close();
            audio.AudioManager.stopBGM();
            Core.setDemoServices(null, null);
            FileManager.setSessionStorageDirectory(null);
        }
    }

    private static void stationaryMouseKeepsKeyboardSelection() throws Exception {
        TitleScreen title = new TitleScreen(448,520,60);
        Method titleUpdate = TitleScreen.class.getDeclaredMethod("update"); titleUpdate.setAccessible(true);
        titleUpdate.invoke(title);
        int hoverY = -1;
        for (int y = 0; y < 520; y++) {
            if (Core.getDrawManager().getMenuOptionAt(title,224,y) == 2) { hoverY = y; break; }
        }
        Canvas source = new Canvas(); InputManager input = Core.getInputManager();
        input.mouseMoved(new MouseEvent(source,MouseEvent.MOUSE_MOVED,0,0,224,hoverY,0,false));
        titleUpdate.invoke(title);
        Field inputDelay = Screen.class.getDeclaredField("inputDelay"); inputDelay.setAccessible(true);
        inputDelay.set(title,Core.getCooldown(0));
        Field repeat = TitleScreen.class.getDeclaredField("selectionCooldown"); repeat.setAccessible(true);
        repeat.set(title,Core.getCooldown(0));
        input.keyPressed(new java.awt.event.KeyEvent(source,java.awt.event.KeyEvent.KEY_PRESSED,0,0,
                java.awt.event.KeyEvent.VK_DOWN,java.awt.event.KeyEvent.CHAR_UNDEFINED));
        titleUpdate.invoke(title);
        input.keyReleased(new java.awt.event.KeyEvent(source,java.awt.event.KeyEvent.KEY_RELEASED,0,0,
                java.awt.event.KeyEvent.VK_DOWN,java.awt.event.KeyEvent.CHAR_UNDEFINED));
        titleUpdate.invoke(title);
        Field code = Screen.class.getDeclaredField("returnCode"); code.setAccessible(true);
        check(code.getInt(title) == 10, "stationary mouse on Play cannot undo keyboard selection of Shop");
        ShipSelectionScreen ships = new ShipSelectionScreen(448,520,60);
        Method shipUpdate = ShipSelectionScreen.class.getDeclaredMethod("update"); shipUpdate.setAccessible(true);
        shipUpdate.invoke(ships);
        for (int y = 0; y < 520; y++) {
            if (Core.getDrawManager().getShipSelectionOptionAt(ships,entity.ShipType.values(),224,y) == 0) { hoverY = y; break; }
        }
        input.mouseMoved(new MouseEvent(source,MouseEvent.MOUSE_MOVED,0,0,224,hoverY,0,false));
        shipUpdate.invoke(ships);
        inputDelay.set(ships,Core.getCooldown(0));
        Field shipRepeat = ShipSelectionScreen.class.getDeclaredField("selectionCooldown"); shipRepeat.setAccessible(true);
        shipRepeat.set(ships,Core.getCooldown(0));
        input.keyPressed(new java.awt.event.KeyEvent(source,java.awt.event.KeyEvent.KEY_PRESSED,0,0,
                java.awt.event.KeyEvent.VK_DOWN,java.awt.event.KeyEvent.CHAR_UNDEFINED));
        shipUpdate.invoke(ships);
        input.keyReleased(new java.awt.event.KeyEvent(source,java.awt.event.KeyEvent.KEY_RELEASED,0,0,
                java.awt.event.KeyEvent.VK_DOWN,java.awt.event.KeyEvent.CHAR_UNDEFINED));
        shipUpdate.invoke(ships);
        check(ships.getSelectedShip() == entity.ShipType.SPEEDSTER,
                "stationary mouse on Standard cannot undo keyboard selection of Speedster");
    }

    private static Screen clickMenu(DemoRunner runner, int code) throws Exception {
        TitleScreen title = new TitleScreen(448, 520, 60);
        title.setPresentation(new DemoPlayback("Welcome", "Click any game menu option.", 1, 24,
                1000, new DemoControls()));
        Method update = TitleScreen.class.getDeclaredMethod("update");
        update.setAccessible(true);
        // First draw initializes the same metrics used for mouse hit testing.
        update.invoke(title);
        Field delay = Screen.class.getDeclaredField("inputDelay");
        delay.setAccessible(true);
        ((Cooldown) delay.get(title)).setDuration(0);
        Thread.sleep(2);
        int y = -1;
        for (int candidate = 0; candidate < title.getHeight(); candidate++) {
            if (Core.getDrawManager().getMenuOptionAt(title, 224, candidate) == code) {
                y = candidate;
                break;
            }
        }
        if (y < 0) throw new AssertionError("No hit area for menu code " + code);
        Canvas source = new Canvas();
        InputManager input = Core.getInputManager();
        input.mouseMoved(new MouseEvent(source, MouseEvent.MOUSE_MOVED, 0, 0, 224, y, 0, false));
        input.mousePressed(new MouseEvent(source, MouseEvent.MOUSE_PRESSED, 0, 0, 224, y, 1, false, MouseEvent.BUTTON1));
        update.invoke(title);
        input.mouseReleased(new MouseEvent(source, MouseEvent.MOUSE_RELEASED, 0, 0, 224, y, 1, false, MouseEvent.BUTTON1));
        Field result = Screen.class.getDeclaredField("returnCode");
        result.setAccessible(true);
        check(result.getInt(title) == code, "actual menu click selects " + code);
        return follow(runner, title, result.getInt(title));
    }

    private static Screen follow(DemoRunner runner, Screen previous, int code) throws Exception {
        Method method = DemoRunner.class.getDeclaredMethod("followGameNavigation", Screen.class, int.class);
        method.setAccessible(true);
        return (Screen) method.invoke(runner, previous, code);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
