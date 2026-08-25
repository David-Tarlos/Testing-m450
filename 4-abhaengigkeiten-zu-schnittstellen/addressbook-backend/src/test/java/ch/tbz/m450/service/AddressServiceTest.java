package ch.tbz.m450.service;

import ch.tbz.m450.repository.Address;
import ch.tbz.m450.repository.AddressRepository;
import ch.tbz.m450.util.AddressComparator.SortField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Kernstueck der Aufgabe: "Versuchen Sie den Service zu testen indem Sie die h2
 * Datenbank weg mocken."
 *
 * <p>{@link AddressService} haengt ueber {@link AddressRepository} an der Datenbank.
 * {@code @Mock} ersetzt das Repository durch ein Test Double, {@code @InjectMocks}
 * schiebt es ueber den Konstruktor in den Service. Es laeuft dadurch weder Spring
 * noch H2 - diese Tests brauchen wenige Millisekunden statt einen Kontextstart.
 *
 * <p>Bewusst kommen beide Kategorien aus dem Kapitel vor:
 * <ul>
 *   <li><b>Stub</b> (State Testing): {@code when(...).thenReturn(...)} liefert feste
 *       Daten, geprueft wird das Ergebnis des Service.</li>
 *   <li><b>Mock</b> (Behavioral Testing): {@code verify(...)} prueft, ob der Service
 *       das Repository ueberhaupt und richtig aufgerufen hat.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressService addressService;

    @Captor
    private ArgumentCaptor<Address> savedAddress;

    private Address meierAnna;
    private Address meierBeat;
    private Address aebiZoe;

    private static Address address(int id, String firstname, String lastname, String phone, long epochMillis) {
        return new Address(id, firstname, lastname, phone, new Date(epochMillis));
    }

    @BeforeEach
    void setUp() {
        meierAnna = address(3, "Anna", "Meier", "079 300 00 00", 3_000L);
        meierBeat = address(1, "Beat", "Meier", "079 100 00 00", 1_000L);
        aebiZoe = address(2, "Zoe", "Aebi", "079 200 00 00", 2_000L);
    }

    // ---------------------------------------------------------------- save()

    @Test
    @DisplayName("save() gibt zurueck, was das Repository liefert")
    void saveReturnsWhatTheRepositoryReturns() {
        Address persisted = address(3, "Anna", "Meier", "079 300 00 00", 3_000L);
        when(addressRepository.save(meierAnna)).thenReturn(persisted);

        Address result = addressService.save(meierAnna);

        assertSame(persisted, result, "der Service reicht das Ergebnis unveraendert durch");
    }

    @Test
    @DisplayName("save() reicht genau das uebergebene Objekt an das Repository weiter")
    void savePassesTheAddressThrough() {
        when(addressRepository.save(any(Address.class))).thenReturn(meierAnna);

        addressService.save(meierAnna);

        // Behavioral Testing: nicht das Ergebnis interessiert, sondern der Aufruf selbst.
        verify(addressRepository).save(savedAddress.capture());
        assertEquals("Meier", savedAddress.getValue().getLastname());
        assertEquals("Anna", savedAddress.getValue().getFirstname());
        verifyNoMoreInteractions(addressRepository);
    }

    // -------------------------------------------------------------- getAll()

    @Test
    @DisplayName("getAll() sortiert die Daten aus dem Repository")
    void getAllSortsTheRepositoryResult() {
        // Der Stub liefert bewusst unsortiert - die Sortierung muss vom Service kommen.
        when(addressRepository.findAll()).thenReturn(List.of(meierAnna, meierBeat, aebiZoe));

        List<Address> result = addressService.getAll();

        assertEquals(List.of(2, 3, 1), result.stream().map(Address::getId).toList(),
                "Aebi Zoe, Meier Anna, Meier Beat");
    }

    @Test
    @DisplayName("getAll() fragt das Repository genau einmal")
    void getAllHitsTheRepositoryOnce() {
        when(addressRepository.findAll()).thenReturn(List.of(meierAnna));

        addressService.getAll();

        verify(addressRepository, times(1)).findAll();
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAll() liefert eine leere Liste, wenn nichts gespeichert ist")
    void getAllReturnsEmptyListWhenRepositoryIsEmpty() {
        when(addressRepository.findAll()).thenReturn(List.of());

        assertTrue(addressService.getAll().isEmpty());
    }

    @Test
    @DisplayName("getAll() veraendert die Liste des Repository nicht")
    void getAllDoesNotModifyTheSourceList() {
        List<Address> fromRepository = List.of(meierAnna, meierBeat, aebiZoe);
        when(addressRepository.findAll()).thenReturn(fromRepository);

        addressService.getAll();

        assertEquals(List.of(3, 1, 2), fromRepository.stream().map(Address::getId).toList(),
                "stream().sorted() erzeugt eine neue Liste, die Originalreihenfolge bleibt");
    }

    // ------------------------------------------------------- getAllSortedBy()

    @Test
    @DisplayName("Aufgabe 2: getAllSortedBy() sortiert nach dem gewuenschten Attribut")
    void getAllSortedByUsesTheGivenField() {
        when(addressRepository.findAll()).thenReturn(List.of(meierAnna, meierBeat, aebiZoe));

        List<Address> byFirstname = addressService.getAllSortedBy(SortField.FIRSTNAME);

        assertEquals(List.of("Anna", "Beat", "Zoe"),
                byFirstname.stream().map(Address::getFirstname).toList());
    }

    @Test
    @DisplayName("Aufgabe 2: getAllSortedBy() beherrscht mehrstufige Sortierung")
    void getAllSortedByCombinesFields() {
        when(addressRepository.findAll()).thenReturn(List.of(meierAnna, meierBeat, aebiZoe));

        List<Address> byLastnameThenId = addressService.getAllSortedBy(SortField.LASTNAME, SortField.ID);

        assertEquals(List.of(2, 1, 3), byLastnameThenId.stream().map(Address::getId).toList());
    }

    // ---------------------------------------------------------- getAddress()

    @Test
    @DisplayName("getAddress() liefert die gefundene Adresse")
    void getAddressReturnsTheFoundAddress() {
        when(addressRepository.findById(3)).thenReturn(Optional.of(meierAnna));

        Optional<Address> result = addressService.getAddress(3);

        assertTrue(result.isPresent());
        assertSame(meierAnna, result.get());
    }

    @Test
    @DisplayName("getAddress() liefert ein leeres Optional bei unbekannter Id")
    void getAddressReturnsEmptyForUnknownId() {
        when(addressRepository.findById(999)).thenReturn(Optional.empty());

        assertFalse(addressService.getAddress(999).isPresent());
    }

    @Test
    @DisplayName("getAddress() reicht die Id unveraendert weiter und laedt nicht alles")
    void getAddressForwardsTheIdWithoutLoadingEverything() {
        when(addressRepository.findById(anyInt())).thenReturn(Optional.empty());

        addressService.getAddress(42);

        verify(addressRepository).findById(42);
        verify(addressRepository, never()).findAll();
    }
}
