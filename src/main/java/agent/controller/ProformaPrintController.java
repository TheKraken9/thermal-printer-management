package agent.controller;

import agent.dto.ProformaDTO;
import agent.printer.ProformaThermalPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStream;

/**
 * Handler POST /print-proforma
 * Reçoit un ProformaDTO en JSON et l'envoie à l'imprimante thermique.
 */
public class ProformaPrintController implements HttpHandler {

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
            ProformaDTO proforma = mapper.readValue(body, ProformaDTO.class);

            new ProformaThermalPrinter().print(proforma);

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