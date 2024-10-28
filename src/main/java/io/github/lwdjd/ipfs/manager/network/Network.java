package io.github.lwdjd.ipfs.manager.network;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class Network {
    /**
     * 不使用代理发送GET请求
     * @param url 请求的url
     * @return 返回请求的结果
     */
    public static String get(String url) throws Exception {
        // 创建一个HttpClient对象
        HttpClient client = HttpClient.newHttpClient();

        // 创建一个HttpRequest对象，设置请求方法为GET，请求地址为url加上params
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();

        // 发送请求，获取响应对象
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 返回响应体的字符串内容
        return response.body();

    }

    // 不使用代理的POST请求
    public static String Post(String url, String jsonBody) throws Exception {
        // 创建HttpClient对象
        HttpClient client = HttpClient.newHttpClient();

        // 创建一个HttpRequest对象，设置请求方法为POST，请求地址为url，并且带有JSON格式的请求体。
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .uri(URI.create(url))
                .header("content-type","application/json;charset=UTF-8")
                .build();

        // 发送请求，获取响应对象。
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 返回响应体的字符串内容。
        return response.body();
    }

    // 不使用代理的POST请求，带请求头(pin专用)。
    public static String pinPost(String url, String jsonBody, String sign) throws Exception {
        // 创建HttpClient对象
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        // 创建一个HttpRequest对象，设置请求方法为POST，请求地址为url，并且带有JSON格式的请求体。
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .uri(URI.create(url))
                .header("authorization",sign)
                .header("origin","https://apps.crust.network")
                .header("content-type","application/json;charset=UTF-8")
                .build();
        // 发送请求，获取响应对象。
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        // 返回响应体的字符串内容。
        return response.body();
    }
}
