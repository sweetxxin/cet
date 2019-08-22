package com.xxin.cet.controller;

import com.xxin.cet.entity.Message;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.print.attribute.standard.MediaSize;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author xxin
 * @Created
 * @Date 2019/8/21 0:33
 * @Description
 */
@Controller
public class GradeController {
    CloseableHttpClient client = HttpClients.createDefault();

    public void login() throws IOException {
        HttpGet get = new HttpGet("https://www.chsi.com.cn/cet");
        CloseableHttpResponse res = client.execute(get);
        res.close();
    }
//    @GetMapping("/cetCode")
//    private void getCetCodeImg(HttpServletResponse response){
//        HttpGet get = new HttpGet("https://www.chsi.com.cn/cet/ValidatorIMG.JPG?ID="+Math.random()*10);
//        response.setContentType("image/jpeg");//设置相应类型,告诉浏览器输出的内容为图片
//        response.setHeader("Pragma", "No-cache");//设置响应头信息，告诉浏览器不要缓存此内容
//        response.setHeader("Cache-Control", "no-cache");
//        response.setDateHeader("Expire", 0);
//        try {
//            login();
//            CloseableHttpResponse res = client.execute(get);
//            HttpEntity entity = res.getEntity();
//            InputStream in = entity.getContent();
//            OutputStream out = response.getOutputStream();
//            int end;
//            while ((end=in.read())!=-1){
//                out.write(end);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    @PostMapping("/getGrade")
    @ResponseBody
    public Message getGrade(@RequestParam("sid")String sid,
                          @RequestParam("name")String name
    ) throws IOException {
        login();
        System.out.println("查成绩");
        HttpPost post = new HttpPost("https://www.chsi.com.cn/cet/query");
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("zkzh",sid));
        nvps.add(new BasicNameValuePair("xm",name));
        post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
        post.setHeader("Content-type", "application/x-www-form-urlencoded");
        post.setHeader("Referer","https://www.chsi.com.cn/cet/");
        post.setHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
        CloseableHttpResponse res = client.execute(post);
        HttpEntity entity = res.getEntity();
        Document doc = Jsoup.parse(EntityUtils.toString(entity));
        Elements elements = doc.select("tr");
        HashMap<String,String> map = new HashMap<>();
        for( Element element : elements ){
            Elements th = element.getElementsByTag("th");
            Elements td = element.getElementsByTag("td");
            if (!td.text().equals("")&&!td.text().equals("--")){
                map.put(th.text().replace("：","" ).replace(" ","" ),td.text());
            }
        }
        Message message = new Message();
        message.setData(map);
        System.out.println(map);
        message.setSuccess(true);
        return message;
    }

    public static void main(String[] args) throws IOException {
        GradeController controller = new GradeController();
        controller.login();
       controller.getGrade("440950191211307", "芮钰烨");
    }
}
