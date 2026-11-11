package com.unibague.pagoservice.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad de pago. Database per Service: tabla propia en pagos_db.
 * El dinero se maneja con BigDecimal + NUMERIC (no double).
 */
@Entity
@Table(name = "pago")
public class Pago {

    public enum Estado {
        PENDIENTE, APROBADO, RECHAZADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private String pedidoId;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    // Getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPedidoId() { return pedidoId; }
    public void setPedidoId(String pedidoId) { this.pedidoId = pedidoId; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
}
