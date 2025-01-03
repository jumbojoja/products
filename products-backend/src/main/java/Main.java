import utils.ConnectConfig;
import utils.DatabaseConnector;
import com.alibaba.fastjson.*;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import entities.*;
import queries.ApiResult;
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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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

            // 注册
            server.createContext("/adduser", new AddUserHandler());
            // 登录
            server.createContext("/checkuser", new CheckUserHandler());
            // 查询商品
            server.createContext("/search", new SearchGoodsHandler());
            // 查询历史价格
            server.createContext("/pricesearch", new SearchPriceHandler());
            // 添加收藏
            server.createContext("/addcollect", new CollectGoodsHandler());
            // 查询用户收藏
            server.createContext("/searchcollect", new SearchCollectsHandler());

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

                /* // 显式等待，直到价格元素加载完成
                WebDriverWait wait = new WebDriverWait(driver, 10);
                // 等待 <i data-price> 元素加载完成，等待条件是元素存在于 DOM 中
                wait.until(ExpectedConditions.textToBePresentInElementLocated(
                    By.cssSelector("i[data-price]"),
                    "5"
                )); */

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
                    // 过滤内层标签
                    if ("ps-item".equals(element.attr("class"))) {
                        continue;
                    }
                    Element link = element.select("a").first();
                    if (link != null) {
                        // href
                        String href = link.attr("href");
                        href = "https:" + href;

                        // img_url
                        String img_url = element.getElementsByTag("img").first().attr("data-lazy-img");
                        if (img_url.equals("done")) {
                            Element imgElement = element.select("img").first();
                            img_url = imgElement != null ? imgElement.attr("src") : "//img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg";
                        }
                        img_url = "https:" + img_url;

                        // price
                        Element priceElement = element.select("i[data-price]").first();
                        String sprice = priceElement != null ? priceElement.text() : "1024.00";
                        double price;
                        try {
                            // 尝试解析价格
                            price = Double.parseDouble(sprice);
                        } catch (NumberFormatException e) {
                            // 如果解析失败，设置默认价格
                            price = 1024.0;
                        }

                        // platform
                        String shopName = element.getElementsByClass("p-shop").first().text();

                        // skuId
                        String sku_id = element != null ? element.attr("data-sku") : "";

                        // goodsname
                        Element emElement = element.select("a > em").first();

                        // 提取商品名（去除多余的空格）
                        String productName = emElement != null ? emElement.text().trim() : "";

                        library.addgoods(sku_id, productName, href, img_url, price, shopName);
                        // 此处并没有用到goods_id
                        Goods agoods = new Goods(1, productName, href, img_url, price, shopName, sku_id);
                        Goods_list.add(agoods);
                    }
                    
                }

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

                        // skuID
                        String regex = "skuId=(\\d+)";  // 匹配 skuId= 后的数字部分
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(href);
                        String skuId = "100";
                        if (matcher.find()) {
                            // 获取匹配的 skuId
                            skuId = matcher.group(1);
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
                        double price;
                        try {
                            // 尝试解析价格
                            price = Double.parseDouble(goodsprice);
                        } catch (NumberFormatException e) {
                            // 如果解析失败，设置默认价格
                            price = 1024.0;
                        }

                        String platform = "淘宝";

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

            WebDriver driver2 = new ChromeDriver(options);
            // 唯品会
            try {
                // 登录后访问商品页面（示例链接）
                String targetUrl = "https://category.vip.com/suggest.php?keyword=" + to_search;
                driver2.get(targetUrl);

                driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

                /* // 给用户时间登录
                System.out.println("请手动登录唯品会，登录后按回车键继续...");
                
                // 等待用户手动登录并按回车键继续
                System.in.read();  // 阻塞程序，直到用户按下回车 */

                // 获取页面 HTML
                String pageSource = driver2.getPageSource();

                // 使用 Jsoup 解析 HTML
                Document document = Jsoup.parse(pageSource);

                Elements section = document.getElementsByClass("goods-list c-goods-list--normal");

                Elements DivList = section.select("div");
                for (Element element : DivList) {
                    Elements goods = element.select("[class*='c-goods-item  J-goods-item c-goods-item--auto-width']");
                    Element link = goods.select("a").first();
                    if (link != null) {
                        // href
                        String href = link.attr("href");
                        href = "https:" + href;

                        // skuID
                        String sku_id = element != null ? element.attr("data-product-id") : "";

                        // img_url
                        Element imgElement = goods.select("img").first();
                        String img_url = imgElement != null ? imgElement.attr("data-original") : "https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg";
                        img_url = "https:" + img_url;

                        // goods_name
                        Element nameElement = goods.select("img").first();
                        String goods_name = nameElement != null ? nameElement.attr("alt") : "占位符";

                        // price
                        Element priceIntElement = link.select("[class*='c-goods-item__sale-price J-goods-item__sale-price']").first();
                        Element priceFloatElement = goods.select("[class*='c-goods-item__sale-price-decimal']").first();
                        String priceInt = priceIntElement != null ? priceIntElement.text().replace("¥", "").trim() : "1024";
                        String priceFloat = priceFloatElement != null ? priceFloatElement.text() : ".00";
                        String goodsprice = priceInt + priceFloat;
                        double price;
                        try {
                            // 尝试解析价格
                            price = Double.parseDouble(goodsprice);
                        } catch (NumberFormatException e) {
                            // 如果解析失败，设置默认价格
                            price = 1024.0;
                        }

                        String platform = "唯品会";

                        library.addgoods(sku_id, goods_name, href, img_url, price, platform);
                        // 此处并没有用到goods_id
                        Goods agoods = new Goods(1, goods_name, href, img_url, price, platform, sku_id);
                        Goods_list.add(agoods);

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                driver2.quit(); // 关闭浏览器
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

    static class SearchPriceHandler implements HttpHandler {
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
            String to_search = jobj.getString("sku_id");

            ApiResult apiresult = library.searchgoods(to_search);
            String response = JSON.toJSONString(apiresult.payload);
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

    static class CollectGoodsHandler implements HttpHandler {
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
            int user_id = jobj.getInteger("user_id");
            String sku_id = jobj.getString("sku_id");
            ApiResult result = library.addcollect(sku_id, user_id);
            String ret = JSON.toJSONString(result.payload);
        
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

    static class SearchCollectsHandler implements HttpHandler {
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
            int user_id = jobj.getInteger("user_id");

            ApiResult apiresult = library.showCollects(user_id);
            String response = JSON.toJSONString(apiresult.payload);
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
    
}



