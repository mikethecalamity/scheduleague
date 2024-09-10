package org.scheduleague.domain;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

public class LockedPinningFilter implements PinningFilter<Schedule, Match> {
	
    @Override
    public boolean accept(Schedule schedule, Match match) {
    	return match.isLocked();
    }
}
