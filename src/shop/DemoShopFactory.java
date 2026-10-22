package shop;

import currency.PlayerAssets;
import engine.FileManager;
import java.util.List;
import java.io.IOException;

/** Rehearsal-only, deterministic save failure using the real transaction controller. */
public final class DemoShopFactory {
    private DemoShopFactory() { }
    public static final class Session {
        public final ShopController controller;
        private boolean failNext;
        public Session(PlayerAssets assets, FileManager files, List<ShopProduct> products) {
            controller = new ShopController(assets, files, products, state -> {
                if (failNext) {
                    failNext = false;
                    throw new IOException("Prepared one-shot demo save failure");
                }
                files.savePlayerAssets(state);
            });
        }
        public void failNextSave() { failNext = true; }
    }
}
