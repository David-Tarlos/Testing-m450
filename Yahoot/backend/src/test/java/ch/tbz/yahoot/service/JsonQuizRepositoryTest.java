package ch.tbz.yahoot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JsonQuizRepositoryTest {

    @Test
    void loadsTheQuizzesShippedWithTheApp() {
        JsonQuizRepository repository = new JsonQuizRepository("classpath*:quizzes/*.json");

        assertThat(repository.findAll()).isNotEmpty();
        Quiz quiz = repository.findById("allgemeinwissen").orElseThrow();
        assertThat(quiz.title()).isNotBlank();
        assertThat(quiz.questions()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(quiz.questions().getFirst().timeLimit()).isGreaterThan(Duration.ZERO);
    }

    @Test
    void unknownQuizIsEmpty() {
        JsonQuizRepository repository = new JsonQuizRepository("classpath*:quizzes/*.json");

        assertThat(repository.findById("gibts-nicht")).isEmpty();
    }

    @Test
    void brokenQuizStopsTheStartWithAClearMessage() {
        assertThatThrownBy(() -> new JsonQuizRepository("classpath*:quizzes-kaputt/*.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("falsche-loesung.json");
    }
}
