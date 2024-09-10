package org.scheduleague.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Inputs(@JsonProperty(required = true) int weeks, @JsonProperty(required = true) LocalDate startDate,
		@JsonProperty(required = true) Collection<DayTimeSlots> dayTimeSlots,
		@JsonProperty(required = true) Collection<Venue> venues, @JsonProperty(required = true) Collection<Team> teams,
		Constraints constraints, Collection<InputMatch> initialState) {

	public record DayTimeSlots(@JsonProperty(required = true) DayOfWeek day,
			@JsonProperty(required = true) Collection<LocalTime> startTimes) {
	}

	public record InputMatch(LocalDateTime datetime, int venueId, int homeTeamId, int awayTeamId) {

		public Match toMatch(int id, int week, Function<Integer, Venue> venueMapper,
				Function<Integer, Team> teamMapper) {
			return new Match(id, week, datetime, venueMapper.apply(venueId), teamMapper.apply(homeTeamId),
					teamMapper.apply(awayTeamId));
		}
	}
}
