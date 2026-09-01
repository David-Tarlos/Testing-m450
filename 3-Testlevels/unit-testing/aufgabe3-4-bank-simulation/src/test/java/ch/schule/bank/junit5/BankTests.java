package ch.schule.bank.junit5;

import ch.schule.Bank;
import ch.schule.SavingsAccount;
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
 * Tests für die Klasse Bank.
 *
 * <p>Die Bank ist die Fassade über den Konten: sie erzeugt sie, findet sie
 * anhand der Kontonummer und leitet Ein-/Auszahlungen weiter. Getestet werden
 * deshalb vor allem die Kontonummern-Vergabe und das Verhalten bei
 * unbekannten Kontonummern.</p>
 *
 * @author David Tarlos
 * @version 2.0
 */
class BankTests {

    private Bank bank;

    @BeforeEach
    void setUp() {
        bank = new Bank();
    }

    @Nested
    @DisplayName("Konten eröffnen")
    class CreateTests {

        @Test
        @DisplayName("Die Kontonummer beginnt bei 1000 und zählt hoch")
        void testCreate() {
            assertAll("Kontonummern-Vergabe",
                    () -> assertEquals("S-1000", bank.createSavingsAccount()),
                    () -> assertEquals("S-1001", bank.createSavingsAccount()),
                    () -> assertEquals("Y-1002", bank.createPromoYouthSavingsAccount()),
                    () -> assertEquals("P-1003", bank.createSalaryAccount(-5000)));
        }

        @Test
        @DisplayName("Das Präfix zeigt den Kontotyp an (S / Y / P)")
        void testCreatePrefixes() {
            assertTrue(bank.createSavingsAccount().startsWith("S-"));
            assertTrue(bank.createPromoYouthSavingsAccount().startsWith("Y-"));
            assertTrue(bank.createSalaryAccount(-1000).startsWith("P-"));
        }

        @Test
        @DisplayName("Ein neu eröffnetes Konto hat Saldo 0")
        void testNewAccountIsEmpty() {
            String id = bank.createSavingsAccount();

            assertEquals(0, bank.getBalance(id));
        }

        @Test
        @DisplayName("Eine positive Kreditlimite ist ungültig und liefert null")
        void testCreateSalaryAccountWithInvalidLimit() {
            assertNull(bank.createSalaryAccount(1), "Die Limite muss negativ oder 0 sein");
        }

        @Test
        @DisplayName("Ein abgelehntes Lohnkonto verbraucht keine Kontonummer")
        void testRejectedAccountDoesNotConsumeId() {
            bank.createSalaryAccount(5000);

            assertEquals("S-1000", bank.createSavingsAccount());
        }

        @Test
        @DisplayName("Eine Kreditlimite von 0 ist zulässig (Grenzfall)")
        void testCreateSalaryAccountWithZeroLimit() {
            assertEquals("P-1000", bank.createSalaryAccount(0));
        }

        @Test
        @DisplayName("Zwei Banken zählen ihre Kontonummern unabhängig voneinander")
        void testBanksAreIndependent() {
            Bank andereBank = new Bank();

            assertEquals("S-1000", bank.createSavingsAccount());
            assertEquals("S-1000", andereBank.createSavingsAccount());
        }
    }

    @Nested
    @DisplayName("Ein- und Auszahlen")
    class TransactionTests {

        private String id;

        @BeforeEach
        void openAccount() {
            id = bank.createSavingsAccount();
        }

        @Test
        @DisplayName("Einzahlen erhöht den Saldo des richtigen Kontos")
        void testDeposit() {
            assertTrue(bank.deposit(id, 13560, 12000));
            assertEquals(12000, bank.getBalance(id));
        }

        @Test
        @DisplayName("Einzahlen auf ein unbekanntes Konto schlägt fehl")
        void testDepositUnknownAccount() {
            assertFalse(bank.deposit("gibt-es-nicht", 13560, 12000));
        }

        @Test
        @DisplayName("Abheben vermindert den Saldo")
        void testWithdraw() {
            bank.deposit(id, 13560, 12000);

            assertTrue(bank.withdraw(id, 13561, 2000));
            assertEquals(10000, bank.getBalance(id));
        }

        @Test
        @DisplayName("Abheben von einem unbekannten Konto schlägt fehl")
        void testWithdrawUnknownAccount() {
            assertFalse(bank.withdraw("gibt-es-nicht", 13560, 12000));
        }

        @Test
        @DisplayName("Die Bank reicht die Regeln des Kontos durch")
        void testAccountRulesApply() {
            bank.deposit(id, 13560, 1000);

            assertFalse(bank.withdraw(id, 13561, 2000), "Sparkonto darf nicht ins Minus");
            assertEquals(1000, bank.getBalance(id));
        }

        @Test
        @DisplayName("Der Saldo eines unbekannten Kontos ist 0")
        void testBalanceOfUnknownAccount() {
            assertEquals(0, bank.getBalance("gibt-es-nicht"));
        }
    }

    @Nested
    @DisplayName("Gesamtsaldo der Bank")
    class BalanceTests {

        @Test
        @DisplayName("Eine Bank ohne Konten hat den Gesamtsaldo 0")
        void testBalanceOfEmptyBank() {
            assertEquals(0, bank.getBalance());
        }

        @Test
        @DisplayName("Der Gesamtsaldo ist die NEGATIVE Summe aller Kundenguthaben")
        void testBalance() {
            String a = bank.createSavingsAccount();
            String b = bank.createSavingsAccount();
            bank.deposit(a, 13560, 12000);
            bank.deposit(b, 13560, 8000);

            // Bank.getBalance() rechnet "balance -= konto.getBalance()".
            // Aus Sicht der Bank sind Kundenguthaben Verbindlichkeiten, deshalb
            // ist der Wert negativ. Wer die Summe der Guthaben will, muss das
            // Vorzeichen drehen. Siehe DOKUMENTATION.md, Abschnitt Stolperfallen.
            assertEquals(-20000, bank.getBalance());
        }
    }

    @Nested
    @DisplayName("Auswertungen und Ausgaben")
    class PrintTests {

        @BeforeEach
        void openSixAccounts() {
            // S-1000 = 6000, S-1001 = 5000, ... S-1005 = 1000
            for (int i = 0; i < 6; ++i) {
                String id = bank.createSavingsAccount();
                bank.deposit(id, 13560, 6000 - i * 1000);
            }
        }

        @Test
        @DisplayName("print() gibt den Kontoauszug des gewählten Kontos aus")
        void testPrint() {
            String[] lines = ConsoleOutput.captureLines(() -> bank.print("S-1000"));

            assertEquals(3, lines.length, "2 Kopfzeilen + 1 Buchung");
            assertEquals("Kontoauszug 'S-1000'", lines[0]);
        }

        @Test
        @DisplayName("print() gibt für ein unbekanntes Konto nichts aus")
        void testPrintUnknownAccount() {
            String[] lines = ConsoleOutput.captureLines(() -> bank.print("gibt-es-nicht"));

            assertEquals(0, lines.length);
        }

        @Test
        @DisplayName("print(jahr, monat) gibt den Monatsauszug aus")
        void testMonthlyPrint() {
            String[] lines = ConsoleOutput.captureLines(() -> bank.print("S-1000", 2007, 9));

            assertEquals(3, lines.length);
            assertEquals("Kontoauszug 'S-1000' Monat: 9.2007", lines[0]);
        }

        @Test
        @DisplayName("print(jahr, monat) gibt für ein unbekanntes Konto nichts aus")
        void testMonthlyPrintUnknownAccount() {
            String[] lines = ConsoleOutput.captureLines(
                    () -> bank.print("gibt-es-nicht", 2007, 9));

            assertEquals(0, lines.length);
        }

        @Test
        @DisplayName("printTop5() zeigt die 5 reichsten Konten absteigend")
        void testTop5() {
            String[] lines = ConsoleOutput.captureLines(bank::printTop5);

            assertEquals(5, lines.length, "Auch bei 6 Konten werden nur 5 ausgegeben");
            assertEquals("S-1000: 6000", lines[0]);
            assertEquals("S-1004: 2000", lines[4]);
        }

        @Test
        @DisplayName("printBottom5() zeigt die 5 ärmsten Konten aufsteigend")
        void testBottom5() {
            String[] lines = ConsoleOutput.captureLines(bank::printBottom5);

            assertEquals(5, lines.length);
            assertEquals("S-1005: 1000", lines[0]);
            assertEquals("S-1001: 5000", lines[4]);
        }

        @Test
        @DisplayName("printTop5() kommt auch mit weniger als 5 Konten zurecht")
        void testTop5WithFewAccounts() {
            Bank kleineBank = new Bank();
            kleineBank.createSavingsAccount();

            assertEquals(1, ConsoleOutput.captureLines(kleineBank::printTop5).length);
            assertEquals(1, ConsoleOutput.captureLines(kleineBank::printBottom5).length);
        }

        @Test
        @DisplayName("printTop5() gibt bei einer leeren Bank nichts aus")
        void testTop5WithoutAccounts() {
            Bank leereBank = new Bank();

            assertEquals(0, ConsoleOutput.captureLines(leereBank::printTop5).length);
            assertEquals(0, ConsoleOutput.captureLines(leereBank::printBottom5).length);
        }
    }

    @Test
    @DisplayName("Die Account-Referenz lässt sich setzen und lesen")
    void testReferences() {
        assertNull(bank.getAccount());

        SavingsAccount account = new SavingsAccount("S-4711");
        bank.setAccount(account);

        assertSame(account, bank.getAccount());
    }
}
