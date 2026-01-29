package dev.addition.ten.auction;

import dev.addition.ten.economy.Balance;
import dev.addition.ten.util.persistence.BatchQueryBuilder;
import dev.addition.ten.util.persistence.Repository;
import dev.addition.ten.util.persistence.RowMapper;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionListingRepository extends Repository<AuctionListing, String> {

    public static final AuctionListingRepository INSTANCE = new AuctionListingRepository();

    private AuctionListingRepository() {}

    @Override
    protected String getTableName() {
        return "auction_listings";
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS auction_listings (
                listing_uuid VARCHAR(36) PRIMARY KEY,
                item BLOB NOT NULL,
                seller_uuid VARCHAR(36) NOT NULL,
                seller_name VARCHAR(36) NOT NULL,
                price TEXT NOT NULL, -- Changed from TINYTEXT for safety
                description TEXT,
                list_time BIGINT NOT NULL
            )
        """);
    }

    @Override
    protected String getIdColumn() {
        return "listing_uuid";
    }

    @Override
    protected RowMapper<AuctionListing> getRowMapper() {
        return rs -> new AuctionListing(
                UUID.fromString(rs.getString("listing_uuid")),
                ItemStack.deserializeBytes(rs.getBytes("item")),
                UUID.fromString(rs.getString("seller_uuid")),
                rs.getString("seller_name"),
                Balance.fromJson(rs.getString("price")),
                rs.getString("description"),
                Instant.ofEpochMilli(rs.getLong("list_time"))
        );
    }

    @Override
    public void syncMemoryToStorage() {
        BatchQueryBuilder batch = database.batch()
                .insertInto(getTableName(), "listing_uuid", "item", "seller_uuid", "seller_name", "price", "description", "list_time");

        for (AuctionListing listing : AuctionManager.getListings()) {
            batch.addBatch(
                    listing.listingId().toString(),
                    listing.item().serializeAsBytes(),
                    listing.sellerId().toString(),
                    listing.sellerName(),
                    listing.price().toJson(),
                    listing.description(),
                    listing.listTime().toEpochMilli()
            );
        }

        batch.onDuplicateKeyUpdate("item", "seller_uuid", "seller_name", "price", "description", "list_time")
                .execute();

        int fixedListings = AuctionManager.cleanUp();
        LOGGER.info("Saved {} auction listings, synced {} listings (by removing)", AuctionManager.getListings().size(), fixedListings);
    }

    @Override
    public void syncStorageToMemory() {
        Map<UUID, AuctionListing> listings = new HashMap<>();

        for (AuctionListing listing : AuctionListingRepository.INSTANCE.findAll()) {
            listings.put(listing.listingId(), listing);
        }

        LOGGER.info("Loaded {} auction listings", listings.size());
        AuctionManager.setListings(listings);
    }

    void deleteBatch(List<UUID> listingIds) {
        if (listingIds.isEmpty()) return;

        BatchQueryBuilder batch = new BatchQueryBuilder(database)
                .deleteFrom(getTableName(), getIdColumn());

        for (UUID id : listingIds) {
            batch.addBatch(id.toString());
        }

        batch.execute();
    }
}