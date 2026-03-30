package com.BFF.CN_II.Biblioteca.controller;

import com.BFF.CN_II.Biblioteca.model.Prestamo;
import com.BFF.CN_II.Biblioteca.model.Usuario;
import com.BFF.CN_II.Biblioteca.service.BibliotecaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bff")
public class BibliotecaController {

    private final BibliotecaService bibliotecaService;

    public BibliotecaController(BibliotecaService bibliotecaService) {
        this.bibliotecaService = bibliotecaService;
    }

    @GetMapping("/usuarios")
    public ResponseEntity<String> listarUsuarios() {
        return ResponseEntity.ok(bibliotecaService.listarUsuarios());
    }

    @PostMapping("/usuarios")
    public ResponseEntity<String> crearUsuario(@RequestBody Usuario usuario) {
        return ResponseEntity.ok(bibliotecaService.crearUsuario(usuario));
    }

    @GetMapping("/prestamos")
    public ResponseEntity<String> listarPrestamos() {
        return ResponseEntity.ok(bibliotecaService.listarPrestamos());
    }

    @PostMapping("/prestamos")
    public ResponseEntity<String> crearPrestamo(@RequestBody Prestamo prestamo) {
        return ResponseEntity.ok(bibliotecaService.crearPrestamo(prestamo));
    }
}