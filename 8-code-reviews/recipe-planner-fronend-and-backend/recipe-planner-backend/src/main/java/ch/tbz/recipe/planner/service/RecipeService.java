package ch.tbz.recipe.planner.service;

import ch.tbz.recipe.planner.entities.IngredientEntity;
import ch.tbz.recipe.planner.entities.RecipeEntity;
import ch.tbz.recipe.planner.mapper.IngredientEntityMapper;
import ch.tbz.recipe.planner.mapper.RecipeEntityMapper;
import ch.tbz.recipe.planner.repository.RecipeRepository;
import ch.tbz.recipe.planner.domain.Ingredient;
import ch.tbz.recipe.planner.domain.Recipe;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RecipeService {

    private final RecipeEntityMapper mapper;

    private final IngredientEntityMapper ingredientMapper;

    private final RecipeRepository repository;

    public RecipeService(RecipeEntityMapper mapper, IngredientEntityMapper ingredientMapper, RecipeRepository repository) {
        this.mapper = mapper;
        this.ingredientMapper = ingredientMapper;
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

    @Transactional
    public Recipe updateRecipe(UUID recipeId, Recipe recipe) {
        RecipeEntity entity = repository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));

        entity.setName(recipe.getName());
        entity.setDescription(recipe.getDescription());
        entity.setImageUrl(recipe.getImageUrl());
        replaceIngredients(entity, recipe.getIngredients());

        return mapper.entityToDomain(repository.save(entity));
    }

    // The ingredient list is replaced as a whole: orphanRemoval deletes the rows that are gone.
    // The ids are cleared so the remaining ingredients are stored as new rows instead of
    // colliding with the ones that are being deleted in the same transaction.
    private void replaceIngredients(RecipeEntity entity, List<Ingredient> ingredients) {
        List<IngredientEntity> newIngredients =
                ingredientMapper.domainsToEntities(ingredients == null ? List.of() : ingredients);
        newIngredients.forEach(ingredient -> ingredient.setId(null));

        entity.getIngredients().clear();
        entity.getIngredients().addAll(newIngredients);
    }
}
