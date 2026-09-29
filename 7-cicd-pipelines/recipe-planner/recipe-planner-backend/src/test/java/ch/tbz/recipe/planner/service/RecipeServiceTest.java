package ch.tbz.recipe.planner.service;

import ch.tbz.recipe.planner.domain.Ingredient;
import ch.tbz.recipe.planner.domain.Recipe;
import ch.tbz.recipe.planner.domain.Unit;
import ch.tbz.recipe.planner.entities.IngredientEntity;
import ch.tbz.recipe.planner.entities.RecipeEntity;
import ch.tbz.recipe.planner.mapper.RecipeEntityMapper;
import ch.tbz.recipe.planner.mapper.RecipeEntityMapperImpl;
import ch.tbz.recipe.planner.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ueber die Aufgabenstellung hinaus, aber fuer Aufgabe 2 wichtig: ohne diese Tests
 * stuende die Service-Schicht im Coverage-Report bei 0 %.
 *
 * <p>Das Repository wird gemockt - der Service laeuft dadurch ohne H2 und ohne
 * Spring-Kontext. Der Mapper wird <b>nicht</b> gemockt: die generierte Implementierung
 * ist reine Kopierlogik ohne Abhaengigkeiten, und mit einem Mapper-Mock wuerde der Test
 * nur beweisen, dass Mockito funktioniert.
 */
@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    private static final UUID REZEPT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID UNBEKANNT = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Mock
    private RecipeRepository repository;

    @Captor
    private ArgumentCaptor<RecipeEntity> gespeicherteEntity;

    private RecipeEntityMapper mapper;
    private RecipeService service;

    private RecipeEntity lasagneEntity;

    @BeforeEach
    void setUp() {
        mapper = new RecipeEntityMapperImpl();
        service = new RecipeService(mapper, repository);

        lasagneEntity = new RecipeEntity(REZEPT_ID, "Lasagne al Forno", "Mit Bechamel",
                "https://example.test/lasagne.jpg",
                List.of(new IngredientEntity(
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        "Tomate", "Die grossen", Unit.PIECE, 5)));
    }

    @Test
    @DisplayName("getRecipes() bildet jede Entity auf ihre Domaenenklasse ab")
    void getRecipesMapsEveryEntity() {
        when(repository.findAll()).thenReturn(List.of(lasagneEntity));

        List<Recipe> ergebnis = service.getRecipes();

        assertThat(ergebnis).hasSize(1);
        assertThat(ergebnis.get(0).getName()).isEqualTo("Lasagne al Forno");
        assertThat(ergebnis.get(0).getIngredients()).hasSize(1);
    }

    @Test
    @DisplayName("getRecipes() liefert eine leere Liste, wenn nichts gespeichert ist")
    void getRecipesReturnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.getRecipes()).isEmpty();
    }

    @Test
    @DisplayName("getRecipeById() liefert das gefundene Rezept")
    void getRecipeByIdReturnsTheRecipe() {
        when(repository.findById(REZEPT_ID)).thenReturn(Optional.of(lasagneEntity));

        Recipe ergebnis = service.getRecipeById(REZEPT_ID);

        assertThat(ergebnis).isNotNull();
        assertThat(ergebnis.getId()).isEqualTo(REZEPT_ID);
    }

    @Test
    @DisplayName("Befund S-01: getRecipeById() liefert bei unbekannter Id null statt Optional.empty()")
    void getRecipeByIdReturnsNullForUnknownId() {
        // repository.findById(...).orElse(null) - das null wandert unveraendert bis in
        // den Controller und wird dort zu "200 mit leerem Body" (siehe Befund C-01).
        when(repository.findById(UNBEKANNT)).thenReturn(Optional.empty());

        assertThat(service.getRecipeById(UNBEKANNT)).isNull();
    }

    @Test
    @DisplayName("getRecipeById() laedt gezielt und nicht die ganze Tabelle")
    void getRecipeByIdDoesNotLoadEverything() {
        when(repository.findById(UNBEKANNT)).thenReturn(Optional.empty());

        service.getRecipeById(UNBEKANNT);

        verify(repository).findById(UNBEKANNT);
        verify(repository, never()).findAll();
    }

    @Test
    @DisplayName("addRecipe() speichert die abgebildete Entity und gibt das Ergebnis zurueck")
    void addRecipeSavesAndReturns() {
        Recipe eingabe = new Recipe(REZEPT_ID, "Lasagne al Forno", "Mit Bechamel",
                "https://example.test/lasagne.jpg",
                List.of(new Ingredient(
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        "Tomate", "Die grossen", Unit.PIECE, 5)));
        when(repository.save(any(RecipeEntity.class))).thenReturn(lasagneEntity);

        Recipe ergebnis = service.addRecipe(eingabe);

        verify(repository).save(gespeicherteEntity.capture());
        assertThat(gespeicherteEntity.getValue().getName()).isEqualTo("Lasagne al Forno");
        assertThat(gespeicherteEntity.getValue().getIngredients()).hasSize(1);
        assertThat(ergebnis.getName()).isEqualTo("Lasagne al Forno");
    }

    @Test
    @DisplayName("Befund S-02: addRecipe() uebernimmt eine mitgeschickte Id ungeprueft")
    void addRecipeKeepsAClientSuppliedId() {
        // Der Client bestimmt den Primaerschluessel. Zusammen mit save() als
        // "insert or update" kann ein Aufrufer damit fremde Rezepte ueberschreiben.
        Recipe mitFremderId = new Recipe(REZEPT_ID, "Pizza", "Untergeschoben", null, List.of());
        when(repository.save(any(RecipeEntity.class))).thenReturn(lasagneEntity);

        service.addRecipe(mitFremderId);

        verify(repository).save(gespeicherteEntity.capture());
        assertThat(gespeicherteEntity.getValue().getId()).isEqualTo(REZEPT_ID);
    }
}
