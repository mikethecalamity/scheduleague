package org.scheduleague.domain;

import org.apache.commons.lang3.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;

//@JsonIdentityInfo(scope = Venue.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public record Venue(@JsonProperty(required = true) @PlanningId int id, @JsonProperty(required = true) String name) implements Comparable<Venue> {
	
	@Override
	public int compareTo(Venue other) {
		return ObjectUtils.compare(id, other.id);
	}
	
	@Override
	public String toString() {
		return name;
	}
}
