package persistencia.json;
import negocio.model.Pedido; import persistencia.dao.PedidoDAO; import java.util.*;
public class JsonPedidoDAO implements PedidoDAO {
 private final JsonDatabase db; public JsonPedidoDAO(JsonDatabase db){this.db=db;}
 public List<Pedido> listar(){return db.pedidos();}
 public Pedido buscar(String id){return listar().stream().filter(p->p.id.equals(id)).findFirst().orElse(null);}
 public void guardar(Pedido p){db.savePedido(p);}
 public void actualizar(Pedido p){db.savePedido(p);}
}
