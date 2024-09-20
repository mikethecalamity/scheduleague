package org.scheduleague.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.scheduleague.solver.ScheduleConstraintProvider;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;

@PlanningSolution
public class Schedule {
    
    @ProblemFactProperty
    private Constraints constraints;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private Collection<Team> teams;
    
    @PlanningEntityCollectionProperty
    private List<Match> matches;
    
    private ConstraintWeightOverrides<HardSoftBigDecimalScore> constraintWeightOverrides;
    
    @PlanningScore
    private HardSoftBigDecimalScore score;
    
    // No-arg constructor required for Timefold
    public Schedule() {
    }

    public Schedule(Constraints constraints, Collection<Team> teams, List<Match> matches) {
        this.constraints = constraints;
        this.teams = teams;
        this.matches = matches;
        this.constraintWeightOverrides = ConstraintWeightOverrides.of(//
                Map.of(ScheduleConstraintProvider.TEAM_DAY_BALANCING_CONSTRAINT,
                        constraints.balanceOrder().getWeight(BalanceConstraint.DAY),
                        ScheduleConstraintProvider.TEAM_DAY_TIME_BALANCING_CONSTRAINT,
                        constraints.balanceOrder().getWeight(BalanceConstraint.TIME),
                        ScheduleConstraintProvider.TEAM_VENUE_BALANCING_CONSTRAINT,
                        constraints.balanceOrder().getWeight(BalanceConstraint.VENUE)));
    }

    public Collection<Team> getTeams() {
        return teams;
    }

    public List<Match> getMatches() {
        return matches;
    }
    
    public ConstraintWeightOverrides<HardSoftBigDecimalScore> getConstraintWeightOverrides() {
        return constraintWeightOverrides;
    }
    
    public void setConstraintWeightOverrides(ConstraintWeightOverrides<HardSoftBigDecimalScore> constraintWeightOverrides) {
        this.constraintWeightOverrides = constraintWeightOverrides;
    }

    public HardSoftBigDecimalScore getScore() {
        return score;
    }

    public void setScore(HardSoftBigDecimalScore score) {
        this.score = score;
    }
}
