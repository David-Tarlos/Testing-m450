package ch.tbz.m450.util;

import ch.tbz.m450.repository.Address;
import ch.tbz.m450.util.AddressComparator.SortField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests zu Aufgabe 1 (Comparator korrekt implementieren) und Aufgabe 2 (Erweiterung
 * auf zusaetzliche Attribute). Ohne Spring und ohne Datenbank - reines JUnit.
 */
class AddressComparatorTest {

    private Address meierAnna;
    private Address meierBeat;
    private Address aebiZoe;
    private List<Address> unsorted;

    private static Address address(int id, String firstname, String lastname, String phone, long epochMillis) {
        return new Address(id, firstname, lastname, phone, new Date(epochMillis));
    }

    private static List<String> lastnames(List<Address> addresses) {
        return addresses.stream().map(Address::getLastname).toList();
    }

    private static List<Integer> ids(List<Address> addresses) {
        return addresses.stream().map(Address::getId).toList();
    }

    @BeforeEach
    void setUp() {
        meierAnna = address(3, "Anna", "Meier", "079 300 00 00", 3_000L);
        meierBeat = address(1, "Beat", "Meier", "079 100 00 00", 1_000L);
        aebiZoe = address(2, "Zoe", "Aebi", "079 200 00 00", 2_000L);
        unsorted = new ArrayList<>(List.of(meierAnna, meierBeat, aebiZoe));
    }

    @Nested
    @DisplayName("Vertrag von java.util.Comparator")
    class ContractTests {

        @Test
        @DisplayName("Ein Element mit sich selbst verglichen ergibt 0")
        void comparingAnElementWithItselfIsZero() {
            // Genau dieser Test scheitert an der Vorgabe, die konstant -1 lieferte.
            assertEquals(0, new AddressComparator().compare(meierAnna, meierAnna));
        }

        @Test
        @DisplayName("compare(a,b) und compare(b,a) haben umgekehrte Vorzeichen")
        void comparisonIsAntisymmetric() {
            AddressComparator comparator = new AddressComparator();

            int forward = comparator.compare(meierAnna, aebiZoe);
            int backward = comparator.compare(aebiZoe, meierAnna);

            assertTrue(forward > 0, "Meier steht nach Aebi");
            assertTrue(backward < 0, "Aebi steht vor Meier");
            assertEquals(Integer.signum(forward), -Integer.signum(backward));
        }

        @Test
        @DisplayName("Die Sortierung ist transitiv")
        void comparisonIsTransitive() {
            AddressComparator comparator = new AddressComparator();

            assertTrue(comparator.compare(aebiZoe, meierBeat) < 0);
            assertTrue(comparator.compare(meierBeat, meierAnna) > 0, "Beat steht nach Anna");
            assertTrue(comparator.compare(aebiZoe, meierAnna) < 0, "also steht Aebi auch vor Meier Anna");
        }

        @Test
        @DisplayName("Sortieren dreht die Liste nicht einfach um, sondern wertet die Daten aus")
        void sortingActuallyLooksAtTheData() {
            // Regressionstest gegen den urspruenglichen Fehler: die Vorgabe lieferte die
            // Liste in umgekehrter Einfuegereihenfolge zurueck, unabhaengig vom Inhalt.
            List<Address> sorted = unsorted.stream().sorted(new AddressComparator()).toList();
            List<Address> reversedInput = new ArrayList<>(unsorted);
            Collections.reverse(reversedInput);

            assertEquals(List.of("Aebi", "Meier", "Meier"), lastnames(sorted));
            assertFalse(ids(sorted).equals(ids(reversedInput)), "darf nicht bloss die umgekehrte Eingabe sein");
        }

        @Test
        @DisplayName("Auch 100 Elemente werden korrekt sortiert")
        void sortsALargeListWithoutLosingElements() {
            List<Address> many = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                many.add(address(i, "Vorname", String.format("Name%03d", 99 - i), "079", i));
            }

            List<Address> sorted = many.stream().sorted(new AddressComparator()).toList();

            assertEquals(100, sorted.size());
            assertEquals("Name000", sorted.get(0).getLastname());
            assertEquals("Name099", sorted.get(99).getLastname());
        }
    }

    @Nested
    @DisplayName("Standardreihenfolge Nachname, Vorname, Id")
    class DefaultOrderTests {

        @Test
        @DisplayName("Sortiert primaer nach Nachname")
        void sortsByLastname() {
            List<Address> sorted = unsorted.stream().sorted(new AddressComparator()).toList();

            assertEquals(List.of("Aebi", "Meier", "Meier"), lastnames(sorted));
        }

        @Test
        @DisplayName("Bei gleichem Nachnamen entscheidet der Vorname")
        void firstnameBreaksTheTie() {
            List<Address> sorted = unsorted.stream().sorted(new AddressComparator()).toList();

            assertEquals(List.of(2, 3, 1), ids(sorted), "Aebi Zoe, Meier Anna, Meier Beat");
        }

        @Test
        @DisplayName("Bei gleichem Namen entscheidet die Id")
        void idBreaksTheRemainingTie() {
            Address first = address(7, "Anna", "Meier", "079", 0L);
            Address second = address(9, "Anna", "Meier", "079", 0L);

            assertTrue(new AddressComparator().compare(first, second) < 0);
        }

        @Test
        @DisplayName("Die Standardreihenfolge ist Nachname, Vorname, Id")
        void exposesItsSortFields() {
            assertEquals(List.of(SortField.LASTNAME, SortField.FIRSTNAME, SortField.ID),
                    new AddressComparator().getSortFields());
        }
    }

    @Nested
    @DisplayName("Aufgabe 2 - Sortierung nach frei waehlbaren Attributen")
    class ConfigurableSortFieldTests {

        @Test
        @DisplayName("Sortiert nach Vorname")
        void sortsByFirstname() {
            List<Address> sorted = unsorted.stream().sorted(new AddressComparator(SortField.FIRSTNAME)).toList();

            assertEquals(List.of("Anna", "Beat", "Zoe"),
                    sorted.stream().map(Address::getFirstname).toList());
        }

        @Test
        @DisplayName("Sortiert nach Telefonnummer")
        void sortsByPhonenumber() {
            List<Address> sorted = unsorted.stream().sorted(new AddressComparator(SortField.PHONENUMBER)).toList();

            assertEquals(List.of(1, 2, 3), ids(sorted));
        }

        @Test
        @DisplayName("Sortiert nach Registrierungsdatum")
        void sortsByRegistrationDate() {
            List<Address> sorted = unsorted.stream()
                    .sorted(new AddressComparator(SortField.REGISTRATION_DATE)).toList();

            assertEquals(List.of(1, 2, 3), ids(sorted));
        }

        @Test
        @DisplayName("Sortiert nach Id")
        void sortsById() {
            List<Address> sorted = unsorted.stream().sorted(new AddressComparator(SortField.ID)).toList();

            assertEquals(List.of(1, 2, 3), ids(sorted));
        }

        @Test
        @DisplayName("Mehrere Felder werden in der angegebenen Reihenfolge ausgewertet")
        void combinesSeveralFieldsInOrder() {
            List<Address> byLastnameThenId = unsorted.stream()
                    .sorted(new AddressComparator(SortField.LASTNAME, SortField.ID)).toList();
            List<Address> byIdThenLastname = unsorted.stream()
                    .sorted(new AddressComparator(SortField.ID, SortField.LASTNAME)).toList();

            assertEquals(List.of(2, 1, 3), ids(byLastnameThenId), "Aebi, dann Meier nach Id");
            assertEquals(List.of(1, 2, 3), ids(byIdThenLastname), "Id allein entscheidet bereits");
        }

        @Test
        @DisplayName("reversed() dreht jede Reihenfolge um")
        void supportsDescendingOrder() {
            List<Address> descending = unsorted.stream()
                    .sorted(new AddressComparator(SortField.ID).reversed()).toList();

            assertEquals(List.of(3, 2, 1), ids(descending));
        }

        @Test
        @DisplayName("Ohne Sortierfeld wird der Konstruktor abgelehnt")
        void rejectsAnEmptyFieldList() {
            assertThrows(IllegalArgumentException.class, () -> new AddressComparator(new SortField[0]));
        }

        @Test
        @DisplayName("Ein null-Sortierfeld wird abgelehnt")
        void rejectsANullField() {
            assertThrows(IllegalArgumentException.class,
                    () -> new AddressComparator(SortField.ID, null));
        }
    }

    @Nested
    @DisplayName("Randfaelle")
    class EdgeCaseTests {

        @Test
        @DisplayName("Adressen ohne Nachnamen stehen am Schluss")
        void nullFieldsAreSortedLast() {
            Address withoutLastname = address(4, "Ohne", null, "079", 0L);
            List<Address> input = new ArrayList<>(List.of(withoutLastname, aebiZoe, meierAnna));

            List<Address> sorted = input.stream().sorted(new AddressComparator()).toList();

            assertEquals(List.of(2, 3, 4), ids(sorted));
        }

        @Test
        @DisplayName("null-Adressen stehen am Schluss")
        void nullAddressesAreSortedLast() {
            List<Address> input = new ArrayList<>(Arrays.asList(meierAnna, null, aebiZoe));

            List<Address> sorted = input.stream().sorted(new AddressComparator()).toList();

            assertEquals(aebiZoe, sorted.get(0));
            assertEquals(meierAnna, sorted.get(1));
            assertNull(sorted.get(2));
        }

        @Test
        @DisplayName("Gross- und Kleinschreibung aendert die Reihenfolge nicht")
        void comparesTextCaseInsensitively() {
            Address lower = address(1, "anna", "meier", "079", 0L);
            Address upper = address(2, "ANNA", "MEIER", "079", 0L);

            Comparator<Address> byNameOnly = new AddressComparator(SortField.LASTNAME, SortField.FIRSTNAME);

            assertEquals(0, byNameOnly.compare(lower, upper));
        }

        @Test
        @DisplayName("Eine leere Liste bleibt leer")
        void handlesAnEmptyList() {
            assertEquals(List.of(), new ArrayList<Address>().stream().sorted(new AddressComparator()).toList());
        }
    }
}
