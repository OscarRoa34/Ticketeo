package co.edu.uptc.Ticketeo.events.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "eventos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evento_seq")
    @SequenceGenerator(name = "evento_seq", sequenceName = "evento_seq", allocationSize = 1)
    @Column(name = "id_evento")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_categoria")
    private EventCategory category;

    @Column(name = "nombre_evento", nullable = false, length = 150)
    private String name;

    @Column(name = "fecha_evento")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Column(name = "fecha_creacion_evento", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "valor_minimo_evento")
    private Double price;

    @Column(name = "descripcion_evento", length = 500)
    private String description;

    @Column(name = "imagen_evento")
    private String imageUrl;

    @Column(name = "estado")
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventTicketType> ticketTypes = new ArrayList<>();

    @PrePersist
    @SuppressWarnings("unused")
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}