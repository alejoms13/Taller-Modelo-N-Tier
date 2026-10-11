package negocio.model;

public class Cliente {
    public String id;
    public String nombre;
    public String correo;
    public String nivel;
    public int devPoints;
    public int comprasAcumuladas;

    public Cliente() {}
    public Cliente(String id, String nombre, String correo, String nivel, int devPoints, int comprasAcumuladas) {
        this.id=id; this.nombre=nombre; this.correo=correo; this.nivel=nivel;
        this.devPoints=devPoints; this.comprasAcumuladas=comprasAcumuladas;
    }
}
