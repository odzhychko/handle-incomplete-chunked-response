package dev.oleks;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

public class ClientJava {

    public static void main(String[] args) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (int requestNumber = 1; requestNumber <= 1_000; ++requestNumber) {
                System.out.println("Sending request: " + requestNumber);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/get-incomplete-response-and-close-connection"))
                        .build();


                try {
                    client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                            .thenApply(response -> {
                                try (var reader = new BufferedReader(new InputStreamReader(response.body()))) {
                                    while (true) {
                                        var line = reader.readLine();
                                        if (line == null) {
                                            break;
                                        }
                                        if (!line.endsWith("_END")) {
                                            System.out.println("read line incompletely");
                                        }
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                throw new IllegalStateException("did not fail as expected");
                            }).join();
                } catch (Exception e) {
                    if ("java.lang.RuntimeException: java.io.IOException: closed".equals(e.getMessage())) {
                        System.out.println("Failed with expected exception.");
                    } else {
                        throw e;
                    }
                }
            }
        }
    }
}
