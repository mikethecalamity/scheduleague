package org.scheduleague.rest;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.scheduleague.domain.Constraints;
import org.scheduleague.domain.Inputs;
import org.scheduleague.domain.Inputs.DayTimeSlots;
import org.scheduleague.domain.Match;
import org.scheduleague.domain.Team;
import org.scheduleague.domain.Venue;

import com.fasterxml.jackson.databind.type.TypeFactory;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
@TestHTTPEndpoint(Resource.class)
public class ResourceTest {
    
    private static final RestAssuredConfig CONFIG = RestAssured.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.socket.timeout", 600_000)
                    .setParam("http.connection.timeout", 600_000));

    @Test
    public void pingTest() {
        final String response = given().when().get("/ping").then().statusCode(200).extract().body().asString();
        assertThat(response).isEqualTo("ping");
    }

    @Test
    public void generateTestSmall() {
        final int weeks = 3;
        final LocalDate startDate = LocalDate.of(2024, 9, 9);
        final DayTimeSlots dayTimeSlot1 = new DayTimeSlots(DayOfWeek.TUESDAY, List.of(LocalTime.of(20, 0)));
        final List<DayTimeSlots> dayTimeSlots = List.of(dayTimeSlot1);
        final List<Team> teams = buildTeams(3);
        final List<Venue> venues = buildVenues(1);
        final Constraints constraints = new Constraints(1, null);
        final Inputs request = new Inputs(weeks, startDate, dayTimeSlots, venues, teams, constraints, null);
        final Type responseType = TypeFactory.defaultInstance().constructCollectionLikeType(ArrayList.class,
                Match.class);

        final List<Match> response = given().config(CONFIG).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).body(request)
                .when().post("/generate").then().statusCode(200).extract().body().as(responseType);
        
        // Check all date/time/venue combinations
        final LocalDateTime first = LocalDateTime.of(2024, 9, 10, 20, 0);
        for (int i = 0; i < weeks; i ++) {
            final Match match = response.get(i);
            assertThat(match.getId()).isEqualTo(i);
            assertThat(match.getWeek()).isEqualTo(i + 1);
            assertThat(match.getDatetime()).isEqualTo(first.plusWeeks(i));
            assertThat(match.getVenue()).isEqualTo(venues.get(0));
        }

        // Check that all combinations of teams exist
        assertThat(response).satisfiesExactlyInAnyOrder(m -> {
            assertThat(m.containsTeams(teams.get(0), teams.get(1))).isTrue();
        }, m -> {
            assertThat(m.containsTeams(teams.get(0), teams.get(2))).isTrue();
        }, m -> {
            assertThat(m.containsTeams(teams.get(1), teams.get(2))).isTrue();
        });
    }
    
    @Test
    public void generateTest() {
        final int weeks = 5;
        final LocalDate startDate = LocalDate.of(2024, 9, 9);
        final DayTimeSlots dayTimeSlot1 = new DayTimeSlots(DayOfWeek.TUESDAY,
                List.of(LocalTime.of(18, 30), LocalTime.of(20, 0)));
        final DayTimeSlots dayTimeSlot2 = new DayTimeSlots(DayOfWeek.THURSDAY,
                List.of(LocalTime.of(19, 0), LocalTime.of(20, 30)));
        final List<DayTimeSlots> dayTimeSlots = List.of(dayTimeSlot1, dayTimeSlot2);
        final List<Team> teams = buildTeams(9);
        final List<Venue> venues = buildVenues(2);
        final Constraints constraints = new Constraints(1, null);
        final Inputs request = new Inputs(weeks, startDate, dayTimeSlots, venues, teams, constraints, null);
        final Type responseType = TypeFactory.defaultInstance().constructCollectionLikeType(ArrayList.class,
                Match.class);

        final List<Match> response = given().config(CONFIG).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).body(request)
                .when().post("/generate").then().statusCode(200).extract().body().as(responseType);

        assertThat(response).isNotEmpty();
    }
    
    @Test
    public void generateTestInitialState() {
        final int weeks = 5;
        final LocalDate startDate = LocalDate.of(2024, 9, 9);
        final DayTimeSlots dayTimeSlot1 = new DayTimeSlots(DayOfWeek.TUESDAY,
                List.of(LocalTime.of(18, 30), LocalTime.of(20, 0)));
        final DayTimeSlots dayTimeSlot2 = new DayTimeSlots(DayOfWeek.THURSDAY,
                List.of(LocalTime.of(19, 0), LocalTime.of(20, 30)));
        final List<DayTimeSlots> dayTimeSlots = List.of(dayTimeSlot1, dayTimeSlot2);
        final List<Team> teams = buildTeams(9);
        final List<Venue> venues = buildVenues(2);
        final Constraints constraints = new Constraints(1, null);
        final Inputs.InputMatch match1 = new Inputs.InputMatch(LocalDateTime.of(LocalDate.of(2024, 9, 10), LocalTime.of(18, 30)), 1, 1, 2);
        final Inputs.InputMatch match2 = new Inputs.InputMatch(LocalDateTime.of(LocalDate.of(2024, 9, 10), LocalTime.of(18, 30)), 2, 3, 4);
        final Inputs.InputMatch match3 = new Inputs.InputMatch(LocalDateTime.of(LocalDate.of(2024, 9, 10), LocalTime.of(20, 0)), 1, 5, 6);
        final Inputs.InputMatch match4 = new Inputs.InputMatch(LocalDateTime.of(LocalDate.of(2024, 9, 10), LocalTime.of(20, 0)), 2, 7, 8);
        final Inputs request = new Inputs(weeks, startDate, dayTimeSlots, venues, teams, constraints,
                List.of(match1, match2, match3, match4));
        final Type responseType = TypeFactory.defaultInstance().constructCollectionLikeType(ArrayList.class,
                Match.class);

        final List<Match> response = given().config(CONFIG).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).body(request)
                .when().post("/generate").then().statusCode(200).extract().body().as(responseType);

        assertThat(response).isNotEmpty();
    }
    
    @Test
    public void generateTestMyLeague() {
        final int weeks = 12;
        final LocalDate startDate = LocalDate.of(2024, 9, 9);
        final DayTimeSlots dayTimeSlot1 = new DayTimeSlots(DayOfWeek.TUESDAY,
                List.of(LocalTime.of(18, 30), LocalTime.of(20, 0)));
        final List<DayTimeSlots> dayTimeSlots = List.of(dayTimeSlot1);
        final List<Team> teams = buildTeams(8);
        final List<Venue> venues = buildVenues(2);
        final Constraints constraints = new Constraints(1, null);
        final Inputs.InputMatch match1 = new Inputs.InputMatch(LocalDateTime.of(LocalDate.of(2024, 9, 10), LocalTime.of(18, 30)), 1, 1, 2);
        final Inputs.InputMatch match2 = new Inputs.InputMatch(LocalDateTime.of(LocalDate.of(2024, 9, 10), LocalTime.of(18, 30)), 2, 3, 4);
        final Inputs.InputMatch match3 = new Inputs.InputMatch(LocalDateTime.of(LocalDate.of(2024, 9, 10), LocalTime.of(20, 0)), 1, 5, 6);
        final Inputs.InputMatch match4 = new Inputs.InputMatch(LocalDateTime.of(LocalDate.of(2024, 9, 10), LocalTime.of(20, 0)), 2, 7, 8);
        final Inputs request = new Inputs(weeks, startDate, dayTimeSlots, venues, teams, constraints,
                List.of(match1, match2, match3, match4));
        final Type responseType = TypeFactory.defaultInstance().constructCollectionLikeType(ArrayList.class,
                Match.class);

        final List<Match> response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).body(request)
                .when().post("/generate").then().statusCode(200).extract().body().as(responseType);

        assertThat(response).isNotEmpty();
    }

    private List<Team> buildTeams(int num) {
        return IntStream.range(1, num + 1).mapToObj(i -> new Team(i, "Team" + i)).toList();
    }

    private List<Venue> buildVenues(int num) {
        return IntStream.range(1, num + 1).mapToObj(i -> new Venue(i, "Venue" + i)).toList();
    }
}
