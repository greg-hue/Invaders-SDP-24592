package engine;

import demo.DemoCombatConfig;
import demo.DemoControls;
import demo.DemoPlayback;
import demo.DemoProfile;
import entity.Boss;
import entity.Ship;
import entity.ShipType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.Canvas;
import java.awt.event.KeyEvent;
import screen.GameScreen;
import currency.PurchaseResult;

/** Exercises the requested demonstrations against real transactions, storage and gameplay. */
public final class DemoRequirementsTest {
    private static int checks;
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("restart")) {
            Path directory = Paths.get(args[1]);
            System.setProperty("invaders.demo.profile", directory.toString());
            System.setProperty("java.util.prefs.PreferencesFactory", "demo.DemoPreferencesFactory");
            FileManager.setSessionStorageDirectory(directory);
            DemoProfile saved = new DemoProfile(directory);
            try {
                check(saved.assets.getBalance() == 450, "credits survive JVM restart");
                check(currency.PermanentUpgrade.HULL.getLevel(saved.shop.snapshot()) == 2, "upgrade tiers survive JVM restart");
                check(saved.shop.isShipAvailable(ShipType.SPEEDSTER), "ship unlock survives JVM restart");
                check(ControlsConfig.getKey(ControlsConfig.LEFT) == KeyEvent.VK_A, "controls survive JVM restart");
                check(GameplayConfig.getDifficulty() == GameplayConfig.HARD, "difficulty survives JVM restart");
            } finally { saved.records.close(); }
            return;
        }
        Path directory = Files.createTempDirectory("invaders-demo-requirements-");
        System.setProperty("invaders.demo.profile", directory.toString());
        System.setProperty("java.util.prefs.PreferencesFactory", "demo.DemoPreferencesFactory");
        FileManager.setSessionStorageDirectory(directory);
        DemoProfile profile = new DemoProfile(directory);
        try {
            check(profile.records.getTopRuns().size() == 10, "prepared history");
            check(!profile.shop.isShipAvailable(ShipType.SPEEDSTER), "Speedster initially locked");
            check(profile.shop.purchase("upgrade.hull.1") == PurchaseResult.SUCCESS, "real Hull purchase");
            check(profile.shop.purchase("upgrade.hull.1") == PurchaseResult.ALREADY_UNLOCKED, "duplicate rejected");
            check(profile.shop.purchase("upgrade.hull.3") != PurchaseResult.SUCCESS, "tier prerequisite enforced");
            String speedster = profile.shop.getProducts().stream().filter(p -> p.getShipType() == ShipType.SPEEDSTER).findFirst().get().getId();
            check(profile.shop.purchase(speedster) == PurchaseResult.SUCCESS, "real ship unlock");
            check(profile.shop.isShipAvailable(ShipType.SPEEDSTER), "Hangar sees unlock");
            profile.shopSession.failNextSave();
            check(profile.shop.purchase("upgrade.hull.2") == PurchaseResult.SUCCESS, "purchase retained after save failure");
            check(profile.shop.getSaveStatus() == shop.ShopController.SaveStatus.FAILED, "failure visible");
            check(currency.PermanentUpgrade.HULL.getLevel(Core.getFileManager().loadPlayerAssets()) == 1, "disk remains at previous tier");
            check(profile.shop.retrySave() && profile.shop.getBalance() == 450, "save retry never charges twice");
            check(CampaignUpgrades.from(profile.shop.snapshot()).getMaxLives(ShipType.STANDARD) == 6, "bought Hull applies to campaign");
            for (int stage = 1; stage <= 3; stage++) {
                GameScreen movement = game(stage, 3, new DemoCombatConfig());
                check(field(field(movement, "enemyShipFormation"), "movementPattern").toString().equals(
                        new String[]{"CLASSIC", "WAVE_BOUNCE", "DIAGONAL_BOUNCE"}[stage-1]), "production stage pattern " + stage);
            }
            GameScreen low = game(2, 1, new DemoCombatConfig()); update(low);
            check(DifficultyAdjustWhilePlaying.getInstance().adjustEnemyBulletSpeed(4) == 2, "real low-life difficulty");
            GameScreen normal = game(2, 2, new DemoCombatConfig()); update(normal);
            check(DifficultyAdjustWhilePlaying.getInstance().adjustEnemyBulletSpeed(4) == 4, "difficulty recovers");
            GameScreen obstacle = game(1, 3, new DemoCombatConfig()); ready(obstacle);
            java.util.List<entity.PlayerStatusEvent.Type> events = new java.util.ArrayList<>();
            obstacle.getGameplayEvents().subscribe(entity.PlayerStatusEvent.class, e -> events.add(e.getType()));
            obstacle.demonstrate(demo.DemoAction.OBSTACLE);
            java.util.Set<entity.FallingObstacle> falling = (java.util.Set<entity.FallingObstacle>) field(obstacle,"fallingObstacles");
            Ship ship = (Ship) field(obstacle,"ship");
            falling.iterator().next().setPositionY(ship.getPositionY()-2);
            update(obstacle);
            check(obstacle.getPlayerStatus().isSlowActive() && obstacle.getPlayerStatus().getLives()==3, "real obstacle slows without damage");
            check(events.contains(entity.PlayerStatusEvent.Type.SLOW_STARTED), "production slow event published");
            ControlsConfig.setKey(ControlsConfig.LEFT, KeyEvent.VK_A);
            GameplayConfig.setDifficulty(GameplayConfig.HARD);
            profile.records.close();
            Process child = new ProcessBuilder(Paths.get(System.getProperty("java.home"),"bin","java.exe").toString(),
                    "-Djava.awt.headless=true", "-cp", System.getProperty("java.class.path"), DemoRequirementsTest.class.getName(),
                    "restart", directory.toString()).redirectErrorStream(true).redirectOutput(Paths.get("out/demo-restart-check.log").toFile()).start();
            if (!child.waitFor(30,java.util.concurrent.TimeUnit.SECONDS)) { child.destroyForcibly(); throw new AssertionError("restart timeout"); }
            check(child.exitValue()==0,"separate JVM persistence");
            renderPresentationChecks();
            System.out.println("Demo requirements: " + checks + " checks passed.");
        } finally { profile.records.close(); audio.AudioManager.stopBGM(); FileManager.setSessionStorageDirectory(null); }
    }

    private static void renderPresentationChecks() throws Exception {
        java.lang.reflect.Constructor<DemoRunner> constructor = DemoRunner.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        DemoRunner runner = constructor.newInstance();
        try {
            java.util.List<?> scenes = (java.util.List<?>) field(runner, "scenes");
            System.out.println("Scene coverage: " + scenes.size());
            check(scenes.size() > 40, "expanded conductor includes team requirements");
            for (Object scene : scenes) {
                String id = (String) field(scene, "id");
                if (!java.util.Arrays.asList("economy", "reload", "integration-status", "shop", "records-before", "high-scores", "json-tools").contains(id)) continue;
                @SuppressWarnings("unchecked")
                java.util.function.Supplier<screen.Screen> factory = (java.util.function.Supplier<screen.Screen>) field(scene, "screen");
                screen.Screen screen = factory.get();
                screen.setPresentation(new DemoPlayback((String) field(scene, "title"), (String) field(scene, "note"),
                        scenes.indexOf(scene)+1, scenes.size(), 10000, new DemoControls()));
                screen.initialize();
                render(screen, "out/demo-" + id + ".png");
            }
            Method load = DemoRunner.class.getDeclaredMethod("loadCustomExample"); load.setAccessible(true); load.invoke(runner);
            level.LevelDefinition definition = (level.LevelDefinition) field(runner,"customDefinition");
            check(definition != null && definition.getWaves().size() == 2, "bundled JSON imports both waves");
            Path copy = Files.createTempFile("demo-json-export-", ".json");
            new level.JsonLevelWriter().write(copy,definition);
            check(new level.JsonLevelReader().read(copy).getWaves().size()==2, "JSON export roundtrip");
            Method custom = DemoRunner.class.getDeclaredMethod("customGame"); custom.setAccessible(true);
            screen.Screen customScreen = (screen.Screen)custom.invoke(runner);
            check(((java.util.Set<?>)field(runner,"customScreens")).contains(customScreen), "custom gameplay is isolated");
            int beforeRecords = Core.getRecordsService().getRecordCount();
            Method finish = DemoRunner.class.getDeclaredMethod("finishRun", screen.Screen.class); finish.setAccessible(true); finish.invoke(runner,customScreen);
            check(Core.getRecordsService().getRecordCount()==beforeRecords, "custom attempt cannot submit official records");
            screen.ScoreScreen failed = new screen.ScoreScreen(448,520,60, new GameState(2,0,0,0,0),
                    java.util.Collections.emptyList());
            check(!(Boolean) field(failed,"isNewRecord") && (Boolean) field(failed,"stageFailed"),
                    "prepared full score table selects standard failed-stage results");
            check(((java.util.List<?>) field(failed, "runUnlocks")).isEmpty(), "standard result has no achievement layout");
            render(failed, "out/demo-standard-game-over.png");
            screen.ScoreScreen victory = new screen.ScoreScreen(448,520,60,new GameState(8,500,3,30,20),
                    java.util.Collections.emptyList());
            check((Boolean) field(victory,"gameCleared"), "final campaign result displays Game Cleared");
            render(victory, "out/demo-campaign-victory.png");
        } finally { Core.getRecordsService().close(); Core.setDemoServices(null,null); }
    }

    private static void render(screen.Screen screen, String path) throws Exception {
        Method update = screen.Screen.class.getDeclaredMethod("update"); update.setAccessible(true); update.invoke(screen);
        javax.imageio.ImageIO.write((java.awt.image.BufferedImage) field(Core.getDrawManager(),"backBuffer"), "png", new java.io.File(path));
    }

    private static GameScreen game(int stage, int lives, DemoCombatConfig config) {
        GameScreen screen = new GameScreen(new GameState(stage, 0, lives, 0, 0), new GameSettings(3,2,60,10000),
                false,448,520,60,ShipType.STANDARD, new records.RunSession(java.util.UUID.randomUUID().toString()));
        screen.setPresentation(new DemoPlayback("Requirements", "Prepared presentation adapter",1,1,100000,new DemoControls()));
        screen.configurePresentation(config); screen.initialize(); return screen;
    }
    private static void ready(GameScreen screen) throws Exception {
        ((Cooldown) field(screen,"inputDelay")).setDuration(0); Thread.sleep(2);
    }
    private static void update(GameScreen screen) throws Exception {
        Method method = GameScreen.class.getDeclaredMethod("update"); method.setAccessible(true); method.invoke(screen);
    }
    private static Object field(Object target, String name) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(target); }
            catch (NoSuchFieldException exception) { }
        }
        throw new NoSuchFieldException(name);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message); checks++;
    }
}
