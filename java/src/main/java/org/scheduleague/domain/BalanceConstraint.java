package org.scheduleague.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;

public enum BalanceConstraint {
	DAY,
	TIME,
	VENUE;
	
	public static final Order DEFAULT_ORDER = Order.of(DAY, TIME, VENUE);
	
	public static class Order extends ArrayList<BalanceConstraint> {
		
		private static final long serialVersionUID = 1L;
		
		public static Order of(BalanceConstraint... constraints) {
			final Order order = new Order();
			Stream.of(constraints).forEach(order::add);
			return order;
		}

		public HardSoftBigDecimalScore getWeight(BalanceConstraint constraint) {
			if (indexOf(constraint) < 0) {
				return HardSoftBigDecimalScore.ZERO;
			}
			
			final int rank = size() - indexOf(constraint);
			return HardSoftBigDecimalScore.ofSoft(BigDecimal.valueOf(rank * 10));
		}
	}
}
