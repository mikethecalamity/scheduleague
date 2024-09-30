package org.scheduleague.domain;

import java.time.LocalDateTime;

import org.apache.commons.lang3.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity(pinningFilter = LockedPinningFilter.class)
public class Match implements Comparable<Match> {

	@PlanningId
	private int id;

	private int week;
	private LocalDateTime datetime;

	@JsonIdentityReference
	private Venue venue;

	@JsonUnwrapped
	@PlanningVariable
	private Matchup matchup;

	@JsonIgnore
	private final boolean locked;

	// No-arg constructor required for Timefold
	public Match() {
		this.locked = false;
	}

	public Match(int id, int week, LocalDateTime datetime, Venue venue) {
		this.id = id;
		this.week = week;
		this.datetime = datetime;
		this.venue = venue;
		this.locked = false;
	}

	public Match(int id, int week, LocalDateTime datetime, Venue venue, Team homeTeam, Team awayTeam) {
		this.id = id;
		this.week = week;
		this.datetime = datetime;
		this.venue = venue;
		this.matchup = new Matchup(homeTeam, awayTeam);
		this.locked = true;
	}

	public int getId() {
		return id;
	}

	public int getWeek() {
		return week;
	}

	public LocalDateTime getDatetime() {
		return datetime;
	}

	public Venue getVenue() {
		return venue;
	}

	public Matchup getMatchup() {
		return matchup;
	}

	public void setMatchup(Matchup matchup) {
		this.matchup = matchup;
	}

	public boolean isLocked() {
		return locked;
	}

	public Team getOpponent(Team team) {
		return matchup.getOpponent(team);
	}

	public boolean containsTeam(Team team) {
		return matchup.containsTeam(team);
	}

	public boolean containsTeams(Team team1, Team team2) {
		return matchup.containsTeams(team1, team2);
	}

	public boolean teamsEqual(Match match) {
		return matchup.teamsEqual(match.matchup);
	}

	public boolean anyTeamsEqual(Match match) {
		return matchup.containsTeam(match.matchup.homeTeam()) || matchup.containsTeam(match.matchup.awayTeam());
	}

	public Match lock() {
		return new Match(id, week, datetime, venue, matchup.homeTeam(), matchup.awayTeam());
	}

	@Override
	public int compareTo(Match other) {
		int cmp = ObjectUtils.compare(week, other.week);

		if (cmp == 0) {
			cmp = ObjectUtils.compare(datetime, other.datetime);
		}

		if (cmp == 0) {
			cmp = ObjectUtils.compare(venue, other.venue);
		}

		return cmp;
	}

	@Override
	public String toString() {
		if (matchup == null) {
			return String.format("Week %s: %s - Venue %s", week, datetime, venue);
		}
		return String.format("Week %s: %s - Venue %s - %s vs %s", week, datetime, venue, matchup.homeTeam(), matchup.awayTeam());
	}
}