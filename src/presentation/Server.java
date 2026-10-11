
package presentation;

import com.sun.net.httpserver.*;
import negocio.model.*;
import negocio.services.*;
import persistencia.json.*;
import persistencia.dao.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Server {
    private static final int PORT = 8080;

    private final JsonDatabase db;
    private final ProductoDAO productoDAO;
    private final ClienteDAO clienteDAO;
    private final PedidoDAO pedidoDAO;

    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final PedidoService pedidoService;

    private final Path webRoot;

    public Server() {
        Path root = Paths.get("").toAbsolutePath();
        db = new JsonDatabase(root.resolve("data"));

        productoDAO = new JsonProductoDAO(db);
        clienteDAO = new JsonClienteDAO(db);
        pedidoDAO = new JsonPedidoDAO(db);

        productoService = new ProductoService(productoDAO);
        clienteService = new ClienteService(clienteDAO);

        pedidoService = new PedidoService(
                productoDAO,
                clienteDAO,
                pedidoDAO,
                new FidelidadService(),
                new InventarioService()
        );

        webRoot = Files.exists(root.resolve("web"))
                ? root.resolve("web")
                : root.resolve("../web").normalize();
    }

    public static void main(String[] args) throws Exception {
        new Server().start();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", PORT), 0
        );

        server.createContext("/api/", this::api);
        server.createContext("/", this::staticFile);
        server.setExecutor(
                java.util.concurrent.Executors.newFixedThreadPool(8)
        );

        server.start();
        System.out.println(
                "Café Overflow disponible en http://localhost:" + PORT
        );
    }

    private void api(HttpExchange x) throws IOException {
        try {
            String method = x.getRequestMethod();
            String path = x.getRequestURI().getPath();

            if (method.equals("OPTIONS")) {
                x.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                x.getResponseHeaders().set(
                        "Access-Control-Allow-Methods",
                        "GET,POST,PATCH,OPTIONS"
                );
                x.getResponseHeaders().set(
                        "Access-Control-Allow-Headers", "Content-Type"
                );
                x.sendResponseHeaders(204, -1);
                return;
            }

            if (path.equals("/api/productos") && method.equals("GET")) {
                HttpUtil.json(x, 200, productoService.listar());
                return;
            }

            if (path.equals("/api/clientes") && method.equals("GET")) {
                HttpUtil.json(x, 200, clienteService.listar());
                return;
            }

            if (path.equals("/api/clientes") && method.equals("POST")) {
                Map<String, Object> b = HttpUtil.body(x);
                Cliente c = clienteService.crear(
                        str(b, "nombre"),
                        str(b, "correo")
                );
                HttpUtil.json(x, 201, c);
                return;
            }

            if (path.equals("/api/pedidos") && method.equals("GET")) {
                HttpUtil.json(x, 200, pedidoService.listar());
                return;
            }

            if (path.equals("/api/pedidos") && method.equals("POST")) {
                Map<String, Object> b = HttpUtil.body(x);

                List<Map<String, Object>> items =
                        (List<Map<String, Object>>) b.get("items");

                int pts = num(b, "puntosRedimir", 0);

                Pedido pedido = pedidoService.crear(
                        str(b, "clienteId"),
                        items,
                        pts,
                        str(b, "modalidad")
                );

                HttpUtil.json(x, 201, pedido);
                return;
            }

            if (path.startsWith("/api/pedidos/")
                    && path.endsWith("/estado")
                    && method.equals("PATCH")) {

                String prefijo = "/api/pedidos/";
                String sufijo = "/estado";

                String id = path.substring(
                        prefijo.length(),
                        path.length() - sufijo.length()
                );

                // Depuración temporal: muestra la ruta y el ID recibido.
                System.out.println(
                        "DEBUG PATCH: path=[" + path + "], id=[" + id + "]"
                );

                if (id.isBlank() || id.contains("/")) {
                    HttpUtil.json(
                            x, 400,
                            HttpUtil.map("error", "Identificador de pedido inválido")
                    );
                    return;
                }

                Map<String, Object> b = HttpUtil.body(x);

                Pedido pedido = pedidoService.cambiarEstado(
                        id,
                        str(b, "estado")
                );

                HttpUtil.json(x, 200, pedido);
                return;
            }

            HttpUtil.json(
                    x, 404,
                    HttpUtil.map("error", "Ruta no encontrada")
            );

        } catch (IllegalArgumentException e) {
            HttpUtil.json(
                    x, 400,
                    HttpUtil.map("error", e.getMessage())
            );
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.json(
                    x, 500,
                    HttpUtil.map(
                            "error",
                            "Error interno al procesar la solicitud"
                    )
            );
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : String.valueOf(v);
    }

    private int num(Map<String, Object> m, String k, int d) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).intValue() : d;
    }

    private void staticFile(HttpExchange x) throws IOException {
        String raw = x.getRequestURI().getPath();

        if (raw.equals("/")) {
            raw = "/index.html";
        }

        Path p = webRoot.resolve(raw.substring(1)).normalize();

        if (!p.startsWith(webRoot)
                || !Files.exists(p)
                || Files.isDirectory(p)) {

            byte[] b = "No encontrado".getBytes(StandardCharsets.UTF_8);
            x.sendResponseHeaders(404, b.length);

            try (OutputStream os = x.getResponseBody()) {
                os.write(b);
            }
            return;
        }

        String type;

        if (p.toString().endsWith(".css")) {
            type = "text/css; charset=utf-8";
        } else if (p.toString().endsWith(".js")) {
            type = "application/javascript; charset=utf-8";
        } else {
            type = "text/html; charset=utf-8";
        }

        byte[] b = Files.readAllBytes(p);
        x.getResponseHeaders().set("Content-Type", type);
        x.sendResponseHeaders(200, b.length);

        try (OutputStream os = x.getResponseBody()) {
            os.write(b);
        }
    }
}
