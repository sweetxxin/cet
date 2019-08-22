package com.xxin.cet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxin.cet.entity.Message;
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
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author xxin
 * @Created
 * @Date 2019/8/21 0:50
 * @Description
 */
@Controller
public class NumController {
    CloseableHttpClient client = HttpClients.createDefault();
    @GetMapping("/index")
    public String num() throws IOException {
        HttpGet get = new HttpGet("http://cet-bm.neea.edu.cn/Home/QuickPrintTestTicket");
        CloseableHttpResponse res = client.execute(get);
        res.close();
        return "index";
    }
    @GetMapping("/numCode")
    private void getNumCodeImg(HttpServletResponse response){
        HttpGet get = new HttpGet("http://cet-bm.neea.edu.cn/Home/VerifyCodeImg?a="+Math.random()*10);
        response.setContentType("image/jpeg");//设置相应类型,告诉浏览器输出的内容为图片
        response.setHeader("Pragma", "No-cache");//设置响应头信息，告诉浏览器不要缓存此内容
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expire", 0);

        try {
            CloseableHttpResponse res = client.execute(get);
            HttpEntity entity = res.getEntity();
            InputStream in = entity.getContent();
            OutputStream out = response.getOutputStream();
            int end;
            while ((end=in.read())!=-1){
                out.write(end);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @PostMapping("/getNum")
    @ResponseBody
    public Message getNum(@RequestParam("num")String num,
                          @RequestParam("name")String name,
                          @RequestParam("code") String code
    ) throws IOException {
        HttpPost post = new HttpPost("http://cet-bm.neea.edu.cn/Home/ToQuickPrintTestTicket");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("provinceCode","44"));
        nvps.add(new BasicNameValuePair("IDTypeCode","1" ));
        nvps.add(new BasicNameValuePair("IDNumber",num ));
        nvps.add(new BasicNameValuePair("Name",name ));
        nvps.add(new BasicNameValuePair("verificationCode",code ));

        post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
        post.setHeader("Content-type", "application/x-www-form-urlencoded");
        post.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        CloseableHttpResponse res = client.execute(post);
        HttpEntity entity = res.getEntity();
        String json = EntityUtils.toString(entity);
        Message message = new Message();
        if (json.contains("验证码错误")){
            message.setMsg("验证码错误");
            return message;
        }
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String,Object>map = null;
            map = mapper.readValue(json, HashMap.class);
        if (json.contains("-1")){
           message.setMsg(map.get("Message").toString());
        }
        try {
            List<HashMap<String,Object>>list = mapper.readValue(map.get("Message").toString(), List.class);
            String sid = list.get(0).get("SID").toString();
            String data = download(sid);

            message.setData(data);
            message.setSuccess(true);
            return message;
        } catch (IOException e) {
            return message;
        }
    }
    private String download(String sid) throws IOException {
        String url = "http://cet-bm.neea.edu.cn/Home/DownTestTicket?SID="+sid;
        HttpGet get = new HttpGet(url);
        System.out.println("下载文件");
        CloseableHttpResponse res = client.execute(get);
        HttpEntity entity = res.getEntity();
        String path = "num.zip";
        byte[] bytes = EntityUtils.toByteArray(entity);
        File file = new File(path);
        if ( file.createNewFile()){
            FileChannel channel = FileChannel.open(Paths.get(path), StandardOpenOption.WRITE);
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length+1024);
            byteBuffer.clear();
            byteBuffer.put(bytes);
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()){
                channel.write(byteBuffer);
            }
            channel.close();
            res.close();
            return parsePDF(path);
        }else {
            return "系统出错啦!!!";
        }
    }
    private static String parsePDF(String path) throws IOException {
        ZipFile zipFile=new ZipFile(path, Charset.forName("gbk"));
        ZipEntry entry=null;
        InputStream stream = null;
        Enumeration enums=zipFile.entries();
        while(enums.hasMoreElements()) {
            entry = (ZipEntry) enums.nextElement();
            stream = zipFile.getInputStream(entry);
            break;
        }
        // 新建一个PDF解析器对象
        PDFParser parser = new PDFParser(new RandomAccessBuffer(stream));
        // 对PDF文件进行解析
        parser.parse();
        // 获取解析后得到的PDF文档对象
        PDDocument pdfdocument = parser.getPDDocument();
        // 新建一个PDF文本剥离器
        PDFTextStripper stripper = new PDFTextStripper();
        // 从PDF文档对象中剥离文本
        String result = stripper.getText(pdfdocument);
        String num = result.substring(result.indexOf("准考证号")+5,result.indexOf("准考证号")+20);
        System.out.println(num);
        zipFile.close();
        Files.delete(Paths.get(path));
        pdfdocument.close();
        return num;
    }
}
