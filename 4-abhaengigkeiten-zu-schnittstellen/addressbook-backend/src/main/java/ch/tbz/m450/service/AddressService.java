package ch.tbz.m450.service;

import ch.tbz.m450.repository.Address;
import ch.tbz.m450.util.AddressComparator;
import ch.tbz.m450.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {
    private final AddressRepository addressRepository;

    @Autowired
    public AddressService(AddressRepository addressRepository) {
        this.addressRepository =  addressRepository;
    }

    public Address save(Address address) {
        return addressRepository.save(address);
    }

    public List<Address> getAll() {
        return addressRepository.findAll().stream().sorted(new AddressComparator()).toList();
    }

    /**
     * Aufgabe 2: gibt alle Adressen nach frei waehlbaren Attributen sortiert zurueck.
     * Bewusst als eigener Name statt als Ueberladung von {@link #getAll()}, weil ein
     * argumentloser Aufruf sonst zwischen beiden Methoden mehrdeutig aussieht.
     */
    public List<Address> getAllSortedBy(AddressComparator.SortField... sortFields) {
        return addressRepository.findAll().stream().sorted(new AddressComparator(sortFields)).toList();
    }

    public Optional<Address> getAddress(int id) {
        return addressRepository.findById(id);
    }
}
