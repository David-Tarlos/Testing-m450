package ch.tbz.recipe.planner.mapper;

import ch.tbz.recipe.planner.domain.Ingredient;
import ch.tbz.recipe.planner.domain.Unit;
import ch.tbz.recipe.planner.entities.IngredientEntity;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Die zweite Domaenenklasse aus Aufgabe 1: {@link Ingredient}. Ebenfalls gegen die von
 * MapStruct generierte Implementierung, ebenfalls mit SoftAssertions - hier lohnen sie
 * besonders, weil der Mapper fuenf Felder auf einmal kopiert.
 */
class IngredientEntityMapperTest {

    private static final UUID ZUTAT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private IngredientEntityMapper mapper;

    private IngredientEntity entity;
    private Ingredient domain;

    @BeforeEach
    void setUp() {
        mapper = new IngredientEntityMapperImpl();
        entity = new IngredientEntity(ZUTAT_ID, "Tomate", "Die grossen", Unit.PIECE, 5);
        domain = new Ingredient(ZUTAT_ID, "Tomate", "Die grossen", Unit.PIECE, 5);
    }

    @Test
    @DisplayName("entityToDomain uebertraegt alle fuenf Felder")
    void entityToDomainMapsAllFields() {
        Ingredient ergebnis = mapper.entityToDomain(entity);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(ergebnis.getId()).as("id").isEqualTo(ZUTAT_ID);
        softly.assertThat(ergebnis.getName()).as("name").isEqualTo("Tomate");
        softly.assertThat(ergebnis.getComment()).as("comment").isEqualTo("Die grossen");
        softly.assertThat(ergebnis.getUnit()).as("unit").isEqualTo(Unit.PIECE);
        softly.assertThat(ergebnis.getAmount()).as("amount").isEqualTo(5);
        softly.assertAll();
    }

    @Test
    @DisplayName("domainToEntity uebertraegt alle fuenf Felder")
    void domainToEntityMapsAllFields() {
        IngredientEntity ergebnis = mapper.domainToEntity(domain);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(ergebnis.getId()).as("id").isEqualTo(ZUTAT_ID);
        softly.assertThat(ergebnis.getName()).as("name").isEqualTo("Tomate");
        softly.assertThat(ergebnis.getComment()).as("comment").isEqualTo("Die grossen");
        softly.assertThat(ergebnis.getUnit()).as("unit").isEqualTo(Unit.PIECE);
        softly.assertThat(ergebnis.getAmount()).as("amount").isEqualTo(5);
        softly.assertAll();
    }

    @ParameterizedTest
    @EnumSource(Unit.class)
    @DisplayName("jede Einheit des Unit-Enums wird unveraendert uebertragen")
    void everyUnitSurvivesTheMapping(Unit unit) {
        entity.setUnit(unit);

        assertThat(mapper.entityToDomain(entity).getUnit()).isEqualTo(unit);
    }

    @Test
    @DisplayName("null wird auf null abgebildet, in beide Richtungen")
    void mapsNullToNull() {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(mapper.entityToDomain(null)).as("entityToDomain(null)").isNull();
        softly.assertThat(mapper.domainToEntity(null)).as("domainToEntity(null)").isNull();
        softly.assertThat(mapper.entitiesToDomains(null)).as("entitiesToDomains(null)").isNull();
        softly.assertThat(mapper.domainsToEntities(null)).as("domainsToEntities(null)").isNull();
        softly.assertAll();
    }

    @Test
    @DisplayName("die Listenabbildung behaelt Reihenfolge und Inhalt")
    void listMappingKeepsOrderAndContent() {
        IngredientEntity zweite = new IngredientEntity(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                "Mehl", "Type 550", Unit.GRAMM, 500);

        List<Ingredient> ergebnis = mapper.entitiesToDomains(List.of(entity, zweite));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(ergebnis).as("groesse").hasSize(2);
        softly.assertThat(ergebnis.get(0).getName()).as("erste").isEqualTo("Tomate");
        softly.assertThat(ergebnis.get(1).getName()).as("zweite").isEqualTo("Mehl");
        softly.assertThat(ergebnis.get(1).getUnit()).as("zweite.unit").isEqualTo(Unit.GRAMM);
        softly.assertThat(ergebnis.get(1).getAmount()).as("zweite.amount").isEqualTo(500);
        softly.assertAll();
    }

    @Test
    @DisplayName("eine leere Liste bleibt eine leere Liste")
    void emptyListStaysEmpty() {
        assertThat(mapper.entitiesToDomains(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Befund M-02: amount ist ein primitives int - eine fehlende Menge wird zu 0")
    void missingAmountBecomesZero() {
        // Ein nicht gesetztes amount ist nicht unterscheidbar von einer bewusst
        // eingetragenen 0. Ein Integer (oder eine Validierung) wuerde das trennen.
        IngredientEntity ohneMenge = new IngredientEntity();
        ohneMenge.setName("Salz");

        Ingredient ergebnis = mapper.entityToDomain(ohneMenge);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(ergebnis.getAmount()).as("amount").isZero();
        softly.assertThat(ergebnis.getUnit()).as("unit").isNull();
        softly.assertAll();
    }
}
