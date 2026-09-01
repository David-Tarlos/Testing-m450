package ch.tbz.m450.calculator;

/**
 * Einfacher Taschenrechner mit den vier Grundoperationen.
 *
 * <p>Die Klasse ist bewusst zustandslos gehalten: jede Methode berechnet ihr
 * Resultat ausschliesslich aus den uebergebenen Parametern. Das macht sie
 * ideal fuer Unit-Tests, weil kein Setup und kein Teardown noetig ist.</p>
 *
 * @author David Tarlos
 * @version 1.0
 */
public class Calculator {

    /**
     * Addiert zwei Zahlen.
     *
     * @param summand1 erster Summand
     * @param summand2 zweiter Summand
     * @return die Summe
     */
    public double add(double summand1, double summand2) {
        return summand1 + summand2;
    }

    /**
     * Subtrahiert den Subtrahenden vom Minuenden.
     *
     * @param minuend    die Zahl, von der abgezogen wird
     * @param subtrahend die Zahl, die abgezogen wird
     * @return die Differenz
     */
    public double subtract(double minuend, double subtrahend) {
        return minuend - subtrahend;
    }

    /**
     * Multipliziert zwei Zahlen.
     *
     * @param faktor1 erster Faktor
     * @param faktor2 zweiter Faktor
     * @return das Produkt
     */
    public double multiply(double faktor1, double faktor2) {
        return faktor1 * faktor2;
    }

    /**
     * Dividiert den Dividenden durch den Divisor.
     *
     * <p>Achtung: Bei <code>double</code> liefert Java fuer eine Division
     * durch 0 kein Fehlerverhalten, sondern <code>Infinity</code> bzw.
     * <code>NaN</code>. Weil das fachlich unbrauchbar ist, wird der Fall
     * hier explizit als Fehler behandelt.</p>
     *
     * @param dividend die Zahl, die geteilt wird
     * @param divisor  die Zahl, durch die geteilt wird (darf nicht 0 sein)
     * @return der Quotient
     * @throws ArithmeticException falls der Divisor 0 ist
     */
    public double divide(double dividend, double divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Division durch 0 ist nicht erlaubt");
        }
        return dividend / divisor;
    }
}
