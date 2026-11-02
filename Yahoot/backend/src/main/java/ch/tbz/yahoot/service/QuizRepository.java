package ch.tbz.yahoot.service;

import java.util.List;
import java.util.Optional;

/** Woher die Quiz kommen. Im Test wird diese Schnittstelle gemockt. */
public interface QuizRepository {

    List<Quiz> findAll();

    Optional<Quiz> findById(String id);
}
