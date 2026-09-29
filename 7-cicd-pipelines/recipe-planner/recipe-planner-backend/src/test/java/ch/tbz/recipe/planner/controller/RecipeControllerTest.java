package ch.tbz.recipe.planner.controller;

import ch.tbz.recipe.planner.domain.Ingredient;
import ch.tbz.recipe.planner.domain.Recipe;
import ch.tbz.recipe.planner.domain.Unit;
import ch.tbz.recipe.planner.mapper.RecipeEntityMapper;
import ch.tbz.recipe.planner.service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aufgabe 1: "Testen Sie alle Controller Methoden via MockMvc [...]".
 *
 * <p>{@code @WebMvcTest} startet nur den Web-Layer - kein JPA, keine H2, kein
 * CommandLineRunner. {@link RecipeService} und {@link RecipeEntityMapper} werden mit
 * {@code @MockBean} durch Test Doubles ersetzt und in den Spring-Kontext gestellt.
 * Geprueft wird dadurch ausschliesslich, was der Controller selbst leistet: Routing,
 * Deserialisierung des Request-Body, Statuscodes und die Serialisierung der Antwort.
 *
 * <p>Hinweis zur Annotation: Dieses Projekt laeuft auf Spring Boot 3.2.1. Das
 * neuere {@code @MockitoBean} gibt es erst ab Boot 3.4 / Spring Framework 6.2 -
 * hier ist {@code @MockBean} die korrekte Wahl und noch nicht deprecated.
 */
@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    private static final UUID LASAGNE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SPAGHETTI_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNBEKANNTE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecipeService service;

    @MockBean
    private RecipeEntityMapper mapper;

    /**
     * Befund A-01: eigentlich hat der Web-Layer mit dem Repository nichts zu tun.
     * {@code RecipePlannerApplication} deklariert aber die Seed-Daten als
     * {@code @Bean CommandLineRunner init(RecipeRepository)}. Die Application-Klasse ist
     * zugleich die Spring-Konfiguration und wird auch im {@code @WebMvcTest}-Slice
     * ausgewertet - ohne diesen Mock scheitert der Kontextstart mit
     * "No qualifying bean of type RecipeRepository available".
     */
    @MockBean
    private ch.tbz.recipe.planner.repository.RecipeRepository repository;

    private Recipe lasagne;
    private Recipe spaghetti;

    @BeforeEach
    void setUp() {
        Ingredient tomate = new Ingredient(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Tomate", "Die grossen", Unit.PIECE, 5);
        lasagne = new Recipe(LASAGNE_ID, "Lasagne al Forno", "Mit Bechamel",
                "https://example.test/lasagne.jpg", List.of(tomate));
        spaghetti = new Recipe(SPAGHETTI_ID, "Spaghetti Bolognese", "Klassisch",
                "https://example.test/spaghetti.jpg", List.of());
    }

    // ------------------------------------------------------- GET /api/recipes

    @Test
    @DisplayName("GET /api/recipes antwortet mit 200 und der Liste des Service")
    void getRecipesReturns200WithList() throws Exception {
        when(service.getRecipes()).thenReturn(List.of(lasagne, spaghetti));

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Lasagne al Forno"))
                .andExpect(jsonPath("$[0].ingredients[0].unit").value("PIECE"))
                .andExpect(jsonPath("$[1].name").value("Spaghetti Bolognese"));

        verify(service).getRecipes();
    }

    @Test
    @DisplayName("GET /api/recipes liefert ein leeres Array, wenn nichts gespeichert ist")
    void getRecipesReturnsEmptyArray() throws Exception {
        when(service.getRecipes()).thenReturn(List.of());

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Das JSON eines Rezepts haelt den Contract ein (5 Felder)")
    void recipeJsonKeepsItsContract() throws Exception {
        when(service.getRecipes()).thenReturn(List.of(lasagne));

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(LASAGNE_ID.toString()))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].imageUrl").exists())
                .andExpect(jsonPath("$[0].ingredients").isArray());
    }

    // ------------------------------------- GET /api/recipes/recipe/{recipeId}

    @Test
    @DisplayName("GET /api/recipes/recipe/{id} antwortet mit 200 und dem Rezept")
    void getRecipeReturns200() throws Exception {
        when(service.getRecipeById(LASAGNE_ID)).thenReturn(lasagne);

        mockMvc.perform(get("/api/recipes/recipe/{recipeId}", LASAGNE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lasagne al Forno"))
                .andExpect(jsonPath("$.ingredients.length()").value(1));
    }

    @Test
    @DisplayName("Die Id wird unveraendert an den Service durchgereicht, ohne alles zu laden")
    void getRecipeForwardsTheIdWithoutLoadingEverything() throws Exception {
        when(service.getRecipeById(LASAGNE_ID)).thenReturn(lasagne);

        mockMvc.perform(get("/api/recipes/recipe/{recipeId}", LASAGNE_ID))
                .andExpect(status().isOk());

        verify(service).getRecipeById(LASAGNE_ID);
        verify(service, never()).getRecipes();
    }

    @Test
    @DisplayName("Befund C-01: eine unbekannte Id ergibt 200 mit leerem Body statt 404")
    void unknownIdYields200WithEmptyBody() throws Exception {
        // RecipeService.getRecipeById() gibt bei unbekannter Id null zurueck
        // (repository.findById(...).orElse(null)). Der Controller verpackt das
        // unbesehen in ResponseEntity<>(null, HttpStatus.OK).
        when(service.getRecipeById(UNBEKANNTE_ID)).thenReturn(null);

        mockMvc.perform(get("/api/recipes/recipe/{recipeId}", UNBEKANNTE_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Eine nicht-UUID-foermige Id fuehrt zu 400, der Service wird nicht gerufen")
    void nonUuidIdIsRejectedBeforeReachingTheService() throws Exception {
        mockMvc.perform(get("/api/recipes/recipe/{recipeId}", "keine-uuid"))
                .andExpect(status().isBadRequest());

        verify(service, never()).getRecipeById(any());
    }

    // ------------------------------------------------------ POST /api/recipes

    @Test
    @DisplayName("Befund C-02: POST /api/recipes antwortet mit 200 statt 201 Created")
    void addRecipeReturns200() throws Exception {
        when(service.addRecipe(any(Recipe.class))).thenReturn(lasagne);

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lasagne)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LASAGNE_ID.toString()))
                .andExpect(jsonPath("$.name").value("Lasagne al Forno"));

        verify(service).addRecipe(any(Recipe.class));
    }

    @Test
    @DisplayName("Der deserialisierte Request-Body kommt vollstaendig beim Service an")
    void requestBodyArrivesCompletelyAtTheService() throws Exception {
        when(service.addRecipe(any(Recipe.class))).thenReturn(lasagne);

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lasagne)))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<Recipe> captor =
                org.mockito.ArgumentCaptor.forClass(Recipe.class);
        verify(service).addRecipe(captor.capture());

        Recipe gesendet = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("Lasagne al Forno", gesendet.getName());
        org.junit.jupiter.api.Assertions.assertEquals(1, gesendet.getIngredients().size());
        org.junit.jupiter.api.Assertions.assertEquals(Unit.PIECE, gesendet.getIngredients().get(0).getUnit());
    }

    @Test
    @DisplayName("Befund C-03: ein leeres Rezept wird angenommen - es gibt keine Validierung")
    void emptyRecipeIsAccepted() throws Exception {
        when(service.addRecipe(any(Recipe.class))).thenReturn(new Recipe());

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(service).addRecipe(any(Recipe.class));
    }

    @Test
    @DisplayName("Kaputtes JSON wird mit 400 abgelehnt, der Service wird nicht gerufen")
    void brokenJsonIsRejected() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest());

        verify(service, never()).addRecipe(any());
    }

    @Test
    @DisplayName("Ein falscher Content-Type wird mit 415 abgelehnt")
    void wrongContentTypeIsRejected() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Lasagne"))
                .andExpect(status().isUnsupportedMediaType());

        verify(service, never()).addRecipe(any());
    }

    // ------------------------------------------------------- Routen und CORS

    @Test
    @DisplayName("Nicht implementierte Routen und Methoden werden abgewiesen")
    void unimplementedRoutesAreRejected() throws Exception {
        mockMvc.perform(get("/api/recipes/unbekannt"))
                .andExpect(status().isNotFound());

        // Die API kann nur lesen und anlegen - kein PUT, kein DELETE.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/recipes/recipe/{recipeId}", LASAGNE_ID))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("CORS erlaubt das React-Frontend auf Port 3000, aber keine fremde Origin")
    void corsAllowsOnlyTheFrontend() throws Exception {
        when(service.getRecipes()).thenReturn(List.of());

        mockMvc.perform(get("/api/recipes").header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        mockMvc.perform(get("/api/recipes").header("Origin", "http://evil.example"))
                .andExpect(status().isForbidden());
    }
}
