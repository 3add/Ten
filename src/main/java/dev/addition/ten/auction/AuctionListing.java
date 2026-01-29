package dev.addition.ten.auction;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.addition.ten.economy.Balance;

import java.time.Instant;
import java.util.UUID;

public record AuctionListing(@NotNull UUID listingId,
                             @NotNull ItemStack item,
                             @NotNull UUID sellerId,
                             @NotNull String sellerName,
                             @NotNull Balance price,
                             @Nullable String description,
                             @NotNull Instant listTime) {

    public AuctionListing {
        if (item.isEmpty()) {
            throw new IllegalArgumentException("Auction item cannot be empty. (" + item + ")");
        }
    }
}
