package co.edu.uptc.Ticketeo.events.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evento_tipos_entrada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventTicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evento_tipo_entrada_seq")
    @SequenceGenerator(
            name = "evento_tipo_entrada_seq",
            sequenceName = "evento_tipo_entrada_seq",
            allocationSize = 1
    )
    @Column(name = "id_evento_tipo_entrada")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_entrada", nullable = false)
    private TicketType ticketType;

    @Column(name = "cantidad_disponible", nullable = false)
    private Integer availableQuantity;

    @Column(name = "precio_boleto")
    private Double ticketPrice;
}
