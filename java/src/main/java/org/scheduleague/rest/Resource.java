package org.scheduleague.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.scheduleague.domain.Inputs;
import org.scheduleague.domain.Match;
import org.scheduleague.domain.Matchup;
import org.scheduleague.domain.Schedule;
import org.scheduleague.solver.PlanningEntityBuilder;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("")
@Tag(name = "League Scheduler", description = "League scheduler service assigning team matchups to venues and timeslots.")
public class Resource {

    @Inject
    PlanningEntityBuilder planningEntityBuilder;

    @Inject
    SolverManager<Schedule, UUID> solverManager;

    @Inject
    SolutionManager<Schedule, HardSoftBigDecimalScore> solutionManager;

    @POST
    @Path("generate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Submit schedule inputs and constraints to generate a schedule.")
    @RequestBody(description = "The schedule inputs", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Inputs.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "The matches with assigned teams, venues, and timeslots.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = Match.class)))
    })
    public List<Match> generate(Inputs inputs) throws InterruptedException, ExecutionException {
    	final List<Matchup> matchups1 = planningEntityBuilder.buildMatchups(inputs.teams());
    	final List<Matchup> matchups2 = matchups1.stream().map(Matchup::reverse).toList();
        final List<Match> emptyMatches = planningEntityBuilder.buildMatches(inputs.weeks(), inputs.startDate(),
                inputs.dayTimeSlots(), inputs.venues());
        final List<Match> unscheduledMatches = planningEntityBuilder.addInitialState(inputs.venues(), inputs.teams(),
                inputs.initialState(), emptyMatches);

        final List<List<Match>> partitions = ListUtils.partition(unscheduledMatches, matchups1.size());    
        
        List<Match> scheduledMatches = new ArrayList<>();
        for (int i = 0; i < partitions.size(); i++) {
        	final List<Match> partition = partitions.get(i);
        	if (partition.stream().allMatch(Match::isLocked)) {
        		scheduledMatches = partition;
        		continue;
        	}
        	
        	final List<Match> matches = Stream.concat(scheduledMatches.stream(), partition.stream()).toList();
        	final List<Matchup> matchups = i % 2 == 0 ? matchups1 : matchups2;
	        final Schedule schedule = new Schedule(inputs.constraints(), inputs.teams(), matchups, matches);
	
	        final SolverJob<Schedule, UUID> job = solverManager.solve(UUID.randomUUID(), schedule);
	
	        scheduledMatches = job.getFinalBestSolution().getMatches().stream().map(Match::lock).toList();;
        }
        return scheduledMatches;
    }
}