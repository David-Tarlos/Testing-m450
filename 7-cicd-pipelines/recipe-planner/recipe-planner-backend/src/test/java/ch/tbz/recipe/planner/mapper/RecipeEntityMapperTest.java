package ch.tbz.recipe.planner.mapper;

import ch.tbz.recipe.planner.domain.Ingredient;
import ch.tbz.recipe.planner.domain.Recipe;
import ch.tbz.recipe.planner.domain.Unit;
import ch.tbz.recipe.planner.entities.IngredientEntity;
import ch.tbz.recipe.planner.entities.RecipeEntity;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Aufgabe 1: "Testen Sie die Mapper Klasse fuer die zwei vorhandenen Domaenen Klassen
 * und benutzen Sie dazu SoftAssertions."
 *
 * <p>Getestet wird die von MapStruct generierte Implementierung
 * {@code RecipeEntityMapperImpl}. Sie hat keine Abhaengigkeiten und laesst sich direkt
 * instanziieren - es braucht weder Spring noch Mockito.
 *
 * <p><b>Warum SoftAssertions?</b> Ein Mapper kopiert viele Felder auf einmal. Mit
 * einzelnen {@code assertEquals} bricht der Test beim ersten abweichenden Feld ab: man
 * repariert, laesst laufen, findet das naechste. SoftAssertions sammeln alle
 * Abweichungen und melden sie gemeinsam - eine Fehlersuche statt fuenf. Genau dafuer
 * sind sie gemacht, und genau dafuer ist ein Mapper der Musterfall. Der Nachweis steht
 * unten in {@link SoftAssertionsNachweis}.
 */
class RecipeEntityMapperTest {

    private static final UUID REZEPT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ZUTAT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private RecipeEntityMapper mapper;

    private RecipeEntity entity;
    private Recipe domain;

    @BeforeEach
    void setUp() {
        mapper = new RecipeEntityMapperImpl();

        IngredientEntity zutatEntity = new IngredientEntity(
                ZUTAT_ID, "Tomate", "Die grossen", Unit.PIECE, 5);
        entity = new RecipeEntity(REZEPT_ID, "Lasagne al Forno", "Mit Bechamel",
                "https://example.test/lasagne.jpg", List.of(zutatEntity));

        Ingredient zutatDomain = new Ingredient(ZUTAT_ID, "Tomate", "Die grossen", Unit.PIECE, 5);
        domain = new Recipe(REZEPT_ID, "Lasagne al Forno", "Mit Bechamel",
                "https://example.test/lasagne.jpg", List.of(zutatDomain));
    }

    @Nested
    @DisplayName("entityToDomain")
    class EntityToDomain {

        @Test
        @DisplayName("uebertraegt alle fuenf Felder des Rezepts")
        void mapsAllRecipeFields() {
            Recipe ergebnis = mapper.entityToDomain(entity);

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(ergebnis.getId()).as("id").isEqualTo(REZEPT_ID);
            softly.assertThat(ergebnis.getName()).as("name").isEqualTo("Lasagne al Forno");
            softly.assertThat(ergebnis.getDescription()).as("description").isEqualTo("Mit Bechamel");
            softly.assertThat(ergebnis.getImageUrl()).as("imageUrl").isEqualTo("https://example.test/lasagne.jpg");
            softly.assertThat(ergebnis.getIngredients()).as("ingredients").hasSize(1);
            softly.assertAll();
        }

        @Test
        @DisplayName("uebertraegt auch die verschachtelte Zutat vollstaendig")
        void mapsTheNestedIngredient() {
            Ingredient ergebnis = mapper.entityToDomain(entity).getIngredients().get(0);

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(ergebnis.getId()).as("zutat.id").isEqualTo(ZUTAT_ID);
            softly.assertThat(ergebnis.getName()).as("zutat.name").isEqualTo("Tomate");
            softly.assertThat(ergebnis.getComment()).as("zutat.comment").isEqualTo("Die grossen");
            softly.assertThat(ergebnis.getUnit()).as("zutat.unit").isEqualTo(Unit.PIECE);
            softly.assertThat(ergebnis.getAmount()).as("zutat.amount").isEqualTo(5);
            softly.assertAll();
        }

        @Test
        @DisplayName("gibt null zurueck, wenn null hineingeht")
        void mapsNullToNull() {
            assertThat(mapper.entityToDomain(null)).isNull();
        }

        @Test
        @DisplayName("behaelt eine leere Zutatenliste als leere Liste")
        void keepsAnEmptyIngredientList() {
            entity.setIngredients(List.of());

            assertThat(mapper.entityToDomain(entity).getIngredients()).isEmpty();
        }

        @Test
        @DisplayName("Befund M-01: null-Zutaten bleiben null statt zu einer leeren Liste zu werden")
        void nullIngredientsStayNull() {
            // MapStruct bildet null auf null ab. Wer die Liste ungeprueft durchlaeuft,
            // faengt sich eine NullPointerException - im Frontend ebenso wie im Backend.
            entity.setIngredients(null);

            assertThat(mapper.entityToDomain(entity).getIngredients()).isNull();
        }
    }

    @Nested
    @DisplayName("domainToEntity")
    class DomainToEntity {

        @Test
        @DisplayName("uebertraegt alle fuenf Felder des Rezepts")
        void mapsAllRecipeFields() {
            RecipeEntity ergebnis = mapper.domainToEntity(domain);

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(ergebnis.getId()).as("id").isEqualTo(REZEPT_ID);
            softly.assertThat(ergebnis.getName()).as("name").isEqualTo("Lasagne al Forno");
            softly.assertThat(ergebnis.getDescription()).as("description").isEqualTo("Mit Bechamel");
            softly.assertThat(ergebnis.getImageUrl()).as("imageUrl").isEqualTo("https://example.test/lasagne.jpg");
            softly.assertThat(ergebnis.getIngredients()).as("ingredients").hasSize(1);
            softly.assertAll();
        }

        @Test
        @DisplayName("gibt null zurueck, wenn null hineingeht")
        void mapsNullToNull() {
            assertThat(mapper.domainToEntity(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Hin- und Rueckabbildung")
    class RoundTrip {

        @Test
        @DisplayName("ueberlebt den Weg Entity -> Domain -> Entity ohne Verlust")
        void survivesTheRoundTrip() {
            RecipeEntity zurueck = mapper.domainToEntity(mapper.entityToDomain(entity));

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(zurueck.getId()).as("id").isEqualTo(entity.getId());
            softly.assertThat(zurueck.getName()).as("name").isEqualTo(entity.getName());
            softly.assertThat(zurueck.getDescription()).as("description").isEqualTo(entity.getDescription());
            softly.assertThat(zurueck.getImageUrl()).as("imageUrl").isEqualTo(entity.getImageUrl());
            softly.assertThat(zurueck.getIngredients()).as("ingredients").hasSameSizeAs(entity.getIngredients());
            softly.assertAll();
        }

        @Test
        @DisplayName("erzeugt ein neues Objekt und reicht nicht dieselbe Referenz durch")
        void createsANewObject() {
            Recipe ergebnis = mapper.entityToDomain(entity);

            assertThat((Object) ergebnis).isNotSameAs(entity);
            assertThat(ergebnis.getIngredients()).isNotSameAs(entity.getIngredients());
        }
    }

    @Nested
    @DisplayName("Nachweis: was SoftAssertions gegenueber assertEquals bringen")
    class SoftAssertionsNachweis {

        /**
         * Belegt den Vorteil, statt ihn zu behaupten: drei Felder sind absichtlich falsch.
         * {@code assertAll()} wirft daraufhin <b>einen</b> Fehler, der <b>alle drei</b>
         * Abweichungen auflistet. Mit einzelnen {@code assertEquals} waere nur die erste
         * sichtbar gewesen.
         */
        @Test
        @DisplayName("assertAll() meldet alle drei Abweichungen auf einmal")
        void collectsEveryFailure() {
            Recipe falsch = new Recipe(
                    UUID.fromString("00000000-0000-0000-0000-000000000000"),  // falsche id
                    "Pizza",                                                  // falscher name
                    "Mit Bechamel",                                           // korrekt
                    "https://example.test/pizza.jpg",                         // falsche imageUrl
                    List.of());

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(falsch.getId()).as("id").isEqualTo(REZEPT_ID);
            softly.assertThat(falsch.getName()).as("name").isEqualTo("Lasagne al Forno");
            softly.assertThat(falsch.getDescription()).as("description").isEqualTo("Mit Bechamel");
            softly.assertThat(falsch.getImageUrl()).as("imageUrl").isEqualTo("https://example.test/lasagne.jpg");

            assertThatThrownBy(softly::assertAll)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Multiple Failures (3 failures)")
                    .hasMessageContaining("[id]")
                    .hasMessageContaining("[name]")
                    .hasMessageContaining("[imageUrl]")
                    // das korrekte Feld taucht nicht auf
                    .hasMessageNotContaining("[description]");
        }
    }
}
