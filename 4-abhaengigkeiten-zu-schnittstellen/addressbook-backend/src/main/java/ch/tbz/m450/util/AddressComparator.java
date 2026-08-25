package ch.tbz.m450.util;

import ch.tbz.m450.repository.Address;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * Vergleicht zwei {@link Address}-Objekte.
 *
 * <p>Aufgabe 1: Die Vorgabe lieferte konstant {@code -1} zurueck. Das verletzt den
 * Vertrag von {@link Comparator} gleich zweifach: {@code compare(a, a)} muss 0 sein
 * (Reflexivitaet) und {@code compare(a, b)} muss das umgekehrte Vorzeichen von
 * {@code compare(b, a)} haben (Antisymmetrie). Praktische Folge war, dass
 * {@code stream().sorted(...)} die Liste lediglich umgedreht hat, ohne die Daten
 * ueberhaupt anzuschauen.
 *
 * <p>Aufgabe 2: Ueber {@link SortField} laesst sich nach beliebigen Attributen und
 * mehrstufig sortieren. Der parameterlose Konstruktor behaelt die bisherige Signatur,
 * damit bestehende Aufrufe unveraendert weiterlaufen.
 */
public class AddressComparator implements Comparator<Address> {

    /** Attribute, nach denen sortiert werden kann. */
    public enum SortField {
        ID,
        FIRSTNAME,
        LASTNAME,
        PHONENUMBER,
        REGISTRATION_DATE;

        Comparator<Address> comparator() {
            return switch (this) {
                case ID -> Comparator.comparingInt(Address::getId);
                case FIRSTNAME -> text(Address::getFirstname);
                case LASTNAME -> text(Address::getLastname);
                case PHONENUMBER -> text(Address::getPhonenumber);
                case REGISTRATION_DATE -> Comparator.comparing(
                        Address::getRegistrationDate,
                        Comparator.nullsLast(Comparator.<Date>naturalOrder()));
            };
        }

        /** Textvergleich ohne Ruecksicht auf Gross-/Kleinschreibung, {@code null} zuletzt. */
        private static Comparator<Address> text(Function<Address, String> getter) {
            return Comparator.comparing(getter, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }
    }

    /**
     * Standardreihenfolge: Nachname, dann Vorname, dann Id. Die Id am Schluss stellt
     * sicher, dass die Sortierung auch bei Namensgleichheit eindeutig bleibt.
     */
    private static final SortField[] DEFAULT_ORDER = {
            SortField.LASTNAME, SortField.FIRSTNAME, SortField.ID
    };

    private final List<SortField> sortFields;
    private final Comparator<Address> delegate;

    /** Sortiert nach Nachname, Vorname, Id. */
    public AddressComparator() {
        this(DEFAULT_ORDER);
    }

    /**
     * Sortiert nach den angegebenen Feldern in genau dieser Reihenfolge.
     *
     * @throws IllegalArgumentException wenn kein Feld oder ein {@code null}-Feld uebergeben wird
     */
    public AddressComparator(SortField... sortFields) {
        if (sortFields == null || sortFields.length == 0) {
            throw new IllegalArgumentException("Mindestens ein Sortierfeld angeben");
        }
        for (SortField field : sortFields) {
            if (field == null) {
                throw new IllegalArgumentException("Sortierfeld darf nicht null sein");
            }
        }
        this.sortFields = List.of(sortFields);

        Comparator<Address> combined = null;
        for (SortField field : this.sortFields) {
            combined = (combined == null) ? field.comparator() : combined.thenComparing(field.comparator());
        }
        this.delegate = Comparator.nullsLast(combined);
    }

    @Override
    public int compare(Address a1, Address a2) {
        return delegate.compare(a1, a2);
    }

    /** Die verwendete Sortierreihenfolge, unveraenderlich. */
    public List<SortField> getSortFields() {
        return sortFields;
    }
}
