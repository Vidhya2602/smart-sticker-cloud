import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Server {

    static List<String> alerts = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT","8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new HomeHandler());
        server.createContext("/alert", new AlertHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Smart Sticker Cloud Server running on port " + port);
    }

    // Dashboard Page
    static class HomeHandler implements HttpHandler {

        public void handle(HttpExchange exchange) throws IOException {

            StringBuilder html = new StringBuilder();

            html.append("<html>");
            html.append("<head>");
            html.append("<title>Smart Sticker Dashboard</title>");

            html.append("<style>");
            html.append("body{font-family:Arial;background:#eef2f7;margin:0;padding:0;}");

            html.append(
                    "header{background:#1e88e5;color:white;padding:20px;text-align:center;font-size:28px;font-weight:bold;}");

            html.append(".container{padding:30px;}");

            html.append(
                    ".card{background:white;border-radius:10px;padding:20px;margin-bottom:20px;box-shadow:0 4px 10px rgba(0,0,0,0.1);}");

            html.append("table{width:100%;border-collapse:collapse;margin-top:10px;}");

            html.append("th{background:#1976d2;color:white;padding:12px;}");

            html.append("td{padding:10px;text-align:center;border-bottom:1px solid #ddd;}");

            html.append("tr:hover{background:#f1f1f1;}");

            html.append(".status-red{color:red;font-weight:bold;}");
            html.append(".status-green{color:green;font-weight:bold;}");

            html.append("</style>");
            html.append("</head>");

            html.append("<body>");

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
                html.append("<tr>");
                html.append("<th>Device ID</th>");
                html.append("<th>Status</th>");
                html.append("<th>Time</th>");
                html.append("</tr>");

                for (String alert : alerts) {

                    String[] parts = alert.split(",");

                    String device = parts[0].split("=")[1];
                    String status = parts[1].split("=")[1];
                    String time = parts[2].split("=")[1];

                    html.append("<tr>");
                    html.append("<td>" + device + "</td>");

                    if (status.equalsIgnoreCase("Disconnected")) {
                        html.append("<td class='status-red'>" + status + "</td>");
                    } else {
                        html.append("<td class='status-green'>" + status + "</td>");
                    }

                    html.append("<td>" + time + "</td>");
                    html.append("</tr>");
                }

                html.append("</table>");
                html.append("</div>");
            }

            html.append("</div>");

            html.append("</body>");
            html.append("</html>");

            byte[] response = html.toString().getBytes();

            exchange.sendResponseHeaders(200, response.length);

            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    // IoT Alert Receiver
    static class AlertHandler implements HttpHandler {

        public void handle(HttpExchange exchange) throws IOException {

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody()));

                String line;
                StringBuilder body = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }

                String alertData = body.toString();

                System.out.println("Alert received from IoT:");
                System.out.println(alertData);

                alerts.add(alertData);

                String response = "Alert stored successfully";

                exchange.sendResponseHeaders(200, response.length());

                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}