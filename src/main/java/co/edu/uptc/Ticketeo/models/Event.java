package co.edu.uptc.Ticketeo.models;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.persistence.*;
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
    @JoinColumn(name = "id_categoria", nullable = false)
    private EventCategory categoria;

    @Column(name = "nombre_evento", nullable = false, length = 150)
    private String nombre;

    @Column(name = "fecha_evento")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    @Column(name = "valor_evento")
    private Double valor;

    @Column(name = "descripcion_evento", length = 500)
    private String descripcion;

    @Column(name = "imagen_evento")
    private String urlImagen;

    @Column(name = "estado")
    @Builder.Default
    private Boolean estado = true;
}
