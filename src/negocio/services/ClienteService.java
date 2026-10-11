package negocio.services;
import negocio.model.Cliente; import persistencia.dao.ClienteDAO; import java.util.*; import java.util.regex.Pattern;
public class ClienteService {
 private final ClienteDAO dao; public ClienteService(ClienteDAO dao){this.dao=dao;}
 public List<Cliente> listar(){return dao.listar();}
 public Cliente buscar(String id){Cliente c=dao.buscar(id);if(c==null)throw new IllegalArgumentException("Cliente no encontrado");return c;}
 public synchronized Cliente crear(String nombre,String correo){if(nombre==null||nombre.isBlank())throw new IllegalArgumentException("El nombre es obligatorio");if(correo==null||!Pattern.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$",correo))throw new IllegalArgumentException("Correo inválido");for(Cliente c:dao.listar())if(c.correo.equalsIgnoreCase(correo))throw new IllegalArgumentException("Ya existe un cliente con ese correo");Cliente c=new Cliente("C"+UUID.randomUUID().toString().substring(0,8).toUpperCase(),nombre.trim(),correo.trim(),"JUNIOR",0,0);dao.guardar(c);return c;}
 public void actualizar(Cliente c){dao.actualizar(c);}
}
