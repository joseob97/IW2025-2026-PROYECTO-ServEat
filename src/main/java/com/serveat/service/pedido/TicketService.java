package com.serveat.service.pedido;

public interface TicketService {

    byte[] generarTicketCliente(String codigoPedido, String username);

    byte[] generarTicketCamarero(String codigoPedido);
}