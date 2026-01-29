package dev.addition.ten.util.inventory;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class CustomInventoryListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        if (inventory == null) return;

        if (!(inventory.getHolder() instanceof CustomInventory customInventory))
            return;

        customInventory.handleSlotClickEvent(event);
    }
}
