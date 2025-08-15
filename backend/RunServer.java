import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RunServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/submitLeave", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                StringBuilder buf = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) buf.append(line);

                String[] params = buf.toString().split("&");
                Map<String, String> data = new HashMap<>();
                for (String p : params) {
                    String[] kv = p.split("=");
                    if (kv.length == 2) data.put(kv[0], URLDecoder.decode(kv[1], "UTF-8"));
                }

                boolean inserted = DBHandler.insertLeave(
                        data.get("studentName"),
                        data.get("rollNo"),
                        data.get("fromDate"),
                        data.get("toDate"),
                        data.get("reason")
                );

                String response = inserted ? "<h2>✅ Leave request submitted!</h2>" : "<h2>❌ Submission failed</h2>";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        });

        System.out.println("Server running at http://localhost:8080");
        server.setExecutor(null);
        server.start();
    }
}
