package org.scheduleague.domain;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record Matchup(@JsonIdentityReference Team homeTeam, @JsonIdentityReference Team awayTeam) {

	@JsonIgnore
    public Matchup reverse() {
    	return new Matchup(awayTeam, homeTeam);
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
    
    public boolean teamsEqual(Matchup matchup) {
        return containsTeams(matchup.homeTeam, matchup.awayTeam);
    }
}
