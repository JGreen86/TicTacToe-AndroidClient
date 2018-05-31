package sistemas.ites.com.socketiotictactoe;

/**
 * Created by Jacob Green on 30/05/18.
 */

/*
 * Usuario.java
 *
 * Contiene la definición básica (muy básica) de un usuario.
 *
 *
 */
public class Usuario {

    private String id;
    private String nombre;

    public Usuario(String id, String nombre)
    {
        this.id = id;
        this.nombre = nombre;
    }

    public void setId(String id)
    {
        this.id = id;
    }
    public void setNombre(String nombre)
    {
        this.nombre = nombre;
    }
    public String getId()
    {
        return id;
    }
    public String getNombre()
    {
        return nombre;
    }
}
