
public class TestTreiber {

    public static void main(String[] args) {
        if (test_calculate_price()) {
            System.out.println("\nAlle Testfaelle bestanden.");
        } else {
            System.out.println("\nEs sind Testfaelle fehlgeschlagen.");
        }
    }

    static boolean test_calculate_price() {
        boolean test_ok = true;

        test_ok &= pruefe("nur Grundpreis",
                20000,     0,     0,  0,   0,   20000.00);
        test_ok &= pruefe("Grundpreis, 10% Haendlerrabatt",
                20000,     0,     0,  0,  10,   18000.00);
        test_ok &= pruefe("Grundpreis + Sondermodell",
                20000,  2500,     0,  0,   0,   22500.00);
        test_ok &= pruefe("2 Extras -> 0% Rabatt",
                20000,     0,  1000,  2,   0,   21000.00);
        test_ok &= pruefe("3 Extras -> 10% Rabatt",
                20000,     0,  1000,  3,   0,   20900.00);
        test_ok &= pruefe("5 Extras -> 15% Rabatt",
                20000,     0,  1000,  5,   0,   20850.00);
        test_ok &= pruefe("3 Extras + 20% Haendlerrabatt",
                20000,     0,  1000,  3,  20,   16900.00);

        return test_ok;
    }

    /** Ruft die Methode auf, vergleicht Ist mit Soll  */
    static boolean pruefe(String name,
                          double baseprice, double specialprice, double extraprice,
                          int extras, double discount, double erwartet) {

        double price = Preisberechnung.calculatePrice(baseprice, specialprice, extraprice, extras, discount);
        // double nie mit == vergleichen -> kleine Toleranz wegen Rundungsfehlern

        boolean ok = Math.abs(price - erwartet) < 0.001;

        System.out.printf("[%s] %-32s Soll=%9.2f  Ist=%9.2f%n",
                ok ? "OK  " : "FAIL", name, erwartet, price);

        return ok;
    }
}
