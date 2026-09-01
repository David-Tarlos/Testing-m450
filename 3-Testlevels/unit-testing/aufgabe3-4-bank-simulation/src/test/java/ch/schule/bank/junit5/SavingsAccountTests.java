package ch.schule.bank.junit5;

import ch.schule.SavingsAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests für die Klasse SavingsAccount.
 *
 * <p>Das Sparkonto erweitert Account um genau eine Regel: es darf nie ins Minus
 * rutschen. Getestet wird deshalb vor allem die Grenze zwischen "geht gerade
 * noch" und "geht nicht mehr" (Grenzwertanalyse).</p>
 *
 * @author David Tarlos
 * @version 2.0
 */
class SavingsAccountTests {

    private SavingsAccount account;

    @BeforeEach
    void setUp() {
        account = new SavingsAccount("S-1000");
        account.deposit(13560, 10000);
    }

    @Test
    @DisplayName("Ein Sparkonto übernimmt Nummer und Einzahlungen von Account")
    void testInit() {
        SavingsAccount fresh = new SavingsAccount("S-9999");

        assertEquals("S-9999", fresh.getId());
        assertEquals(0, fresh.getBalance());
    }

    @Test
    @DisplayName("Abheben unterhalb des Saldos ist erlaubt")
    void testWithdrawBelowBalance() {
        assertTrue(account.withdraw(13561, 4000));
        assertEquals(6000, account.getBalance());
    }

    @Test
    @DisplayName("Der gesamte Saldo darf abgehoben werden (Grenzfall)")
    void testWithdrawExactBalance() {
        assertTrue(account.withdraw(13561, 10000));
        assertEquals(0, account.getBalance());
    }

    @Test
    @DisplayName("Mehr als der Saldo darf NICHT abgehoben werden")
    void testWithdrawMoreThanBalance() {
        assertFalse(account.withdraw(13561, 10001), "Ein Sparkonto darf nicht ins Minus");
        assertEquals(10000, account.getBalance(), "Der Saldo bleibt unverändert");
    }

    @ParameterizedTest(name = "Abhebung von {0} Millirappen")
    @ValueSource(longs = {10001, 20000, Long.MAX_VALUE})
    @DisplayName("Zu grosse Abhebungen werden konsequent abgelehnt")
    void testWithdrawTooMuch(long amount) {
        assertFalse(account.withdraw(13561, amount));
        assertEquals(10000, account.getBalance());
    }

    @Test
    @DisplayName("Negative Abhebungen werden abgelehnt")
    void testWithdrawNegativeAmount() {
        assertFalse(account.withdraw(13561, -5000),
                "Sonst könnte man sich per negativer Abhebung Geld gutschreiben");
        assertEquals(10000, account.getBalance());
    }

    @Test
    @DisplayName("Nach einer abgelehnten Abhebung wird keine Buchung erfasst")
    void testRejectedWithdrawCreatesNoBooking() {
        account.withdraw(13561, 99999);

        String[] lines = ConsoleOutput.captureLines(account::print);

        assertEquals(3, lines.length, "2 Kopfzeilen + nur die Einzahlung aus dem Setup");
    }
}
