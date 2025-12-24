package com.serveat.service.usuario.impl;

import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.usuario.EmpleadoRepository;
import com.serveat.service.usuario.EmpleadoService;
import com.serveat.service.usuario.exceptions.DuplicadoException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmpleadoServiceImpl implements EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;

    public EmpleadoServiceImpl(
            EmpleadoRepository empleadoRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.empleadoRepository = empleadoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================
    // CONSULTAS
    // =========================
    @Override
    public List<Empleado> obtenerTodos() {
        return empleadoRepository.findAll();
    }

    @Override
    public List<Empleado> obtenerPorRol(String rol) {
        return empleadoRepository.findByRol(rol);
    }

    @Override
    public Empleado obtenerPorId(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado"));
    }

    @Override
    public Empleado obtenerPorUsername(String username) {
        return empleadoRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado"));
    }

    // =========================
    // GUARDAR / VALIDAR
    // =========================
    @Override
    @Transactional
    public Empleado guardar(Empleado empleado) {

        // ---- VALIDAR EMAIL ÚNICO
        empleadoRepository.findByEmail(empleado.getEmail())
                .filter(e -> !e.getId().equals(empleado.getId()))
                .ifPresent(e -> {
                    throw new DuplicadoException("El email ya está en uso por otro empleado");
                });

        // ---- VALIDAR USERNAME ÚNICO
        empleadoRepository.findByUsername(empleado.getUsername())
                .filter(e -> !e.getId().equals(empleado.getId()))
                .ifPresent(e -> {
                    throw new DuplicadoException("El nombre de usuario ya está en uso");
                });

        // ---- CIFRAR PASSWORD SI NO LO ESTÁ
        if (!empleado.getPassword().startsWith("$2a$")) {
            empleado.setPassword(passwordEncoder.encode(empleado.getPassword()));
        }

        return empleadoRepository.save(empleado);
    }

    // =========================
    // ACTIVAR / DESACTIVAR
    // =========================
    @Override
    @Transactional
    public void activar(Empleado empleado) {
        empleado.setEnabled(true);
        empleadoRepository.save(empleado);
    }

    @Override
    @Transactional
    public void desactivar(Empleado empleado) {
        empleado.setEnabled(false);
        empleadoRepository.save(empleado);
    }

    // =========================
    // ELIMINAR
    // =========================
    @Override
    @Transactional
    public void eliminar(Empleado empleado) {
        empleadoRepository.delete(empleado);
    }
}
