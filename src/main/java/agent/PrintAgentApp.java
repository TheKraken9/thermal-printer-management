package agent;

import com.sun.net.httpserver.HttpServer;
import agent.controller.PrintController;
import agent.controller.ProformaPrintController;

import java.net.InetSocketAddress;

public class PrintAgentApp {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(9100), 0);

        // ── Route existante : impression reçu/facture ──────────────────
        server.createContext("/print", new PrintController());

        // ── Nouvelle route : impression proforma thermique ─────────────
        server.createContext("/print-proforma", new ProformaPrintController());

        server.setExecutor(null);
        server.start();

    }
}