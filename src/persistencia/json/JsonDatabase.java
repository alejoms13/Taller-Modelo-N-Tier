package persistencia.json;

import negocio.model.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class JsonDatabase {
    private final Path dir;
    public JsonDatabase(Path dir) { this.dir=dir; try { Files.createDirectories(dir); } catch(IOException e){throw new RuntimeException(e);} seedIfMissing(); }
    private Path path(String name){return dir.resolve(name);}
    private synchronized List<Map<String,Object>> read(String name) {
        try { if(!Files.exists(path(name))) return new ArrayList<>(); String s=Files.readString(path(name),StandardCharsets.UTF_8); if(s.isBlank())return new ArrayList<>(); Object o=MiniJson.parse(s); List<Map<String,Object>> out=new ArrayList<>(); for(Object x:(List<?>)o) out.add((Map<String,Object>)x); return out; }
        catch(Exception e){throw new RuntimeException("No se pudo leer "+name+": "+e.getMessage(),e);}
    }
    private synchronized void write(String name,List<Map<String,Object>> rows) {
        try { Path tmp=dir.resolve(name+".tmp"); Files.writeString(tmp,MiniJson.stringify(rows),StandardCharsets.UTF_8); try {Files.move(tmp,path(name),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);} catch(AtomicMoveNotSupportedException ex){Files.move(tmp,path(name),StandardCopyOption.REPLACE_EXISTING);} }
        catch(IOException e){throw new RuntimeException("No se pudo guardar "+name,e);}
    }
    public synchronized List<Producto> productos(){List<Producto> out=new ArrayList<>();for(Map<String,Object>m:read("productos.json"))out.add(producto(m));return out;}
    public synchronized List<Cliente> clientes(){List<Cliente> out=new ArrayList<>();for(Map<String,Object>m:read("clientes.json"))out.add(cliente(m));return out;}
    public synchronized List<Pedido> pedidos(){List<Pedido> out=new ArrayList<>();for(Map<String,Object>m:read("pedidos.json"))out.add(pedido(m));return out;}
    public synchronized void saveProducto(Producto p){List<Map<String,Object>>r=read("productos.json");upsert(r,productoMap(p));write("productos.json",r);}
    public synchronized void saveCliente(Cliente c){List<Map<String,Object>>r=read("clientes.json");upsert(r,clienteMap(c));write("clientes.json",r);}
    public synchronized void savePedido(Pedido p){List<Map<String,Object>>r=read("pedidos.json");upsert(r,pedidoMap(p));write("pedidos.json",r);}
    private void upsert(List<Map<String,Object>> rows,Map<String,Object> m){String id=String.valueOf(m.get("id"));for(int i=0;i<rows.size();i++)if(id.equals(String.valueOf(rows.get(i).get("id")))){rows.set(i,m);writeFor(rows);return;}rows.add(m);}
    // The caller writes the matching file after upsert; this method is intentionally a no-op.
    private void writeFor(List<Map<String,Object>> rows) {}
    private void seedIfMissing(){
        if(!Files.exists(path("productos.json"))) {List<Map<String,Object>>r=new ArrayList<>();r.add(productoMap(new Producto("P001","Espresso StackTrace",6500,30,"Espresso intenso")));r.add(productoMap(new Producto("P002","Combo BugHunter",18000,15,"Café y snack")));r.add(productoMap(new Producto("P003","Té Async/Await",7000,25,"Té caliente")));r.add(productoMap(new Producto("P004","Latte NullPointer",9500,20,"Latte cremoso")));write("productos.json",r);}
        if(!Files.exists(path("clientes.json")))write("clientes.json",new ArrayList<>());
        if(!Files.exists(path("pedidos.json")))write("pedidos.json",new ArrayList<>());
    }
    private static String s(Map<String,Object>m,String k,String d){Object o=m.get(k);return o==null?d:String.valueOf(o);}
    private static int n(Map<String,Object>m,String k,int d){Object o=m.get(k);return o instanceof Number?((Number)o).intValue():d;}
    private Producto producto(Map<String,Object>m){return new Producto(s(m,"id",""),s(m,"nombre",""),n(m,"precio",0),n(m,"stock",0),s(m,"descripcion",""));}
    private Cliente cliente(Map<String,Object>m){return new Cliente(s(m,"id",""),s(m,"nombre",""),s(m,"correo",""),s(m,"nivel","JUNIOR"),n(m,"devPoints",0),n(m,"comprasAcumuladas",0));}
    private Pedido pedido(Map<String,Object>m){Pedido p=new Pedido();p.id=s(m,"id","");p.clienteId=s(m,"clienteId","");p.estado=s(m,"estado","PENDIENTE_PAGO");p.subtotal=n(m,"subtotal",0);p.descuentoNivel=n(m,"descuentoNivel",0);p.descuentoPuntos=n(m,"descuentoPuntos",0);p.total=n(m,"total",0);p.puntosRedimidos=n(m,"puntosRedimidos",0);p.puntosGanados=n(m,"puntosGanados",0);p.fecha=s(m,"fecha","");p.modalidad=s(m,"modalidad","MESA");Object its=m.get("items");if(its instanceof List)for(Object x:(List<?>)its){Map<String,Object>i=(Map<String,Object>)x;p.items.add(new ItemPedido(s(i,"productoId",""),s(i,"nombreProducto",""),n(i,"cantidad",0),n(i,"precioUnitario",0)));}return p;}
    private Map<String,Object> productoMap(Producto p){Map<String,Object>m=new LinkedHashMap<>();m.put("id",p.id);m.put("nombre",p.nombre);m.put("precio",p.precio);m.put("stock",p.stock);m.put("descripcion",p.descripcion);return m;}
    private Map<String,Object> clienteMap(Cliente c){Map<String,Object>m=new LinkedHashMap<>();m.put("id",c.id);m.put("nombre",c.nombre);m.put("correo",c.correo);m.put("nivel",c.nivel);m.put("devPoints",c.devPoints);m.put("comprasAcumuladas",c.comprasAcumuladas);return m;}
    private Map<String,Object> pedidoMap(Pedido p){Map<String,Object>m=new LinkedHashMap<>();m.put("id",p.id);m.put("clienteId",p.clienteId);m.put("estado",p.estado);m.put("subtotal",p.subtotal);m.put("descuentoNivel",p.descuentoNivel);m.put("descuentoPuntos",p.descuentoPuntos);m.put("total",p.total);m.put("puntosRedimidos",p.puntosRedimidos);m.put("puntosGanados",p.puntosGanados);m.put("fecha",p.fecha);m.put("modalidad",p.modalidad);List<Map<String,Object>>its=new ArrayList<>();for(ItemPedido i:p.items){Map<String,Object>x=new LinkedHashMap<>();x.put("productoId",i.productoId);x.put("nombreProducto",i.nombreProducto);x.put("cantidad",i.cantidad);x.put("precioUnitario",i.precioUnitario);its.add(x);}m.put("items",its);return m;}
}
