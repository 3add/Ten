package dev.addition.randomkits.auction;

import java.util.ArrayList;
import java.util.List;

public class AuctionHouse {

    private static final List<AuctionListing> listings = new ArrayList<>();

    public static void list(AuctionListing listing) {
        AuctionHouse.listings.add(listing);
    }

    public static List<AuctionListing> getListings() {
        return AuctionHouse.listings;
    }
}
