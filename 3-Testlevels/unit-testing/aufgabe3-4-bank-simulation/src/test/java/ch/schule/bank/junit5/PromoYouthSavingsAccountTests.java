package ch.schule.bank.junit5;

import ch.schule.PromoYouthSavingsAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests für das Promo-Jugend-Sparkonto.
 *
 * <p>Dieses Konto ist ein SavingsAccount, der jede Einzahlung mit 1 Prozent
 * Bonus belohnt. Weil mit ganzzahligen Millirappen gerechnet wird, faellt der
 * Bonus bei Beträgen unter 100 Millirappen durch die Integer-Division weg -
 * genau das ist der spannende Grenzfall.</p>
 *
 * @author David Tarlos
 * @version 2.0
 */
class PromoYouthSavingsAccountTests {

    private PromoYouthSavingsAccount account;

    @BeforeEach
    void setUp() {
        account = new PromoYouthSavingsAccount("Y-1000");
    }

    @Test
    @DisplayName("Ein neues Promo-Konto startet mit Saldo 0")
    void testInit() {
        assertEquals("Y-1000", account.getId());
        assertEquals(0, account.getBalance());
    }

    @Test
    @DisplayName("Jede Einzahlung wird mit 1 Prozent Bonus gutgeschrieben")
    void testDepositWithBonus() {
        assertTrue(account.deposit(13560, 10000));
        assertEquals(10100, account.getBalance(), "10000 + 1 Prozent Bonus = 10100");
    }

    @ParameterizedTest(name = "Einzahlung {0} -> Saldo {1}")
    @CsvSource({
            "100,   101",
            "1000,  1010",
            "10000, 10100",
            "99,    99",
            "1,     1",
            "0,     0"
    })
    @DisplayName("Der Bonus wird abgerundet (Integer-Division)")
    void testBonusRounding(long amount, long expectedBalance) {
        assertTrue(account.deposit(13560, amount));
        assertEquals(expectedBalance, account.getBalance());
    }

    @Test
    @DisplayName("Mehrere Einzahlungen erhalten je einzeln den Bonus")
    void testMultipleDeposits() {
        account.deposit(13560, 10000);
        account.deposit(13561, 10000);

        assertEquals(20200, account.getBalance());
    }

    @Test
    @DisplayName("Negative Einzahlungen werden abgelehnt")
    void testDepositNegativeAmount() {
        assertFalse(account.deposit(13560, -10000));
        assertEquals(0, account.getBalance());
    }

    @Test
    @DisplayName("Die Sparkonto-Regel gilt weiterhin: kein Minus-Saldo")
    void testInheritsSavingsAccountRule() {
        account.deposit(13560, 10000);

        assertTrue(account.withdraw(13561, 10100), "Der Bonus darf mit abgehoben werden");
        assertFalse(account.withdraw(13562, 1), "Danach ist das Konto leer");
    }
}
