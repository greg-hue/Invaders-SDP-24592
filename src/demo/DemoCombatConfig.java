package demo;

import entity.Boss;
import shop.ShopController;

/** Opt-in adapters for the presentation only; ordinary game screens leave this null. */
public final class DemoCombatConfig {
    public int movementStage;
    public boolean adaptiveProjectiles;
    public Boss.BossType bossType;
    public int bossHealth = -1;
    public boolean bossFeedback;
    public ShopController purchasedItems;
}
