import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.net.httpserver.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class Server {

    static List<String[]> alerts = new ArrayList<>();

    // ---------------- Email ----------------
    public static void sendEmail(String messageBody) {
        try {
            final String username = System.getenv("EMAIL_USER");
            final String password = System.getenv("EMAIL_PASS");
            final String recipients = System.getenv("OWNER_EMAIL") + "," + System.getenv("DELIVERY_EMAIL");

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            message.setSubject("🚨 Smart Sticker Alert!");
            message.setText(messageBody);
            Transport.send(message);
        } catch (MessagingException e) { e.printStackTrace(); }
    }

    // ---------------- SMS ----------------
    public static void sendSMS(String device, String status, String time) {
        try {
            String accountSID = System.getenv("TWILIO_SID");
            String authToken = System.getenv("TWILIO_AUTH_TOKEN");
            String twilioNumber = System.getenv("TWILIO_NUMBER");
            String ownerPhone = System.getenv("OWNER_PHONE");
            String deliveryPhone = System.getenv("DELIVERY_PHONE");

            Twilio.init(accountSID, authToken);
            String alertMsg = "Alert! Device " + device + " is " + status + " at " + time;

            Message.creator(new PhoneNumber(ownerPhone), new PhoneNumber(twilioNumber), alertMsg).create();
            Message.creator(new PhoneNumber(deliveryPhone), new PhoneNumber(twilioNumber), alertMsg).create();

        } catch (Exception e) { e.printStackTrace(); }
    }

    // ---------------- Main ----------------
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "9000"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // ---------------- Dashboard ----------------
        server.createContext("/", exchange -> {
            String blinkStyle = "<style>@keyframes blink {0%{opacity:1;}50%{opacity:0;}100%{opacity:1;}} " +
                    ".card{display:inline-block;padding:15px;margin:10px;border-radius:10px;color:white;font-weight:bold;} " +
                    ".green{background:green;} .red{background:red;} table{border-collapse:collapse;width:80%;margin-top:20px;} th,td{padding:10px;text-align:center;} </style>";

            int connected = 0, disconnected = 0;
            for(String[] a : alerts){
                if(a[1].equalsIgnoreCase("Disconnected")) disconnected++;
                else connected++;
            }

            String response = blinkStyle + "<h1>🚨 Smart Sticker Cloud Dashboard</h1>";
            response += "<div class='card green'>Connected: "+connected+"</div>";
            response += "<div class='card red'>Disconnected: "+disconnected+"</div>";

            response += "<table border='1'><tr><th>Device ID</th><th>Status</th><th>Time</th></tr>";
            for(String[] alert : alerts){
                String style = alert[1].equalsIgnoreCase("Disconnected") ? "style='color:red;animation:blink 1s infinite;'" : "style='color:green;'";
                response += "<tr "+style+"><td>"+alert[0]+"</td><td>"+alert[1]+"</td><td>"+alert[2]+"</td></tr>";
            }
            response += "</table>";

            // Fake alert buttons
            response += "<br><form method='POST' action='/simulate'>" +
                    "<button type='submit'>Trigger Fake Alert</button></form>";

            exchange.sendResponseHeaders(200,response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        // ---------------- Alert Endpoint ----------------
        server.createContext("/alert", exchange -> {
            if(exchange.getRequestMethod().equalsIgnoreCase("POST")){
                BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(),"utf-8"));
                String data = br.readLine(); // device_id=STK001,status=Disconnected,time=10:45
                String[] parts = data.split(",");
                String device = parts[0].split("=")[1];
                String status = parts[1].split("=")[1];
                String time = parts[2].split("=")[1];

                alerts.add(new String[]{device,status,time});
                if(status.equalsIgnoreCase("Disconnected")){
                    sendEmail("Device: "+device+"\nStatus: "+status+"\nTime: "+time);
                    sendSMS(device,status,time);
                }
            }
            String response="Alert Received";
            exchange.sendResponseHeaders(200,response.length());
            OutputStream os=exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        // ---------------- Fake Simulator ----------------
        server.createContext("/simulate", exchange -> {
            String fakeTime = java.time.LocalTime.now().withNano(0).toString();
            String[] fakeAlert = {"STK001","Disconnected",fakeTime};
            alerts.add(fakeAlert);
            sendEmail("Fake Alert!\nDevice: "+fakeAlert[0]+"\nStatus: "+fakeAlert[1]+"\nTime: "+fakeAlert[2]);
            sendSMS(fakeAlert[0],fakeAlert[1],fakeAlert[2]);

            String response = "Fake alert triggered!";
            exchange.sendResponseHeaders(200,response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port "+port);
    }
}