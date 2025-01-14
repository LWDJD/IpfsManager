package io.github.lwdjd.ipfs.manager.network;


import io.github.lwdjd.ipfs.manager.process.StorageFormatter;
import okhttp3.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class Network {
    /**
     * 不使用代理发送GET请求获取请求头
     * @param url 请求的url
     * @return 返回请求的结果
     */
    public static Headers getHeaders(String url) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            // 确保响应状态码为200，表示请求成功
            if (response.isSuccessful()) {
                // 直接返回响应头
                return response.headers();
            } else {
                throw new RuntimeException("Error: " + response.code());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error: get headers failed", e);
        }finally {
            client.dispatcher().cancelAll();//取消所有请求
            client.connectionPool().evictAll(); // 移除所有连接
        }
        // 关闭响应体，释放连接
    }

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
    public static void fakeDownload(String url,AtomicLong location){
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // 确保响应状态码为200，表示请求成功
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {

//                    final AtomicLong temp = new AtomicLong(0);
//                    FilePreheater.location.put(location,temp);
                    // 读取响应体中的数据记录数量并丢弃
                    body.byteStream().transferTo(new OutputStream() {
                        @Override
                        public void write(int b){
                            location.incrementAndGet();
                        }

                    });//读取流，但不存储。
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            client.dispatcher().cancelAll();//取消所有请求
            client.connectionPool().evictAll(); // 移除所有连接
        }
    }

    public static void fakeDownload(String url ,Headers headers,AtomicLong location){
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // 确保响应状态码为200，表示请求成功
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {

//                    final AtomicLong temp = new AtomicLong(0);
//                    FilePreheater.location.put(location,temp);
                    // 读取响应体中的数据记录数量并丢弃
                    body.byteStream().transferTo(new OutputStream() {
                        @Override
                        public void write(int b){
                            location.incrementAndGet();
                        }
                    });//读取流，但不存储。
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            client.dispatcher().cancelAll();//取消所有请求
            client.connectionPool().evictAll(); // 移除所有连接
        }
    }

    public static void fakeDownload(String url,long startByte,long endByte,AtomicLong location){
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Range", "bytes=" + startByte + "-" + endByte)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // 确保响应状态码为200，表示请求成功
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {

//                    final AtomicLong temp = new AtomicLong(0);
//                    FilePreheater.location.put(location,temp);
                    // 读取响应体中的数据记录数量并丢弃
                    body.byteStream().transferTo(new OutputStream() {
                        @Override
                        public void write(int b){
                            location.incrementAndGet();
                        }

                    });//读取流，但不存储。

                }
            }
        } catch (IOException e) {
//            e.printStackTrace();
        }finally {
            client.dispatcher().cancelAll();//取消所有请求
            client.connectionPool().evictAll(); // 移除所有连接
        }
    }

    public static void fakeDownload(String url, long startByte, long endByte, Headers headers, AtomicLong location){
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .header("Range", "bytes=" + startByte + "-" + endByte)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // 确保响应状态码为200，表示请求成功
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {

//                    final AtomicLong temp = new AtomicLong(0);
//                    FilePreheater.location.put(location,temp);
                    // 读取响应体中的数据记录数量并丢弃
                    body.byteStream().transferTo(new OutputStream() {
                        @Override
                        public void write(int b){
                            location.incrementAndGet();
                        }
                    });//读取流，但不存储。
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            client.dispatcher().cancelAll();//取消所有请求
            client.connectionPool().evictAll(); // 移除所有连接
        }
    }


    public static void main(String[] args){
        try {
            Headers a = getHeaders("https://gw.crustgw.work/ipfs/bafybeihqwtb6y2ta3zeudfcueiamh465w4qbcmwwrnzmudpo5wyvqc3po4");
            System.out.println("Size = "+a.get("content-length"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //测试使用
//    public static void main(String[] args){
//        int a = 1;
//        for (int i = 0; i<a; i++){
//            int finalI = i;
//            new Thread(()-> {
//                String ThName = "A"+finalI;
//                AtomicLong location = new AtomicLong(0);
//                if(FilePreheater.locations.containsKey(ThName)){
//                    location = FilePreheater.locations.get(ThName);
//                }else {
//                    FilePreheater.locations.put(ThName,location);
//                }
//                fakeDownload("https://gw.crustgw.work/ipfs/bafybeihqwtb6y2ta3zeudfcueiamh465w4qbcmwwrnzmudpo5wyvqc3po4",100,2000000,location);
//                System.out.println(ThName+" 下载结束");
//            }).start();
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        for (int i = 0; i<a; i++){
//            int finalI = i;
//            new Thread(()-> {
//                String ThName = "B"+finalI;
//                AtomicLong location = new AtomicLong(0);
//                if(FilePreheater.locations.containsKey(ThName)){
//                    location = FilePreheater.locations.get(ThName);
//                }else {
//                    FilePreheater.locations.put(ThName,location);
//                }
//                fakeDownload("https://gw.crustgw.org/ipfs/bafybeihqwtb6y2ta3zeudfcueiamh465w4qbcmwwrnzmudpo5wyvqc3po4",100,20000000,location);
//                System.out.println(ThName+" 下载结束");
//            }).start();
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        for (int i = 0; i<a; i++){
//            int finalI = i;
//            new Thread(()-> {
//                String ThName = "C"+finalI;
//                AtomicLong location = new AtomicLong(0);
//                if(FilePreheater.locations.containsKey(ThName)){
//                    location = FilePreheater.locations.get(ThName);
//                }else {
//                    FilePreheater.locations.put(ThName,location);
//                }
//                fakeDownload("https://gw.crust-gateway.xyz/ipfs/bafybeihqwtb6y2ta3zeudfcueiamh465w4qbcmwwrnzmudpo5wyvqc3po4",100,200000000,location);
//
//            }).start();
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        for (int i = 0; i<a; i++){
//            int finalI = i;
//            new Thread(()-> {
//                String ThName = "D"+finalI;
//                AtomicLong location = new AtomicLong(0);
//                if(FilePreheater.locations.containsKey(ThName)){
//                    location = FilePreheater.locations.get(ThName);
//                }else {
//                    FilePreheater.locations.put(ThName,location);
//                }
//                fakeDownload("https://gw.crust-gateway.com/ipfs/bafybeihqwtb6y2ta3zeudfcueiamh465w4qbcmwwrnzmudpo5wyvqc3po4",100,20000000,location);
//                System.out.println(ThName+" 下载结束");
//            }).start();
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//
////        new Thread(()-> fakeDownload("https://gw.crustgw.work/ipfs/bafybeihqwtb6y2ta3zeudfcueiamh465w4qbcmwwrnzmudpo5wyvqc3po4","A")).start();
////        new Thread(()-> fakeDownload("https://gw.crustgw.org/ipfs/bafybeihqwtb6y2ta3zeudfcueiamh465w4qbcmwwrnzmudpo5wyvqc3po4","B")).start();
////        new Thread(()-> fakeDownload("https://gw.crust-gateway.xyz/ipfs/bafybeihqwtb6y2ta3zeudfcueiamh465w4qbcmwwrnzmudpo5wyvqc3po4","C")).start();
////        new Thread(()-> fakeDownload("https://gw.crust-gateway.com/ipfs/bafybeihqwtb6y2ta3zeudfcueiamh465w4qbcmwwrnzmudpo5wyvqc3po4","D")).start();
//        new Thread(()-> {
//            while (System.currentTimeMillis() != -76745){
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                System.out.println("当前已下载大小");
//                ConcurrentHashMap<String, AtomicLong> b = new ConcurrentHashMap<>(FilePreheater.locations);
//                for(String key : b.keySet()){
//                    System.out.println("name: "+key+"    location: "+ StorageFormatter.formatBytes(b.get(key).get()));
//                }
//            }
//        }).start();
//    }

//    public static void main(String[] args) throws Exception {
//        System.out.println(fromDataPost("http://127.0.0.1:5001/api/v0/dag/put?store-codec=dag-pb&input-codec=dag-json","{\n" +
//                "    \"Data\": {\n" +
//                "        \"/\": {\n" +
//                "            \"bytes\": \"CAE\"\n" +
//                "        }\n" +
//                "    },\n" +
//                "    \"Links\": [\n" +
//                "        {\n" +
//                "            \"Hash\": {\n" +
//                "                \"/\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\"\n" +
//                "            },\n" +
//                "            \"Name\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\",\n" +
//                "            \"Tsize\": 4654398768647097000\n" +
//                "        },\n" +
//                "        {\n" +
//                "            \"Hash\": {\n" +
//                "                \"/\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\"\n" +
//                "            },\n" +
//                "            \"Name\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\",\n" +
//                "            \"Tsize\": 4654398768647097000\n" +
//                "        },\n" +
//                "        {\n" +
//                "            \"Hash\": {\n" +
//                "                \"/\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\"\n" +
//                "            },\n" +
//                "            \"Name\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\",\n" +
//                "            \"Tsize\": 4654398768647097000\n" +
//                "        },\n" +
//                "        {\n" +
//                "            \"Hash\": {\n" +
//                "                \"/\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\"\n" +
//                "            },\n" +
//                "            \"Name\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\",\n" +
//                "            \"Tsize\": 4654398768647097000\n" +
//                "        },\n" +
//                "        {\n" +
//                "            \"Hash\": {\n" +
//                "                \"/\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\"\n" +
//                "            },\n" +
//                "            \"Name\": \"bafybeif54ksdqydq3rliwjqps3wup6nulfbbrzgcvrrd5cgqqvps46eyma\",\n" +
//                "            \"Tsize\": 4654398768647097000\n" +
//                "        }\n" +
//                "    ]\n" +
//                "}\n"));
//    }
}
