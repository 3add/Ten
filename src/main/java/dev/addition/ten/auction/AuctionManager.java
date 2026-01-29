package dev.addition.ten.auction;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;

public class AuctionManager {

    private static final List<UUID> toRemoveListings = new ArrayList<>();
    private static Map<UUID, AuctionListing> listings;

    public static void addListing(AuctionListing listing) {
        listings.put(listing.listingId(), listing);
    }

    public static void removeListing(UUID listingId) {
        listings.remove(listingId);
        toRemoveListings.add(listingId);
    }

    public static @Nullable AuctionListing getListing(UUID listingId) {
        return listings.get(listingId);
    }

    public static @UnmodifiableView Collection<AuctionListing> getListings() {
        return Collections.unmodifiableCollection(listings.values());
    }

    public static @UnmodifiableView Collection<AuctionListing> getListingsBySeller(UUID sellerId) {
        return listings.values().stream()
                .filter(listing -> listing.sellerId().equals(sellerId))
                .toList();
    }

    static void setListings(Map<UUID, AuctionListing> listings) {
        AuctionManager.listings = listings;
    }

    static int cleanUp() {
        if (toRemoveListings.isEmpty()) return 0;

        List<UUID> idsToDelete = new ArrayList<>(toRemoveListings);
        AuctionListingRepository.INSTANCE.deleteBatch(idsToDelete);

        for (UUID listingId : idsToDelete) {
            listings.remove(listingId); // just to be safe
        }

        toRemoveListings.clear();

        return idsToDelete.size();
    }
}