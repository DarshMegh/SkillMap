package skillmap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpServer {

    private final int port;
    private com.sun.net.httpserver.HttpServer server;

    public HttpServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new FrontendHandler());
        server.createContext("/api/roadmap", new ApiHandler());
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
    }

    static class FrontendHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Serve index.html from frontend/ folder at project root
            Path frontendPath = Paths.get("frontend/index.html");
            byte[] response;
            int statusCode;

            if (Files.exists(frontendPath)) {
                response = Files.readAllBytes(frontendPath);
                statusCode = 200;
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            } else {
                response = "<h1>frontend/index.html not found</h1><p>Make sure the frontend/ folder is in your project root.</p>".getBytes();
                statusCode = 404;
                exchange.getResponseHeaders().set("Content-Type", "text/html");
            }

            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }
}