package trust_authority_client;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class SampleAppServer {

    // private static final Logger logger = LogManager.getLogger(SampleAppServer.class);

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Define handlers for different URLs
        server.createContext("/appraisal/v1/nonce", new NonceHandler());
        server.createContext("/appraisal/v1/attest", new TokenHandler());
        server.createContext("/certs", new CertsHandler());
        server.createContext("/post", new PostHandler());

        server.setExecutor(null); // Default executor

        server.start();
        System.out.println("Server is listening on port " + port);
    }

    static class NonceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = "{\"nonce\" : \"response\"}";
                sendResponse(exchange, response);
            }
        }
    }

    static class CertsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = "{\"keys\":[{\"alg\":\"PS384\",\"e\":\"AQAB\",\"kid\":\"3fd751f2e0d0f52846c0ecd4972c6e99dfc642051cd339dd9b04381af8c0ddb804514a7a1fee4673ac844fd5db7f15fb\",\"kty\":\"RSA\",\"n\":\"vKKV7v7czOHapQ22ZnW677i4BkQIuxVTLk933javfZyLzpM7ZP_Mhvu9QqHrr-iKEqCDBuX1slL_hoB0fTCGGnoFTZ1lTqBdmhFysIgg5uzAqMWL2SJdzYX9RJ_ZXMFnvzTznO-b2jJd864pUI6y72mrzfTqQvgw_60fa3tjc9zjJPiqT1yadKar3G5c0fJqg7AUooTuMkIq291tHqoNhfYzzshZCSFV_d5RruheVMjvgMunx1zISiZ5RNRjcy39G7-08UTCIlSKE_GdsLDNViHqACz60BW3p-kSY5YdoslwKvDUOJnkVZMpJNfdYDoBIiIGgKL2j5H8arHmhSw1A1kl66YdDl7H5Pa46qp4B2FrS5Qpt1D9C-SZXkWN3wzDIQLsHKs0e86R5guLMS9_WcfsPCcHCLjqMZe6S-18SdjwzCK4hbn5vLCZYUzIyVEIcYT8f3mS3s3I1UxJRW53WZOEKkyGVKKGTF8uRxaksFVGrIdW0Q41Wo3mB30N2tqL\",\"x5c\":[\"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\"]}]}";
                sendResponse(exchange, response);
            }
        }
    }

    static class TokenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = "{\"token\" : \"response\"}";
                sendResponse(exchange, response);
            }
            if ("POST".equals(exchange.getRequestMethod())) {
                System.out.println("Received a POST REQUEST...");
                String response = "{\"token\" : \"response\"}";
                sendResponse(exchange, response);
            }
        }
    }

    static class PostHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String response = "This is a POST request response.";
                sendResponse(exchange, response);
            }
        }
    }

    static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}