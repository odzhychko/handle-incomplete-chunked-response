package dev.oleks;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ServerMockServer {

    static int PORT = 8080;
    static String FAULTY_PATH = "/get-incomplete-response-and-close-connection";
    static Random RANDOM = new Random(809438987);

    static String SMALL_INCOMPLETE_RESPONSE = "HTTP/1.1 200 OK\r\n" +
            "Transfer-Encoding: chunked\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "8\r\n" +
            "XX_END\nX\r\n" +
            "8\r\n" +
            "X_END\nX";

    public static void main(String[] args) throws IOException {
        ClientAndServer mockServer = ClientAndServer.startClientAndServer(PORT);
        try(mockServer) {
            HttpRequest httpRequest = HttpRequest.request()
                    .withMethod("GET")
                    .withPath(FAULTY_PATH);
            String incompleteResponse = SMALL_INCOMPLETE_RESPONSE;
//            String incompleteResponse = generateIncompleteResponse();
            HttpError httpError = HttpError.error()
                    .withResponseBytes(incompleteResponse
                            .getBytes(StandardCharsets.UTF_8))
                            .withDropConnection(true);
            mockServer.when(httpRequest)
                      .error(httpError);

            String baseUrl = "http://localhost:" + PORT;
            System.out.println("Server running at " + baseUrl);
            System.out.println("Dashboard at " + baseUrl + "/mockserver/dashboard");
            System.out.println("Faulty endpoint at " + baseUrl + FAULTY_PATH);
            System.out.println("Press some key to stop the server...");
            System.in.read();
        }
    }

    private static String generateIncompleteResponse() {
        // See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Transfer-Encoding#chunked_encoding

        // example for bigger data
        int chunkSize = 4088;
        int dataSize = (int) (chunkSize * 10.534);
        // example for small data
//        int chunkSize = 8;
//        int dataSize = 15;

        Stream<Character> unchunkedResponseBody = Stream.generate(ServerMockServer::generateLine)
                .flatMapToInt(String::chars)
                .mapToObj(asciiInt -> (char) asciiInt)
                .limit(dataSize);

        StringBuilder responseBuilder = new StringBuilder()
                .append("HTTP/1.1 200 OK\r\n")
                .append("Transfer-Encoding: chunked\r\n")
                .append("Content-Type: text/plain\r\n");

        AtomicInteger responseBodyCharIndex = new AtomicInteger();
        unchunkedResponseBody.forEachOrdered(responseBodyChar -> {
            if (responseBodyCharIndex.getAndIncrement() % chunkSize == 0) {
                responseBuilder.append("\r\n");
                responseBuilder.append(Integer.toHexString(chunkSize));
                responseBuilder.append("\r\n");
            }
            responseBuilder.append(responseBodyChar);
        });
        return responseBuilder.toString();
    }

    private static String generateLine() {
        // return "X".repeat(RANDOM.nextInt(2, 100)) + "_END_OF_LINE_CHECK\n";
        return "XX_END\n";
    }
}
