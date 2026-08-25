package ch.tbz.m450.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Testet die Entity selbst, ohne Datenbank und ohne Spring.
 * Einstiegspunkt der Aufgabe: "Fangen Sie an mit dem Testen von Adressen, welche Sie erstellen."
 */
class AddressTest {

    private Address address;
    private Date registrationDate;

    @BeforeEach
    void setUp() {
        registrationDate = new Date(1_700_000_000_000L);
        address = new Address(1, "Max", "Muster", "079 123 45 67", registrationDate);
    }

    @Test
    @DisplayName("Der AllArgsConstructor setzt alle fuenf Felder")
    void allArgsConstructorSetsEveryField() {
        assertEquals(1, address.getId());
        assertEquals("Max", address.getFirstname());
        assertEquals("Muster", address.getLastname());
        assertEquals("079 123 45 67", address.getPhonenumber());
        assertEquals(registrationDate, address.getRegistrationDate());
    }

    @Test
    @DisplayName("Der NoArgsConstructor liefert id 0 und sonst null")
    void noArgsConstructorLeavesFieldsEmpty() {
        Address empty = new Address();

        assertEquals(0, empty.getId(), "int-Id hat keinen null-Zustand, der Default ist 0");
        assertNull(empty.getFirstname());
        assertNull(empty.getLastname());
        assertNull(empty.getPhonenumber());
        assertNull(empty.getRegistrationDate());
    }

    @Test
    @DisplayName("Die Lombok-Setter schreiben jedes Feld")
    void settersOverwriteEveryField() {
        Date other = new Date(1_800_000_000_000L);

        address.setId(42);
        address.setFirstname("Erika");
        address.setLastname("Beispiel");
        address.setPhonenumber("044 000 00 00");
        address.setRegistrationDate(other);

        assertEquals(42, address.getId());
        assertEquals("Erika", address.getFirstname());
        assertEquals("Beispiel", address.getLastname());
        assertEquals("044 000 00 00", address.getPhonenumber());
        assertEquals(other, address.getRegistrationDate());
    }

    @Test
    @DisplayName("Ohne @EqualsAndHashCode sind zwei inhaltsgleiche Adressen nicht gleich")
    void twoIdenticalAddressesAreNotEqual() {
        Address twin = new Address(1, "Max", "Muster", "079 123 45 67", registrationDate);

        assertNotNull(twin);
        // Address erbt equals() von Object -> Referenzvergleich. Befund E-02 im Loesungsdokument.
        assertEquals(false, address.equals(twin));
    }
}
