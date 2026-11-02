package ch.tbz.recipe.planner.service;

import ch.tbz.recipe.planner.entities.RecipeEntity;
import ch.tbz.recipe.planner.mapper.RecipeEntityMapper;
import ch.tbz.recipe.planner.repository.RecipeRepository;
import ch.tbz.recipe.planner.domain.Recipe;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecipeService {

    RecipeEntityMapper mapper;

    RecipeRepository repository;

    public RecipeService(RecipeEntityMapper mapper, RecipeRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }

    public List<Recipe> getRecipes() {
        List<RecipeEntity> recipesEntities = repository.findAll();
        return recipesEntities.stream().map(mapper::entityToDomain).toList();
    }

    public Recipe getRecipeById(UUID recipeId) {
        return mapper.entityToDomain(repository.findById(recipeId).orElse(null));
    }

    public Recipe addRecipe(Recipe recipe) {
        var createdRecipe = repository.save(mapper.domainToEntity(recipe));
        return mapper.entityToDomain(createdRecipe);
    }

    /**
     * Ersetzt Name, Beschreibung, Bild und die komplette Zutatenliste eines Rezepts.
     * Die Id kommt aus dem Pfad, eine Id im Body wird ignoriert.
     * Zutaten werden neu angelegt, auch wenn sie im Body eine Id haben.
     *
     * @return das gespeicherte Rezept oder Optional.empty(), wenn es die Id nicht gibt
     */
    @Transactional
    public Optional<Recipe> updateRecipe(UUID recipeId, Recipe recipe) {
        return repository.findById(recipeId).map(entity -> {
            RecipeEntity changes = mapper.domainToEntity(recipe);
            entity.setName(changes.getName());
            entity.setDescription(changes.getDescription());
            entity.setImageUrl(changes.getImageUrl());

            // Liste nicht ersetzen, sondern leeren und fuellen: orphanRemoval braucht dieselbe Collection
            entity.getIngredients().clear();
            if (changes.getIngredients() != null) {
                changes.getIngredients().forEach(ingredient -> ingredient.setId(null));
                entity.getIngredients().addAll(changes.getIngredients());
            }
            return mapper.entityToDomain(repository.save(entity));
        });
    }
}
