package org.scheduleague.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scheduleague.domain.Inputs.DayTimeSlots;
import org.scheduleague.domain.Inputs.InputMatch;
import org.scheduleague.domain.Match;
import org.scheduleague.domain.Team;
import org.scheduleague.domain.Venue;

@ExtendWith(MockitoExtension.class)
public class PlanningEntityBuilderTest {

    @InjectMocks
    PlanningEntityBuilder builder;

    @Test
    public void buildMatchTimesTest() {
        final int weeks = 5;
        final LocalDate startDate = LocalDate.of(2024, 9, 9);
        final DayTimeSlots dayTimeSlot1 = new DayTimeSlots(DayOfWeek.TUESDAY,
                List.of(LocalTime.of(18, 30), LocalTime.of(20, 0)));
        final DayTimeSlots dayTimeSlot2 = new DayTimeSlots(DayOfWeek.THURSDAY,
                List.of(LocalTime.of(19, 0), LocalTime.of(20, 30)));
        final List<DayTimeSlots> dayTimeSlots = List.of(dayTimeSlot1, dayTimeSlot2);
        final Collection<Venue> venues = buildVenues(2);

        final List<Match> matches = builder.buildMatches(weeks, startDate, dayTimeSlots, venues);

        final int numDayTimeSlots = dayTimeSlots.stream().reduce(0, (i, d) -> i + d.startTimes().size(), Integer::sum);
        final ListAssert<Match> assertElements = assertThat(matches).hasSize(weeks * numDayTimeSlots * venues.size())
                .isSorted();

        int index = 0;
        final Iterable<Integer> weekIter = () -> IntStream.range(1, weeks).iterator();
        for (final int week : weekIter) {
            final Iterable<LocalDate> dateIter = () -> startDate.plusWeeks(week - 1)
                    .datesUntil(startDate.plusWeeks(week), Period.ofDays(1)).iterator();
            for (final LocalDate date : dateIter) {
                final Collection<LocalTime> times;
                if (date.getDayOfWeek() == DayOfWeek.TUESDAY) {
                    times = dayTimeSlot1.startTimes();
                } else if (date.getDayOfWeek() == DayOfWeek.THURSDAY) {
                    times = dayTimeSlot2.startTimes();
                } else {
                    continue;
                }

                if (date.getDayOfWeek() == DayOfWeek.TUESDAY || date.getDayOfWeek() == DayOfWeek.THURSDAY) {
                    for (final LocalTime time : times) {
                        for (final Venue venue : venues) {
                            final int i = index++;
                            assertElements.element(i).satisfies(m -> {
                                assertThat(m.getId()).isEqualTo(i);
                                assertThat(m.getWeek()).isEqualTo(week);
                                assertThat(m.getDatetime()).isEqualTo(date.atTime(time));
                                assertThat(m.getVenue()).isEqualTo(venue);
                            });
                        }
                    }
                }
            }
        }
    }

    @Test
    public void addInitialStateTest() {
        final LocalDate date = LocalDate.of(2024, 9, 9);
        final LocalTime time1 = LocalTime.of(18, 30);
        final LocalTime time2 = LocalTime.of(20, 0);
        final Venue venue = new Venue(1, "Venue");
        final Team team1 = new Team(1, "Team1");
        final Team team2 = new Team(2, "Team2");
        final Match emptyMatch1 = new Match(0, 1, date.atTime(time1), venue);
        final Match emptyMatch2 = new Match(1, 1, date.atTime(time2), venue);
        final InputMatch initialMatch = new InputMatch(date.atTime(time2), venue.id(), team2.id(), team1.id());

        final List<Match> matches = builder.addInitialState(List.of(venue), List.of(team1, team2),
                List.of(initialMatch), List.of(emptyMatch1, emptyMatch2));

        assertThat(matches).satisfiesExactly(m -> {
            assertThat(m).isSameAs(emptyMatch1);
        }, m -> {
            assertThat(m.getId()).isEqualTo(emptyMatch2.getId());
            assertThat(m.getWeek()).isEqualTo(emptyMatch2.getWeek());
            assertThat(m.getVenue()).isSameAs(venue);
            assertThat(m.getMatchup().homeTeam()).isSameAs(team2);
            assertThat(m.getMatchup().awayTeam()).isSameAs(team1);
        });
    }
    
    @Test
    public void addInitialStateTestEmpty() {
        final LocalDate date = LocalDate.of(2024, 9, 9);
        final LocalTime time1 = LocalTime.of(18, 30);
        final LocalTime time2 = LocalTime.of(20, 0);
        final Venue venue = new Venue(1, "Venue");
        final Team team1 = new Team(1, "Team1");
        final Team team2 = new Team(2, "Team2");
        final Match emptyMatch1 = new Match(0, 1, date.atTime(time1), venue);
        final Match emptyMatch2 = new Match(1, 1, date.atTime(time2), venue);

        final List<Match> matches = builder.addInitialState(List.of(venue), List.of(team1, team2),
                List.of(), List.of(emptyMatch1, emptyMatch2));

        assertThat(matches).containsExactly(emptyMatch1, emptyMatch2);
    }

    private List<Venue> buildVenues(int num) {
        return IntStream.range(1, num + 1).mapToObj(i -> new Venue(i, "Venue" + i)).toList();
    }
}
