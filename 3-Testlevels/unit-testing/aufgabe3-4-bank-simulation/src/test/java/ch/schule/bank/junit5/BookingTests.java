package ch.schule.bank.junit5;

import ch.schule.Booking;
import ch.schule.BankUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests für die Klasse Booking.
 *
 * <p>Eine Buchung ist ein reiner Wert-Container: Datum (Banktage seit dem
 * 1.1.1970) und Betrag (in Millirappen). Getestet werden deshalb die
 * Initialisierung, die Getter und die Formatierung der Buchungszeile.</p>
 *
 * @author David Tarlos
 * @version 2.0
 */
class BookingTests {

    /**
     * Tests für die Erzeugung von Buchungen.
     */
    @Test
    @DisplayName("Konstruktor übernimmt Datum und Betrag unverändert")
    void testInitialization() {
        Booking booking = new Booking(13576, 12000);

        assertEquals(13576, booking.getDate(), "Das Datum muss übernommen werden");
        assertEquals(12000, booking.getAmount(), "Der Betrag muss übernommen werden");
    }

    /**
     * Eine Abhebung wird als Buchung mit negativem Betrag abgelegt.
     */
    @ParameterizedTest(name = "Buchung(date={0}, amount={1})")
    @CsvSource({
            "13576,  12000",
            "13576, -12000",
            "0,          0",
            "14000,   50000"
    })
    @DisplayName("Buchungen akzeptieren positive wie negative Beträge")
    void testInitializationWithVariousAmounts(int date, long amount) {
        Booking booking = new Booking(date, amount);

        assertEquals(date, booking.getDate());
        assertEquals(amount, booking.getAmount());
    }

    /**
     * Die Buchungszeile besteht aus Datum, Betrag und dem neuen Saldo.
     */
    @Test
    @DisplayName("print() gibt Datum, Betrag und den neuen Saldo aus")
    void testPrint() {
        Booking booking = new Booking(13576, 12000);

        String[] lines = ConsoleOutput.captureLines(() -> booking.print(100000));

        assertEquals(1, lines.length, "print() gibt genau eine Zeile aus");

        // Erwartete Zeile aus den Formatierern zusammensetzen: so bleibt der
        // Test unabhängig von der Locale des Rechners (Dezimaltrennzeichen!).
        String expected = BankUtils.formatBankDate(13576)
                + " " + BankUtils.formatAmount(12000)
                + " " + BankUtils.formatAmount(100000 + 12000);
        assertEquals(expected, lines[0]);

        // 13576 Banktage = 17.09.2007 (360-Tage-Jahr, 30-Tage-Monat)
        assertEquals("17.09.2007", lines[0].substring(0, 10));
    }

    /**
     * Der übergebene Saldo ist der Saldo VOR der Buchung.
     */
    @Test
    @DisplayName("print() addiert den Betrag auf den übergebenen Saldo")
    void testPrintRunningBalance() {
        Booking withdrawal = new Booking(13576, -5000);

        String line = ConsoleOutput.capture(() -> withdrawal.print(20000)).strip();

        assertEquals(BankUtils.formatAmount(15000).strip(),
                line.substring(line.lastIndexOf(' ') + 1),
                "Am Zeilenende steht der Saldo NACH der Buchung");
    }
}
