package dev.addition.ten.auction.inventory;

import dev.addition.ten.auction.AuctionListing;
import dev.addition.ten.auction.AuctionManager;
import dev.addition.ten.util.inventory.CustomListInventory;
import dev.addition.ten.util.inventory.ItemBuilder;
import dev.addition.ten.util.text.FormatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ListingsInventory extends CustomListInventory<AuctionListing> {

    public ListingsInventory(@Nullable Player viewer, @NotNull Supplier<Collection<AuctionListing>> itemSupplier) {
        super(viewer, Rows.SIX, Component.text("My Listings"), itemSupplier, Rows.SIX.getInnerSlots());
        setTransferSlot(53, new ItemBuilder(Material.RED_DYE)
                        .withName(Component.text("Back", NamedTextColor.RED))
                        .build(),
                () -> new AuctionInventory(viewer).getInventory());
    }

    @Override
    protected @NotNull ItemStack mapToItem(@NonNull AuctionListing listing) {
        List<Component> lore = new ArrayList<>();

        lore.add(Component.text("Price: ").append(FormatUtil.formatComponent(listing.price())));
        lore.add(Component.text("Listed on: ").append(FormatUtil.formatComponent(listing.listTime())));

        String description = listing.description();
        if (description != null && !description.isBlank()) {
            lore.add(Component.empty());
            lore.add(Component.text("Description:", NamedTextColor.GRAY));
            lore.add(Component.text(description, NamedTextColor.WHITE));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Click to remove this listing.", NamedTextColor.RED));

        return new ItemBuilder(listing.item())
                .withLore(lore)
                .build();
    }

    @Override
    protected @Nullable Consumer<InventoryClickEvent> mapClick(@NonNull AuctionListing listing) {
        return event -> {;
            AuctionManager.removeListing(listing.listingId());
            event.getWhoClicked().sendMessage(Component.text("Removed listing", NamedTextColor.GREEN));
            updateListedItems();
        };
    }
}
