package persistencia.json;
import negocio.model.Cliente; import persistencia.dao.ClienteDAO; import java.util.*;
public class JsonClienteDAO implements ClienteDAO {
 private final JsonDatabase db; public JsonClienteDAO(JsonDatabase db){this.db=db;}
 public List<Cliente> listar(){return db.clientes();}
 public Cliente buscar(String id){return listar().stream().filter(c->c.id.equals(id)).findFirst().orElse(null);}
 public void guardar(Cliente c){db.saveCliente(c);}
 public void actualizar(Cliente c){db.saveCliente(c);}
}
