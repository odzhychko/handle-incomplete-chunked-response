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

    public static void main(String[] args) throws IOException {
        ClientAndServer mockServer = ClientAndServer.startClientAndServer(PORT);
        try(mockServer) {
            HttpRequest httpRequest = HttpRequest.request()
                    .withMethod("GET")
                    .withPath(FAULTY_PATH);
            HttpError httpError = HttpError.error()
                    .withResponseBytes(generateIncompleteResponse()
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

        int chunkSize = 4088;
        int dataSize = (int) (chunkSize * 3.534);

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
        return "X".repeat(RANDOM.nextInt(50, 100)) + "_END_OF_LINE_CHECK\n";
    }
}
