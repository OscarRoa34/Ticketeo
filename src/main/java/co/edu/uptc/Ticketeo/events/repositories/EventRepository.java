package co.edu.uptc.Ticketeo.events.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.uptc.Ticketeo.events.models.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

    Page<Event> findByIsActiveTrue(Pageable pageable);

    Page<Event> findByNameContainingIgnoreCaseAndIsActiveTrue(String name, Pageable pageable);

    Page<Event> findByCategory_IdAndIsActiveTrue(Integer categoryId, Pageable pageable);

    Page<Event> findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveTrue(String name, Integer categoryId, Pageable pageable);

    List<Event> findByIsActiveTrue();

    Page<Event> findByIsActiveFalse(Pageable pageable);

    List<Event> findByCategory_Id(Integer categoryId);

    Page<Event> findByNameContainingIgnoreCaseAndIsActiveFalse(String name, Pageable pageable);

    Page<Event> findByCategory_IdAndIsActiveFalse(Integer categoryId, Pageable pageable);

    Page<Event> findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveFalse(String name, Integer categoryId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.isActive = true AND (e.date IS NULL OR e.date >= :today)")
    Page<Event> findManageableActiveEvents(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.isActive = true AND (e.date IS NULL OR e.date >= :today)")
    long countManageableActiveEvents(@Param("today") LocalDate today);

    @Query("SELECT e FROM Event e WHERE e.isActive = true AND (e.date IS NULL OR e.date >= :today) AND LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Event> findManageableActiveEventsByName(@Param("name") String name, @Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.isActive = true AND (e.date IS NULL OR e.date >= :today) AND e.category.id = :categoryId")
    Page<Event> findManageableActiveEventsByCategory(@Param("categoryId") Integer categoryId, @Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.isActive = true AND (e.date IS NULL OR e.date >= :today) AND LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')) AND e.category.id = :categoryId")
    Page<Event> findManageableActiveEventsByNameAndCategory(@Param("name") String name,
                                                             @Param("categoryId") Integer categoryId,
                                                             @Param("today") LocalDate today,
                                                             Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.isActive = true AND e.date < :today")
    Page<Event> findCompletedEvents(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.isActive = true AND e.date < :today")
    long countCompletedEvents(@Param("today") LocalDate today);

    @Query("SELECT e FROM Event e WHERE e.isActive = true AND e.date < :today AND LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Event> findCompletedEventsByName(@Param("name") String name, @Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.isActive = true AND e.date < :today AND e.category.id = :categoryId")
    Page<Event> findCompletedEventsByCategory(@Param("categoryId") Integer categoryId, @Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.isActive = true AND e.date < :today AND LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')) AND e.category.id = :categoryId")
    Page<Event> findCompletedEventsByNameAndCategory(@Param("name") String name,
                                                      @Param("categoryId") Integer categoryId,
                                                      @Param("today") LocalDate today,
                                                      Pageable pageable);

    @Modifying
    @Query("UPDATE Event e SET e.category = null WHERE e.category.id = :categoryId")
    void detachCategory(@Param("categoryId") Integer categoryId);

    @Modifying
    @Query("DELETE FROM Event e WHERE e.category.id = :categoryId")
    void deleteByCategory_Id(Integer categoryId);
}
