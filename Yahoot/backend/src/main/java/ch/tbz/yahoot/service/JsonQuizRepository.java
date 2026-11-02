package ch.tbz.yahoot.service;

import ch.tbz.yahoot.game.Question;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Liest alle Quiz beim Start aus JSON-Dateien. Der Dateiname ohne Endung ist die ID. Ein fehlerhaftes
 * Quiz bricht den Start ab, damit der Fehler nicht erst mitten im Spiel auffaellt.
 */
@Component
public class JsonQuizRepository implements QuizRepository {

    private final Map<String, Quiz> quizzes = new LinkedHashMap<>();

    @Autowired
    public JsonQuizRepository() {
        this("classpath*:quizzes/*.json");
    }

    JsonQuizRepository(String locationPattern) {
        JsonMapper mapper = JsonMapper.builder().build();
        for (Resource resource : resources(locationPattern)) {
            Quiz quiz = read(mapper, resource);
            quizzes.put(quiz.id(), quiz);
        }
    }

    @Override
    public List<Quiz> findAll() {
        return List.copyOf(quizzes.values());
    }

    @Override
    public Optional<Quiz> findById(String id) {
        return Optional.ofNullable(quizzes.get(id));
    }

    private static List<Resource> resources(String locationPattern) {
        try {
            List<Resource> resources = new ArrayList<>(
                    List.of(new PathMatchingResourcePatternResolver().getResources(locationPattern)));
            resources.sort(Comparator.comparing(Resource::getFilename));
            return resources;
        } catch (IOException e) {
            throw new IllegalStateException("Quiz-Dateien nicht lesbar: " + locationPattern, e);
        }
    }

    private static Quiz read(JsonMapper mapper, Resource resource) {
        String filename = resource.getFilename();
        try (InputStream in = resource.getInputStream()) {
            QuizFile file = mapper.readValue(in, QuizFile.class);
            String id = filename.substring(0, filename.length() - ".json".length());
            List<Question> questions = file.questions().stream()
                    .map(q -> new Question(q.text(), q.options(), q.correct(), Duration.ofSeconds(q.seconds())))
                    .toList();
            return new Quiz(id, file.title(), questions);
        } catch (IOException | JacksonException | IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException("Quiz " + filename + " ist fehlerhaft: " + e.getMessage(), e);
        }
    }

    private record QuizFile(String title, List<QuestionFile> questions) {
    }

    private record QuestionFile(String text, List<String> options, int correct, int seconds) {
    }
}
