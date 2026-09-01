package ch.tbz.m450.util;

import ch.tbz.m450.repository.Address;
import ch.tbz.m450.util.AddressComparator.SortDirection;
import ch.tbz.m450.util.AddressComparator.SortField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("AddressComparator")
class AddressComparatorTest {

    private Address anna;
    private Address bert;
    private Address clara;

    @BeforeEach
    void setUp() {
        // Bewusst so gewaehlt, dass jedes Attribut eine andere Reihenfolge ergibt.
        // Ausserdem "Muster" vs. "muster", damit die Gross-/Kleinschreibung auffaellt.
        anna = new Address(3, "Anna", "Muster", "079 300 00 00", date(2023));
        bert = new Address(1, "Bert", "Beispiel", "044 100 00 00", date(2021));
        clara = new Address(2, "clara", "muster", "062 200 00 00", date(2022));
    }

    private static Date date(int year) {
        return Date.from(LocalDate.of(year, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    /** Sortiert die drei Fixture-Adressen und gibt die ids in der Ergebnisreihenfolge zurueck. */
    private List<Integer> idsSortedWith(Comparator<Address> comparator) {
        return Stream.of(anna, bert, clara)
                .sorted(comparator)
                .map(Address::getId)
                .toList();
    }

    @Nested
    @DisplayName("Aufgabe 1 - Standardverhalten")
    class DefaultOrder {

        private AddressComparator comparator;

        @BeforeEach
        void setUp() {
            comparator = new AddressComparator();
        }

        @Test
        @DisplayName("sortiert nach Nachname, bei Gleichstand nach Vorname")
        void sortsByLastnameThenFirstname() {
            // Beispiel < Muster/muster, dort entscheidet dann Anna < clara
            assertThat(idsSortedWith(comparator)).containsExactly(1, 3, 2);
        }

        @Test
        @DisplayName("vergleicht Namen ohne Ruecksicht auf Gross-/Kleinschreibung")
        void comparesCaseInsensitive() {
            Address gross = new Address(7, "Anna", "MUSTER", null, null);
            Address klein = new Address(7, "anna", "muster", null, null);

            assertThat(comparator.compare(gross, klein)).isZero();
            // "Muster" == "muster", also entscheidet der Vorname
            assertThat(comparator.compare(anna, clara)).isNegative();
        }

        @Test
        @DisplayName("ist reflexiv - eine Adresse ist gleich gross wie sie selbst")
        void isReflexive() {
            assertThat(comparator.compare(anna, anna)).isZero();
        }

        @Test
        @DisplayName("ist symmetrisch - genau das war beim 'return -1' kaputt")
        void isSymmetric() {
            assertThat(Integer.signum(comparator.compare(anna, bert)))
                    .isEqualTo(-Integer.signum(comparator.compare(bert, anna)));
        }

        @Test
        @DisplayName("ist transitiv")
        void isTransitive() {
            // bert < anna und anna < clara  =>  bert < clara
            assertThat(comparator.compare(bert, anna)).isNegative();
            assertThat(comparator.compare(anna, clara)).isNegative();
            assertThat(comparator.compare(bert, clara)).isNegative();
        }

        @Test
        @DisplayName("nimmt die id als Tiebreak, wenn Vor- und Nachname identisch sind")
        void usesIdAsTiebreak() {
            Address zwilling = new Address(9, "Anna", "Muster", "000", date(2020));

            assertThat(comparator.compare(anna, zwilling)).isNegative(); // id 3 < id 9
            assertThat(comparator.compare(zwilling, anna)).isPositive();
        }

        @Test
        @DisplayName("wirft NullPointerException fuer null-Adressen")
        void rejectsNullAddresses() {
            assertThatNullPointerException().isThrownBy(() -> comparator.compare(null, anna));
            assertThatNullPointerException().isThrownBy(() -> comparator.compare(anna, null));
        }
    }

    @Nested
    @DisplayName("Aufgabe 2 - Sortierung nach weiteren Attributen")
    class AdditionalFields {

        static Stream<Arguments> expectedAscendingOrder() {
            return Stream.of(
                    Arguments.of(SortField.LASTNAME, List.of(1, 3, 2)),          // Beispiel, Muster/Anna, muster/clara
                    Arguments.of(SortField.FIRSTNAME, List.of(3, 1, 2)),         // Anna, Bert, clara
                    Arguments.of(SortField.PHONENUMBER, List.of(1, 2, 3)),       // 044, 062, 079
                    Arguments.of(SortField.REGISTRATION_DATE, List.of(1, 2, 3)), // 2021, 2022, 2023
                    Arguments.of(SortField.ID, List.of(1, 2, 3)));
        }

        @ParameterizedTest(name = "{0} aufsteigend ergibt {1}")
        @MethodSource("expectedAscendingOrder")
        @DisplayName("sortiert aufsteigend nach dem gewaehlten Attribut")
        void sortsAscendingByField(SortField field, List<Integer> expectedIds) {
            assertThat(idsSortedWith(new AddressComparator(field))).isEqualTo(expectedIds);
        }

        @ParameterizedTest(name = "{0} absteigend")
        @EnumSource(SortField.class)
        @DisplayName("DESC dreht die aufsteigende Reihenfolge um")
        void descendingReversesAscending(SortField field) {
            List<Integer> ascending = idsSortedWith(new AddressComparator(field, SortDirection.ASC));
            List<Integer> descending = idsSortedWith(new AddressComparator(field, SortDirection.DESC));

            assertThat(descending).isEqualTo(ascending.reversed());
        }

        @Test
        @DisplayName("sortiert nach Telefonnummer")
        void sortsByPhonenumber() {
            AddressComparator comparator = new AddressComparator(SortField.PHONENUMBER);

            assertThat(comparator.compare(bert, anna)).isNegative(); // 044 vor 079
            assertThat(idsSortedWith(comparator)).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("sortiert nach Registrierungsdatum - die aelteste Adresse zuerst")
        void sortsByRegistrationDate() {
            AddressComparator comparator = new AddressComparator(SortField.REGISTRATION_DATE);

            assertThat(comparator.compare(bert, anna)).isNegative(); // 2021 vor 2023
            assertThat(idsSortedWith(comparator)).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("schiebt null-Werte ans Ende der aufsteigenden Reihenfolge")
        void putsNullValuesLast() {
            Address unvollstaendig = new Address(4, "Zoe", null, null, null);

            assertThat(new AddressComparator(SortField.LASTNAME).compare(anna, unvollstaendig)).isNegative();
            assertThat(new AddressComparator(SortField.PHONENUMBER).compare(anna, unvollstaendig)).isNegative();
            assertThat(new AddressComparator(SortField.REGISTRATION_DATE).compare(anna, unvollstaendig)).isNegative();
        }

        @Test
        @DisplayName("bringt null-Werte bei DESC nach vorne")
        void putsNullValuesFirstWhenDescending() {
            Address unvollstaendig = new Address(4, "Zoe", null, null, null);
            AddressComparator comparator = new AddressComparator(SortField.LASTNAME, SortDirection.DESC);

            assertThat(comparator.compare(unvollstaendig, anna)).isNegative();
        }

        @Test
        @DisplayName("ohne Richtung wird aufsteigend sortiert")
        void defaultsToAscending() {
            AddressComparator comparator = new AddressComparator(SortField.FIRSTNAME);

            assertThat(comparator.getField()).isEqualTo(SortField.FIRSTNAME);
            assertThat(comparator.getDirection()).isEqualTo(SortDirection.ASC);
        }

        @Test
        @DisplayName("der parameterlose Konstruktor entspricht LASTNAME/ASC")
        void defaultConstructorMatchesLastnameAscending() {
            AddressComparator standard = new AddressComparator();

            assertThat(standard.getField()).isEqualTo(SortField.LASTNAME);
            assertThat(standard.getDirection()).isEqualTo(SortDirection.ASC);
            assertThat(idsSortedWith(standard))
                    .isEqualTo(idsSortedWith(new AddressComparator(SortField.LASTNAME, SortDirection.ASC)));
        }

        @Test
        @DisplayName("weist null als Feld oder Richtung ab")
        void rejectsNullConfiguration() {
            assertThatNullPointerException().isThrownBy(() -> new AddressComparator(null));
            assertThatNullPointerException().isThrownBy(() -> new AddressComparator(SortField.ID, null));
        }
    }
}
