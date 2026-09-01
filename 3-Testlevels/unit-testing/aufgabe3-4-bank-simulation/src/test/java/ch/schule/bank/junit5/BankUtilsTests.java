package ch.schule.bank.junit5;

import ch.schule.BankUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests für die Hilfsklasse BankUtils.
 *
 * <p>BankUtils rechnet das interne Datumsformat (Banktage seit dem 1.1.1970,
 * mit 30-Tage-Monaten und 360-Tage-Jahren) in ein lesbares Datum um und
 * formatiert Millirappen-Beträge als Franken-Beträge.</p>
 *
 * <p>Hinweis: Das Dezimaltrennzeichen von <code>AMOUNT_FORMAT</code> hängt von
 * der Locale des Rechners ab. Die Tests lesen es deshalb aus dem Formatierer
 * aus, statt es fix zu erwarten — sonst wären sie auf einem deutschen System
 * rot und auf einem Schweizer grün.</p>
 *
 * @author David Tarlos
 * @version 1.0
 */
class BankUtilsTests {

    /** Das lokale Dezimaltrennzeichen ("." in de-CH, "," in de-DE). */
    private static final char DEC =
            BankUtils.AMOUNT_FORMAT.getDecimalFormatSymbols().getDecimalSeparator();

    @Test
    @DisplayName("Die Utility-Klasse lässt sich instanzieren (Default-Konstruktor)")
    void testInit() {
        assertNotNull(new BankUtils());
    }

    @ParameterizedTest(name = "Banktag {0} entspricht {1}")
    @CsvSource({
            "0,     01.01.1970",
            "1,     02.01.1970",
            "29,    30.01.1970",
            "30,    01.02.1970",
            "359,   30.12.1970",
            "360,   01.01.1971",
            "13560, 01.09.2007",
            "13576, 17.09.2007"
    })
    @DisplayName("formatBankDate() rechnet Banktage in ein Datum um")
    void testFormatBankDate(int date, String expected) {
        assertEquals(expected, BankUtils.formatBankDate(date));
    }

    @Test
    @DisplayName("formatAmount() rechnet Millirappen in Franken um")
    void testFormatAmount() {
        // 100'000 Millirappen = 1 Franken
        assertEquals("1" + DEC + "00", BankUtils.formatAmount(100000).strip());
        assertEquals("0" + DEC + "12", BankUtils.formatAmount(12000).strip());
        assertEquals("0" + DEC + "00", BankUtils.formatAmount(0).strip());
        assertEquals("-1" + DEC + "20", BankUtils.formatAmount(-120000).strip());
    }

    @Test
    @DisplayName("formatAmount() füllt rechtsbündig auf mindestens 10 Zeichen auf")
    void testFormatAmountPadding() {
        String small = BankUtils.formatAmount(0);

        assertEquals(10, small.length(), "Kurze Beträge werden auf 10 Zeichen aufgefüllt");
        assertTrue(small.startsWith(" "), "Aufgefüllt wird links (rechtsbündig)");

        // Sehr grosse Beträge dürfen länger als 10 Zeichen sein
        assertTrue(BankUtils.formatAmount(Long.MAX_VALUE).length() >= 10);
    }

    @Test
    @DisplayName("TWO_DIGIT_FORMAT stellt Zahlen immer zweistellig dar")
    void testTwoDigitFormat() {
        assertEquals("00", BankUtils.TWO_DIGIT_FORMAT.format(0));
        assertEquals("07", BankUtils.TWO_DIGIT_FORMAT.format(7));
        assertEquals("31", BankUtils.TWO_DIGIT_FORMAT.format(31));
        assertEquals("100", BankUtils.TWO_DIGIT_FORMAT.format(100));
    }
}
