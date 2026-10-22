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
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("restart")) {
            Path directory = Paths.get(args[1]);
            System.setProperty("invaders.demo.profile", directory.toString());
            System.setProperty("java.util.prefs.PreferencesFactory", "demo.DemoPreferencesFactory");
            FileManager.setSessionStorageDirectory(directory);
            DemoProfile reloaded = new DemoProfile(directory);
            try {
                check(reloaded.assets.getBalance() == Integer.parseInt(args[2]), "credits after JVM restart");
                check(reloaded.assets.getItemQuantity(DemoProfile.SHIELD) == Integer.parseInt(args[3]), "stock after JVM restart");
                check(reloaded.assets.isUnlocked(DemoProfile.UNLOCK), "unlock after JVM restart");
                check(ControlsConfig.getKey(ControlsConfig.LEFT) == KeyEvent.VK_A, "bindings after JVM restart");
                check(GameplayConfig.getDifficulty() == GameplayConfig.HARD, "difficulty after JVM restart");
                check(reloaded.records.getRecordCount() == 11, "records after JVM restart");
                System.out.println("Restart persistence: " + checks + " checks passed.");
            } finally { reloaded.records.close(); }
            return;
        }
        Path directory = Files.createTempDirectory("invaders-demo-requirements-");
        System.setProperty("invaders.demo.profile", directory.toString());
        System.setProperty("java.util.prefs.PreferencesFactory", "demo.DemoPreferencesFactory");
        FileManager.setSessionStorageDirectory(directory);
        DemoProfile profile = new DemoProfile(directory);
        try {
            check(profile.records.getTopRuns().size() == 10, "prepared top-ten history");
            check(profile.records.getUnlockTime(records.Achievement.FIRST_ENEMY) == null, "live combat milestone remains locked");
            profile.rewards.rewardEnemy("BASIC");
            profile.rewards.rewardLevel(GameplayConfig.EASY);
            profile.rewards.rewardLevel(GameplayConfig.NORMAL);
            profile.rewards.rewardLevel(GameplayConfig.HARD);
            check(profile.rewards.getPendingCurrency() == 360 && profile.assets.getBalance() == 1000,
                    "difficulty rewards accumulate separately from balance");
            profile.settle();
            check(profile.assets.getBalance() == 1360 && profile.rewards.getPendingCurrency() == 0, "settlement transfers once");
            profile.settle();
            check(profile.assets.getBalance() == 1360, "repeated settlement cannot duplicate credits");
            check(profile.shop.purchase(DemoProfile.SHIELD) == PurchaseResult.SUCCESS, "consumable purchase");
            check(profile.assets.getItemQuantity(DemoProfile.SHIELD) == 1 && profile.assets.getBalance() == 1260, "purchase deltas");
            check(profile.shop.purchase(DemoProfile.UNLOCK) == PurchaseResult.SUCCESS, "permanent purchase");
            check(profile.shop.purchase(DemoProfile.UNLOCK) == PurchaseResult.ALREADY_UNLOCKED, "duplicate ownership rejection");
            check(profile.shop.purchase(DemoProfile.EXPENSIVE) == PurchaseResult.INSUFFICIENT_FUNDS, "insufficient credit rejection");
            check(profile.assets.getBalance() == 960, "rejections leave balance unchanged");
            profile.shopSession.failNextSave();
            check(profile.shop.purchase(DemoProfile.SHIELD) == PurchaseResult.SUCCESS, "purchase succeeds despite prepared save failure");
            check(profile.shop.getSaveStatus() == shop.ShopController.SaveStatus.FAILED, "save failure reported separately");
            check(Core.getFileManager().loadPlayerAssets().getItemQuantities().get(DemoProfile.SHIELD) == 1,
                    "failed save preserves previous disk state");
            check(profile.shop.retrySave(), "save-only retry succeeds");
            check(profile.assets.getBalance() == 860 && profile.assets.getItemQuantity(DemoProfile.SHIELD) == 2,
                    "retry neither charges nor purchases twice");
            DemoCombatConfig paid = new DemoCombatConfig(); paid.purchasedItems = profile.shop;
            GameScreen using = game(2, 3, paid);
            ready(using);
            Canvas source = new Canvas();
            Core.getInputManager().keyPressed(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_B, 'b'));
            update(using); update(using);
            Core.getInputManager().keyReleased(new KeyEvent(source, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_B, 'b'));
            check(((Ship) field(using, "ship")).isShieldActive(), "purchased shield applies the real buff");
            check(profile.assets.getItemQuantity(DemoProfile.SHIELD) == 1 && profile.assets.getBalance() == 860,
                    "one use consumes one item, without another credit charge");
            for (int stage = 1; stage <= 3; stage++) {
                DemoCombatConfig config = new DemoCombatConfig(); config.movementStage = stage;
                GameScreen movement = game(stage, 3, config);
                Object formation = field(movement, "enemyShipFormation");
                check(field(formation, "movementPattern").toString().equals(new String[] {"CLASSIC","WAVE_BOUNCE","DIAGONAL_BOUNCE"}[stage-1]),
                        "presentation selects movement for stage " + stage);
            }
            DemoCombatConfig adaptive = new DemoCombatConfig(); adaptive.adaptiveProjectiles = true;
            GameScreen lowHealth = game(2, 1, adaptive); update(lowHealth);
            check((Integer) field(field(lowHealth, "enemyShipFormation"), "activeBulletSpeed") == 2, "one-life projectile speed");
            GameScreen normalHealth = game(2, 2, adaptive); update(normalHealth);
            check((Integer) field(field(normalHealth, "enemyShipFormation"), "activeBulletSpeed") == 4, "normal projectile speed");
            GameScreen obstacle = game(1, 3, new DemoCombatConfig()); ready(obstacle);
            Ship obstacleShip = (Ship) field(obstacle, "ship");
            int fireInterval = obstacleShip.getShootingInterval();
            obstacle.demonstrate(demo.DemoAction.OBSTACLE);
            entity.Entity falling = (entity.Entity) field(obstacle, "presentationObstacle");
            falling.setPositionY(obstacleShip.getPositionY() - 2);
            update(obstacle);
            check(obstacle.getPlayerStatus().isSlowActive(), "actual obstacle collision applies slow");
            check(obstacle.getPlayerStatus().getLives() == 3 && obstacleShip.getShootingInterval() == fireInterval,
                    "obstacle changes neither lives nor fire rate");
            int x = obstacleShip.getPositionX(); obstacleShip.moveRight(); obstacleShip.moveRight();
            check(obstacleShip.getPositionX() - x == 2, "slow halves movement");
            DemoCombatConfig bossConfig = new DemoCombatConfig(); bossConfig.bossType = Boss.BossType.BOSS_B; bossConfig.bossFeedback = true;
            GameScreen boss = game(5, 3, bossConfig);
            Boss bossEntity = ((boss.BossManager) field(boss, "bossManager")).getBoss();
            check(bossEntity.getMaxHealth() == 800, "Boss B is actually spawned");
            boss.demonstrate(demo.DemoAction.BOSS_PHASE_TWO);
            check(bossEntity.getPhase() == 2, "Boss B phase two");
            boss.demonstrate(demo.DemoAction.BOSS_PHASE_THREE);
            check(bossEntity.getPhase() == 3, "Boss B phase three");
            Method damage = GameScreen.class.getDeclaredMethod("damageBoss", int.class); damage.setAccessible(true);
            damage.invoke(boss, 10000);
            check(bossEntity.isDefeated() && boss.getGameState().getShipsDestroyed() == 1, "boss defeat reaches demo feedback/counting bridge");
            profile.records.completeRun(new records.RunRecord("live-completion-check", 42, 1, 62000, System.currentTimeMillis()));
            check(profile.records.getRecordCount() == 11, "completed run added to baseline");
            check(!profile.records.completeRun(new records.RunRecord("live-completion-check", 42, 1, 62000, System.currentTimeMillis())),
                    "duplicate completion ignored");
            check(profile.records.getUnlockTime(records.Achievement.SIXTY_SECONDS) != null, "survival achievement persisted");
            ControlsConfig.setKey(ControlsConfig.LEFT, KeyEvent.VK_A);
            GameplayConfig.setDifficulty(GameplayConfig.HARD);
            profile.checkRecovery();
            check(profile.message.contains("restored from backup"), "real recovery on disposable copy");
            check(profile.records.getRecordCount() == 11, "recovery demo preserves live history");
            profile.records.close();
            Path output = Paths.get("out", "demo-restart-check.log").toAbsolutePath();
            Process child = new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "java.exe").toString(),
                    "-Djava.awt.headless=true", "-cp", System.getProperty("java.class.path"), DemoRequirementsTest.class.getName(),
                    "restart", directory.toString(), "860", "1").redirectErrorStream(true).redirectOutput(output.toFile()).start();
            if (!child.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) { child.destroyForcibly(); throw new AssertionError("Restart check timed out"); }
            check(child.exitValue() == 0, "separate-JVM reload (see demo-restart-check.log)");
            renderPresentationChecks();
            System.out.println("Demo requirements: " + checks + " checks passed.");
        } finally {
            profile.records.close(); audio.AudioManager.stopBGM(); FileManager.setSessionStorageDirectory(null);
        }
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
                if (!java.util.Arrays.asList("economy", "reload", "integration-status", "shop", "records-before", "high-scores").contains(id)) continue;
                @SuppressWarnings("unchecked")
                java.util.function.Supplier<screen.Screen> factory = (java.util.function.Supplier<screen.Screen>) field(scene, "screen");
                screen.Screen screen = factory.get();
                screen.setPresentation(new DemoPlayback((String) field(scene, "title"), (String) field(scene, "note"),
                        scenes.indexOf(scene)+1, scenes.size(), 10000, new DemoControls()));
                screen.initialize();
                render(screen, "out/demo-" + id + ".png");
            }
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
