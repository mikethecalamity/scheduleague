package org.scheduleague.domain;

public record Constraints(int maxMatchPerDay, BalanceConstraint.Order balanceOrder) {
    public Constraints {
        if (balanceOrder == null) {
            balanceOrder = BalanceConstraint.DEFAULT_ORDER;
        }
    }
}
