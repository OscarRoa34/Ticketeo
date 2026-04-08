package co.edu.uptc.Ticketeo.events.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uptc.Ticketeo.events.models.TicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.events.repositories.TicketTypeRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketTypeService {

    private static final String REQUIRED_NAME_MESSAGE = "El nombre del tipo de ticket es obligatorio.";
    private static final String DUPLICATE_NAME_MESSAGE = "Ya existe un tipo de ticket con ese nombre.";

    private final TicketTypeRepository ticketTypeRepository;
    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final EventService eventService;

    public TicketType saveTicketType(TicketType ticketType) {
        String normalizedName = ticketType.getName() == null ? "" : ticketType.getName().trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException(REQUIRED_NAME_MESSAGE);
        }

        boolean duplicateExists = ticketType.getId() == null
                ? ticketTypeRepository.existsByNameIgnoreCase(normalizedName)
                : ticketTypeRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, ticketType.getId());

        if (duplicateExists) {
            throw new IllegalStateException(DUPLICATE_NAME_MESSAGE);
        }

        ticketType.setName(normalizedName);
        return ticketTypeRepository.save(ticketType);
    }

    public List<TicketType> getAllTicketTypes() {
        return ticketTypeRepository.findAll(Sort.by("name").ascending());
    }

    public Page<TicketType> getTicketTypesPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ticketTypeRepository.findAll(pageable);
    }

    public TicketType getTicketTypeById(Integer id) {
        return ticketTypeRepository.findById(id).orElse(null);
    }

    public List<String> getTicketTypeNamesExcludingId(Integer excludedId) {
        return ticketTypeRepository.findAll(Sort.by("name").ascending())
                .stream()
                .filter(ticketType -> excludedId == null || !excludedId.equals(ticketType.getId()))
                .map(TicketType::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    @Transactional
    public void deleteTicketType(Integer id) {
        List<Integer> impactedEventIds = eventTicketTypeRepository.findDistinctEventIdsByTicketTypeId(id);
        eventTicketTypeRepository.deleteByTicketType_Id(id);
        ticketTypeRepository.deleteById(id);
        if (impactedEventIds != null) {
            impactedEventIds.forEach(eventService::recalculateMinimumAvailablePrice);
        }
    }
}
