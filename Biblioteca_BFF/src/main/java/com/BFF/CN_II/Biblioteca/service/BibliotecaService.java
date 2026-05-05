package com.BFF.CN_II.Biblioteca.service;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BibliotecaService {

    @Value("${azure.eventgrid.endpoint}")
    private String endpoint;

    @Value("${azure.eventgrid.key}")
    private String key;

    private EventGridPublisherClient<EventGridEvent> eventGridClient;

    @PostConstruct
    public void init() {
        this.eventGridClient = new EventGridPublisherClientBuilder()
            .endpoint(endpoint)
            .credential(new AzureKeyCredential(key))
            .buildEventGridEventPublisherClient();
    }

    
    public String enviarEvento(String tipoEvento, Object data) {
        try {
            EventGridEvent evento = new EventGridEvent(
                "biblioteca/operacion", 
                tipoEvento,     //Body del evento específico (ej, "Usuario.Creado")     
                BinaryData.fromObject(data),
                "1.0"
            );
            eventGridClient.sendEvent(evento);
            return "Evento " + tipoEvento + " publicado exitosamente.";
        } catch (Exception e) {
            return "Error al publicar: " + e.getMessage();
        }
    }

    public String enviarEventoSimple(String tipoEvento, int id) {
        try {
            EventGridEvent evento = new EventGridEvent(
                "biblioteca/eliminacion",
                tipoEvento,           //Eliminaciones
                BinaryData.fromObject(id),
                "1.0"
            );
            eventGridClient.sendEvent(evento);
            return "Solicitud de eliminación " + tipoEvento + " enviada.";
        } catch (Exception e) {
            return "Error en eliminación: " + e.getMessage();
        }
    }
}