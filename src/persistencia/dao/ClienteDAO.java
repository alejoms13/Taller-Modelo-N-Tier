package persistencia.dao;
import negocio.model.Cliente;
import java.util.List;
public interface ClienteDAO { List<Cliente> listar(); Cliente buscar(String id); void guardar(Cliente c); void actualizar(Cliente c); }
