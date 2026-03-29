package co.edu.uptc.Ticketeo.events.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipo_entradas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tipo_entrada_seq")
    @SequenceGenerator(
            name = "tipo_entrada_seq",
            sequenceName = "tipo_entrada_seq",
            allocationSize = 1
    )
    @Column(name = "id_tipo_entrada")
    private Integer id;

    @Column(name = "nombre_tipo", nullable = false, length = 100)
    private String name;
}
