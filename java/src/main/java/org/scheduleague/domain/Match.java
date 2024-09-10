package org.scheduleague.domain;

import java.time.LocalDateTime;

import org.apache.commons.lang3.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

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

	@JsonIdentityReference
	@PlanningVariable
	private Team homeTeam;

	@JsonIdentityReference
	@PlanningVariable
	private Team awayTeam;

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
		this.homeTeam = homeTeam;
		this.awayTeam = awayTeam;
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

	public Team getHomeTeam() {
		return homeTeam;
	}

	public void setHomeTeam(Team homeTeam) {
		this.homeTeam = homeTeam;
	}

	public Team getAwayTeam() {
		return awayTeam;
	}

	public void setAwayTeam(Team awayTeam) {
		this.awayTeam = awayTeam;
	}

	public boolean isLocked() {
		return locked;
	}
	
	public Team getOpponent(Team team) {
		if (homeTeam.equals(team)) {
			return awayTeam;
		}
		else if (awayTeam.equals(team)) {
			return homeTeam;
		}
		return null;
	}

	public boolean containsTeam(Team team) {
		return homeTeam.equals(team) || awayTeam.equals(team);
	}

	public boolean containsTeams(Team team1, Team team2) {
		return (homeTeam.equals(team1) && awayTeam.equals(team2)) || (homeTeam.equals(team2) && awayTeam.equals(team1));
	}
	
	public boolean teamsEqual(Match match) {
		return containsTeams(match.homeTeam, match.awayTeam);
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
		return String.format("Week %s: %s - Venue %s -  %s vs %s", week, datetime, venue, homeTeam, awayTeam);
	}
}