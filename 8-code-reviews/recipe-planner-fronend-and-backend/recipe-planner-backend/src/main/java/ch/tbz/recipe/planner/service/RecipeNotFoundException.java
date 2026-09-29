package ch.tbz.recipe.planner.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecipeNotFoundException extends RuntimeException {

    public RecipeNotFoundException(UUID recipeId) {
        super("Recipe %s does not exist".formatted(recipeId));
    }
}
