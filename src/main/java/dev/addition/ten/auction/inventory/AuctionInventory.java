package dev.addition.ten.auction.inventory;

import dev.addition.ten.auction.AuctionListing;
import dev.addition.ten.auction.AuctionManager;
import dev.addition.ten.economy.Balance;
import dev.addition.ten.economy.Wallet;
import dev.addition.ten.player.PlayerManager;
import dev.addition.ten.util.inventory.ItemBuilder;
import dev.addition.ten.util.inventory.CustomListInventory;
import dev.addition.ten.util.text.FormatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AuctionInventory extends CustomListInventory<AuctionListing> {

    public AuctionInventory(Player viewer) {
        super(viewer, Rows.SIX, Component.text("Auction Listings"), AuctionManager::getListings, Rows.SIX.getInnerSlots());
        setTransferSlot(53, new ItemBuilder(Material.CHEST)
                .withName(Component.text("My Listings").color(TextColor.color(0xFFD76E)))
                .build(),
                () -> new ListingsInventory(viewer, () -> AuctionManager.getListingsBySeller(viewer.getUniqueId())).getInventory());
    }

    @Override
    protected @NotNull ItemStack mapToItem(@NonNull AuctionListing listing) {
        List<Component> lore = new ArrayList<>();

        Wallet wallet = PlayerManager.getOnlinePlayer(getViewer()).getData().getWallet();
        boolean canAfford = wallet.canAfford(listing.price());
        Balance missingBalance = canAfford ? null : wallet.getDifference(listing.price());

        lore.add(Component.text("Price: ").append(FormatUtil.formatComponent(listing.price())));
        lore.add(Component.text("Listed on: ").append(FormatUtil.formatComponent(listing.listTime())));
        lore.add(Component.text("Seller: ").append(Component.text(listing.sellerName(), NamedTextColor.YELLOW)));

        String description = listing.description();
        if (description != null && !description.isBlank()) {
            lore.add(Component.empty());
            lore.add(Component.text("Description:", NamedTextColor.GRAY));
            lore.add(Component.text(description, NamedTextColor.WHITE));
        }

        lore.add(Component.empty());

        if (canAfford) {
            lore.add(Component.text("Click to purchase this item.", NamedTextColor.GREEN));
        } else {
            lore.add(Component.text("You don't have enough ", NamedTextColor.RED).append(listing.price().currency().getColoredName()));
            lore.add(Component.text("You need ", NamedTextColor.RED).append(FormatUtil.formatComponent(missingBalance)).append(Component.text(" more", NamedTextColor.RED)));
        }

        return new ItemBuilder(listing.item())
                .withLore(lore)
                .withEnchantedStated(canAfford)
                .build();
    }

    @Override
    protected @Nullable Consumer<InventoryClickEvent> mapClick(@NonNull AuctionListing listing) {
        return event -> {
            Player buyer = (Player) event.getWhoClicked();
            Wallet buyerWallet = PlayerManager.getOnlinePlayer(buyer).getData().getWallet();

            if (AuctionManager.getListing(listing.listingId()) == null) {
                buyer.sendMessage(Component.text("This item has already been sold.", NamedTextColor.RED));
                updateListedItems();
                return;
            }

            if (buyer.getUniqueId().equals(listing.sellerId())) {
                buyer.sendMessage(Component.text("You cannot purchase your own listing.", NamedTextColor.RED));
                return;
            }

            if (!buyerWallet.canAfford(listing.price())) {
                buyer.sendMessage(Component.text("You cannot afford this item. You need ", NamedTextColor.RED)
                        .append(FormatUtil.formatComponent(buyerWallet.getDifference(listing.price()))).append(Component.text(" more", NamedTextColor.RED)));
                return;
            }

            PlayerManager.loadPlayerAsync(listing.sellerId()).thenAccept(optionalSeller -> {
                if (optionalSeller.isEmpty()) {
                    throw new IllegalStateException("Player " + listing.sellerId() + " not found");
                }

                Wallet sellerWallet = optionalSeller.get().getData().getWallet();
                buyerWallet.transferTo(listing.price(), sellerWallet);

                AuctionManager.removeListing(listing.listingId());

                buyer.sendMessage(Component.text("You have purchased the item!", NamedTextColor.GREEN));
                updateListedItems();
            });
        };
    }
}
