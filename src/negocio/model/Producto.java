package negocio.model;

public class Producto {
    public String id;
    public String nombre;
    public int precio;
    public int stock;
    public String descripcion;

    public Producto() {}
    public Producto(String id, String nombre, int precio, int stock, String descripcion) {
        this.id=id; this.nombre=nombre; this.precio=precio; this.stock=stock; this.descripcion=descripcion;
    }
}
