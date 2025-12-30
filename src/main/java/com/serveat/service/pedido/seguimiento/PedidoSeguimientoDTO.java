package com.serveat.service.pedido.seguimiento;

public class PedidoSeguimientoDTO {

    private final String codigoPedido;

    private final String estadoPedido;
    private final String estadoCocina;
    private final String estadoReparto;

    private final String etiquetaTiempoRestante;   // Ej: "12 min 05 s" o "-" ya decidido en servicio
    private final Double progreso;                 // 0..1 o null si indeterminate
    private final String mensaje;                  // opcional

    // Estilos decididos en servicio (para no meter reglas en la vista)
    private final String estiloPedido;             // "ERROR", "OK", "INFO", "NEUTRO"
    private final String estiloCocina;
    private final String estiloReparto;

    public PedidoSeguimientoDTO(String codigoPedido,
                                String estadoPedido,
                                String estadoCocina,
                                String estadoReparto,
                                String etiquetaTiempoRestante,
                                Double progreso,
                                String mensaje,
                                String estiloPedido,
                                String estiloCocina,
                                String estiloReparto) {
        this.codigoPedido = codigoPedido;
        this.estadoPedido = estadoPedido;
        this.estadoCocina = estadoCocina;
        this.estadoReparto = estadoReparto;
        this.etiquetaTiempoRestante = etiquetaTiempoRestante;
        this.progreso = progreso;
        this.mensaje = mensaje;
        this.estiloPedido = estiloPedido;
        this.estiloCocina = estiloCocina;
        this.estiloReparto = estiloReparto;
    }

    public String getCodigoPedido() { return codigoPedido; }
    public String getEstadoPedido() { return estadoPedido; }
    public String getEstadoCocina() { return estadoCocina; }
    public String getEstadoReparto() { return estadoReparto; }

    public String getEtiquetaTiempoRestante() { return etiquetaTiempoRestante; }
    public Double getProgreso() { return progreso; }
    public String getMensaje() { return mensaje; }

    public String getEstiloPedido() { return estiloPedido; }
    public String getEstiloCocina() { return estiloCocina; }
    public String getEstiloReparto() { return estiloReparto; }
}