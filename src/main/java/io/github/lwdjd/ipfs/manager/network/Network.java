package io.github.lwdjd.ipfs.manager.network;


import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

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

//    // 不使用代理的POST请求
//    public static String Post(String url, String jsonBody) throws Exception {
//        // 创建HttpClient对象
//        HttpClient client = HttpClient.newBuilder()
//                .connectTimeout(Duration.ofSeconds(5)) // 设置连接超时为5秒
//                .build();
//
//        // 创建一个HttpRequest对象，设置请求方法为POST，请求地址为url，并且带有JSON格式的请求体。
//        HttpRequest request = HttpRequest.newBuilder()
//                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
//                .uri(URI.create(url))
//                .header("content-type","application/json;charset=UTF-8")
//                .build();
//
//        // 发送请求，获取响应对象。
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//        // 返回响应体的字符串内容。
//        return response.body();
//    }

    // 不使用代理的POST请求
    public static String Post(String url, String jsonBody) throws Exception {
        // 创建HttpClient对象
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)) // 设置连接超时为5秒
                .build();

        // 创建一个HttpRequest对象，设置请求方法为POST，请求地址为url，并且带有JSON格式的请求体。
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .uri(URI.create(url))
                .header("content-type", "application/json;charset=UTF-8")
                .build();

        // 创建一个ExecutorService来执行异步请求
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<HttpResponse<String>> future = CompletableFuture.supplyAsync(() -> {
            try {
                return client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);

        // 设置总体超时时间为10秒
        try {
            HttpResponse<String> response = future.get(5, TimeUnit.SECONDS);
            executor.shutdown();
            return response.body();
        } catch (TimeoutException e) {
            future.cancel(true); // 超时后取消任务
            executor.shutdownNow(); // 尝试立即关闭执行器
            throw new Exception("请求超时，未能在10秒内完成");
        } catch (ExecutionException e) {
            executor.shutdown();
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new Exception("请求执行过程中发生错误", cause);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            throw new Exception("请求被中断", e);
        }
    }

//    // 不使用代理的POST请求，带请求头(pin专用)。
//    public static String pinPost(String url, String jsonBody, String sign) throws Exception {
//        // 创建HttpClient对象
//        HttpClient client = HttpClient.newBuilder()
//                .connectTimeout(Duration.ofSeconds(5)) //设置连接超时(注意：不是响应超时)
//                .build();
//        // 创建一个HttpRequest对象，设置请求方法为POST，请求地址为url，并且带有JSON格式的请求体。
//        HttpRequest request = HttpRequest.newBuilder()
//                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
//                .uri(URI.create(url))
//                .header("authorization",sign)
//                .header("origin","https://apps.crust.network")
//                .header("content-type","application/json;charset=UTF-8")
//                .build();
//        // 发送请求，获取响应对象。
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//        // 返回响应体的字符串内容。
//        return response.body();
//    }

//    // 不使用代理的POST请求，带请求头(pin专用)。
//    public static String pinPost(String url, String jsonBody, String sign) throws Exception {
//        // 创建HttpClient对象
//        HttpClient client = HttpClient.newBuilder()
//                .connectTimeout(Duration.ofSeconds(5)) // 设置连接超时为5秒
//                .build();
//
//
//        // 创建一个HttpRequest对象，设置请求方法为POST，请求地址为url，并且带有JSON格式的请求体。
//        HttpRequest request = HttpRequest.newBuilder()
//                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
//                .uri(URI.create(url))
//                .header("authorization", sign)
//                .header("content-type", "application/json;charset=UTF-8")
//                .header("origin", "https://apps.crust.network")
//                .build();
//
////        System.out.println(request.toString()+"  "+request.headers());
//        // 创建一个ExecutorService来执行异步请求
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        CompletableFuture<HttpResponse<String>> future = CompletableFuture.supplyAsync(() -> {
//            try {
//                return client.send(request, HttpResponse.BodyHandlers.ofString());
//            } catch (Exception e) {
//                throw new CompletionException(e);
//            }
//        }, executor);
//
//        // 设置总体超时时间为10秒
//        try {
//            HttpResponse<String> response = future.get(10, TimeUnit.SECONDS);
//            executor.shutdown();
//            return response.body();
//        } catch (TimeoutException e) {
//            future.cancel(true); // 超时后取消任务
//            executor.shutdownNow(); // 尝试立即关闭执行器
//            throw new Exception("请求超时，未能在10秒内完成");
//        } catch (ExecutionException e) {
//            executor.shutdown();
//            Throwable cause = e.getCause();
//            if (cause instanceof Exception) {
//                throw (Exception) cause;
//            } else {
//                throw new Exception("请求执行过程中发生错误", cause);
//            }
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            executor.shutdownNow();
//            throw new Exception("请求被中断", e);
//        }
//    }
    // 不使用代理的POST请求，带请求头(pin专用)。
    public static String pinPost(String url, String jsonBody, String sign) throws Exception {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS) // 设置连接超时为5秒
                .build();
        MediaType mediaType = MediaType.parse("application/json");
//        RequestBody body = RequestBody.create(mediaType, "{\r\n    \"cid\": \"bafybeienygfsqpymg4gpfgy63grfmva2tmp7ukno4j3qrft66pepnflrki\"\r\n}");
        RequestBody body = RequestBody.create(mediaType, jsonBody);
        Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("origin", "https://apps.crust.network")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", sign)
                .build();
//        Response response = client.newCall(request).execute();


    //        System.out.println(request.toString()+"  "+request.headers());
        // 创建一个ExecutorService来执行异步请求
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<Response> future = CompletableFuture.supplyAsync(() -> {
            try {
                return client.newCall(request).execute();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);

        // 设置总体超时时间为10秒
        try {
            Response response = future.get(10, TimeUnit.SECONDS);
            executor.shutdown();
            if (response.body() != null) {
                return response.body().string();
            }else {
                return "";
            }
        } catch (TimeoutException e) {
            future.cancel(true); // 超时后取消任务
            executor.shutdownNow(); // 尝试立即关闭执行器
            throw new Exception("请求超时，未能在10秒内完成");
        } catch (ExecutionException e) {
            executor.shutdown();
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new Exception("请求执行过程中发生错误", cause);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            throw new Exception("请求被中断", e);
        }
    }

    public static String fromDataPost(String url, String fromData) throws Exception {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("","",
                        RequestBody.create(MediaType.parse("application/octet-stream"), fromData))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .build();
//        Response response = client.newCall(request).execute();
        // 创建一个ExecutorService来执行异步请求
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<Response> future = CompletableFuture.supplyAsync(() -> {
            try {
                return client.newCall(request).execute();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);

        // 设置总体超时时间为10秒
        try {
            Response response = future.get(10, TimeUnit.SECONDS);
            executor.shutdown();
            if (response.body() != null) {
                return response.body().string();
            }else {
                return "";
            }
        } catch (TimeoutException e) {
            future.cancel(true); // 超时后取消任务
            executor.shutdownNow(); // 尝试立即关闭执行器
            throw new Exception("请求超时，未能在10秒内完成");
        } catch (ExecutionException e) {
            executor.shutdown();
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new Exception("请求执行过程中发生错误", cause);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            throw new Exception("请求被中断", e);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(fromDataPost("http://127.0.0.1:5001/api/v0/dag/put?store-codec=dag-pb&input-codec=dag-json","{\n" +
                "    \"Data\": {\n" +
                "        \"/\": {\n" +
                "            \"bytes\": \"CAE\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"Links\": [\n" +
                "        {\n" +
                "            \"Hash\": {\n" +
                "                \"/\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\"\n" +
                "            },\n" +
                "            \"Name\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\",\n" +
                "            \"Tsize\": 4654398768647097000\n" +
                "        },\n" +
                "        {\n" +
                "            \"Hash\": {\n" +
                "                \"/\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\"\n" +
                "            },\n" +
                "            \"Name\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\",\n" +
                "            \"Tsize\": 4654398768647097000\n" +
                "        },\n" +
                "        {\n" +
                "            \"Hash\": {\n" +
                "                \"/\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\"\n" +
                "            },\n" +
                "            \"Name\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\",\n" +
                "            \"Tsize\": 4654398768647097000\n" +
                "        },\n" +
                "        {\n" +
                "            \"Hash\": {\n" +
                "                \"/\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\"\n" +
                "            },\n" +
                "            \"Name\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\",\n" +
                "            \"Tsize\": 4654398768647097000\n" +
                "        },\n" +
                "        {\n" +
                "            \"Hash\": {\n" +
                "                \"/\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\"\n" +
                "            },\n" +
                "            \"Name\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\",\n" +
                "            \"Tsize\": 4654398768647097000\n" +
                "        }\n" +
                "    ]\n" +
                "}\n"));
    }
}
