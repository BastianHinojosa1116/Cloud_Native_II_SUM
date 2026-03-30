package com.BFF.CN_II.Biblioteca.service;

import com.BFF.CN_II.Biblioteca.model.Prestamo;
import com.BFF.CN_II.Biblioteca.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class BibliotecaService {

    @Value("${functions.base.url}")
    private String functionsBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String listarUsuarios() {
        try {
            return restTemplate.getForObject(functionsBaseUrl + "/listarUsuarios", String.class);
        } catch (HttpStatusCodeException e) {
            return "Error Azure Function listarUsuarios: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "Error BFF listarUsuarios: " + e.getMessage();
        }
    }

    public String crearUsuario(Usuario usuario) {
        try {
            String jsonBody = objectMapper.writeValueAsString(usuario);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    functionsBaseUrl + "/crearUsuario",
                    request,
                    String.class);

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            return "Error Azure Function crearUsuario: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "Error BFF crearUsuario: " + e.getMessage();
        }
    }

    public String listarPrestamos() {
        try {
            return restTemplate.getForObject(functionsBaseUrl + "/listarPrestamos", String.class);
        } catch (HttpStatusCodeException e) {
            return "Error Azure Function listarPrestamos: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "Error BFF listarPrestamos: " + e.getMessage();
        }
    }

    public String crearPrestamo(Prestamo prestamo) {
        try {
            String jsonBody = objectMapper.writeValueAsString(prestamo);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    functionsBaseUrl + "/crearPrestamo",
                    request,
                    String.class);

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            return "Error Azure Function crearPrestamo: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "Error BFF crearPrestamo: " + e.getMessage();
        }
    }
}