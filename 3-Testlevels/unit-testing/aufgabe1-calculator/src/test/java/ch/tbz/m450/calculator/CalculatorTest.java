package ch.tbz.m450.calculator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-Tests fuer {@link Calculator}.
 *
 * <p>Diese Klasse zeigt bewusst die wichtigsten JUnit-5-Features:
 * Lifecycle-Annotations, verschachtelte Testklassen, parametrisierte Tests,
 * Gruppen-Assertions und Exception-Tests. Die Erklaerungen dazu stehen in
 * <code>aufgabe2-junit-zusammenfassung.md</code>.</p>
 *
 * @author David Tarlos
 * @version 1.0
 */
@DisplayName("Calculator - Grundoperationen")
@Tag("unit")
class CalculatorTest {

    /** Toleranz fuer Vergleiche von Fliesskommazahlen. */
    private static final double DELTA = 1e-9;

    /** Das Objekt unter Test ("System under Test"). */
    private Calculator calculator;

    @BeforeAll
    static void setUpAll() {
        System.out.println("=== Starte Testklasse CalculatorTest ===");
    }

    @BeforeEach
    void setUp() {
        // Vor JEDEM Test eine frische Instanz -> Tests bleiben unabhaengig.
        calculator = new Calculator();
    }

    @AfterEach
    void tearDown() {
        calculator = null;
    }

    @AfterAll
    static void tearDownAll() {
        System.out.println("=== Testklasse CalculatorTest beendet ===");
    }

    @Test
    @DisplayName("Der Rechner laesst sich instanzieren")
    void testInit() {
        assertNotNull(calculator, "Der Calculator darf nicht null sein");
    }

    @Nested
    @DisplayName("add()")
    class AddTests {

        @Test
        @DisplayName("addiert zwei positive Zahlen")
        void addiertPositiveZahlen() {
            assertEquals(5.0, calculator.add(2.0, 3.0), DELTA);
        }

        @Test
        @DisplayName("addiert negative Zahlen und die Null (Randfaelle)")
        void addiertRandfaelle() {
            // assertAll fuehrt ALLE Assertions aus und meldet alle Fehler auf einmal.
            assertAll("Randfaelle der Addition",
                    () -> assertEquals(-5.0, calculator.add(-2.0, -3.0), DELTA),
                    () -> assertEquals(1.0, calculator.add(-2.0, 3.0), DELTA),
                    () -> assertEquals(3.0, calculator.add(0.0, 3.0), DELTA),
                    () -> assertEquals(0.0, calculator.add(0.0, 0.0), DELTA));
        }

        @Test
        @DisplayName("ist kommutativ: a + b == b + a")
        void istKommutativ() {
            assertEquals(calculator.add(7.5, 2.5), calculator.add(2.5, 7.5), DELTA);
        }

        @ParameterizedTest(name = "{0} + {1} = {2}")
        @CsvSource({
                "2.0,  3.0,  5.0",
                "-1.0, 1.0,  0.0",
                "0.5,  0.25, 0.75",
                "1000000.0, 1.0, 1000001.0"
        })
        @DisplayName("rechnet fuer mehrere Wertepaare korrekt")
        void addiertWertepaare(double a, double b, double erwartet) {
            assertEquals(erwartet, calculator.add(a, b), DELTA);
        }
    }

    @Nested
    @DisplayName("subtract()")
    class SubtractTests {

        @Test
        @DisplayName("subtrahiert zwei positive Zahlen")
        void subtrahiertPositiveZahlen() {
            assertEquals(2.0, calculator.subtract(5.0, 3.0), DELTA);
        }

        @Test
        @DisplayName("liefert ein negatives Resultat, wenn der Subtrahend groesser ist")
        void liefertNegativesResultat() {
            assertTrue(calculator.subtract(3.0, 5.0) < 0);
            assertEquals(-2.0, calculator.subtract(3.0, 5.0), DELTA);
        }

        @ParameterizedTest(name = "{0} - {1} = {2}")
        @CsvSource({
                "5.0,  3.0,  2.0",
                "3.0,  5.0,  -2.0",
                "0.0,  0.0,  0.0",
                "-2.0, -3.0, 1.0"
        })
        void subtrahiertWertepaare(double a, double b, double erwartet) {
            assertEquals(erwartet, calculator.subtract(a, b), DELTA);
        }
    }

    @Nested
    @DisplayName("multiply()")
    class MultiplyTests {

        @Test
        @DisplayName("multipliziert zwei positive Zahlen")
        void multipliziertPositiveZahlen() {
            assertEquals(12.0, calculator.multiply(4.0, 3.0), DELTA);
        }

        @ParameterizedTest(name = "{0} * 0 = 0")
        @ValueSource(doubles = {0.0, 1.0, -1.0, 42.0, 1e6})
        @DisplayName("ergibt mit dem Faktor 0 immer 0")
        void multiplikationMitNull(double faktor) {
            assertEquals(0.0, calculator.multiply(faktor, 0.0), DELTA);
        }

        @Test
        @DisplayName("beachtet die Vorzeichenregeln")
        void beachtetVorzeichen() {
            assertAll("Vorzeichen",
                    () -> assertEquals(-12.0, calculator.multiply(-4.0, 3.0), DELTA),
                    () -> assertEquals(12.0, calculator.multiply(-4.0, -3.0), DELTA));
        }
    }

    @Nested
    @DisplayName("divide()")
    class DivideTests {

        @Test
        @DisplayName("dividiert zwei positive Zahlen")
        void dividiertPositiveZahlen() {
            assertEquals(4.0, calculator.divide(12.0, 3.0), DELTA);
        }

        @Test
        @DisplayName("liefert auch nicht ganzzahlige Resultate")
        void liefertKommaresultat() {
            assertEquals(0.5, calculator.divide(1.0, 2.0), DELTA);
        }

        @Test
        @DisplayName("wirft eine ArithmeticException bei Division durch 0")
        void wirftExceptionBeiNull() {
            ArithmeticException ex = assertThrows(
                    ArithmeticException.class,
                    () -> calculator.divide(10.0, 0.0));

            assertEquals("Division durch 0 ist nicht erlaubt", ex.getMessage());
        }

        @ParameterizedTest(name = "{0} / {1} = {2}")
        @CsvSource({
                "12.0, 3.0,  4.0",
                "-12.0, 3.0, -4.0",
                "12.0, -3.0, -4.0",
                "0.0,  5.0,  0.0"
        })
        void dividiertWertepaare(double a, double b, double erwartet) {
            assertEquals(erwartet, calculator.divide(a, b), DELTA);
        }
    }
}
