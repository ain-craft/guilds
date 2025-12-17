package org.aincraft.vault.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.aincraft.vault.Vault;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Creates the vault inventory GUI.
 */
public class VaultGUI implements InventoryHolder {
    public static final String INVENTORY_TITLE = "Guild Vault";

    private final Vault vault;
    private final boolean canDeposit;
    private final boolean canWithdraw;
    private final Inventory inventory;

    public VaultGUI(Vault vault, boolean canDeposit, boolean canWithdraw) {
        this.vault = vault;
        this.canDeposit = canDeposit;
        this.canWithdraw = canWithdraw;
        this.inventory = createInventory();
    }

    private Inventory createInventory() {
        Inventory inv = Bukkit.createInventory(this, Vault.STORAGE_SIZE,
                Component.text(INVENTORY_TITLE).color(NamedTextColor.DARK_PURPLE));
        inv.setContents(vault.getContents());
        return inv;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Vault getVault() {
        return vault;
    }

    public boolean canDeposit() {
        return canDeposit;
    }

    public boolean canWithdraw() {
        return canWithdraw;
    }
}
