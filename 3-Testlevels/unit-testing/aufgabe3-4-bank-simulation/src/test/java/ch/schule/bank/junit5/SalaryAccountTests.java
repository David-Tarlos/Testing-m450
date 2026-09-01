package ch.schule.bank.junit5;

import ch.schule.SalaryAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests der Klasse SalaryAccount.
 *
 * <p>Das Lohnkonto darf bis zur Kreditlimite (eine negative Zahl!) überzogen
 * werden. Die interessanten Fälle liegen also genau auf und knapp neben
 * dieser Limite.</p>
 *
 * @author David Tarlos
 * @version 2.0
 */
class SalaryAccountTests {

    /** Kreditlimite: das Konto darf bis -5000 Millirappen überzogen werden. */
    private static final long CREDIT_LIMIT = -5000;

    private SalaryAccount account;

    @BeforeEach
    void setUp() {
        account = new SalaryAccount("P-1000", CREDIT_LIMIT);
    }

    @Test
    @DisplayName("Ein neues Lohnkonto startet mit Saldo 0")
    void testInit() {
        assertEquals("P-1000", account.getId());
        assertEquals(0, account.getBalance());
    }

    @Test
    @DisplayName("Abheben bei gedecktem Saldo funktioniert wie gewohnt")
    void testWithdrawWithBalance() {
        account.deposit(13560, 10000);

        assertTrue(account.withdraw(13561, 4000));
        assertEquals(6000, account.getBalance());
    }

    @Test
    @DisplayName("Das Konto darf bis zur Kreditlimite überzogen werden")
    void testWithdrawIntoCreditLimit() {
        assertTrue(account.withdraw(13560, 3000), "3000 liegt innerhalb der Limite");
        assertEquals(-3000, account.getBalance());
    }

    @Test
    @DisplayName("Genau auf die Kreditlimite abheben ist erlaubt (Grenzfall)")
    void testWithdrawExactlyToCreditLimit() {
        assertTrue(account.withdraw(13560, 5000));
        assertEquals(CREDIT_LIMIT, account.getBalance());
    }

    @Test
    @DisplayName("Einen Millirappen über die Kreditlimite ist verboten (Grenzfall)")
    void testWithdrawBeyondCreditLimit() {
        assertFalse(account.withdraw(13560, 5001));
        assertEquals(0, account.getBalance(), "Der Saldo bleibt unverändert");
    }

    @ParameterizedTest(name = "Saldo {0}, Abhebung {1} -> erlaubt={2}")
    @CsvSource({
            "10000, 15000, true",
            "10000, 15001, false",
            "0,     5000,  true",
            "0,     5001,  false",
            "0,     0,     true"
    })
    @DisplayName("Die Kreditlimite gilt relativ zum aktuellen Saldo")
    void testCreditLimitIsRelativeToBalance(long initialBalance, long amount, boolean expected) {
        if (initialBalance > 0) {
            account.deposit(13560, initialBalance);
        }

        assertEquals(expected, account.withdraw(13561, amount));
    }

    @Test
    @DisplayName("Negative Abhebungen werden auch beim Lohnkonto abgelehnt")
    void testWithdrawNegativeAmount() {
        // Wichtig: die Limiten-Pruefung laesst einen negativen Betrag durch
        // (der Saldo wuerde ja steigen) - erst Account.withdraw() faengt ihn ab.
        assertFalse(account.withdraw(13560, -1000));
        assertEquals(0, account.getBalance());
    }

    @Test
    @DisplayName("Eine Kreditlimite von 0 verhält sich wie ein Sparkonto")
    void testZeroCreditLimit() {
        SalaryAccount strict = new SalaryAccount("P-2000", 0);
        strict.deposit(13560, 1000);

        assertTrue(strict.withdraw(13561, 1000));
        assertFalse(strict.withdraw(13562, 1));
    }
}
