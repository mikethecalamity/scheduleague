package org.scheduleague.solver;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.scheduleague.domain.Constraints;
import org.scheduleague.domain.Match;
import org.scheduleague.domain.Schedule;
import org.scheduleague.domain.Team;
import org.scheduleague.domain.Venue;

import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class ScheduleConstraintProviderTest {

    private static final Team TEAM1 = new Team(1, "Team1");
    private static final Team TEAM2 = new Team(2, "Team2");
    private static final Team TEAM3 = new Team(3, "Team3");
    private static final Team TEAM4 = new Team(4, "Team4");
    private static final Venue VENUE1 = new Venue(1, "Venue1");
    private static final Venue VENUE2 = new Venue(2, "Venue2");

    @Inject
    ConstraintVerifier<ScheduleConstraintProvider, Schedule> constraintVerifier;

    @Test
    void teamConflictTest() {
        final Match match2 = new Match(0, 1, LocalDateTime.of(2024, 9, 9, 9, 0), VENUE1, TEAM2, TEAM3);

        // Check that the team cannot play at the same time (as home team)
        final Match conflictingMatch = new Match(1, 1, LocalDateTime.of(2024, 9, 9, 7, 0), VENUE2, TEAM1, TEAM1);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamMatchConflict).given(conflictingMatch, match2)
                .penalizesBy(BigDecimal.ONE);
    }

    @Test
    void teamDatetimeConflictTest() {
        // Build two initial matches
        final Match match1 = new Match(0, 1, LocalDateTime.of(2024, 9, 9, 7, 0), VENUE1, TEAM1, TEAM2);
        final Match match2 = new Match(2, 1, LocalDateTime.of(2024, 9, 9, 9, 0), VENUE1, TEAM2, TEAM3);

        // Check that the team cannot play at the same time (as home team)
        final Match conflictingMatch2 = new Match(1, 1, LocalDateTime.of(2024, 9, 9, 7, 0), VENUE2, TEAM1, TEAM2);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamDatetimeConflict)
                .given(match1, conflictingMatch2, match2).penalizesBy(BigDecimal.ONE);

        // Check that the team cannot play at the same time (as away team)
        final Match conflictingMatch3 = new Match(1, 1, LocalDateTime.of(2024, 9, 9, 7, 0), VENUE2, TEAM2, TEAM1);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamDatetimeConflict)
                .given(match1, conflictingMatch3, match2).penalizesBy(BigDecimal.ONE);
    }

    @Test
    void teamMaxMatchesPerDayTest() {
        final Match match1 = new Match(0, 1, LocalDateTime.of(2024, 9, 9, 7, 0), VENUE1, TEAM1, TEAM2);
        final Match match2 = new Match(1, 1, LocalDateTime.of(2024, 9, 11, 9, 0), VENUE1, TEAM3, TEAM1);

        // Check that the team can play an unlimited number of times in a day
        final Constraints constraints1 = new Constraints(0, null);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamMaxMatchesPerDay)
                .given(constraints1, TEAM1, TEAM2, TEAM3, match1, match2).penalizesBy(BigDecimal.ZERO);
        
        // Check that the team can play at least once a day
        final Constraints constraints2 = new Constraints(1, null);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamMaxMatchesPerDay)
                .given(constraints1, TEAM1, TEAM2, TEAM3, match1, match2).penalizesBy(BigDecimal.ZERO);

        // Check that the team cannot play more than the specified number of times in a day
        final Match match3 = new Match(2, 1, LocalDateTime.of(2024, 9, 11, 10, 0), VENUE1, TEAM2, TEAM3);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamMaxMatchesPerDay)
                .given(constraints2, TEAM1, TEAM2, TEAM3, match1, match2, match3).penalizesBy(BigDecimal.ONE);
    }
    
    @Test
    void teamOpponentBalancingTest() {
        final Match match1 = new Match(0, 1, LocalDateTime.of(2024, 9, 9, 7, 0), VENUE1, TEAM1, TEAM2);
        final Match match2 = new Match(1, 2, LocalDateTime.of(2024, 9, 16, 7, 0), VENUE1, TEAM3, TEAM1);
        final Match match3 = new Match(2, 3, LocalDateTime.of(2024, 9, 23, 7, 0), VENUE1, TEAM2, TEAM3);

        // Check that the matchups are balanced
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamOpponentBalancing)
                .given(TEAM1, TEAM2, TEAM3, match1, match2, match3).penalizesBy(BigDecimal.ZERO);

        // Check that the matchups are unbalanced
        final Match match4 = new Match(3, 4, LocalDateTime.of(2024, 9, 30, 7, 0), VENUE1, TEAM2, TEAM1);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamOpponentBalancing)
                .given(TEAM1, TEAM2, TEAM3, match1, match2, match3, match4)
                .penalizesByMoreThan(BigDecimal.ZERO);
        
        // Check that the matchups are still unbalanced
        final Match match5 = new Match(4, 5, LocalDateTime.of(2024, 10, 7, 7, 0), VENUE1, TEAM1, TEAM3);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamOpponentBalancing)
                .given(TEAM1, TEAM2, TEAM3, match1, match2, match3, match4, match5)
                .penalizesByMoreThan(BigDecimal.ZERO);
        
        // Check that the matchups are balanced again
        final Match match6 = new Match(4, 5, LocalDateTime.of(2024, 10, 14, 7, 0), VENUE1, TEAM3, TEAM2);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamOpponentBalancing)
                .given(TEAM1, TEAM2, TEAM3, match1, match2, match3, match4, match5, match6)
                .penalizesByMoreThan(BigDecimal.ZERO);
    }

    @Test
    void teamDayBalancingTest() {
        final Constraints constraints = new Constraints(0, null);
        final Match match1 = new Match(0, 1, LocalDateTime.of(2024, 9, 9, 7, 0), VENUE1, TEAM1, TEAM2);
        final Match match2 = new Match(1, 1, LocalDateTime.of(2024, 9, 11, 9, 0), VENUE2, TEAM2, TEAM1);

        // Check that the days the teams play is completely balanced
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamDayBalancing)
                .given(constraints, TEAM1, TEAM2, TEAM3, TEAM4, match1, match2).penalizesBy(BigDecimal.ZERO);

        // Check that the days the teams play is unbalanced
        final Match match3 = new Match(2, 2, LocalDateTime.of(2024, 9, 16, 9, 0), VENUE1, TEAM3, TEAM1);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamDayBalancing)
                .given(constraints, TEAM1, TEAM2, TEAM3, TEAM4, match1, match2, match3)
                .penalizesByMoreThan(BigDecimal.ONE);

        // Check that the days the teams play is still unbalanced
        final Match match4 = new Match(3, 2, LocalDateTime.of(2024, 9, 18, 7, 0), VENUE2, TEAM1, TEAM4);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamDayBalancing)
                .given(constraints, TEAM1, TEAM2, TEAM3, TEAM4, match1, match2, match3, match4)
                .penalizesByMoreThan(BigDecimal.ONE);
        
        // Check that the days the teams play is completely balanced again
        final Match match5 = new Match(4, 3, LocalDateTime.of(2024, 9, 23, 7, 0), VENUE1, TEAM3, TEAM2);
        final Match match6 = new Match(5, 3, LocalDateTime.of(2024, 9, 25, 9, 0), VENUE2, TEAM2, TEAM4);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamDayBalancing)
                .given(constraints, TEAM1, TEAM2, TEAM3, TEAM4, match1, match2, match3, match4, match5, match6)
                .penalizesBy(BigDecimal.ZERO);
    }
    
    @Test
    void teamDayTimeBalancingTest() {
        final Constraints constraints = new Constraints(0, null);
        final Match match1 = new Match(0, 1, LocalDateTime.of(2024, 9, 2, 7, 0), VENUE1, TEAM1, TEAM2);
        final Match match2 = new Match(1, 2, LocalDateTime.of(2024, 9, 9, 9, 0), VENUE2, TEAM2, TEAM1);

        // Check that the times the teams play is completely balanced
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamDayTimeBalancing)
                .given(constraints, TEAM1, TEAM2, TEAM3, TEAM4, match1, match2).penalizesBy(BigDecimal.ZERO);

        // Check that the times the teams play is unbalanced
        final Match match3 = new Match(2, 3, LocalDateTime.of(2024, 9, 11, 9, 0), VENUE1, TEAM3, TEAM1);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamDayTimeBalancing)
                .given(constraints, TEAM1, TEAM2, TEAM3, TEAM4, match1, match2, match3)
                .penalizesByMoreThan(BigDecimal.ONE);

        // Check that the times the teams play is still unbalanced
        final Match match4 = new Match(3, 4, LocalDateTime.of(2024, 9, 16, 7, 0), VENUE2, TEAM1, TEAM4);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamDayTimeBalancing)
                .given(constraints, TEAM1, TEAM2, TEAM3, TEAM4, match1, match2, match3, match4)
                .penalizesByMoreThan(BigDecimal.ONE);
        
        // Check that the times the teams play is completely balanced again
        final Match match5 = new Match(4, 5, LocalDateTime.of(2024, 9, 23, 7, 0), VENUE1, TEAM3, TEAM2);
        final Match match6 = new Match(5, 6, LocalDateTime.of(2024, 9, 30, 9, 0), VENUE2, TEAM2, TEAM4);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamDayTimeBalancing)
                .given(constraints, TEAM1, TEAM2, TEAM3, TEAM4, match1, match2, match3, match4, match5, match6)
                .penalizesBy(BigDecimal.ZERO);
    }
    
    @Test
    void teamVenueBalancingTest() {
        final Constraints constraints = new Constraints(0, null);
        final Match match1 = new Match(0, 1, LocalDateTime.of(2024, 9, 9, 7, 0), VENUE1, TEAM1, TEAM2);
        final Match match2 = new Match(1, 2, LocalDateTime.of(2024, 9, 18, 9, 0), VENUE2, TEAM2, TEAM1);

        // Check that the venues the teams play is completely balanced
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamVenueBalancing)
                .given(constraints, TEAM1, TEAM2, TEAM3, TEAM4, match1, match2).penalizesBy(BigDecimal.ZERO);

        // Check that the venues the teams play is unbalanced
        final Match match3 = new Match(2, 2, LocalDateTime.of(2024, 9, 16, 7, 0), VENUE2, TEAM3, TEAM1);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamVenueBalancing)
                .given(constraints, TEAM1, TEAM2, TEAM3, TEAM4, match1, match2, match3)
                .penalizesByMoreThan(BigDecimal.ONE);

        // Check that the venues the teams play is still unbalanced
        final Match match4 = new Match(3, 3, LocalDateTime.of(2024, 9, 25, 9, 0), VENUE1, TEAM1, TEAM4);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamVenueBalancing)
                .given(constraints, TEAM1, TEAM2, TEAM3, TEAM4, match1, match2, match3, match4)
                .penalizesByMoreThan(BigDecimal.ONE);
        
        // Check that the venues the teams play is completely balanced again
        final Match match5 = new Match(4, 3, LocalDateTime.of(2024, 9, 23, 7, 0), VENUE1, TEAM3, TEAM2);
        final Match match6 = new Match(5, 4, LocalDateTime.of(2024, 10, 1, 9, 0), VENUE2, TEAM2, TEAM4);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::teamVenueBalancing)
                .given(constraints, TEAM1, TEAM2, TEAM3, TEAM4, match1, match2, match3, match4, match5, match6)
                .penalizesBy(BigDecimal.ZERO);
    }
}
