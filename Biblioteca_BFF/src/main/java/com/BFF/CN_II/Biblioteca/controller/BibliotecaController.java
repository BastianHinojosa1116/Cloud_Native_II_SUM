package com.BFF.CN_II.Biblioteca.controller;

import com.BFF.CN_II.Biblioteca.model.Libro;
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

    // --- USUARIOS ---
    @PostMapping("/usuarios")
    public ResponseEntity<String> crearUsuario(@RequestBody Usuario usuario) {
        return ResponseEntity.ok(bibliotecaService.enviarEvento("Usuario.Creado", usuario));
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<String> eliminarUsuario(@PathVariable int id) {
        return ResponseEntity.ok(bibliotecaService.enviarEventoSimple("Usuario.Eliminado", id));
    }

    // --- LIBROS ---
    @PostMapping("/libros")
    public ResponseEntity<String> crearLibro(@RequestBody Libro libro) {
        return ResponseEntity.ok(bibliotecaService.enviarEvento("Libro.Creado", libro));
    }

    @DeleteMapping("/libros/{id}")
    public ResponseEntity<String> eliminarLibro(@PathVariable int id) {
        return ResponseEntity.ok(bibliotecaService.enviarEventoSimple("Libro.Eliminado", id));
    }

    // --- PRÉSTAMOS ---
    @PostMapping("/prestamos")
    public ResponseEntity<String> crearPrestamo(@RequestBody Prestamo prestamo) {
        return ResponseEntity.ok(bibliotecaService.enviarEvento("Prestamo.Creado", prestamo));
    }

    @DeleteMapping("/prestamos/{id}")
    public ResponseEntity<String> eliminarPrestamo(@PathVariable int id) {
        return ResponseEntity.ok(bibliotecaService.enviarEventoSimple("Prestamo.Eliminado", id));
    }

    @PutMapping("/usuarios")
    public ResponseEntity<String> actualizarUsuario(@RequestBody Usuario usuario) {

        String resultado = bibliotecaService.enviarEvento("Usuario.Modificado", usuario);
        return ResponseEntity.ok(resultado);
    }
}