package org.scheduleague.solver;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.scheduleague.domain.Inputs.DayTimeSlots;
import org.scheduleague.domain.Inputs.InputMatch;
import org.scheduleague.domain.Match;
import org.scheduleague.domain.Team;
import org.scheduleague.domain.Venue;

import jakarta.enterprise.context.Dependent;

@Dependent
public class PlanningEntityBuilder {

    public List<Match> buildMatches(int weeks, LocalDate startDate, Collection<DayTimeSlots> dayTimeSlots,
            Collection<Venue> venues) {
        final EnumMap<DayOfWeek, List<LocalTime>> dayTimes = new EnumMap<>(DayOfWeek.class);
        dayTimeSlots.forEach(d -> dayTimes.computeIfAbsent(d.day(), k -> new ArrayList<>()).addAll(d.startTimes()));

        final List<Match> matches = new ArrayList<>();

        int id = 0;
        final LocalDate endDate = startDate.plusWeeks(weeks);
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            if (dayTimes.containsKey(date.getDayOfWeek())) {
                final int week = (int) ChronoUnit.WEEKS.between(startDate, date) + 1;
                for (final LocalTime time : dayTimes.get(date.getDayOfWeek())) {
                    for (final Venue venue : venues) {
                        matches.add(new Match(id++, week, date.atTime(time), venue));
                    }
                }
            }
        }

        return matches;
    }

    public List<Match> addInitialState(Collection<Venue> venues, Collection<Team> teams,
            Collection<InputMatch> initialState, List<Match> matches) {
        if (initialState == null || initialState.isEmpty()) {
            return matches;
        }
        
        final Map<MatchKey, Match> allMatches = matches.stream()
                .collect(Collectors.toMap(m -> new MatchKey(m.getDatetime(), m.getVenue()), Function.identity()));

        final Map<Integer, Venue> venueMapper = venues.stream()
                .collect(Collectors.toMap(Venue::id, Function.identity()));
        final Map<Integer, Team> teamMapper = teams.stream().collect(Collectors.toMap(Team::id, Function.identity()));
        for (InputMatch match : initialState) {
            final Venue venue = venueMapper.get(match.venueId());
            final MatchKey key = new MatchKey(match.datetime(), venue);
            allMatches.computeIfPresent(key,
                    (k, v) -> match.toMatch(v.getId(), v.getWeek(), venueMapper::get, teamMapper::get));
        }

        return allMatches.values().stream().sorted().toList();
    }

    private record MatchKey(LocalDateTime datetime, Venue venue) {

    }
}
