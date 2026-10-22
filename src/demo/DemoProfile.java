package demo;

import currency.PlayerAssets;
import currency.PlayerAssetsState;
import currency.RewardSystem;
import engine.Core;
import engine.FileManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import records.RecordsService;
import records.RunRecord;
import records.Achievement;
import records.ProfileSnapshot;
import records.ProfileStore;
import shop.ShopController;
import shop.ShopProduct;
import currency.ShopCatalog.Category;

/** Persistent rehearsal data and explicitly labelled sample products, not a production catalog. */
public final class DemoProfile {
    public static final String SHIELD = "demo.energy-shield";
    public static final String UNLOCK = "demo.permanent-unlock";
    public static final String EXPENSIVE = "demo.unaffordable";
    public final Path directory;
    public final PlayerAssets assets = new PlayerAssets();
    public final RewardSystem rewards = new RewardSystem(assets);
    public RecordsService records;
    public final ShopController shop;
    public final shop.DemoShopFactory.Session shopSession;
    public String message = "Sample catalog. Starting credits are rehearsal data.";

    public DemoProfile(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
        FileManager files = Core.getFileManager();
        Path save = directory.resolve(FileManager.ASSETS_FILENAME);
        if (Files.exists(save)) assets.restore(files.loadPlayerAssets(save));
        else {
            assets.restore(new PlayerAssetsState(1000, Collections.<String>emptySet(),
                    Collections.<String, Integer>emptyMap()));
            files.savePlayerAssets(assets.snapshot(), save);
        }
        shopSession = new shop.DemoShopFactory.Session(assets, files, products());
        shop = shopSession.controller;
        Path recordFile = directory.resolve("records.properties");
        boolean newProfile = !Files.exists(recordFile);
        records = new RecordsService(recordFile);
        if (newProfile) {
            // Prepared ranking baseline; combat milestones remain locked for live demonstration.
            for (int i = 0; i < 10; i++) records.completeRun(new RunRecord(
                    "prepared-history-" + i, i, 0, i * 1000L, System.currentTimeMillis() - 60000 + i));
            while (records.pollNotification() != null) { /* fixture unlocks are not live gameplay notifications */ }
        }
        Path highScores = directory.resolve("scores");
        if (!Files.exists(highScores)) {
            java.util.ArrayList<engine.Score> scores = new java.util.ArrayList<engine.Score>();
            for (int i = 0; i < 7; i++) scores.add(new engine.Score("DEM", 1000000 - i * 1000));
            files.saveHighScores(scores);
        }
    }

    public static List<ShopProduct> products() {
        return Arrays.asList(
                new ShopProduct(SHIELD, "Energy Shield", Category.INVENTORY, 100, "Sample consumable: 15-second shield"),
                new ShopProduct(UNLOCK, "Demo License", Category.UNLOCKABLE, 300, "Sample permanent ownership flag"),
                new ShopProduct(EXPENSIVE, "Luxury Sample", Category.UNLOCKABLE, Integer.MAX_VALUE, "Insufficient-funds example"));
    }

    public void reload() {
        try {
            records.close();
            records = new RecordsService(directory.resolve("records.properties"));
            if (records.isWritesBlocked()) throw new IOException(records.getSaveError());
            if (!assets.restore(Core.getFileManager().loadPlayerAssets(directory.resolve(FileManager.ASSETS_FILENAME))))
                throw new IOException("Invalid saved assets");
            message = "Reloaded from disk: credits, quantities, unlocks and records restored.";
        } catch (IOException exception) { message = "Reload failed: " + exception.getMessage(); }
    }

    public void settle() {
        engine.SettlementResult result = new engine.RewardSettlementCoordinator(
                rewards, assets, Core.getFileManager()).processSettlement();
        message = "Settlement: " + result + ". Retrying a save does not settle new rewards.";
    }

    public void checkRecovery() {
        try {
            // Exercise the real recovery implementation on a separate disposable copy.
            Path target = Files.createTempDirectory(directory, "recovery-check-").resolve("records.properties");
            Files.copy(directory.resolve("records.properties"), target);
            ProfileStore store = new ProfileStore(target);
            ProfileSnapshot snapshot = store.load();
            store.write(snapshot);
            Files.write(target, "prepared corrupt copy".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ProfileSnapshot recovered = store.load();
            message = "Copy recovery: " + recovered.getRecords().size() + " records. " + store.getLoadWarning();
        } catch (IOException exception) { message = "Recovery check failed: " + exception.getMessage(); }
    }

}
