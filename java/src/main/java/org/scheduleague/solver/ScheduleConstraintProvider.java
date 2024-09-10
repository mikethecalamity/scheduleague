package org.scheduleague.solver;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.scheduleague.domain.Constraints;
import org.scheduleague.domain.Match;
import org.scheduleague.domain.Team;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;

public class ScheduleConstraintProvider implements ConstraintProvider {
	
	// Hard constraints
	public static final String TEAM_MATCH_CONSTRAINT = "Team match conflict";
	public static final String TEAM_DATETIME_CONSTRAINT = "Team datetime conflict";
	public static final String TEAM_MAX_MATCHES_PER_DAY_CONSTRAINT = "Team maximum matches per day";
	// Soft constraints
	public static final String MATCHUP_REPEAT_CONSTRAINT = "Matchup repeat";
	public static final String MATCHUP_SEPARATION_CONSTRAINT = "Matchup separation";
	public static final String TEAM_OPPONENT_BALANCING_CONSTRAINT = "Team opponent balancing";
	public static final String TEAM_DAY_BALANCING_CONSTRAINT = "Team day balancing";
	public static final String TEAM_DAY_TIME_BALANCING_CONSTRAINT = "Team day+time balancing";
	public static final String TEAM_VENUE_BALANCING_CONSTRAINT = "Team venue balancing";

	@Override
	public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
        		teamMatchConflict(constraintFactory),
        		teamDatetimeConflict(constraintFactory),
        		teamMaxMatchesPerDay(constraintFactory),
                // Soft constraints
        		matchupRepeat(constraintFactory),
        		//teamOpponentBalancing(constraintFactory),
        		//maxMatchupSeparation(constraintFactory),
        		//teamDayBalancing(constraintFactory),
        		//teamDayTimeBalancing(constraintFactory),
        		//teamVenueBalancing(constraintFactory)
        };
	}
	
	Constraint teamMatchConflict(ConstraintFactory constraintFactory) {
		// A team cannot play itself
		return constraintFactory
				.forEach(Match.class)
				.filter(m -> m.getHomeTeam().equals(m.getAwayTeam()))
				// penalize with a hard weight
				.penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD)
				.asConstraint(TEAM_MATCH_CONSTRAINT);
	}
	
	Constraint teamDatetimeConflict(ConstraintFactory constraintFactory) {
		// A team cannot play at the same date/time
		return constraintFactory
				// Select each pair of 2 matches
				.forEachUniquePair(Match.class,
						Joiners.equal(Match::getDatetime),
						Joiners.filtering((m1, m2) -> m1.getHomeTeam().equals(m2.getHomeTeam())
								|| m1.getHomeTeam().equals(m2.getAwayTeam())
								|| m1.getAwayTeam().equals(m2.getHomeTeam())
								|| m1.getAwayTeam().equals(m2.getAwayTeam())))
				// penalize with a hard weight
				.penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD)
				.asConstraint(TEAM_DATETIME_CONSTRAINT);
	}
	
	Constraint teamMaxMatchesPerDay(ConstraintFactory constraintFactory) {
		// A team is not allowed to play more than the alloted matches per day
		return constraintFactory
				.forEach(Match.class)
				.join(Match.class, Joiners.equal(m -> m.getDatetime().toLocalDate()))
				.join(Team.class, Joiners.filtering((m1, m2, t) -> m1.containsTeam(t) && m2.containsTeam(t)))
				.groupBy(ConstraintCollectors.countTri())
				.join(Constraints.class)
				.filter((i, c) -> c.maxMatchPerDay() > 0 && i > c.maxMatchPerDay())
				// penalize with a hard weight
				.penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD)
				.asConstraint(TEAM_MAX_MATCHES_PER_DAY_CONSTRAINT);
	}
	
//	Constraint teamMaxByeSeparation(ConstraintFactory constraintFactory) {
//		// A team should prefer byes further from the previous
//		return constraintFactory
//				.forEach(Team.class)
//				.join(Match.class, Joiners.filtering((t, m) ->  m.containsTeam(t)))
//				.groupBy((t, m) -> m.getWeek(), ConstraintCollectors.countBi())
//				.filter((w, i) -> i == 0)
//				.
//				.rewardBigDecimal(HardSoftBigDecimalScore.ONE_SOFT, (w1, w2) -> BigDecimal.valueOf(w1 - w2).abs())
//				.asConstraint("Team bye separation");
//	}
	
	Constraint matchupRepeat(ConstraintFactory constraintFactory) {
		// A matchup should be repeated as few times as possible
		return constraintFactory
				.forEachUniquePair(Match.class, Joiners.filtering(Match::teamsEqual))
				// penalize with a soft weight
				.penalizeBigDecimal(HardSoftBigDecimalScore.ofSoft(BigDecimal.valueOf(100_000L)))
				.asConstraint(MATCHUP_REPEAT_CONSTRAINT);
	}
	
	Constraint teamOpponentBalancing(ConstraintFactory constraintFactory) {
		// A team should be balanced across opponents
		return constraintFactory
				.forEach(Team.class)
				.join(Match.class, Joiners.filtering((t, m) ->  m.containsTeam(t)))
				.groupBy(ConstraintCollectors.loadBalance((t, m) -> m.getOpponent(t)))
				// penalize with a soft weight
				.penalizeBigDecimal(HardSoftBigDecimalScore.ofSoft(BigDecimal.valueOf(10_000L)), LoadBalance::unfairness)
				.asConstraint(TEAM_OPPONENT_BALANCING_CONSTRAINT);
	}
	
	Constraint matchupSeparation(ConstraintFactory constraintFactory) {
		// A matchup between 2 specific teams should prefer reoccurring further from the previous 
		return constraintFactory
				.forEachUniquePair(Match.class, Joiners.filtering(Match::teamsEqual))
				// reward matches with the same matchups that are further apart
				.penalizeBigDecimal(HardSoftBigDecimalScore.ofSoft(BigDecimal.valueOf(1_000L)), (m1, m2) -> {
					final long separationDays = m1.getDatetime().until(m2.getDatetime(), ChronoUnit.DAYS);
					return BigDecimal.valueOf(1.0 - (separationDays * 0.01));
				})
				.asConstraint(MATCHUP_SEPARATION_CONSTRAINT);
	}
	
	Constraint teamDayBalancing(ConstraintFactory constraintFactory) {
		// A team should be balanced across days
		return constraintFactory
				.forEach(Team.class)
				.join(Match.class, Joiners.filtering((t, m) ->  m.containsTeam(t)))
				.groupBy(ConstraintCollectors.loadBalance((t, m) -> m.getDatetime().getDayOfWeek()))
				// penalize with a soft weight
				.penalizeBigDecimal(HardSoftBigDecimalScore.ONE_SOFT, LoadBalance::unfairness)
				.asConstraint(TEAM_DAY_BALANCING_CONSTRAINT);
	}
	
	Constraint teamDayTimeBalancing(ConstraintFactory constraintFactory) {
		// A team should be balanced across day/time pairs
		return constraintFactory
				.forEach(Team.class)
				.join(Match.class, Joiners.filtering((t, m) ->  m.containsTeam(t)))
				.groupBy(ConstraintCollectors.loadBalance((t, m) -> new DayTime(m)))
				// penalize with a soft weight
				.penalizeBigDecimal(HardSoftBigDecimalScore.ONE_SOFT, LoadBalance::unfairness)
				.asConstraint(TEAM_DAY_TIME_BALANCING_CONSTRAINT);
	}
	
	Constraint teamVenueBalancing(ConstraintFactory constraintFactory) {
		// A team should be balanced across venues
		return constraintFactory
				.forEach(Team.class)
				.join(Match.class, Joiners.filtering((t, m) ->  m.containsTeam(t)))
				.groupBy(ConstraintCollectors.loadBalance((t, m) -> m.getVenue()))
				// penalize with a soft weight
				.penalizeBigDecimal(HardSoftBigDecimalScore.ONE_SOFT, LoadBalance::unfairness)
				.asConstraint(TEAM_VENUE_BALANCING_CONSTRAINT);
	}
	
	private record DayTime(DayOfWeek day, LocalTime time) {
		public DayTime(Match match) {
			this(match.getDatetime().getDayOfWeek(), match.getDatetime().toLocalTime());
		}
	}
}
