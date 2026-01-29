package dev.addition.ten.economy;

import dev.addition.ten.util.persistence.serialization.JsonSerializer;
import org.jspecify.annotations.NonNull;

public record Balance(Currency currency, double amount) {

    public static Balance of(Currency currency, double amount) {
        return new Balance(currency, amount);
    }

    public String toJson() {
        return JsonSerializer.getInstance().serialize(this);
    }

    public static @NonNull Balance fromJson(@NonNull String json) {
        return JsonSerializer.getInstance().deserialize(json, Balance.class);
    }

    @Override
    public @NonNull String toString() {
        return "Balance{" +
                "amount=" + amount +
                ", currency=" + currency +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Balance(Currency otherCurrency, double otherAmount))) return false;
        return Double.compare(otherAmount, amount) == 0 && currency.equals(otherCurrency);
    }
}
