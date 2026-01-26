package dev.addition.randomkits.auction;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.addition.randomkits.economy.Balance;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuctionListing(@NotNull ItemStack item,
                             @NotNull UUID ownerId,
                             @NotNull UUID listingId,
                             @NotNull Balance price,
                             @NotNull Instant listTime,
                             @Nullable Component description) {

    public static class Builder {
        private ItemStack item;
        private UUID ownerId;
        private Balance price;
        private Component description;

        private Builder() {
        }

        public Builder item(@NotNull ItemStack item) {
            this.item = Objects.requireNonNull(item, "Item cannot be null");
            return this;
        }

        public Builder ownerId(@NotNull UUID ownerId) {
            this.ownerId = Objects.requireNonNull(ownerId, "Owner ID cannot be null");
            return this;
        }

        public Builder price(@NotNull Balance price) {
            this.price = Objects.requireNonNull(price, "Price cannot be null");
            return this;
        }

        public Builder description(@Nullable Component description) {
            this.description = description;
            return this;
        }

        public AuctionListing build() {
            Objects.requireNonNull(item, "Item is required");
            Objects.requireNonNull(ownerId, "Owner ID is required");
            Objects.requireNonNull(price, "Price is required");

            if (item.getAmount() <= 0) {
                throw new IllegalArgumentException("Item amount must be positive");
            }

            return new AuctionListing(item, ownerId, UUID.randomUUID(), price, Instant.now(), description);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
