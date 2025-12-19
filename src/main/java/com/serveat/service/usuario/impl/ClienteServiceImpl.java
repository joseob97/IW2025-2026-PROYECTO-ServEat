package com.serveat.service.usuario.impl;

import com.serveat.domain.usuario.Cliente;
import com.serveat.repository.usuario.ClienteRepository;
import com.serveat.service.usuario.ClienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepo;

    public ClienteServiceImpl(ClienteRepository clienteRepo) {
        this.clienteRepo = clienteRepo;
    }

    @Override
    public Cliente obtenerPorUsername(String username) {
        return clienteRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }
}