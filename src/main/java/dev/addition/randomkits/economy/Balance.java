package dev.addition.randomkits.economy;

public record Balance(Currency currency, double amount) {

    public static Balance of(Currency currency, double amount) {
        return new Balance(currency, amount);
    }
}
