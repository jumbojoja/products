import utils.ConnectConfig;
import utils.DatabaseConnector;
import com.alibaba.fastjson.*;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import entities.*;
import queries.ApiResult;
import queries.CardList;
import queries.GoodsResults;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URL;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.concurrent.TimeUnit;
import java.time.Duration;

public class Main {

    public static LibraryManagementSystem library;

    @SuppressWarnings("restriction")
    public static void main(String[] args) throws IOException {

        final Logger log = Logger.getLogger(Main.class.getName());
        try {
            // parse connection config from "resources/application.yaml"
            ConnectConfig conf = new ConnectConfig();
            log.info("Success to parse connect config. " + conf.toString());
            // connect to database
            DatabaseConnector connector = new DatabaseConnector(conf);

            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }

            library = new LibraryManagementSystemImpl(connector);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    // release database connection handler
                    if (connector.release()) {
                        log.info("Success to release connection.");
                    } else {
                        log.warning("Failed to release connection.");
                    }
                }
            });
            
            // 创建HTTP服务器，监听指定端口
            // 这里是8000，建议不要80端口，容易和其他的撞
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

            // 添加handler，这里就绑定到/card路由
            // 所以localhost:8000/card是会有handler来处理
            server.createContext("/card", new CardHandler());
            server.createContext("/ModifyCard", new ModifyCardHandler());
            server.createContext("/RemoveCard", new RemoveCardHandler());
            server.createContext("/borrowHistory", new BorrowHistoryHandler());
            server.createContext("/book", new BookHandler());
            server.createContext("/incbook", new IncBookHandler());
            server.createContext("/modifyBook", new ModifyBookHandler());
            server.createContext("/borrowbook", new BorrowBookHandler());
            server.createContext("/returnbook", new ReturnBookHandler());
            server.createContext("/removebook", new RemoveBookHandler());
            server.createContext("/addbooks", new AddBooksHandler());

            // 注册
            server.createContext("/adduser", new AddUserHandler());
            // 登录
            server.createContext("/checkuser", new CheckUserHandler());
            // 查询
            server.createContext("/search", new SearchGoodsHandler());

            // 启动服务器
            server.start();

            // 标识一下，这样才知道我的后端启动了（确信
            System.out.println("Server is listening on port 8000");

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    static class SearchGoodsHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();

            if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            /* BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody)); */
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody, "UTF-8"));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            String to_search = jobj.getString("goodsToSearch");

            System.setProperty("webdriver.chrome.driver", "C:\\Program Files\\Google\\Chrome\\Application\\chromedriver.exe");
            System.setProperty("webdriver.chrome.whitelistedIps", "");

            // 设置 Chrome 用户数据目录
            String userDataDir = "E:\\chrome_userdata"; // 修改为你自己的用户数据目录路径
            ChromeOptions options = new ChromeOptions();
            options.addArguments("user-data-dir=" + userDataDir); // 指定用户数据目录，Chrome 会在这个目录下保存登录信息

            WebDriver driver = new ChromeDriver(options);

            List<Goods> Goods_list = new ArrayList<>();

            // 京东
            try {
                // 登录后访问商品页面（示例链接）
                String targetUrl = "https://search.jd.com/Search?keyword=" + to_search + "&enc=utf-8";
                driver.get(targetUrl);

                driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

                /* // 给用户时间登录
                System.out.println("请手动登录京东，登录后按回车键继续...");
                
                // 等待用户手动登录并按回车键继续
                System.in.read();  // 阻塞程序，直到用户按下回车 */

                // 获取页面 HTML
                String pageSource = driver.getPageSource();

                // 使用 Jsoup 解析 HTML
                Document document = Jsoup.parse(pageSource);


                // 通过class获取ul标签
                Elements ul = document.getElementsByClass("gl-warp clearfix");
                // 获取ul标签下的所有li标签
                Elements liList = ul.select("li");
                for (Element element : liList) {
                    System.out.println("------------------");
                    // 过滤内层标签
                    if ("ps-item".equals(element.attr("class"))) {
                        continue;
                    }
                    String img_url = element.getElementsByTag("img").first().attr("data-lazy-img");
                    if (img_url.equals("done")) {
                        Element imgElement = element.select("img").first();
                        img_url = imgElement != null ? imgElement.attr("src") : "//img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg";
                    }
                    img_url = "https:" + img_url;

                    Element priceElement = element.select("i[data-price]").first();
                    String price = priceElement != null ? priceElement.text() : "1024.00"; 
                    /* String price = element.getElementsByClass("p-price").first().text(); */
                    String pricehtml = element.getElementsByClass("p-price").first().html();
                    String shopName = element.getElementsByClass("p-shop").first().text();
                    System.out.println(img_url);
                    System.out.println(price);
                    System.out.println(pricehtml);
                    System.out.println(shopName);
                }

                // a
                /* Elements div = document.getElementsByClass("tbpc-row tbpc-row-start");

                Elements DivList = div.select("div");
                for (Element element : DivList) {
                    Elements goods = element.select("[class*='search-content-col']");
                    Element link = goods.select("a").first();
                    if (link != null) {
                        // href
                        String href = link.attr("href");
                        href = "https:" + href;

                        // skuID
                        String regex = "skuId=(\\d+)";  // 匹配 skuId= 后的数字部分
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(href);
                        String skuId = "100";
                        if (matcher.find()) {
                            // 获取匹配的 skuId
                            skuId = matcher.group(1);
                        } else {
                        }

                        // img_url
                        Element imgElement = goods.select("img").first();
                        String img_url = imgElement != null ? imgElement.attr("src") : "https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg";

                        // goods_name
                        Element nameElement = goods.select("[class*='title--qJ7Xg_90']").first().select("span").first();
                        String goods_name = nameElement != null ? nameElement.text() : "未找到";

                        // price
                        Element priceIntElement = goods.select("[class*='priceInt']").first();
                        Element priceFloatElement = goods.select("[class*='priceFloat']").first();
                        String priceInt = priceIntElement != null ? priceIntElement.text() : "1024";
                        String priceFloat = priceFloatElement != null ? priceFloatElement.text() : ".00";
                        String goodsprice = priceInt + priceFloat;
                        double price = Double.parseDouble(goodsprice);

                        String platform = "淘宝";

                        library.addgoods(skuId, goods_name, href, img_url, price, platform);
                        // 此处并没有用到goods_id
                        Goods agoods = new Goods(1, goods_name, href, img_url, price, platform, skuId);
                        Goods_list.add(agoods);

                    }
                } */
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                driver.quit();
            }

            WebDriver driver1 = new ChromeDriver(options);
            // 淘宝
            try {
                // 登录后访问商品页面（示例链接）
                String targetUrl = "https://s.taobao.com/search?commend=all&ie=utf8&initiative_id=tbindexz_20170306&page=1&q=" + to_search + "&tab=all";
                driver1.get(targetUrl);

                driver1.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

                /* // 给用户时间登录
                System.out.println("请手动登录淘宝，登录后按回车键继续...");
                
                // 等待用户手动登录并按回车键继续
                System.in.read();  // 阻塞程序，直到用户按下回车 */

                // 获取页面 HTML
                String pageSource = driver1.getPageSource();

                // 使用 Jsoup 解析 HTML
                Document document = Jsoup.parse(pageSource);

                Elements div = document.getElementsByClass("tbpc-row tbpc-row-start");

                Elements DivList = div.select("div");
                for (Element element : DivList) {
                    Elements goods = element.select("[class*='search-content-col']");
                    Element link = goods.select("a").first();
                    if (link != null) {
                        // href
                        String href = link.attr("href");
                        href = "https:" + href;
                        /* System.out.println("------------------");  
                        System.out.println(href); */

                        // skuID
                        String regex = "skuId=(\\d+)";  // 匹配 skuId= 后的数字部分
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(href);
                        String skuId = "100";
                        if (matcher.find()) {
                            // 获取匹配的 skuId
                            skuId = matcher.group(1);
                            /* System.out.println("Extracted skuId: " + skuId); */
                        } else {
                            /* System.out.println("skuId not found in the URL."); */
                        }

                        // img_url
                        /* Element imgElement = goods.select("[class*='mainPic--Ds3X7I8z']").first(); */
                        Element imgElement = goods.select("img").first();
                        String img_url = imgElement != null ? imgElement.attr("src") : "https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg";
                        /* System.out.println(img_url); */

                        // goods_name
                        Element nameElement = goods.select("[class*='title--qJ7Xg_90']").first().select("span").first();
                        String goods_name = nameElement != null ? nameElement.text() : "未找到";
                        /* System.out.println(goods_name); */

                        // price
                        Element priceIntElement = goods.select("[class*='priceInt']").first();
                        Element priceFloatElement = goods.select("[class*='priceFloat']").first();
                        String priceInt = priceIntElement != null ? priceIntElement.text() : "1024";
                        String priceFloat = priceFloatElement != null ? priceFloatElement.text() : ".00";
                        String goodsprice = priceInt + priceFloat;
                        double price = Double.parseDouble(goodsprice);
                        /* System.out.println(price); */

                        String platform = "淘宝";
                        /* System.out.println(platform); */

                        library.addgoods(skuId, goods_name, href, img_url, price, platform);
                        // 此处并没有用到goods_id
                        Goods agoods = new Goods(1, goods_name, href, img_url, price, platform, skuId);
                        Goods_list.add(agoods);

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                driver1.quit(); // 关闭浏览器
            }
            
            GoodsResults result = new GoodsResults(Goods_list);
            String response = JSON.toJSONString(result);
            JSONObject object = JSONObject.parseObject(response);
            JSONArray jsarr = object.getJSONArray("results");
            String ret = JSON.toJSONString(jsarr);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(ret.getBytes("UTF-8"));
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("successfull".getBytes());
            outputStream.close();
        }
    }

    static class AddUserHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();

            if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            String user_name = jobj.getString("user_name");
            String password = jobj.getString("password");
            String email = jobj.getString("email");
            ApiResult result = library.adduser(user_name, password, email);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok == true) {
                outputStream.write("1".getBytes());
            } else {
                outputStream.write("0".getBytes());
            }
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("successfull".getBytes());
            outputStream.close();
        }
    }

    static class CheckUserHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();

            if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            String user_name = jobj.getString("user_name");
            String password = jobj.getString("password");
            String email = jobj.getString("email");
            ApiResult result = library.checkuser(user_name, password, email);
            String ret = JSON.toJSONString(result.payload);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok == true) {
                outputStream.write(ret.getBytes());
            } else {
                outputStream.write("0".getBytes());
            }
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("successfull".getBytes());
            outputStream.close();
        }
    }
    
    static class CardHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();

            ApiResult result = library.showCards();
            String response = JSON.toJSONString(result.payload);
            JSONObject object = JSONObject.parseObject(response);
            JSONArray jsarr = object.getJSONArray("cards");
            String ret = JSON.toJSONString(jsarr);
            // 写
            outputStream.write(ret.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            String name = jobj.getString("name");
            String department = jobj.getString("department");
            String type = jobj.getString("type");
            Card newcard = new Card();
            newcard.setName(name);
            newcard.setDepartment(department);
            if("S".equals(type)){
                newcard.setType(Card.CardType.Student);
            } else {
                newcard.setType(Card.CardType.Teacher);
            }
            ApiResult result = library.registerCard(newcard);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok == true) {
                outputStream.write("1".getBytes());
            } else {
                outputStream.write("0".getBytes());
            }
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
    }

    static class ModifyCardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法
            String requestMethod = exchange.getRequestMethod();

            if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            int id = jobj.getIntValue("id");
            String name = jobj.getString("name");
            String department = jobj.getString("department");
            String type = jobj.getString("type");
            Card newcard = new Card();
            newcard.setCardId(id);
            newcard.setName(name);
            newcard.setDepartment(department);
            if("S".equals(type)){
                newcard.setType(Card.CardType.Student);
            } else {
                newcard.setType(Card.CardType.Teacher);
            }
            ApiResult result = library.ModifyCard(newcard);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok == true) {
                outputStream.write("1".getBytes());
            } else {
                outputStream.write("0".getBytes());
            }
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
    }

    static class RemoveCardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法
            String requestMethod = exchange.getRequestMethod();

            if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            int id = jobj.getIntValue("id");
            ApiResult result = library.removeCard(id);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok == true) {
                outputStream.write("1".getBytes());
            } else {
                outputStream.write("0".getBytes());
            }
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
    }    

    static class BorrowHistoryHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String str_id = query.split("=")[1];
            int id = Integer.parseInt(str_id);
            // System.out.println(id);
            /* JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            int id = jobj.getIntValue("cardID"); */
            ApiResult result = library.showBorrowHistory(id);
            String response = JSON.toJSONString(result.payload);
            JSONObject object = JSONObject.parseObject(response);
            JSONArray jsarr = object.getJSONArray("items");
            String ret = JSON.toJSONString(jsarr);
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();

            //JSONObject object = JSONObject.parseObject(response);
            //JSONArray jsarr = object.getJSONArray("cards");
            //String ret = JSON.toJSONString(jsarr);
            // 写
            outputStream.write(ret.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
    }

    static class BookHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();

            ApiResult result = library.showBooks();
            String response = JSON.toJSONString(result.payload);
            JSONObject object = JSONObject.parseObject(response);
            JSONArray jsarr = object.getJSONArray("results");
            String ret = JSON.toJSONString(jsarr);
            // 写
            outputStream.write(ret.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            String category = jobj.getString("category");
            String title = jobj.getString("title");
            String press = jobj.getString("press");
            //String publish_year = jobj.getString("publish_year");
            int publish_year = jobj.getIntValue("publish_year");
            String author = jobj.getString("author");
            //String price = jobj.getString("price");
            double price = jobj.getDouble("price");
            //String stock = jobj.getString("stock");
            int stock = jobj.getIntValue("stock");
            Book newbook = new Book();
            newbook.setCategory(category);
            newbook.setTitle(title);
            newbook.setPress(press);
            newbook.setPublishYear(publish_year);
            newbook.setAuthor(author);
            newbook.setPrice(price);
            newbook.setStock(stock);
            ApiResult result = library.storeBook(newbook);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok == true) {
                outputStream.write("1".getBytes());
            } else {
                outputStream.write("0".getBytes());
            }
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
    }

    static class IncBookHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            int bookId = jobj.getIntValue("book_id");
            int deltaStock = jobj.getIntValue("deltaStock");
            ApiResult result = library.incBookStock(bookId, deltaStock);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok == true) {
                outputStream.write("1".getBytes());
            } else {
                outputStream.write("0".getBytes());
            }
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
    }

    static class ModifyBookHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            int id = jobj.getIntValue("book_id");
            String category = jobj.getString("category");
            String title = jobj.getString("title");
            String press = jobj.getString("press");
            //String publish_year = jobj.getString("publish_year");
            int publish_year = jobj.getIntValue("publish_year");
            String author = jobj.getString("author");
            //String price = jobj.getString("price");
            double price = jobj.getDouble("price");
            Book newbook = new Book();
            newbook.setBookId(id);
            newbook.setCategory(category);
            newbook.setTitle(title);
            newbook.setPress(press);
            newbook.setPublishYear(publish_year);
            newbook.setAuthor(author);
            newbook.setPrice(price);
            ApiResult result = library.modifyBookInfo(newbook);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok == true) {
                outputStream.write("1".getBytes());
            } else {
                outputStream.write("0".getBytes());
            }
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
    }

    static class BorrowBookHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            int bookId = jobj.getIntValue("book_id");
            int cardId = jobj.getIntValue("card_id");
            Borrow borrow = new Borrow();
            borrow.setBookId(bookId);
            borrow.setCardId(cardId);
            borrow.resetBorrowTime();
            
            ApiResult result = library.borrowBook(borrow);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok == true) {
                outputStream.write("1".getBytes());
            } else {
                outputStream.write("0".getBytes());
            }
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
    }

    static class ReturnBookHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            int bookId = jobj.getIntValue("book_id");
            int cardId = jobj.getIntValue("card_id");
            Borrow borrow = new Borrow();
            borrow.setBookId(bookId);
            borrow.setCardId(cardId);
            borrow.resetReturnTime();
            
            ApiResult result = library.returnBook(borrow);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok == true) {
                outputStream.write("1".getBytes());
            } else {
                outputStream.write("0".getBytes());
            }
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
    }

    static class RemoveBookHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            JSONObject jobj = JSON.parseObject(requestBodyBuilder.toString());
            int bookId = jobj.getIntValue("book_id");
            
            ApiResult result = library.removeBook(bookId);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok == true) {
                outputStream.write("1".getBytes());
            } else {
                outputStream.write("0".getBytes());
            }
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
    }

    static class AddBooksHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }

            // System.out.println("Received POST request : " + requestBodyBuilder.toString());
        
            // String hhh = "[{\"book_id\":\"1\",\"category\":\"Others\",\"title\":\"Meta\",\"press\":\"Press_jjj\",\"publish_year\":2020,\"author\":\"kankan\",\"price\":120.9,\"stock\":3},{\"book_id\":\"2\",\"category\":\"Others\",\"title\":\"ants\",\"press\":\"Press_kkk\",\"publish_year\":2019,\"author\":\"kankan\",\"price\":120.8,\"stock\":2},{\"book_id\":\"3\",\"category\":\"Others\",\"title\":\"haha\",\"press\":\"Press_iuyo\",\"publish_year\":2001,\"author\":\"kankan\",\"price\":12.8,\"stock\":1},{\"book_id\":\"4\",\"category\":\"Others\",\"title\":\"kee\",\"press\":\"Press_ooo\",\"publish_year\":2004,\"author\":\"kan\",\"price\":23.8,\"stock\":2}]";

            JSONObject object = JSONObject.parseObject(requestBodyBuilder.toString());
            String hhh = object.getString("boos");
            JSONArray jsarr = JSON.parseArray(hhh);

            List<Book> bookList = new ArrayList<>();

            for (int i = 0; i < jsarr.size(); i++) {
                JSONObject jobj = (JSONObject)jsarr.get(i);
                String category = jobj.getString("category");
                String title = jobj.getString("title");
                String press = jobj.getString("press");
                int publish_year = jobj.getIntValue("publish_year");
                String author = jobj.getString("author");
                double price = jobj.getDouble("price");
                int stock = jobj.getIntValue("stock");
                Book newbook = new Book();
                newbook.setCategory(category);
                newbook.setTitle(title);
                newbook.setPress(press);
                newbook.setPublishYear(publish_year);
                newbook.setAuthor(author);
                newbook.setPrice(price);
                newbook.setStock(stock);
                bookList.add(newbook);
            }

            ApiResult result = library.storeBook(bookList);
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok == true) {
                outputStream.write("1".getBytes());
            } else {
                outputStream.write("0".getBytes());
            }
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 读取OPTION请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received OPTION request : " + requestBodyBuilder.toString());
        
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
    }
}



