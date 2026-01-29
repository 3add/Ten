package dev.addition.ten.economy;

import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import dev.addition.ten.util.persistence.serialization.JsonSerializer;

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;

public class Wallet {

    private static final Type BALANCES_TYPE = new TypeToken<Map<Currency, Double>>() {}.getType();

    private final Map<Currency, Double> balances = new EnumMap<>(Currency.class);

    public Wallet() {
        for (Currency currency : Currency.values()) {
            balances.put(currency, 0.0);
        }
    }

    private Wallet(Map<Currency, Double> balances) {
        for (Currency currency : Currency.values()) {
            this.balances.put(currency, 0.0);
        }
        if (balances != null) this.balances.putAll(balances);
    }

    public void addBalance(@NotNull Balance balance) {
        if (balance.amount() < 0) throw new IllegalArgumentException("Cannot add negative balance");
        balances.merge(balance.currency(), balance.amount(), Double::sum);
    }

    public boolean removeBalance(@NotNull Balance balance) {
        if (balance.amount() < 0) throw new IllegalArgumentException("Cannot remove negative balance");

        double currentAmount = balances.get(balance.currency());
        if (currentAmount >= balance.amount()) {
            balances.put(balance.currency(), currentAmount - balance.amount());
            return true;
        }
        return false;
    }

    public void setBalance(@NotNull Balance balance) {
        if (balance.amount() < 0) throw new IllegalArgumentException("Balance cannot be negative");
        balances.put(balance.currency(), balance.amount());
    }

    public @NotNull Balance getBalance(@NotNull Currency currency) {
        return Balance.of(currency, balances.get(currency));
    }

    public boolean canAfford(@NotNull Balance balance) {
        return balances.get(balance.currency()) >= balance.amount();
    }

    public boolean transferTo(@NotNull Balance balance, @NotNull Wallet targetWallet) {
        if (removeBalance(balance)) {
            targetWallet.addBalance(balance);
            return true;
        }
        return false;
    }

    /**
     * Calculates how much more is needed to afford the given balance.
     * * @param balance The cost to check against
     * @return A Balance representing the deficit (0 if they can afford it)
     */
    public @NotNull Balance getDifference(@NotNull Balance balance) {
        double current = balances.getOrDefault(balance.currency(), 0.0);
        double needed = Math.max(0, balance.amount() - current);
        return Balance.of(balance.currency(), needed);
    }

    public String toJson() {
        return JsonSerializer.getInstance().serialize(balances);
    }

    public static Wallet fromJson(String json) {
        if (json == null || json.isBlank()) return new Wallet();

        Map<Currency, Double> loadedBalances = JsonSerializer.getInstance().deserialize(json, BALANCES_TYPE);
        return new Wallet(loadedBalances);
    }
}