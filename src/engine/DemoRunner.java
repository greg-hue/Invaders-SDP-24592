package engine;

import audio.AudioControls;
import audio.AudioManager;
import campaign.CampaignStageData;
import currency.PlayerAssets;
import demo.DemoAction;
import demo.DemoControls;
import demo.DemoPlayback;
import entity.ShipType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;
import records.RecordsService;
import records.RunSession;
import screen.*;
import shop.ShopController;

/** Manual gameplay with presenter-controlled scenes and isolated rehearsal data. */
public final class DemoRunner {
    private static final int FPS = 60;
    private final List<Scene> scenes = new ArrayList<Scene>();
    private final DemoControls controls = new DemoControls();
    private final Properties durations = new Properties();
    private final double scale;
    private final Frame frame;
    private final int width;
    private final int height;
    private RecordsService records;
    private final ShopController shop;
    private final demo.DemoProfile profile;
    private RunSession run;
    private ShipType selectedShip = ShipType.STANDARD;
    private GameState lastState = new GameState(1, 0, 3, 0, 0);
    private final java.util.Map<Screen, AutoCloseable> rewardSubscriptions = new java.util.HashMap<>();
    private final CampaignRewards campaignRewards;
    private final EndlessRewards endlessRewards;
    private CampaignUpgrades runUpgrades = CampaignUpgrades.none();
    private final java.util.Set<Screen> customScreens = new java.util.HashSet<>();
    private level.LevelDefinition customDefinition;
    private String jsonMessage = "Load the bundled two-wave example, export it, then play the next scene.";
    private final java.util.Set<Screen> hookedScreens = new java.util.HashSet<>();
    private final java.util.Set<Screen> settledScreens = new java.util.HashSet<>();
    private final java.util.Set<Screen> encounterScreens = new java.util.HashSet<>();
    private final java.util.List<String> statusEvents = new java.util.ArrayList<>();
    private String latestEvent = "Live event bus: waiting for a gameplay event";

    private static final class Scene {
        final String id;
        final String title;
        final String note;
        final Supplier<Screen> screen;
        final DemoAction[] actions;
        Scene(String id, String title, String note, Supplier<Screen> screen,
                DemoAction... actions) {
            this.id = id; this.title = title; this.note = note;
            this.screen = screen; this.actions = actions;
        }
    }

    private DemoRunner() throws IOException {
        scale = Double.parseDouble(System.getProperty("invaders.demo.scale", "1"));
        if (!Double.isFinite(scale) || scale <= 0 || scale > 100)
            throw new IllegalArgumentException("Demo scale must be in (0, 100].");
        try (InputStream input = DemoRunner.class.getClassLoader().getResourceAsStream("demo.properties")) {
            if (input != null) durations.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        String custom = System.getProperty("invaders.demo.script");
        if (custom != null) {
            try (java.io.Reader reader = Files.newBufferedReader(java.nio.file.Paths.get(custom),
                    StandardCharsets.UTF_8)) { durations.load(reader); }
        }
        // Fail on invalid timing before opening the application window.
        for (String key : durations.stringPropertyNames()) {
            double seconds = Double.parseDouble(durations.getProperty(key));
            if (!Double.isFinite(seconds) || seconds <= 0 || seconds > 3600)
                throw new IllegalArgumentException("Invalid scene duration: " + key);
        }
        String requested = System.getProperty("invaders.demo.profile");
        Path directory = requested != null ? java.nio.file.Paths.get(requested).toAbsolutePath()
                : java.awt.GraphicsEnvironment.isHeadless() ? Files.createTempDirectory("invaders-demo-")
                : java.nio.file.Paths.get(".demo-profile").toAbsolutePath();
        System.setProperty("invaders.demo.profile", directory.toString());
        FileManager.setSessionStorageDirectory(directory);
        profile = new demo.DemoProfile(directory);
        records = profile.records;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> profile.records.close(), "demo-records-shutdown"));
        shop = profile.shop;
        Core.setDemoServices(records, shop);
        campaignRewards = new CampaignRewards(profile.assets, Core.getFileManager());
        endlessRewards = new EndlessRewards(profile.assets, Core.getFileManager());
        Core.setDemoRewardServices(campaignRewards, endlessRewards);
        AudioControls.applySettings();
        frame = java.awt.GraphicsEnvironment.isHeadless() ? null
                : new Frame(448, 520 + DemoPlayback.FOOTER_HEIGHT);
        Core.getDrawManager().setFrame(frame);
        if (frame != null) {
            frame.setPresentationSize(448, 520 + DemoPlayback.FOOTER_HEIGHT);
            frame.setTitle("Invaders — Main Project Demo");
        }
        width = frame == null ? 448 : frame.getWidth();
        height = frame == null ? 520 : frame.getHeight() - DemoPlayback.FOOTER_HEIGHT;
        buildScenes();
        Core.getLogger().info("Presentation scenes: " + scenes.size());
        Core.getLogger().info("Demo profile: " + directory);
    }

    public static void run() {
        // Core dispatches here before any game setting reads Preferences.
        System.setProperty("java.util.prefs.PreferencesFactory", "demo.DemoPreferencesFactory");
        try {
            new DemoRunner().present();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot prepare the demo.", exception);
        }
    }

    private void buildScenes() {
        add("menu", "Welcome aboard", "Frenchies / VFX / Audio: navigation, stars and menu sounds.",
                () -> new TitleScreen(width, height, FPS));
        add("settings", "Prepare your mission", "Frenchies: all settings in one menu.",
                () -> new SettingsScreen(width, height, FPS));
        add("audio", "Sound and music", "Chanyoung: adjust BGM/SFX volumes, then press M to toggle mute.",
                () -> new AudioControlScreen(width, height, FPS));
        add("controls", "Custom controls", "Frenchies: select an action, press Space, then choose a new key.",
                () -> new ControlsSettingsScreen(width, height, FPS));
        add("difficulty", "Choose your difficulty", "Frenchies / Octopus: Easy, Normal or Hard for campaign gameplay.",
                () -> new GameplaySettingsScreen(width, height, FPS));
        add("settings-check", "Retained settings", "Reopen Controls and Gameplay; confirm cancel/duplicate rejection and retained choices.",
                () -> new SettingsScreen(width, height, FPS));
        add("ships", "Choose your ship", "KimchiBaguette: compare speed, fire rate, lives and projectiles.",
                () -> new ShipSelectionScreen(width, height, FPS));
        add("color", "Customize your ship", "Frenchies / VFX: use Left/Right to change the ship color.",
                () -> new HangarScreen(width, height, FPS));
        add("modes", "Two ways to play", "Octopus / Frenchies: campaign progression and endless survival.",
                () -> new GameModeScreen(width, height, FPS));
        add("records-before", "Records before the mission", "Chinese can fly: prepared top-ten baseline. Inspect bests and five achievement conditions.",
                () -> new PlayerRecordsScreen(width, height, FPS, records));
        combat("campaign", "First mission", "Octopus / Friends / VFX / Audio: play using your selected ship.",
                1, () -> selectedShip);
        add("hud", "Campaign HUD and floating points", "Final HUD: stage, lives, score and Credits. Shoot an enemy; watch points and pending Credits update.",
                () -> preparedGame(1, ShipType.STANDARD, 3, false, new GameSettings(3, 2, 60, 2000), null));
        combat("speedster", "Speedster: movement and bursts", "KimchiBaguette: speed 4, two-shot bursts and three lives. Unlock it in the Shop for normal selection.",
                1, () -> ShipType.SPEEDSTER);
        combat("tank", "Heavy Tank: spread shots", "KimchiBaguette: speed 1, three projectiles and five lives.",
                1, () -> ShipType.TANK);
        combat("status", "Damage particles and flashing",
                "VFX: staged hit at 8s; watch the life loss, red/green flash and player particles.",
                1, () -> ShipType.STANDARD, DemoAction.DAMAGE);
        add("extra-life", "Third-stage entry: extra life", "Prepared stage-3 entry with 3/4 lives: the final bonus-life rule grants the fourth heart.",
                () -> preparedGame(3, ShipType.STANDARD, 3, true, new GameSettings(3, 2, 40, 2000), null));
        add("life-cap", "Extra life is capped", "Prepared stage-3 entry with Heavy Tank at 5/5 lives: the bonus cannot exceed the cap.",
                () -> preparedGame(3, ShipType.TANK, 5, true, new GameSettings(3, 2, 40, 2000), null));
        for (int stage = 1; stage <= 3; stage++) {
            final int movementStage = stage;
            String pattern = new String[] { "Classic sweep", "Wave bounce", "Diagonal bounce" }[stage - 1];
            add("movement-" + stage, "Enemy movement: " + pattern,
                    "Final game: patterns rotate by stage. Watch smooth movement, aimed shots and acceleration as enemies fall.",
                    () -> {
                        demo.DemoCombatConfig config = new demo.DemoCombatConfig();
                        return preparedGame(movementStage, ShipType.STANDARD, 3, false,
                                new GameSettings(4, 2, 50 - movementStage * 8, 1500), config);
                    });
        }
        for (int lives : new int[] { 2, 1 }) {
            final int initialLives = lives;
            add("adaptive-" + lives, "Enemy projectiles: " + lives + (lives == 1 ? " life" : " lives"),
                    "Final adaptive difficulty: newly fired shots use speed " + (lives == 1 ? "2 (reduced)." : "4 (normal)."),
                    () -> {
                        demo.DemoCombatConfig config = new demo.DemoCombatConfig();
                        return preparedGame(2, ShipType.STANDARD, initialLives, false, new GameSettings(3, 2, 50, 900), config);
                    });
        }
        scenes.add(new Scene("obstacle", "Falling obstacle: movement penalty",
                "Real red obstacle staged at 8s. Catch it: movement halves for 2s. A shield blocks the slow.",
                () -> preparedGame(1, ShipType.STANDARD, 3, false, new GameSettings(3, 2, 60, 10000),
                        new demo.DemoCombatConfig()), DemoAction.OBSTACLE));
        combat("shield", "Energy Shield", "Best French: Heavy Tank only: staged shield pickup at 8s, protecting you for 15s.",
                2, () -> ShipType.TANK, DemoAction.SHIELD);
        combat("twin", "Twin Cannon", "Best French: staged activation at 8s; shoot to try both cannons.",
                3, () -> ShipType.STANDARD, DemoAction.TWIN_CANNON);
        combat("hyperfire", "Hyperfire", "Best French: staged pickup at 8s; hold your fire key to compare fire rates.",
                2, () -> ShipType.STANDARD, DemoAction.HYPERFIRE);
        combat("chameleon", "Chameleon Hull", "Best French: transformation at 8s; use Up/Down and touch enemies.",
                1, () -> ShipType.STANDARD, DemoAction.CHAMELEON);
        combat("grenade", "Cluster Grenade", "Best French: staged pickup at 8s; press G to launch (if G is unbound).",
                2, () -> ShipType.STANDARD, DemoAction.GRENADE);
        for (entity.Boss.BossType type : entity.Boss.BossType.values()) {
            scenes.add(new Scene("boss-" + type.name(), type == entity.Boss.BossType.BOSS_A
                    ? "Boss A: 850 HP, slow" : "Boss B: 800 HP, fast",
                    "Final boss mechanics and music; staged phase changes at 12/20s. Defeat it for real score, rewards, VFX and sound.",
                    () -> {
                        demo.DemoCombatConfig config = new demo.DemoCombatConfig(); config.bossType = type;
                        return preparedGame(type == entity.Boss.BossType.BOSS_A ? 3 : 5, ShipType.TANK, 5, false, new GameSettings(1, 1, 60, 3000), config);
                    }, DemoAction.BOSS_PHASE_TWO, DemoAction.BOSS_PHASE_THREE));
        }
        combat("laser", "Orbital Annihilator: stage 7",
                "Best French: staged pickup at 8s; hold Space to charge the laser.",
                7, () -> ShipType.STANDARD, DemoAction.ANNIHILATOR);
        add("infinite", "Endless survival", "Octopus / KimchiBaguette: zigzags and rising difficulty. Next ends this run.",
                () -> {
                    startRun();
                    return new InfiniteGameScreen(new GameState(1, 0, 3, 0, 0), width, height, FPS, run);
                });
        add("results", "Mission results", "Review your demo score, lives, accuracy and unlocked achievements.",
                () -> new DemoResultsScreen(width, height, FPS, lastState, records.getCurrentRunUnlocks()));
        scenes.add(new Scene("standard-game-over", "Standard Game Over: no-fire run",
                "Do not shoot. Prepared lethal hit at 8s; full high-score table prevents name entry. Check Stage 2 Failed and replay.",
                () -> preparedGame(2, ShipType.STANDARD, 1, false, new GameSettings(1, 1, 60, 10000), null), DemoAction.END_RUN));
        add("stage-clear", "Stage clear and progression", "Prepared one-enemy stage: defeat it while alive; the real campaign advances to stage 2.",
                () -> preparedGame(1, ShipType.STANDARD, 3, false, new GameSettings(1, 1, 60, 3000), null));
        add("campaign-clear", "Campaign victory", "Prepared final encounter: one enemy, boss with 50 HP. Clear both alive to show Game Cleared!",
                () -> {
                    demo.DemoCombatConfig config = new demo.DemoCombatConfig(); config.bossType = entity.Boss.BossType.BOSS_B;
                    config.bossHealth = 50;
                    return preparedGame(7, ShipType.STANDARD, 3, false, new GameSettings(1, 1, 60, 4000), config);
                });
        scenes.add(new Scene("status-events", "Life, shield and slow events",
                "Heavy Tank: hit at 8s, life at 12s, shield at 16s, obstacle at 20s. Shield blocks the obstacle; watch real events.",
                () -> preparedGame(1, ShipType.TANK, 3, false, new GameSettings(3, 2, 60, 10000),
                        new demo.DemoCombatConfig()),
                DemoAction.DAMAGE, DemoAction.EXTRA_LIFE, DemoAction.STATUS_SHIELD, DemoAction.OBSTACLE));
        add("status-bus", "Status event bus", "Actual life, shield and slow events from the final game are listed here after gameplay.",
                () -> new DemoPanelScreen(width, height, FPS, "Gameplay event subscribers", () -> {
                    java.util.List<String> lines = new java.util.ArrayList<>();
                    lines.add("Life and shield events use the shared GameplayEventPublisher.");
                    lines.add("Slow start, refresh and end events now use the production event bus.");
                    lines.addAll(statusEvents); return lines;
                }, new String[0]));
        add("records", "Records and achievements", "Chinese can fly: Left/Right switches tabs. This demo uses a separate profile.",
                () -> new PlayerRecordsScreen(width, height, FPS, records));
        add("high-scores", "High-score medals and icons", "The Diversity Hires: rank medals, hearts, stars, settings and title icons.",
                () -> new HighScoreScreen(width, height, FPS));
        add("shop", "Permanent upgrades and ship unlocks", "Final catalog: Hull, Engine and Reloader have three tiers. Speedster costs 200 Credits. Purchases persist.",
                () -> new ShopScreen(width, height, FPS, shop));
        add("economy", "Credits, pending rewards and save retry", "Real campaign and endless reward services. Optional buttons prepare a one-shot purchase save failure.",
                this::economyPanel);
        add("upgraded-campaign", "Purchased upgrades in campaign", "Start a fresh campaign: bought Hull, Engine and Reloader upgrades affect lives, movement and firing. Endless is unchanged.",
                () -> { startRun(); return campaignScreen(new GameState(1, 0, runUpgrades.getMaxLives(selectedShip), 0, 0), selectedShip, false); });
        add("json-tools", "Custom levels: JSON import and export", "Octopus: load and validate the bundled example, export a copy to the demo profile, then click Next to play it.", this::jsonPanel);
        add("json-play", "Custom level: two authored waves", "Play the imported enemy grid and both waves. Custom play does not earn official credits, records or campaign unlocks.", this::customGame);
        add("reload", "Save, reload and restart", "Reload real saved data here. Relaunch with the same demo profile to verify persistence.",
                this::reloadPanel);
        add("integration-status", "Final build and presentation notes", "The demo now uses main's final gameplay. Only scene preparation and navigation are presentation-specific.",
                () -> new DemoPanelScreen(width, height, FPS, "Final integration notes", () -> java.util.Arrays.asList(
                    "Final production catalog, rewards, credit HUD, boss effects and slow events are integrated.",
                    "JSON import/export and custom gameplay are available through this demo panel; main has no custom-level menu.",
                    "Shop purchases are permanent upgrades and ship ownership, not consumable shield stock.",
                    "Starting credits and ranking history are rehearsal data. Staged hits, pickups and boss phases are presenter aids."), new String[0]));
        add("end", "End of the demonstration", "Ready for questions. Use Back to revisit a scene, or Finish to close the demo.",
                () -> new TitleScreen(width, height, FPS));
    }

    private GameScreen preparedGame(int stage, ShipType type, int lives, boolean extraLife,
            GameSettings settings, demo.DemoCombatConfig config) {
        startRun();
        GameScreen game = new GameScreen(new GameState(stage, 0, lives, 0, 0), settings,
                extraLife, width, height, FPS, type, run);
        if (config != null) game.configurePresentation(config);
        return game;
    }

    private void attachDemoServices(Screen screen, DemoPlayback playback) {
        if (!hookedScreens.add(screen) || customScreens.contains(screen)) return;
        if (screen instanceof InfiniteGameScreen) {
            InfiniteGameScreen infinite = (InfiniteGameScreen) screen;
            rewardSubscriptions.put(screen, endlessRewards.subscribe(infinite.getGameplayEvents(), infinite.getActiveRunId()));
            playback.setLiveStatus(() -> latestEvent);
            return;
        }
        if (!(screen instanceof GameScreen)) return;
        GameScreen game = (GameScreen) screen;
        rewardSubscriptions.put(screen, campaignRewards.subscribe(game.getGameplayEvents(), game.getGameState().getLevel()));
        game.getGameplayEvents().subscribe(entity.PlayerStatusEvent.class,
                event -> logStatus(event.getType().getEventName() + " | lives " + event.getLives()));

        playback.setLiveStatus(() -> latestEvent);
    }

    private void logStatus(String event) {
        latestEvent = event;
        statusEvents.add(event);
        while (statusEvents.size() > 8) statusEvents.remove(0);
    }

    private void settleGameplay(Screen screen) {
        if (!settledScreens.add(screen)) return;
        AutoCloseable subscription = rewardSubscriptions.remove(screen);
        if (subscription != null) {
            try { subscription.close(); } catch (Exception e) { throw new IllegalStateException(e); }
        }
    }

    private Screen economyPanel() {
        return new DemoPanelScreen(width, height, FPS, "Credits and permanent ownership", () -> java.util.Arrays.asList(
                "Rehearsal starting balance; live kills use final production rewards.",
                "Credits: " + shop.getBalance() + " | Pending: " + (campaignRewards.getPendingCurrency() + endlessRewards.getPendingCurrency()),
                upgradeSummary(),
                "Speedster available: " + shop.isShipAvailable(ShipType.SPEEDSTER),
                "Purchase save status: " + shop.getSaveStatus(), profile.message),
                new String[] { "Buy next Hull + fail save", "Retry purchase save", "Retry reward settlement", "Reload saved data" },
                () -> {
                    int owned = currency.PermanentUpgrade.HULL.getLevel(shop.snapshot());
                    if (owned == 3) { profile.message = "Hull already at maximum tier."; return; }
                    String id = currency.PermanentUpgrade.HULL.getProductId(owned + 1);
                    if (shop.getBalance() < currency.PermanentUpgrade.HULL.getPrice(owned + 1)) {
                        profile.message = "Not enough Credits. Earn rewards in campaign or endless first."; return;
                    }
                    profile.shopSession.failNextSave();
                    profile.message = "Purchase: " + shop.purchase(id) + ". Retry SAVE without buying again.";
                },
                () -> { int before = shop.getBalance(); profile.message = "Save retry: " + shop.retrySave() + "; Credits " + before + " -> " + shop.getBalance(); },
                () -> { profile.message = "Reward retry: " + Core.retryRewards(campaignRewards, endlessRewards); },
                this::reloadProfile);
    }

    private String upgradeSummary() {
        return "Hull " + currency.PermanentUpgrade.HULL.getLevel(shop.snapshot()) + "/3 | Engine "
                + currency.PermanentUpgrade.ENGINE.getLevel(shop.snapshot()) + "/3 | Reloader "
                + currency.PermanentUpgrade.RELOADER.getLevel(shop.snapshot()) + "/3";
    }

    private Path customSource() { return profile.directory.resolve("presentation-level.json"); }

    private void loadCustomExample() {
        try (InputStream input = DemoRunner.class.getClassLoader().getResourceAsStream("demo-level.json")) {
            if (input == null) throw new IOException("Missing bundled level example");
            if (!Files.exists(customSource())) Files.copy(input, customSource());
            level.LevelDefinition loaded = new level.JsonLevelReader().read(customSource());
            LevelGameplayAdapter.prepare(loaded, width, height, GameplayConfig.getMultiplier());
            customDefinition = loaded;
            jsonMessage = "Imported and validated: " + loaded.getName() + "; " + loaded.getWaves().size() + " waves.";
        } catch (IOException | IllegalArgumentException e) { jsonMessage = "Import rejected: " + e.getMessage(); }
    }

    private Screen jsonPanel() {
        return new DemoPanelScreen(width, height, FPS, "Custom level workflow", () -> java.util.Arrays.asList(
                "The final JSON reader validates schema, grids and wave settings.",
                "Example: two waves, typed enemies, holes in the second grid.",
                "Source: demo profile / presentation-level.json",
                "Export: demo profile / exported-level.json",
                "Next opens custom gameplay without official rewards or records.", jsonMessage),
                new String[] { "Import / reload JSON", "Export loaded level", "Validate exported copy" },
                this::loadCustomExample,
                () -> { if (customDefinition == null) loadCustomExample();
                    if (customDefinition != null) try {
                        new level.JsonLevelWriter().write(profile.directory.resolve("exported-level.json"), customDefinition);
                        jsonMessage = "Exported original level definition successfully.";
                    } catch (IOException e) { jsonMessage = "Export failed: " + e.getMessage(); } },
                () -> { try {
                    level.LevelDefinition copy = new level.JsonLevelReader().read(profile.directory.resolve("exported-level.json"));
                    jsonMessage = "Export validated: " + copy.getName() + "; " + copy.getWaves().size() + " waves.";
                } catch (IOException e) { jsonMessage = "Validation failed: " + e.getMessage(); } });
    }

    private Screen customGame() {
        if (customDefinition == null) loadCustomExample();
        if (customDefinition == null) return jsonPanel();
        GameScreen game = GameScreen.forCustomLevel(new GameState(1, 0, 3, 0, 0), customDefinition,
                GameplayConfig.getMultiplier(), width, height, FPS);
        customScreens.add(game);
        return game;
    }

    private void reloadProfile() {
        profile.reload(); records = profile.records; Core.setDemoServices(records, shop);
    }

    private Screen reloadPanel() {
        return new DemoPanelScreen(width, height, FPS, "Save and reload", () -> java.util.Arrays.asList(
                "This rehearsal profile persists across application restarts.",
                "Credits: " + profile.assets.getBalance() + " | Speedster: " + shop.isShipAvailable(ShipType.SPEEDSTER),
                upgradeSummary(),
                "Completed records: " + records.getRecordCount() + " | Best score: " + records.getHighestScore(),
                "Difficulty: " + new String[] {"Easy", "Normal", "Hard"}[GameplayConfig.getDifficulty()] + " | Fire key code: "
                        + ControlsConfig.getKey(ControlsConfig.FIRE),
                "Restart using the same command/profile, then revisit this scene.", profile.message),
                new String[] { "Reload from disk", "Test recovery on a copy" }, this::reloadProfile, profile::checkRecovery);
    }

    private void add(String id, String title, String note, Supplier<Screen> factory) {
        scenes.add(new Scene(id, title, note, factory));
    }

    private void combat(String id, String title, String note, int stage,
            Supplier<ShipType> type, DemoAction... actions) {
        scenes.add(new Scene(id, title, note, () -> {
            startRun();
            ShipType ship = type.get();
            GameSettings settings = Core.applyDifficulty(CampaignStageData.getStageSettings(stage),
                    GameplayConfig.getMultiplier());
            return new GameScreen(new GameState(stage, 0, ship.getMaxLives(), 0, 0),
                    settings, false, width, height, FPS, ship, run);
        }, actions));
    }

    private void startRun() {
        DifficultyAdjustWhilePlaying.getInstance().reset();
        runUpgrades = CampaignUpgrades.from(profile.assets.snapshot());
        run = new RunSession(UUID.randomUUID().toString());
        campaignRewards.startRun(run.getRunId());
        records.startRun(run.getRunId());
    }

    private void present() {
        try {
            for (int index = 0; index < scenes.size();) {
                Scene scene = scenes.get(index);
                long duration = Math.max(1L, Math.round(1000 * scale
                        * Double.parseDouble(durations.getProperty(scene.id, "25"))));
                DemoPlayback playback = new DemoPlayback(scene.title, scene.note,
                        index + 1, scenes.size(), duration, controls, System::nanoTime,
                        Boolean.getBoolean("invaders.demo.autoAdvance"));
                Screen screen = scene.screen.get();
                if (scene.id.startsWith("boss")) encounterScreens.add(screen);
                screen.setPresentation(playback);
                if (screen instanceof GameScreen && scene.actions.length > 0)
                    ((GameScreen) screen).setPreparedPresentation(true);
                for (int cue = 0; cue < scene.actions.length; cue++) {
                    DemoAction action = scene.actions[cue];
                    long time = scene.id.startsWith("boss") ? 12000L + cue * 8000L : 8000L + cue * 4000L;
                    playback.at(time, s -> {
                        if (s == screen) ((GameScreen) s).demonstrate(action);
                    });
                }
                Screen active = screen;
                while (active != null) {
                    active.setPresentation(playback);
                    attachDemoServices(active, playback);
                    int result;
                    if (frame == null) {
                        active.initialize();
                        result = active.run();
                    } else result = frame.setScreen(active);
                    if (playback.isComplete()) {
                        if (active instanceof ShipSelectionScreen)
                            selectedShip = ((ShipSelectionScreen) active).getSelectedShip();
                        if (active instanceof InfiniteGameScreen
                                && playback.getNavigation() == DemoControls.Command.NEXT)
                            ((InfiniteGameScreen) active).endPresentationRun();
                        finishRun(active);
                        break;
                    }
                    active = followGameNavigation(active, result);
                    if (active == null) return;
                }
                switch (playback.getNavigation()) {
                case EXIT: return;
                case PREVIOUS: index = Math.max(0, index - 1); break;
                case RESTART: break;
                default: index++; break;
                }
            }
        } finally {
            for (Screen pending : new java.util.ArrayList<>(rewardSubscriptions.keySet())) settleGameplay(pending);
            Core.retryRewards(campaignRewards, endlessRewards);
            records.close();
            AudioManager.stopBGM();
            if (frame != null) frame.dispose();
            Core.setDemoServices(null, null);
            FileManager.setSessionStorageDirectory(null);
        }
    }

    /** Normal game navigation stays available above the presentation footer. */
    private Screen followGameNavigation(Screen previous, int result) {
        if (customScreens.contains(previous)) {
            return new DemoPanelScreen(width, height, FPS, "Custom level finished", () -> java.util.Arrays.asList(
                    "This custom attempt has ended. No official progression, records or Credits were submitted.",
                    "Click Restart to play both waves again, or Next to continue."), new String[0]);
        }
        if (previous instanceof GameScreen) {
            settleGameplay(previous);
            GameScreen game = (GameScreen) previous;
            GameState state = game.getGameState();
            if (game.isStageCleared() && encounterScreens.contains(game)) {
                finishRun(game);
                return new DemoResultsScreen(width, height, FPS, lastState, records.getCurrentRunUnlocks());
            }
            if (game.isStageCleared() && state.getLevel() < CampaignStageData.getTotalStages()) {
                campaign.CampaignManager.updateMaxCampaignStage(state.getLevel() + 1);
                ShipType type = game.getShipType();
                int nextLevel = state.getLevel() + 1;
                return campaignScreen(new GameState(nextLevel, state.getScore(), state.getLivesRemaining(),
                        state.getBulletsShot(), state.getShipsDestroyed()), type,
                        nextLevel % 3 == 0 && state.getLivesRemaining() < runUpgrades.getMaxLives(type));
            }
            finishRun(game);
            GameState results = game.isStageCleared()
                    ? new GameState(state.getLevel() + 1, state.getScore(), state.getLivesRemaining(),
                            state.getBulletsShot(), state.getShipsDestroyed()) : state;
            return new ScoreScreen(width, height, FPS, results, records.getCurrentRunUnlocks());
        }
        if (previous instanceof InfiniteGameScreen) {
            finishRun(previous);
            return new ScoreScreen(width, height, FPS, lastState, true, records.getCurrentRunUnlocks());
        }
        if (previous instanceof ShipSelectionScreen) {
            if (result == ShipSelectionScreen.SHIP_SELECTED)
                selectedShip = ((ShipSelectionScreen) previous).getSelectedShip();
            result = 11;
        } else if (previous instanceof HangarScreen) result = 11;
        else if (previous instanceof TitleScreen && result == 2) result = 14;
        return screenForCode(result);
    }

    private GameScreen campaignScreen(GameState state, ShipType type, boolean bonusLife) {
        return new GameScreen(state, Core.applyDifficulty(CampaignStageData.getStageSettings(state.getLevel()),
                GameplayConfig.getMultiplier()), bonusLife, width, height, FPS, type, run, runUpgrades);
    }

    private Screen screenForCode(int code) {
        switch (code) {
        case 0: return null;
        case 1: return new TitleScreen(width, height, FPS);
        case 14: return new GameModeScreen(width, height, FPS);
        case 2:
            startRun();
            return campaignScreen(new GameState(1, 0, runUpgrades.getMaxLives(selectedShip), 0, 0), selectedShip, false);
        case 3: return new HighScoreScreen(width, height, FPS);
        case 4: return new SettingsScreen(width, height, FPS);
        case 5: return new AudioControlScreen(width, height, FPS);
        case 6: return new ControlsSettingsScreen(width, height, FPS);
        case 7: return new GameplaySettingsScreen(width, height, FPS);
        case 8:
            startRun();
            return new InfiniteGameScreen(new GameState(1, 0, 3, 0, 0), width, height, FPS, run);
        case 9: return new PlayerRecordsScreen(width, height, FPS, records);
        case 10: return new ShopScreen(width, height, FPS, shop);
        case 11: return new HangarMenuScreen(width, height, FPS);
        case 12: return new ShipSelectionScreen(width, height, FPS);
        case 13: return new HangarScreen(width, height, FPS);
        default: throw new IllegalArgumentException("Unknown screen code: " + code);
        }
    }

    private void finishRun(Screen screen) {
        settleGameplay(screen);
        if (customScreens.contains(screen)) return;
        boolean completed = false;
        if (screen instanceof GameScreen) {
            GameScreen game = (GameScreen) screen;
            lastState = game.getGameState();
            completed = game.isStageCleared() || lastState.getLivesRemaining() == 0;
        } else if (screen instanceof InfiniteGameScreen) {
            InfiniteGameScreen game = (InfiniteGameScreen) screen;
            lastState = game.getGameState();
            completed = !game.isAbandoned() && lastState.getLivesRemaining() == 0;
        } else return;
        if (completed) records.completeRun(run.finish(lastState.getScore(),
                lastState.getShipsDestroyed(), System.currentTimeMillis()));
        else records.abandonRun(run.getRunId());
    }
}
