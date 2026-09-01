package ch.tbz.m450.service;

import ch.tbz.m450.repository.Address;
import ch.tbz.m450.repository.AddressRepository;
import ch.tbz.m450.util.AddressComparator.SortDirection;
import ch.tbz.m450.util.AddressComparator.SortField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests fuer den Service. Das Repository ist ein Mockito-Mock, damit hier
 * weder H2 noch ein Spring-Context hochgefahren werden muss - die Tests pruefen
 * nur die Logik des Service (Delegation + Sortierung).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService (Repository gemockt)")
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressService addressService;

    private Address anna;
    private Address bert;
    private Address clara;

    @BeforeEach
    void setUp() {
        anna = new Address(3, "Anna", "Muster", "079 300 00 00", date(2023));
        bert = new Address(1, "Bert", "Beispiel", "044 100 00 00", date(2021));
        clara = new Address(2, "clara", "muster", "062 200 00 00", date(2022));
    }

    private static Date date(int year) {
        return Date.from(LocalDate.of(year, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    @Test
    @DisplayName("save reicht die Adresse ans Repository weiter und gibt das Ergebnis zurueck")
    void saveDelegatesToRepository() {
        when(addressRepository.save(any(Address.class))).thenReturn(anna);

        Address saved = addressService.save(anna);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(anna);
        assertThat(saved).isSameAs(anna);
        verifyNoMoreInteractions(addressRepository);
    }

    @Test
    @DisplayName("getAll sortiert die unsortierte Repository-Antwort nach Nachname/Vorname")
    void getAllSortsByLastname() {
        when(addressRepository.findAll()).thenReturn(List.of(anna, clara, bert));

        List<Address> result = addressService.getAll();

        assertThat(result).extracting(Address::getId).containsExactly(1, 3, 2);
        verify(addressRepository).findAll();
    }

    @Test
    @DisplayName("getAll laesst die Liste aus dem Repository unveraendert - es wird eine neue erstellt")
    void getAllDoesNotModifyRepositoryResult() {
        List<Address> fromRepository = List.of(anna, clara, bert);
        when(addressRepository.findAll()).thenReturn(fromRepository);

        List<Address> result = addressService.getAll();

        assertThat(fromRepository).containsExactly(anna, clara, bert);
        assertThat(result).isNotSameAs(fromRepository);
    }

    @Test
    @DisplayName("getAll mit Sortierfeld nutzt das gewuenschte Attribut")
    void getAllWithSortFieldUsesThatField() {
        when(addressRepository.findAll()).thenReturn(List.of(anna, clara, bert));

        List<Address> result = addressService.getAll(SortField.FIRSTNAME, SortDirection.ASC);

        assertThat(result).extracting(Address::getFirstname).containsExactly("Anna", "Bert", "clara");
    }

    @Test
    @DisplayName("getAll mit DESC dreht die Reihenfolge um")
    void getAllDescending() {
        when(addressRepository.findAll()).thenReturn(List.of(anna, clara, bert));

        List<Address> result = addressService.getAll(SortField.ID, SortDirection.DESC);

        assertThat(result).extracting(Address::getId).containsExactly(3, 2, 1);
    }

    @Test
    @DisplayName("getAll gibt eine leere Liste zurueck, wenn das Repository nichts liefert")
    void getAllReturnsEmptyList() {
        when(addressRepository.findAll()).thenReturn(List.of());

        assertThat(addressService.getAll()).isEmpty();
    }

    @Test
    @DisplayName("getAddress gibt die gefundene Adresse zurueck")
    void getAddressReturnsFoundAddress() {
        when(addressRepository.findById(3)).thenReturn(Optional.of(anna));

        Optional<Address> result = addressService.getAddress(3);

        assertThat(result).containsSame(anna);
        verify(addressRepository).findById(3);
        verify(addressRepository, never()).findAll();
    }

    @Test
    @DisplayName("getAddress gibt Optional.empty zurueck, wenn es die id nicht gibt")
    void getAddressReturnsEmptyForUnknownId() {
        when(addressRepository.findById(999)).thenReturn(Optional.empty());

        assertThat(addressService.getAddress(999)).isEmpty();
    }
}
