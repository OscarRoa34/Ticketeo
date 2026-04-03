package co.edu.uptc.Ticketeo.purchase.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import co.edu.uptc.Ticketeo.user.models.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "compras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compra_seq")
    @SequenceGenerator(name = "compra_seq", sequenceName = "compra_seq", allocationSize = 1)
    @Column(name = "id_compra")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private User user;

    @Column(name = "id_evento")
    private Integer eventId;

    @Column(name = "nombre_evento", nullable = false, length = 150)
    private String eventName;

    @Column(name = "fecha_evento")
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime purchaseDate;

    @Column(name = "total_pagado", nullable = false)
    private Double totalPaid;

    @Column(name = "cantidad_boletos", nullable = false)
    private Integer totalTickets;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchasedTicket> tickets = new ArrayList<>();
}

