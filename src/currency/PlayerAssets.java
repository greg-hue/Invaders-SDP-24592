package currency;

import java.util.Map;

/**
 * Central facade and unified access point for all in-memory persistent player assets.
 *
 * PlayerAssets coordinates three distinct domain managers:
 * - CurrencyBalance: Owns fungible currency balance and affordability checks.
 * - UnlockManager: Tracks permanent, one-time boolean content unlocks.
 * - InventoryManager: Manages quantity-based consumable item stock.
 *
 * Architectural Invariants and Encapsulation:
 * - Delegation Only: State validation rules are strictly delegated to internal managers;
 *   this class performs no duplicate validation logic.
 * - Access Restrictions: Direct transaction mutators (addCurrency, deductCurrency, unlock,
 *   addItem, removeItem) are package-private so that gameplay screens and external layers
 *   must route transactions through authorized currency services (such as PurchaseSystem
 *   or ItemUseService). As an exception, restore() is public so that game lifecycle code
 *   (such as Core) can initialize and load saved assets from disk.
 * - Local State Isolation: This class handles in-memory runtime assets only. File I/O,
 *   gameplay scoring, active buff durations, and drop probabilities are handled outside this class.
 *
 * @see CurrencyBalance
 * @see UnlockManager
 * @see InventoryManager
 * @see PlayerAssetsState
 */
public class PlayerAssets {
    private CurrencyBalance currencyBalance;
    private UnlockManager unlockManager;
    private InventoryManager inventoryManager;

    /**
     * Creates an empty initial asset state with zero currency, no unlocked content,
     * and an empty inventory.
     */
    public PlayerAssets() {
        this.currencyBalance = new CurrencyBalance();
        this.unlockManager = new UnlockManager();
        this.inventoryManager = new InventoryManager();
    }

    // Public Query Operations

    /**
     * Retrieves the current available currency balance.
     *
     * @return current non-negative currency amount
     */
    public int getBalance() {
        return this.currencyBalance.getBalance();
    }

    /**
     * Checks whether the player holds at least the specified currency amount.
     *
     * @param amount the currency amount to verify; must be strictly positive (greater than 0)
     * @return true if amount is positive and balance is sufficient; false otherwise
     */
    public boolean canAfford(int amount) {
        return this.currencyBalance.canAfford(amount);
    }

    /**
     * Queries whether a specific permanent content item has been unlocked.
     *
     * @param contentId canonical content identifier (such as ship type, stage, or BGM)
     * @return true if unlocked; false if locked, null, or blank
     */
    public boolean isUnlocked(String contentId) {
        return this.unlockManager.isUnlocked(contentId);
    }

    /**
     * Checks whether the player owns at least one unit of the specified consumable item.
     *
     * @param itemId canonical item identifier
     * @return true if owned quantity is at least 1; false otherwise
     */
    public boolean hasItem(String itemId) {
        return this.inventoryManager.has(itemId);
    }

    /**
     * Retrieves the current owned quantity of the specified consumable item.
     *
     * @param itemId canonical item identifier
     * @return owned quantity, or 0 if the item is not owned, null, or blank
     */
    public int getItemQuantity(String itemId) {
        return this.inventoryManager.getQuantity(itemId);
    }

    // Package-Private State Mutation Operations
    // (Restricted to authorized currency-package services)

    /**
     * Credits currency to the player's balance.
     * <p>
     * Rejects non-positive amounts and additions that would result in integer overflow.
     * Restricted to package-private access to prevent unauthorized balance modifications.
     *
     * @param amount currency to credit; must be strictly positive (greater than 0)
     * @return true if credited successfully; false if rejected (balance unchanged)
     */
    boolean addCurrency(int amount) {
        return this.currencyBalance.add(amount);
    }

    /**
     * Deducts currency from the player's balance.
     * <p>
     * Rejects non-positive amounts and attempts to overspend beyond the current balance.
     * Restricted to package-private access to prevent unauthorized deductions.
     *
     * @param amount currency to deduct; must be strictly positive (greater than 0)
     * @return true if deduction succeeded; false if insufficient funds or invalid amount
     */
    boolean deductCurrency(int amount) {
        return this.currencyBalance.deduct(amount);
    }

    /**
     * Permanently unlocks the specified content item.
     * <p>
     * Rejects null/blank IDs and content that is already unlocked.
     * Restricted to package-private access.
     *
     * @param contentId canonical content identifier; must be non-null and non-blank
     * @return true if content transitioned from locked to unlocked; false if already unlocked or invalid
     */
    boolean unlock(String contentId) {
        return this.unlockManager.unlock(contentId);
    }

    /**
     * Increases the owned quantity of a consumable inventory item.
     * <p>
     * Rejects non-positive quantities, invalid IDs, and additions causing integer overflow.
     * Restricted to package-private access.
     *
     * @param itemId canonical item identifier; must be non-null and non-blank
     * @param quantity quantity to add; must be strictly positive (greater than 0)
     * @return true if stock increased; false if rejected (inventory unchanged)
     */
    boolean addItem(String itemId, int quantity) {
        return this.inventoryManager.add(itemId, quantity);
    }

    /**
     * Decreases the owned quantity of a consumable inventory item.
     * <p>
     * If the remaining quantity reaches zero, the entry is removed from internal storage.
     * Rejects requests attempting to remove more units than currently held.
     * Restricted to package-private access.
     *
     * @param itemId canonical item identifier; must be non-null and non-blank
     * @param quantity quantity to remove; must be strictly positive (greater than 0)
     * @return true if removal succeeded; false if insufficient stock or invalid parameters
     */
    boolean removeItem(String itemId, int quantity) {
        return this.inventoryManager.remove(itemId, quantity);
    }

    // Snapshot and Restoration Operations

    /**
     * Captures a point-in-time, immutable snapshot of all current persistent player assets.
     * <p>
     * The snapshot is completely independent of subsequent runtime asset modifications.
     *
     * @return an immutable PlayerAssetsState snapshot
     */
    public PlayerAssetsState snapshot() {
        return new PlayerAssetsState(getBalance(),
                this.unlockManager.copyUnlockedContentIds(),
                this.inventoryManager.copyItemQuantities());
    }

    /**
     * Atomically restores in-memory player assets from a validated snapshot.
     * <p>
     * Atomicity Guarantee: All components of the snapshot are completely validated
     * in temporary structures before mutating runtime state. If any element is corrupted,
     * invalid, or null, the entire restore operation is rejected, leaving current assets unchanged.
     *
     * @param state immutable asset snapshot to restore
     * @return true if state was completely validated and applied; false if state was null or invalid
     */
    public boolean restore(PlayerAssetsState state) {
        if (state == null) {
            return false;
        }

        CurrencyBalance restoredBalance;
        try {
            restoredBalance = new CurrencyBalance(state.getBalance());
        } catch (IllegalArgumentException exception) {
            return false;
        }
        UnlockManager restoredUnlocks = new UnlockManager();
        for (String contentId : state.getUnlockedContentIds()) {
            if (!restoredUnlocks.unlock(contentId)) {
                return false;
            }
        }
        InventoryManager restoredInventory = new InventoryManager();
        for (Map.Entry<String, Integer> item : state.getItemQuantities().entrySet()) {
            if (!restoredInventory.add(item.getKey(), item.getValue())) {
                return false;
            }
        }

        this.currencyBalance = restoredBalance;
        this.unlockManager = restoredUnlocks;
        this.inventoryManager = restoredInventory;
        return true;
    }
}
