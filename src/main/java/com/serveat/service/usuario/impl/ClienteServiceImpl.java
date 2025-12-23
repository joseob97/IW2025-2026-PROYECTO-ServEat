package com.serveat.service.usuario.impl;

import com.serveat.domain.usuario.Cliente;
import com.serveat.repository.usuario.ClienteRepository;
import com.serveat.service.usuario.ClienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepo;

    public ClienteServiceImpl(ClienteRepository clienteRepo) {
        this.clienteRepo = clienteRepo;
    }

    // =========================
    // LOGIN / SEGURIDAD
    // =========================
    @Override
    public Cliente obtenerPorUsername(String username) {
        return clienteRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }

    // =========================
    // GESTIÓN DE CLIENTES (ADMIN)
    // =========================
    @Override
    public List<Cliente> obtenerTodos() {
        return clienteRepo.findAll();
    }

    @Override
    @Transactional
    public Cliente guardar(Cliente cliente) {
        return clienteRepo.save(cliente);
    }

    @Override
    @Transactional
    public void desactivar(Cliente cliente) {
        cliente.setActivo(false);
        clienteRepo.save(cliente);
    }

    @Override
    @Transactional
    public void eliminar(Cliente cliente) {
        clienteRepo.delete(cliente);
    }
}
