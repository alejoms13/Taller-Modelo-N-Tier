package persistencia.json;
import negocio.model.Producto; import persistencia.dao.ProductoDAO; import java.util.*;
public class JsonProductoDAO implements ProductoDAO {
 private final JsonDatabase db; public JsonProductoDAO(JsonDatabase db){this.db=db;}
 public List<Producto> listar(){return db.productos();}
 public Producto buscar(String id){return listar().stream().filter(p->p.id.equals(id)).findFirst().orElse(null);}
 public void guardar(Producto p){db.saveProducto(p);}
 public void actualizar(Producto p){db.saveProducto(p);}
}
