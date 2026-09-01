package ch.schule.bank.junit5;

import ch.schule.Account;
import ch.schule.AccountBalanceComparator;
import ch.schule.AccountInverseBalanceComparator;
import ch.schule.SavingsAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests für die beiden Comparatoren.
 *
 * <p>Die Bank sortiert ihre Konten mit diesen Comparatoren fuer printTop5()
 * bzw. printBottom5(). Getestet werden die drei möglichen Rückgabewerte
 * (kleiner / gleich / grösser) sowie die resultierende Sortierreihenfolge.</p>
 *
 * @author David Tarlos
 * @version 1.0
 */
class AccountComparatorTests {

    private Account arm;
    private Account mittel;
    private Account reich;

    @BeforeEach
    void setUp() {
        arm = new SavingsAccount("S-1");
        mittel = new SavingsAccount("S-2");
        reich = new SavingsAccount("S-3");

        arm.deposit(13560, 100);
        mittel.deposit(13560, 5000);
        reich.deposit(13560, 90000);
    }

    @Test
    @DisplayName("AccountBalanceComparator sortiert absteigend (groesster Saldo zuerst)")
    void testBalanceComparator() {
        Comparator<Object> comparator = new AccountBalanceComparator();

        assertTrue(comparator.compare(reich, arm) < 0, "Das reichere Konto kommt zuerst");
        assertTrue(comparator.compare(arm, reich) > 0);
        assertEquals(0, comparator.compare(arm, arm), "Gleicher Saldo = gleichwertig");

        Account[] konten = {arm, reich, mittel};
        Arrays.sort(konten, comparator);

        assertEquals("S-3", konten[0].getId());
        assertEquals("S-2", konten[1].getId());
        assertEquals("S-1", konten[2].getId());
    }

    @Test
    @DisplayName("AccountInverseBalanceComparator sortiert aufsteigend")
    void testInverseBalanceComparator() {
        Comparator<Object> comparator = new AccountInverseBalanceComparator();

        assertTrue(comparator.compare(arm, reich) < 0, "Das aermere Konto kommt zuerst");
        assertTrue(comparator.compare(reich, arm) > 0);
        assertEquals(0, comparator.compare(mittel, mittel));

        Account[] konten = {reich, arm, mittel};
        Arrays.sort(konten, comparator);

        assertEquals("S-1", konten[0].getId());
        assertEquals("S-2", konten[1].getId());
        assertEquals("S-3", konten[2].getId());
    }

    @Test
    @DisplayName("Die beiden Comparatoren sind genau invers zueinander")
    void testComparatorsAreInverse() {
        Comparator<Object> normal = new AccountBalanceComparator();
        Comparator<Object> inverse = new AccountInverseBalanceComparator();

        assertEquals(normal.compare(arm, reich), -inverse.compare(arm, reich));
        assertEquals(normal.compare(reich, arm), -inverse.compare(reich, arm));
        assertEquals(normal.compare(arm, arm), inverse.compare(arm, arm));
    }
}
