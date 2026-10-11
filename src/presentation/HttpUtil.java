package presentation;
import persistencia.json.MiniJson;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public final class HttpUtil {
 private HttpUtil(){}
 public static Map<String,Object> body(HttpExchange x)throws IOException{String s=new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);if(s.isBlank())return new LinkedHashMap<>();Object o=MiniJson.parse(s);if(!(o instanceof Map))throw new IllegalArgumentException("El cuerpo debe ser un objeto JSON");return (Map<String,Object>)o;}
 public static void json(HttpExchange x,int status,Object value)throws IOException{byte[] b=MiniJson.stringify(value).getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");x.getResponseHeaders().set("Access-Control-Allow-Origin","*");x.sendResponseHeaders(status,b.length);try(OutputStream os=x.getResponseBody()){os.write(b);}}
 public static Map<String,Object> map(Object...kv){Map<String,Object>m=new LinkedHashMap<>();for(int i=0;i<kv.length;i+=2)m.put(String.valueOf(kv[i]),kv[i+1]);return m;}
}
