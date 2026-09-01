package ch.tbz.m450.util;

import ch.tbz.m450.repository.Address;

import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

/**
 * Vergleicht {@link Address}-Objekte.
 * <p>
 * Ohne Argumente wird nach Nachname, dann Vorname sortiert (aufsteigend) - das ist das
 * Verhalten, welches der Service seit jeher erwartet hat.
 * Zusaetzlich kann ueber {@link SortField} nach einem anderen Attribut und ueber
 * {@link SortDirection} absteigend sortiert werden.
 * <p>
 * Regeln:
 * <ul>
 *     <li>Texte werden case-insensitive verglichen ("anna" kommt vor "Bert").</li>
 *     <li>{@code null}-Werte landen immer am Ende der aufsteigenden Reihenfolge.</li>
 *     <li>Bei Gleichstand entscheidet die id, damit die Reihenfolge deterministisch bleibt.</li>
 *     <li>{@code DESC} kehrt das gesamte Ergebnis um (inkl. null-Position und Tiebreak).</li>
 * </ul>
 */
public class AddressComparator implements Comparator<Address> {

    /** Attribut, nach welchem verglichen wird. */
    public enum SortField {
        LASTNAME,
        FIRSTNAME,
        PHONENUMBER,
        REGISTRATION_DATE,
        ID
    }

    /** Sortierrichtung. */
    public enum SortDirection {
        ASC,
        DESC
    }

    private static final Comparator<String> TEXT =
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<Date> DATE =
            Comparator.nullsLast(Comparator.<Date>naturalOrder());

    private final SortField field;
    private final SortDirection direction;
    private final Comparator<Address> delegate;

    /** Standard: Nachname, dann Vorname, aufsteigend. */
    public AddressComparator() {
        this(SortField.LASTNAME, SortDirection.ASC);
    }

    /** Nach beliebigem Attribut, aufsteigend. */
    public AddressComparator(SortField field) {
        this(field, SortDirection.ASC);
    }

    public AddressComparator(SortField field, SortDirection direction) {
        this.field = Objects.requireNonNull(field, "field darf nicht null sein");
        this.direction = Objects.requireNonNull(direction, "direction darf nicht null sein");
        this.delegate = delegateFor(field);
    }

    @Override
    public int compare(Address a1, Address a2) {
        Objects.requireNonNull(a1, "a1 darf nicht null sein");
        Objects.requireNonNull(a2, "a2 darf nicht null sein");

        int result = delegate.compare(a1, a2);
        if (result == 0 && field != SortField.ID) {
            // Tiebreak, damit gleiche Namen nicht zufaellig die Reihenfolge tauschen
            result = Integer.compare(a1.getId(), a2.getId());
        }
        return direction == SortDirection.DESC ? -result : result;
    }

    public SortField getField() {
        return field;
    }

    public SortDirection getDirection() {
        return direction;
    }

    private static Comparator<Address> delegateFor(SortField field) {
        return switch (field) {
            case LASTNAME -> Comparator.comparing(Address::getLastname, TEXT)
                    .thenComparing(Address::getFirstname, TEXT);
            case FIRSTNAME -> Comparator.comparing(Address::getFirstname, TEXT)
                    .thenComparing(Address::getLastname, TEXT);
            case PHONENUMBER -> Comparator.comparing(Address::getPhonenumber, TEXT);
            case REGISTRATION_DATE -> Comparator.comparing(Address::getRegistrationDate, DATE);
            case ID -> Comparator.comparingInt(Address::getId);
        };
    }
}
