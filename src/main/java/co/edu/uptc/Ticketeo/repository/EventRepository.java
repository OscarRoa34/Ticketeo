package co.edu.uptc.Ticketeo.repository;

import co.edu.uptc.Ticketeo.models.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    Page<Event> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Event> findByCategory_Id(Integer categoryId, Pageable pageable);

    Page<Event> findByNameContainingIgnoreCaseAndCategory_Id(String name, Integer categoryId, Pageable pageable);

    List<Event> findByCategory_Id(Integer categoryId);

    @Modifying
    @Query("DELETE FROM Event e WHERE e.category.id = :categoryId")
    void deleteByCategory_Id(Integer categoryId);
}


