import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

public class Server {

    static List<String> alerts = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        // ✅ FIXED: Dynamic PORT for Render
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new HomeHandler());

        // ✅ Both endpoints (so /tamper also works)
        server.createContext("/alert", new AlertHandler());
        server.createContext("/tamper", new AlertHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("🚀 Smart Sticker Cloud Server running on port " + port);
    }

    // ================= HOME DASHBOARD =================
    static class HomeHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            StringBuilder html = new StringBuilder();

            html.append("<html><head><title>Dashboard</title>");
            html.append("<style>");
            html.append("body{font-family:Arial;background:#eef2f7;margin:0;}");
            html.append("header{background:#1e88e5;color:white;padding:20px;text-align:center;font-size:28px;}");
            html.append(".container{padding:30px;}");
            html.append(".card{background:white;padding:20px;border-radius:10px;margin-bottom:20px;box-shadow:0 4px 10px rgba(0,0,0,0.1);}");
            html.append("table{width:100%;border-collapse:collapse;}");
            html.append("th{background:#1976d2;color:white;padding:10px;}");
            html.append("td{padding:10px;text-align:center;border-bottom:1px solid #ddd;}");
            html.append(".red{color:red;font-weight:bold;}");
            html.append(".green{color:green;font-weight:bold;}");
            html.append("</style></head><body>");

            html.append("<header>🚨 Smart Sticker Cloud Monitoring Dashboard</header>");
            html.append("<div class='container'>");

            if (alerts.isEmpty()) {
                html.append("<div class='card'>");
                html.append("<h2>No Alerts Yet</h2>");
                html.append("<p>Your IoT devices are operating normally.</p>");
                html.append("</div>");
            } else {
                html.append("<div class='card'>");
                html.append("<h2>Alert Logs</h2>");
                html.append("<table>");
                html.append("<tr><th>Device</th><th>Status</th><th>Time</th></tr>");

                for (String alert : alerts) {

                    String[] parts = alert.split(",");
                    String device = parts[0];
                    String status = parts[1];
                    String time = parts[2];

                    html.append("<tr>");
                    html.append("<td>" + device + "</td>");

                    if (status.equalsIgnoreCase("Disconnected")) {
                        html.append("<td class='red'>" + status + "</td>");
                    } else {
                        html.append("<td class='green'>" + status + "</td>");
                    }

                    html.append("<td>" + time + "</td>");
                    html.append("</tr>");
                }

                html.append("</table></div>");
            }

            html.append("</div></body></html>");

            byte[] response = html.toString().getBytes();

            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    // ================= ALERT HANDLER =================
    static class AlertHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            // ✅ Allow GET for easy browser testing
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {

                String alert = "Device1,Disconnected," + new Date();

                alerts.add(alert);

                String response = "🚨 Fake Alert Triggered";

                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }

            // ✅ POST for real IoT data
            else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {

                BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                StringBuilder body = new StringBuilder();

                String line;
                while ((line = br.readLine()) != null) {
                    body.append(line);
                }

                alerts.add(body.toString());

                String response = "Alert stored successfully";

                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }

            else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}
