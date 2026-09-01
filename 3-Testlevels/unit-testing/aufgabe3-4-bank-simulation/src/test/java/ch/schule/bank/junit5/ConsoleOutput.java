package ch.schule.bank.junit5;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Test-Hilfsklasse: faengt alles ab, was auf {@link System#out} geschrieben wird.
 *
 * <p>Die Banken-Simulation gibt Kontoauszuege ueber <code>System.out.println()</code>
 * aus. Um diese Methoden ueberhaupt testen zu koennen, muss der Standard-Output
 * waehrend des Tests umgeleitet werden. Diese Klasse kapselt das Umleiten und
 * stellt sicher, dass der originale Stream danach wieder gesetzt wird.</p>
 *
 * @author David Tarlos
 * @version 1.0
 */
final class ConsoleOutput {

    private ConsoleOutput() {
        // Utility-Klasse, keine Instanzen
    }

    /**
     * Fuehrt den gegebenen Code aus und liefert dessen Konsolen-Ausgabe zurueck.
     *
     * @param code der auszufuehrende Code (z.B. <code>account::print</code>)
     * @return alles, was der Code auf System.out geschrieben hat
     */
    static String capture(Runnable code) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            code.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * Wie {@link #capture(Runnable)}, liefert die Ausgabe aber zeilenweise.
     *
     * @param code der auszufuehrende Code
     * @return die Ausgabezeilen ohne abschliessende Leerzeile
     */
    static String[] captureLines(Runnable code) {
        String output = capture(code);
        if (output.isEmpty()) {
            return new String[0];
        }
        return output.stripTrailing().split("\\R");
    }
}
