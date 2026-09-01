package ch.schule.bank.junit5;

import ch.schule.Account;
import ch.schule.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests für die Klasse Account.
 *
 * <p>Account ist abstrakt. Damit hier wirklich nur das Verhalten der Basisklasse
 * getestet wird (und nicht zusätzlich die Regeln von SavingsAccount oder
 * SalaryAccount), definiert dieser Test eine eigene minimale Unterklasse
 * {@link TestAccount}. Das ist ein typisches Test-Stub-Muster.</p>
 *
 * @author David Tarlos
 * @version 2.0
 */
class AccountTests {

    /** Minimale konkrete Unterklasse, um die abstrakte Basisklasse testen zu können. */
    private static final class TestAccount extends Account {
        TestAccount(String id) {
            super(id);
        }
    }

    private TestAccount account;

    @BeforeEach
    void setUp() {
        account = new TestAccount("TEST-1");
    }

    /**
     * Testet die Initialisierung eines Kontos.
     */
    @Test
    @DisplayName("Ein neues Konto hat die gesetzte Nummer und Saldo 0")
    void testInit() {
        assertAll("Initialzustand",
                () -> assertEquals("TEST-1", account.getId()),
                () -> assertEquals(0, account.getBalance()),
                () -> assertTrue(account.canTransact(0), "Ohne Buchungen ist jedes Datum erlaubt"));
    }

    @Nested
    @DisplayName("deposit()")
    class DepositTests {

        /**
         * Testet das Einzahlen auf ein Konto.
         */
        @Test
        @DisplayName("erhöht den Saldo um den einbezahlten Betrag")
        void testDeposit() {
            assertTrue(account.deposit(13560, 12000), "Einzahlung muss gelingen");
            assertEquals(12000, account.getBalance());

            assertTrue(account.deposit(13561, 8000));
            assertEquals(20000, account.getBalance(), "Einzahlungen summieren sich");
        }

        @Test
        @DisplayName("lehnt negative Beträge ab und lässt den Saldo unverändert")
        void testDepositNegativeAmount() {
            assertFalse(account.deposit(13560, -1), "Negative Einzahlung ist unzulässig");
            assertEquals(0, account.getBalance());
        }

        @Test
        @DisplayName("akzeptiert den Betrag 0 (Grenzfall)")
        void testDepositZero() {
            assertTrue(account.deposit(13560, 0));
            assertEquals(0, account.getBalance());
        }
    }

    @Nested
    @DisplayName("withdraw()")
    class WithdrawTests {

        /**
         * Testet das Abheben von einem Konto.
         */
        @Test
        @DisplayName("vermindert den Saldo um den abgehobenen Betrag")
        void testWithdraw() {
            account.deposit(13560, 20000);

            assertTrue(account.withdraw(13561, 5000));
            assertEquals(15000, account.getBalance());
        }

        @Test
        @DisplayName("lehnt negative Beträge ab")
        void testWithdrawNegativeAmount() {
            account.deposit(13560, 20000);

            assertFalse(account.withdraw(13561, -1));
            assertEquals(20000, account.getBalance());
        }

        @Test
        @DisplayName("erlaubt in der Basisklasse einen negativen Saldo (keine Limite)")
        void testWithdrawIntoMinus() {
            // Erst die Unterklassen SavingsAccount/SalaryAccount schränken das ein.
            assertTrue(account.withdraw(13560, 5000));
            assertEquals(-5000, account.getBalance());
        }
    }

    /**
     * Testet das Flag canTransact: Buchungen müssen chronologisch erfolgen.
     */
    @Test
    @DisplayName("canTransact() verlangt ein Datum ab der letzten Buchung")
    void testCanTransact() {
        assertTrue(account.canTransact(0), "Ohne Buchungen ist alles erlaubt");

        account.deposit(13560, 10000);

        assertAll("Nach einer Buchung am Tag 13560",
                () -> assertTrue(account.canTransact(13560), "Gleicher Tag ist erlaubt"),
                () -> assertTrue(account.canTransact(13561), "Späterer Tag ist erlaubt"),
                () -> assertFalse(account.canTransact(13559), "Rückdatieren ist verboten"));
    }

    @Test
    @DisplayName("Rückdatierte Buchungen werden abgelehnt")
    void testTransactionsMustBeChronological() {
        account.deposit(13560, 10000);

        assertFalse(account.deposit(13559, 5000), "Einzahlung in der Vergangenheit");
        assertFalse(account.withdraw(13559, 5000), "Abhebung in der Vergangenheit");
        assertEquals(10000, account.getBalance(), "Der Saldo darf sich nicht verändert haben");
    }

    /**
     * Testet die (aus dem Klassendiagramm stammende) Referenz auf eine Buchung.
     */
    @Test
    @DisplayName("Die Booking-Referenz lässt sich setzen und lesen")
    void testReferences() {
        assertNull(account.getBooking(), "Anfangs ist keine Buchung referenziert");

        Booking booking = new Booking(13560, 12000);
        account.setBooking(booking);

        assertSame(booking, account.getBooking());
    }

    /**
     * Testet den vollständigen Kontoauszug.
     */
    @Test
    @DisplayName("print() gibt Kopfzeilen und alle Buchungen mit laufendem Saldo aus")
    void testPrint() {
        account.deposit(13560, 20000);
        account.withdraw(13561, 5000);

        String[] lines = ConsoleOutput.captureLines(account::print);

        assertEquals(4, lines.length, "2 Kopfzeilen + 2 Buchungszeilen");
        assertEquals("Kontoauszug 'TEST-1'", lines[0]);
        assertEquals("Datum          Betrag      Saldo", lines[1]);
        assertTrue(lines[2].startsWith("01.09.2007"), "Banktag 13560 = 01.09.2007");
        assertTrue(lines[3].startsWith("02.09.2007"), "Banktag 13561 = 02.09.2007");
    }

    @Test
    @DisplayName("print() gibt bei einem leeren Konto nur die Kopfzeilen aus")
    void testPrintEmptyAccount() {
        String[] lines = ConsoleOutput.captureLines(account::print);

        assertEquals(2, lines.length);
    }

    /**
     * Testet den Monats-Kontoauszug.
     */
    @Test
    @DisplayName("print(jahr, monat) zeigt nur die Buchungen des gewählten Monats")
    void testMonthlyPrint() {
        account.deposit(13556, 1000);   // 27.08.2007
        account.deposit(13570, 2000);   // 11.09.2007
        account.deposit(13600, 3000);   // 11.10.2007

        String[] lines = ConsoleOutput.captureLines(() -> account.print(2007, 9));

        assertEquals(3, lines.length, "2 Kopfzeilen + 1 Buchung im September");
        assertEquals("Kontoauszug 'TEST-1' Monat: 9.2007", lines[0]);
        assertTrue(lines[2].startsWith("11.09.2007"));
    }

    @Test
    @DisplayName("print(jahr, monat) zeigt für einen Monat ohne Buchungen nur die Kopfzeilen")
    void testMonthlyPrintWithoutBookings() {
        account.deposit(13570, 2000);   // September 2007

        String[] lines = ConsoleOutput.captureLines(() -> account.print(2007, 8));

        assertEquals(2, lines.length);
    }
}
