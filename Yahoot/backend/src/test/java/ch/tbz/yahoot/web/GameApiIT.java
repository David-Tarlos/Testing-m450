package ch.tbz.yahoot.web;

import static org.assertj.core.api.Assertions.assertThat;

import ch.tbz.yahoot.web.StompTestClient.Inbox;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameApiIT {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    private HttpClient http;
    private StompTestClient host;
    private StompTestClient phone;

    @BeforeEach
    void setUp() throws Exception {
        http = HttpClient.newHttpClient();
        host = StompTestClient.connect(port);
        phone = StompTestClient.connect(port);
    }

    @AfterEach
    void tearDown() {
        host.close();
        phone.close();
        http.close();
    }

    @Test
    void quizzesAreListed() throws Exception {
        HttpResponse<String> response = get("/api/quizzes");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"id\":\"allgemeinwissen\"", "\"questions\":6");
    }

    @Test
    void wholeRoundRunsOverWebSocket() throws Exception {
        Map<String, Object> game = createGame("test-langsam");
        String code = (String) game.get("code");
        String topic = "/topic/games/" + code;

        Inbox phoneEvents = phone.subscribe(topic);
        Inbox answers = phone.subscribe("/user/queue/answers");
        phone.send("/app/games/" + code + "/join", Map.of("name", "Lena"));
        assertThat(phoneEvents.next("PlayerJoined").get("players")).isEqualTo(List.of("Lena"));

        host.send("/app/games/" + code + "/start", Map.of("hostToken", game.get("hostToken")));
        Map<String, Object> question = phoneEvents.next("QuestionStarted");
        assertThat(question).containsEntry("text", "2 + 2?").containsEntry("number", 1)
                .doesNotContainKey("correctOption");

        phone.send("/app/games/" + code + "/answer", Map.of("option", 1));
        assertThat(answers.next()).containsEntry("result", "ACCEPTED");

        host.send("/app/games/" + code + "/close", Map.of("hostToken", game.get("hostToken")));
        Map<String, Object> reveal = phoneEvents.next("QuestionClosed");
        assertThat(reveal).containsEntry("correctOption", 1);
        @SuppressWarnings("unchecked")
        Map<String, Object> lena = ((List<Map<String, Object>>) reveal.get("ranking")).getFirst();
        assertThat(lena).containsEntry("name", "Lena");
        assertThat((Integer) lena.get("score")).isGreaterThan(500);
    }

    @Test
    void timerClosesTheQuestionWithoutTheHost() throws Exception {
        Map<String, Object> game = createGame("test-schnell");
        String code = (String) game.get("code");
        Inbox events = phone.subscribe("/topic/games/" + code);
        phone.send("/app/games/" + code + "/join", Map.of("name", "Tim"));
        events.next("PlayerJoined");

        host.send("/app/games/" + code + "/start", Map.of("hostToken", game.get("hostToken")));
        events.next("QuestionStarted");

        assertThat(events.next("QuestionClosed")).containsEntry("number", 1);
        assertThat(events.next()).containsEntry("type", "GameFinished");
    }

    @Test
    void playerCanRejoinWithTheTokenAfterLosingTheConnection() throws Exception {
        Map<String, Object> game = createGame("test-langsam");
        String code = (String) game.get("code");
        Inbox events = host.subscribe("/topic/games/" + code);
        Inbox joined = phone.subscribe("/user/queue/joined");
        phone.send("/app/games/" + code + "/join", Map.of("name", "Lena"));
        String token = (String) joined.next().get("playerToken");
        events.next("PlayerJoined");
        host.send("/app/games/" + code + "/start", Map.of("hostToken", game.get("hostToken")));
        events.next("QuestionStarted");

        phone.close();
        phone = StompTestClient.connect(port);
        Inbox rejoined = phone.subscribe("/user/queue/joined");
        Inbox answers = phone.subscribe("/user/queue/answers");
        phone.send("/app/games/" + code + "/rejoin", Map.of("playerToken", token));

        assertThat(rejoined.next()).containsEntry("name", "Lena").containsEntry("playerToken", token);
        phone.send("/app/games/" + code + "/answer", Map.of("option", 1));
        assertThat(answers.next()).containsEntry("result", "ACCEPTED");
    }

    @Test
    void rejoinWithAWrongTokenIsRefused() throws Exception {
        String code = (String) createGame("test-langsam").get("code");
        Inbox errors = phone.subscribe("/user/queue/errors");

        phone.send("/app/games/" + code + "/rejoin", Map.of("playerToken", "geraten"));

        assertThat(errors.next().get("message")).asString().contains("wieder verbinden");
    }

    @Test
    void tokenAndPlayerOfOneGameAreNotValidInAnother() throws Exception {
        String first = (String) createGame("test-langsam").get("code");
        Map<String, Object> second = createGame("test-langsam");
        String secondCode = (String) second.get("code");
        Inbox joined = phone.subscribe("/user/queue/joined");
        Inbox errors = phone.subscribe("/user/queue/errors");
        phone.send("/app/games/" + first + "/join", Map.of("name", "Lena"));
        String token = (String) joined.next().get("playerToken");

        phone.send("/app/games/" + secondCode + "/answer", Map.of("option", 1));
        assertThat(errors.next().get("message")).asString().contains("noch nicht beigetreten");

        phone.send("/app/games/" + secondCode + "/rejoin", Map.of("playerToken", token));
        assertThat(errors.next().get("message")).asString().contains("wieder verbinden");
    }

    @Test
    void playedCardIsConfirmedToThePlayer() throws Exception {
        Map<String, Object> game = createGame("test-langsam");
        String code = (String) game.get("code");
        Map<String, Object> hostCommand = Map.of("hostToken", game.get("hostToken"));
        Inbox events = host.subscribe("/topic/games/" + code);
        Inbox cards = phone.subscribe("/user/queue/cards");
        phone.send("/app/games/" + code + "/join", Map.of("name", "Lena"));
        events.next("PlayerJoined");
        host.send("/app/games/" + code + "/join", Map.of("name", "Tim"));
        events.next("PlayerJoined");
        host.send("/app/games/" + code + "/start", hostCommand);
        events.next("QuestionStarted");
        host.send("/app/games/" + code + "/close", hostCommand);
        events.next("QuestionClosed");
        host.send("/app/games/" + code + "/sabotage", hostCommand);
        events.next("SabotageOpened");

        phone.send("/app/games/" + code + "/card", Map.of("card", "FREEZE", "target", "Tim"));

        assertThat(cards.next()).containsEntry("card", "FREEZE").containsEntry("target", "Tim");
    }

    @Test
    void stateContainsTheOpenQuestionWithoutTheSolution() throws Exception {
        Map<String, Object> game = createGame("test-langsam");
        String code = (String) game.get("code");
        Inbox events = host.subscribe("/topic/games/" + code);
        phone.send("/app/games/" + code + "/join", Map.of("name", "Lena"));
        events.next("PlayerJoined");
        host.send("/app/games/" + code + "/start", Map.of("hostToken", game.get("hostToken")));
        events.next("QuestionStarted");

        assertThat(get("/api/games/" + code).body())
                .contains("\"phase\":\"QUESTION\"", "\"text\":\"2 + 2?\"", "\"correctOption\":null", "\"secondsLeft\"");
    }

    @Test
    void onlyTheHostCanStartTheGame() throws Exception {
        String code = (String) createGame("test-langsam").get("code");
        Inbox errors = phone.subscribe("/user/queue/errors");
        phone.send("/app/games/" + code + "/join", Map.of("name", "Lena"));

        phone.send("/app/games/" + code + "/start", Map.of("hostToken", "geraten"));

        assertThat(errors.next().get("message")).asString().contains("Host");
        assertThat(get("/api/games/" + code).body()).contains("\"phase\":\"LOBBY\"");
    }

    @Test
    void ruleViolationsGoOnlyToTheSender() throws Exception {
        String code = (String) createGame("test-langsam").get("code");
        Inbox phoneErrors = phone.subscribe("/user/queue/errors");
        Inbox hostErrors = host.subscribe("/user/queue/errors");
        phone.send("/app/games/" + code + "/join", Map.of("name", "Lena"));

        phone.send("/app/games/" + code + "/join", Map.of("name", "   "));

        assertThat(phoneErrors.next().get("message")).asString().contains("leer");
        assertThat(hostErrors.isEmptyFor(300)).isTrue();
    }

    @Test
    void unknownGameIsNotFound() throws Exception {
        assertThat(get("/api/games/ZZZZZZ").statusCode()).isEqualTo(404);

        Inbox errors = phone.subscribe("/user/queue/errors");
        phone.send("/app/games/ZZZZZZ/join", Map.of("name", "Lena"));
        assertThat(errors.next().get("message")).asString().contains("ZZZZZZ");
    }

    @Test
    void unknownQuizIsNotFound() throws Exception {
        assertThat(post("/api/games", "{\"quizId\":\"gibts-nicht\"}").statusCode()).isEqualTo(404);
    }

    private Map<String, Object> createGame(String quizId) throws Exception {
        HttpResponse<String> response = post("/api/games", "{\"quizId\":\"" + quizId + "\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        @SuppressWarnings("unchecked")
        Map<String, Object> game = JSON.readValue(response.body(), Map.class);
        return game;
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
