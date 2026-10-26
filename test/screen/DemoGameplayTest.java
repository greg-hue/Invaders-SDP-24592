package screen;

import demo.DemoAction;
import demo.DemoControls;
import demo.DemoPlayback;
import engine.GameState;
import engine.GameSettings;
import engine.Core;
import entity.Ship;
import entity.Boss;
import entity.ShipType;
import boss.BossManager;
import java.lang.reflect.Field;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/** Prepared cues exercise the real item, status and boss systems. */
public final class DemoGameplayTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.prefs.PreferencesFactory", "demo.DemoPreferencesFactory");
        GameScreen ordinary = game(1);
        try {
            ordinary.demonstrate(DemoAction.SHIELD);
            throw new AssertionError("normal gameplay accepted a staged cue");
        } catch (IllegalStateException expected) { checks++; }
        for (DemoAction action : new DemoAction[] { DemoAction.SHIELD, DemoAction.TWIN_CANNON,
                DemoAction.HYPERFIRE, DemoAction.CHAMELEON, DemoAction.GRENADE, DemoAction.ANNIHILATOR }) {
            GameScreen screen = action == DemoAction.SHIELD ? preparedShield() : prepared(action == DemoAction.ANNIHILATOR ? 7 : 1);
            screen.demonstrate(action);
            screen.update();
            Ship ship = (Ship) field(screen, "ship");
            // Bypass only the opening countdown, then use the actual update/collision path.
            engine.Cooldown delay = (engine.Cooldown) field(screen, "inputDelay");
            delay.setDuration(0);
            Thread.sleep(2);
            screen.update();
            switch (action) {
            case SHIELD: check(ship.isShieldActive(), "shield pickup"); break;
            case TWIN_CANNON: check(ship.isTwinCannonActive(), "twin cannon"); break;
            case HYPERFIRE: check(ship.isHyperfireEnabled(), "hyperfire pickup"); break;
            case CHAMELEON: check(ship.isChameleonHullActive(), "chameleon pickup"); break;
            case GRENADE: check(ship.getGrenadeCount() == 1, "grenade pickup"); break;
            case ANNIHILATOR:
                check(((entity.item.OrbitalAnnihilator) field(screen, "annihilator")).isReady(), "laser pickup");
                break;
            default: break;
            }
        }
        GameScreen status = prepared(1);
        status.demonstrate(DemoAction.DAMAGE);
        check(status.getPlayerStatus().getLives() == 2, "prepared damage");
        status.demonstrate(DemoAction.EXTRA_LIFE);
        check(status.getPlayerStatus().getLives() == 3, "extra life");
        status.demonstrate(DemoAction.SLOW);
        check(status.getPlayerStatus().isSlowActive(), "slow debuff");
        GameScreen boss = prepared(5);
        boss.demonstrate(DemoAction.BOSS_PHASE_TWO);
        Boss entity = ((BossManager) field(boss, "bossManager")).getBoss();
        check(entity.getPhase() == 2, "second boss phase");
        boss.demonstrate(DemoAction.BOSS_PHASE_THREE);
        check(entity.getPhase() == 3 && !entity.isDefeated(), "third boss phase remains playable");
        boss.update();
        Object buffer = field(Core.getDrawManager(), "backBuffer");
        BufferedImage image = (BufferedImage) buffer;
        check(image.getHeight() == 520 + DemoPlayback.FOOTER_HEIGHT, "footer is outside playfield");
        ImageIO.write(image, "png", new File("out/demo-preview.png"));
        InfiniteGameScreen infinite = new InfiniteGameScreen(new GameState(1, 0, 3, 0, 0),448,520,60);
        infinite.setPresentation(playback());
        infinite.initialize();
        infinite.endPresentationRun();
        infinite.update();
        check(infinite.getGameState().getLivesRemaining() == 0 && !infinite.isAbandoned(), "prepared results ending");
        System.out.println("Demo gameplay: " + checks + " checks passed.");
    }
    private static GameScreen game(int stage) {
        return new GameScreen(new GameState(stage, 0, 3, 0, 0),
                new GameSettings(3, 2, 60, 2000), false,448,520,60,ShipType.STANDARD);
    }
    private static DemoPlayback playback() {
        return new DemoPlayback("Boss: three phases", "KimchiBaguette: staged phase changes at 12s and 20s; dodge and shoot.",
                18,24,45000,new DemoControls());
    }
    private static GameScreen preparedShield() {
        GameScreen screen = new GameScreen(new GameState(2,0,5,0,0), new GameSettings(3,2,60,2000),false,448,520,60,ShipType.TANK);
        screen.setPresentation(playback()); screen.initialize(); return screen;
    }
    private static GameScreen prepared(int stage) {
        GameScreen screen = game(stage);
        screen.setPresentation(playback());
        screen.initialize();
        return screen;
    }
    private static Object field(Object target, String name) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(target); }
            catch (NoSuchFieldException exception) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
