package com.serveat.domain.caja;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CIERRES_CAJA")
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalDateTime fechaHoraCierre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGeneral;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEfectivo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTarjeta;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPaypal;

    protected CierreCaja() {}

    public CierreCaja(LocalDate fecha, BigDecimal totalGeneral, BigDecimal totalEfectivo, BigDecimal totalTarjeta, BigDecimal totalPaypal) {
        this.fecha = fecha;
        this.fechaHoraCierre = LocalDateTime.now();
        this.totalGeneral = totalGeneral;
        this.totalEfectivo = totalEfectivo;
        this.totalTarjeta = totalTarjeta;
        this.totalPaypal = totalPaypal;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalDateTime getFechaHoraCierre() {
        return fechaHoraCierre;
    }

    public BigDecimal getTotalGeneral() {
        return totalGeneral;
    }

    public BigDecimal getTotalEfectivo() {
        return totalEfectivo;
    }

    public BigDecimal getTotalTarjeta() {
        return totalTarjeta;
    }

    public BigDecimal getTotalPaypal() {
        return totalPaypal;
    }
}
