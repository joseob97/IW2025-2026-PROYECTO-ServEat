package com.serveat.service.usuario.impl;

import com.serveat.domain.usuario.Cliente;
import com.serveat.repository.usuario.ClienteRepository;
import com.serveat.service.usuario.ClienteService;
import com.serveat.service.usuario.exceptions.DuplicadoException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepo;
    private final PasswordEncoder passwordEncoder;

    public ClienteServiceImpl(ClienteRepository clienteRepo,
                              PasswordEncoder passwordEncoder) {
        this.clienteRepo = clienteRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================
    // LOGIN / SEGURIDAD
    // =========================
    @Override
    @Cacheable(value = "clientes", key = "#username")
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
    public Cliente obtenerPorId(Long id) {
        return clienteRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }

    @Override
    @Transactional
    @CacheEvict(value = "clientes", key = "#cliente.username")
    public Cliente guardar(Cliente cliente) {

        // =========================
        // VALIDAR EMAIL DUPLICADO
        // =========================
        if (clienteRepo.existsByEmail(cliente.getEmail())) {
            Cliente existente = clienteRepo.findByEmail(cliente.getEmail()).orElse(null);
            if (existente != null && !existente.getId().equals(cliente.getId())) {
                throw new DuplicadoException("El email ya está registrado");
            }
        }

        // =========================
        // VALIDAR USERNAME DUPLICADO
        // =========================
        if (clienteRepo.existsByUsername(cliente.getUsername())) {
            Cliente existente = clienteRepo.findByUsername(cliente.getUsername()).orElse(null);
            if (existente != null && !existente.getId().equals(cliente.getId())) {
                throw new DuplicadoException("El nombre de usuario ya está en uso");
            }
        }

        // =========================
        // ENCRIPTAR PASSWORD
        // =========================
        if (cliente.getPassword() != null &&
                !cliente.getPassword().startsWith("$2a$")) {

            cliente.setPassword(
                    passwordEncoder.encode(cliente.getPassword())
            );
        }

        return clienteRepo.save(cliente);
    }

    @Override
    @Transactional
    @CacheEvict(value = "clientes", key = "#cliente.username")
    public void activar(Cliente cliente) {
        cliente.setActivo(true);
        clienteRepo.save(cliente);
    }

    @Override
    @Transactional
    @CacheEvict(value = "clientes", key = "#cliente.username")
    public void desactivar(Cliente cliente) {
        cliente.setActivo(false);
        clienteRepo.save(cliente);
    }

    @Override
    @Transactional
    @CacheEvict(value = "clientes", key = "#cliente.username")
    public void eliminar(Cliente cliente) {
        clienteRepo.delete(cliente);
    }
}
