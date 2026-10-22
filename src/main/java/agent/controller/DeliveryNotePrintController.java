package agent.controller;

import agent.dto.DeliveryNoteDTO;
import agent.printer.DeliveryNoteThermalPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStream;

/**
 * Handler POST /print-delivery
 * Reçoit un DeliveryNoteDTO en JSON et imprime le bon de livraison thermique.
 */
public class DeliveryNotePrintController implements HttpHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            // ── CORS ──────────────────────────────────────────────────────
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if (origin != null) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
            } else {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            }
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");

            // ── Preflight ─────────────────────────────────────────────────
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // ── Désérialisation + impression ──────────────────────────────
            InputStream body = exchange.getRequestBody();
            DeliveryNoteDTO note = mapper.readValue(body, DeliveryNoteDTO.class);

            new DeliveryNoteThermalPrinter().print(note);

            byte[] response = "OK".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (Exception ignored) {}
        }
    }
}
