package ch.tbz.m450.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DuplicateKeyException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gegenstueck zu {@code AddressServiceTest}: hier wird bewusst <b>nicht</b> gemockt.
 *
 * <p>{@code @DataJpaTest} startet nur die JPA-Schicht mit der H2-In-Memory-Datenbank
 * und rollt nach jedem Test zurueck. Das ist ein Integrationstest - er prueft, ob das
 * Mapping der Entity und die Repository-Methoden gegen eine echte Datenbank
 * funktionieren. Genau dafuer taugt ein Mock nicht: ein gemocktes Repository wuerde
 * nur beweisen, dass Mockito funktioniert.
 */
@DataJpaTest
class AddressRepositoryTest {

    @Autowired
    private AddressRepository addressRepository;

    private Address meierAnna;
    private Address aebiZoe;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        meierAnna = new Address(3, "Anna", "Meier", "079 300 00 00", new Date(3_000L));
        aebiZoe = new Address(2, "Zoe", "Aebi", "079 200 00 00", new Date(2_000L));
    }

    @Test
    @DisplayName("Eine gespeicherte Adresse laesst sich ueber ihre Id wiederfinden")
    void savedAddressCanBeFoundById() {
        addressRepository.save(meierAnna);

        Optional<Address> found = addressRepository.findById(3);

        assertTrue(found.isPresent());
        assertEquals("Meier", found.get().getLastname());
        assertEquals("Anna", found.get().getFirstname());
    }

    @Test
    @DisplayName("findById() liefert ein leeres Optional bei unbekannter Id")
    void findByIdReturnsEmptyForUnknownId() {
        assertFalse(addressRepository.findById(999).isPresent());
    }

    @Test
    @DisplayName("findAll() liefert alle gespeicherten Adressen")
    void findAllReturnsEverythingThatWasSaved() {
        addressRepository.save(meierAnna);
        addressRepository.save(aebiZoe);

        List<Address> all = addressRepository.findAll();

        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("findAll() liefert unsortiert - die Reihenfolge macht der Service")
    void findAllMakesNoOrderingPromise() {
        addressRepository.save(meierAnna);
        addressRepository.save(aebiZoe);

        List<Address> all = addressRepository.findAll();

        assertTrue(all.stream().anyMatch(a -> a.getId() == 3));
        assertTrue(all.stream().anyMatch(a -> a.getId() == 2));
    }

    @Test
    @DisplayName("Befund E-01a: eine einzelne Adresse ohne gesetzte Id wird auf Id 0 gespeichert")
    void anAddressWithoutAnIdIsStoredUnderZero() {
        // Address.id ist ein primitives int ohne @GeneratedValue - der Default ist 0.
        // Die Datenbank vergibt keine Id, der Wert 0 wird kommentarlos uebernommen.
        addressRepository.save(new Address(0, "Ohne", "Id", "079", new Date(1_000L)));

        assertTrue(addressRepository.findById(0).isPresent(), "Id 0 ist ein regulaerer Schluessel");
    }

    @Test
    @DisplayName("Befund E-01b: eine zweite Adresse ohne Id kollidiert auf Id 0")
    void twoAddressesWithoutAnIdCollide() {
        addressRepository.save(new Address(0, "Erste", "Adresse", "079", new Date(1_000L)));

        // Gemessenes Verhalten: der zweite save() wird nicht etwa still zusammengefuehrt,
        // sondern abgewiesen - Hibernate wirft NonUniqueObjectException, Spring uebersetzt
        // das in eine DuplicateKeyException.
        DuplicateKeyException thrown = assertThrows(DuplicateKeyException.class,
                () -> addressRepository.save(new Address(0, "Zweite", "Adresse", "079", new Date(2_000L))));

        assertTrue(thrown.getMessage().contains("Address#0"), thrown.getMessage());
    }

    @Test
    @DisplayName("deleteAll() leert die Tabelle")
    void deleteAllEmptiesTheTable() {
        addressRepository.save(meierAnna);

        addressRepository.deleteAll();

        assertTrue(addressRepository.findAll().isEmpty());
    }
}
