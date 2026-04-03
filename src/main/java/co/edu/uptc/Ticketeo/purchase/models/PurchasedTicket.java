package co.edu.uptc.Ticketeo.purchase.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "boletos_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchasedTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "boleto_compra_seq")
    @SequenceGenerator(name = "boleto_compra_seq", sequenceName = "boleto_compra_seq", allocationSize = 1)
    @Column(name = "id_boleto_compra")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_compra", nullable = false)
    private Purchase purchase;

    @Column(name = "tipo_boleto", nullable = false, length = 100)
    private String ticketTypeName;

    @Column(name = "precio_unitario", nullable = false)
    private Double unitPrice;

    @Column(name = "codigo_qr", nullable = false, unique = true, length = 120)
    private String qrCode;
}

