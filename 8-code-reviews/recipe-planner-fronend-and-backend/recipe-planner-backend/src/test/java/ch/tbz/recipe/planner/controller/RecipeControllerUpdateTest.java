package ch.tbz.recipe.planner.controller;

import ch.tbz.recipe.planner.domain.Ingredient;
import ch.tbz.recipe.planner.domain.Recipe;
import ch.tbz.recipe.planner.domain.Unit;
import ch.tbz.recipe.planner.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecipeControllerUpdateTest {

    private static final String UPDATED_RECIPE = """
            {
              "name": "Pizza Margherita",
              "description": "Neu",
              "imageUrl": "https://example.com/pizza.jpg",
              "ingredients": [
                {"name": "Mehl", "unit": "GRAMM", "amount": 500},
                {"name": "Tomate", "unit": "PIECE", "amount": 3}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecipeService service;

    @Test
    void updateRecipe_existingId_replacesFieldsAndIngredients() throws Exception {
        Recipe existing = service.addRecipe(new Recipe(null, "Pizza", "Alt", "",
                List.of(new Ingredient(null, "Kaese", null, Unit.GRAMM, 200))));

        mockMvc.perform(put("/api/recipes/{id}", existing.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_RECIPE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId().toString()))
                .andExpect(jsonPath("$.name").value("Pizza Margherita"))
                .andExpect(jsonPath("$.ingredients.length()").value(2));

        Recipe stored = service.getRecipeById(existing.getId());
        assertThat(stored.getName()).isEqualTo("Pizza Margherita");
        assertThat(stored.getIngredients())
                .extracting(Ingredient::getName)
                .containsExactlyInAnyOrder("Mehl", "Tomate");
    }

    @Test
    void updateRecipe_unknownId_returns404() throws Exception {
        mockMvc.perform(put("/api/recipes/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_RECIPE))
                .andExpect(status().isNotFound());
    }
}
