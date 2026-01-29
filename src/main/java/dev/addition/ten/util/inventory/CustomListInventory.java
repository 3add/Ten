package dev.addition.ten.util.inventory;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class CustomListInventory<T> extends CustomInventory {

    private static final ItemStack PREVIOUS_PAGE_ITEM = new ItemBuilder(Material.ARROW)
            .withName(Component.text("Previous Page"))
            .build();
    private static final ItemStack NEXT_PAGE_ITEM = new ItemBuilder(Material.ARROW)
            .withName(Component.text("Next Page"))
            .build();

    private final int PREVIOUS_PAGE_SLOT = getInventory().getSize() - 6;
    private final int NEXT_PAGE_SLOT = getInventory().getSize() - 4;
    private final @Nullable Supplier<Collection<T>> itemSupplier;
    private final List<Integer> pagedSlots;
    private List<T> items;
    private int page = 0;

    public CustomListInventory(@NotNull Rows rows, @NotNull Component inventoryTitle, @NotNull Collection<T> items) {
        this(null, rows, inventoryTitle, items, rows.getAllSlots());
    }

    public CustomListInventory(@Nullable Player viewer, @NotNull Rows rows, @NotNull Component inventoryTitle, @NotNull Collection<T> items) {
        this(viewer, rows, inventoryTitle, items, rows.getAllSlots());
    }

    public CustomListInventory(@Nullable Player viewer, @NotNull Rows rows, @NotNull Component inventoryTitle, @NotNull Collection<T> items, @NotNull LinkedHashSet<Integer> pagedSlots) {
        super(rows, inventoryTitle, viewer);
        this.itemSupplier = null;
        this.items = new ArrayList<>(items); // turn to regular list to create an ordered collection
        this.pagedSlots = new ArrayList<>(pagedSlots); // turn to regular list to create an ordered collection

        updatePagedItems(); // display on page 0
    }

    public CustomListInventory(@Nullable Player viewer, @NotNull Rows rows, @NotNull Component inventoryTitle, @NotNull Supplier<Collection<T>> itemSupplier) {
        this(viewer, rows, inventoryTitle, itemSupplier, rows.getAllSlots());
    }

    public CustomListInventory(@Nullable Player viewer, @NotNull Rows rows, @NotNull Component inventoryTitle, @NotNull Supplier<Collection<T>> itemSupplier, @NotNull LinkedHashSet<Integer> pagedSlots) {
        super(rows, inventoryTitle, viewer);
        this.itemSupplier = itemSupplier;
        this.items = new ArrayList<>(itemSupplier.get()); // turn to regular list to create an ordered collection
        this.pagedSlots = new ArrayList<>(pagedSlots); // turn to regular list to create an ordered collection

        updatePagedItems(); // display on page 0
    }

    protected abstract @NotNull ItemStack mapToItem(@NotNull T object);

    protected abstract @Nullable Consumer<InventoryClickEvent> mapClick(@NotNull T object);

    /**
     * Refreshes the item entries from the supplier and updates the inventory display.
     */
    public void updateListedItems() {
        if (itemSupplier == null) throw new IllegalStateException("Can't refresh items without an item supplier!");

        this.items = new ArrayList<>(itemSupplier.get());

        // Safety check: if items were removed and our current page is now empty
        int maxPage = Math.max(0, (int) Math.ceil((double) items.size() / pagedSlots.size()) - 1);
        if (page > maxPage) {
            page = maxPage;
        }

        updatePagedItems();
    }

    /**
     * Updates the inventory to display the current page of items.
     * (doesn't refresh item entries [see {@link #updateListedItems()}])
     */
    protected void updatePagedItems() {
        clearPagedSlots();

        int itemsPerPage = pagedSlots.size();
        int startIndex = page * itemsPerPage;

        for (int i = 0; i < pagedSlots.size(); i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= items.size()) break;

            T itemObject = items.get(itemIndex);
            ItemStack stack = mapToItem(itemObject);
            Consumer<InventoryClickEvent> click = mapClick(itemObject);

            setSlot(pagedSlots.get(i), stack, click);
        }

        updatePageButtons();
    }

    private void clearPagedSlots() {
        for (Integer slot : pagedSlots) {
            setSlot(slot, null, null);
        }
    }

    private void updatePageButtons() {
        if (page > 0)
            setSlot(PREVIOUS_PAGE_SLOT, PREVIOUS_PAGE_ITEM, event -> previousPage());
        else
            setSlot(PREVIOUS_PAGE_SLOT, PLACEHOLDER, null);

        int itemsPerPage = pagedSlots.size();
        boolean hasNextPage = (page + 1) * itemsPerPage < items.size();

        if (hasNextPage)
            setSlot(NEXT_PAGE_SLOT, NEXT_PAGE_ITEM, event -> nextPage());
        else
            setSlot(NEXT_PAGE_SLOT, PLACEHOLDER, null);
    }

    public void nextPage() {
        int itemsPerPage = pagedSlots.size();
        if ((page + 1) * itemsPerPage < items.size()) {
            page++;
            updatePagedItems();
        }
    }

    public void previousPage() {
        if (page > 0) {
            page--;
            updatePagedItems();
        }
    }
}
