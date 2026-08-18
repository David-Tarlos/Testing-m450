/**
 * Aufgabe 3 - Klasse Preisberechnung (Auto-Verkauf Software)
 *
 * Die Methode calculatePrice ist bewusst 1:1 so uebernommen, wie sie in der
 * Aufgabenstellung vorgegeben wurde. Sie wird NICHT korrigiert - der
 * Testtreiber soll die Fehler ja erst aufdecken.
 *
 * Einzige Anpassung: "static", damit der Testtreiber die Methode ohne
 * Objekt-Instanz aufrufen kann.
 */
public class Preisberechnung {

    static double calculatePrice(double baseprice, double specialprice, double extraprice, int extras, double discount) {
        double addon_discount;
        double result;

        if (extras >= 3)
            addon_discount = 10;
        else if (extras >= 5)
            addon_discount = 15;
        else
            addon_discount = 0;

        if (discount > addon_discount)
            addon_discount = discount;

        result = baseprice / 100.0 * (100 - discount) + specialprice
                + extraprice / 100.0 * (100 - addon_discount);

        return result;
    }
}
