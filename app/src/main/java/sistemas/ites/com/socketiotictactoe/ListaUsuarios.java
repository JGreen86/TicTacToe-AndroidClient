package sistemas.ites.com.socketiotictactoe;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.emitter.Emitter;
import com.google.gson.Gson;

/**
 * Created by Jacob Green on 29/05/18.
 */


/*
 * ListaUsuarios.java
 *
 * Muestra una lista con todos los usuarios conectados al servidor. Permite iniciar un juego con
 * cualquiera de estos usuarios.
 *
 *
 */
public class ListaUsuarios extends AppCompatActivity {

    private String TAG = "ListaUsuarios";
    private Context mContext;
    // Dirección IP del servidor y puerto. Cambiar por su dirección.
    public static final String SERVER = "http://192.168.1.75:3000";

    // ListView donde se mostrarán los usuarios conectados
    private ListView llUserList;
    private UsuarioAdapter usuarioAdapter;
    // ArrayList que contendrá la lista de usuarios conectados que nos envia el servidor
    private ArrayList<Usuario> usuariosArray = new ArrayList<>();

    // Obtener instancia Socket io
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(SERVER);
        } catch (URISyntaxException e) {}
    }
    // Id del usuario que ejecuta la app
    private String miIdUsuario;
    // Nombre del usuario que ejecuta la app
    private String miNombreUsuario;
    // Flag que indica si el usuario está en medio de un juego o no
    private boolean jugando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_usuarios);

        mContext = this;
        // Checamos si el activity anterior envió el nombre de usuario o  no
        if (getIntent().hasExtra("usuario"))
        {
            miNombreUsuario = getIntent().getStringExtra("usuario");
        } else {
            miNombreUsuario = "anonimo";
        }

        // Registrar listeners para eventos socket io

        /*
         * onIdUsuario sucederá cuando el servidor ejecuta la linea:
         *
         * socket.emit('id usuario', socket.id);
         */
        mSocket.on("id usuario", onIdUsuario);
        // onListaUsuario recibirá la lista de usuarios actualiza enviada por el servidor
        mSocket.on("lista usuarios", onListaUsuario);
        // onUsuarioDesconectado sucederá cuando el servidor informa que se ha desconectado un usuario
        mSocket.on("usuario desconectado", onUsuarioDesconectado);
        /*
         * onPeticionJuego recibirá una solicitud de otro usuario para iniciar un juego.
         * Sucederá cuando el servidor ejecuta la linea:
         *
         * socket.broadcast.to(id).emit('peticion juego',socket.id,jugador);
         */
        mSocket.on("peticion juego", onPeticionJuego);
        // onIniciarJuego sucederá cuando el contricante aceptó la solicitud para jugar
        mSocket.on("iniciar juego", onIniciarJuego);
        // onUsuarioOcupado sucederá cuando el usuario al que se envia la petición para jugar ya está
        // jugando
        mSocket.on("usuario ocupado", onUsuarioOcupado);
        /*
         * Realizamos la conexión al servidor.
         * Entonces en el servidor sucederá el evento 'connection'.
         */
        mSocket.connect();

        // Definimos la interfaz gráfica referente a la lista de usuarios
        llUserList = findViewById(R.id.llUserList);
        usuarioAdapter = new UsuarioAdapter(getApplicationContext(), R.layout.row_user, usuariosArray);

        llUserList.setAdapter(usuarioAdapter);

        llUserList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final Usuario t = (Usuario) adapterView.getAdapter().getItem(i);
                Log.i(TAG,"click on " + t.getNombre());
                // Cuando se da click sobre un usuario en la lista, se le envia una petición para jugar
                mSocket.emit("peticion juego",t.getId(),miNombreUsuario);
            }
        });

    }
    @Override
    public void onResume()
    {
        super.onResume();
        // Ponemos el flag 'jugando' en falso cada vez que onResume sucede, por ejemplo, cuando se
        // regresa de TableroActivity a ésta activity (ListaUsuarios)
        jugando = false;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Importante desconectarnos del servidor cuando se cierra la aplicación
        mSocket.disconnect();
        // Dejar de recibir eventos por parte del servidor
        mSocket.off();
    }

    // Eventos Socket io
    // El servidor nos envia nuestro id de usuario
    private Emitter.Listener onIdUsuario = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i(TAG,"onIdUsuario");
            // Como éste evento solo regresa un parametro tomamos args[0] y lo guardos en miIdUsuario
            miIdUsuario = (String) args[0];
            // Procedemos a registrar en el servidor nuestro nombre de usuario
            mSocket.emit("registro", miNombreUsuario);
        }
    };
    /*
     * El servidor nos envia la lista actualiza de usuarios.
     * Como la lista viene en un string con formato json, utilizamos Gson para
     * convertirla a un ArrayList de objetos Usuario, luego la mostramos en nuestro ListView llUserList.
     */
    private Emitter.Listener onListaUsuario = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // Guardamos el primer parametro recibido en users
            String users = (String) args[0];
            Log.i(TAG,"onListaUsuario " + users);
            Gson gson = new Gson();
            // Convertimos el string 'users' a un arreglo de Usuario
            Usuario[] usuarios = gson.fromJson(users, Usuario[].class);
            // Limpiamos nuestro 'usuariosArray' y le agregamos todos los elementos del arreglo 'usuarios'
            usuariosArray.clear();
            usuariosArray.addAll(new ArrayList<>(Arrays.asList(usuarios)));
            // Eliminar el usuario que ejecuta ésta app con el fin de que no se muestre en el ListView
            for (int i=0;i<usuariosArray.size();i++)
            {
                if (usuariosArray.get(i).getId().equals(miIdUsuario))
                {
                    usuariosArray.remove(i);
                    break;
                }
            }
            /*
             * Actualizar ListView.
             * Dado que los eventos socket io suceden en segundo plano (otro hilo) y Android
             * no permite modificar la interfaz gráfica desde otro hilo que no sea el principal,
             * mandamos ejecutar el método 'notifyAdapter' en el hilo principal.
             */
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyAdapter();
                }
            });
        }
    };
    // El servidor nos informa que un usuario se ha desconectado
    private Emitter.Listener onUsuarioDesconectado = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // Pedimos al servidor nos envie una lista actualizada de usuarios
            mSocket.emit("lista usuarios");
        }
    };
    /*
     * El servidor nos informa que un usuario nos invita a jugar.
     * args[0] contiene el socket id del usuario que envia la petición.
     * args[1] contiene el nombre de usuario del usuario que envia la petición.
     */
    private Emitter.Listener onPeticionJuego = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // Procesamos la petición solo si el usuario no está jugando.
            if (!jugando)
            {
                final String idPeticion = (String) args[0];
                final String nombreJugador = (String) args[1];
                Log.i(TAG, "peticion de juego del usuario " + nombreJugador + " con id " + idPeticion);
                // Ejecutamos en el hilo principal
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Notificamos al usuario por medio de un cuadro de dialogo que otro usuario lo
                        // está invitando a jugar
                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                        builder.setMessage("El usuario <" + nombreJugador + "> lo ha invitado a iniciar una partida.");
                        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // En caso de que se acepte jugar, se le notifica al usuario que
                                // envia la petición que se acepta y que inicia el juego. Luego,
                                // se inicia el activity TableroActivity
                                mSocket.emit("iniciar juego", idPeticion, miNombreUsuario);
                                Intent intent = new Intent(ListaUsuarios.this, TableroActivity.class);
                                intent.putExtra("idJugador",idPeticion);
                                intent.putExtra("nombreJugador",nombreJugador);
                                intent.putExtra("miId",miIdUsuario);
                                intent.putExtra("miNombre",miNombreUsuario);
                                intent.putExtra("turno", true); // El que recibe la peticion va primero
                                startActivity(intent);
                                jugando=true;
                            }
                        });
                        builder.setNegativeButton("Cancelar",null);
                        builder.show();
                    }
                });
            } else {
                // En caso de que el usuario ya esté jugando, avisamos que está ocupado.
                mSocket.emit("usuario ocupado", (String)args[0]);
            }
        }
    };
    /*
     * El servidor nos avisa que el usuario al que enviamos la invitación para jugar, aceptó.
     * args[0] contiene el socket id del contricante.
     * args[1] contiene el nombre de usuario del contricante.
     */
    private Emitter.Listener onIniciarJuego = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            String idContricante = (String) args[0];
            String nombreContricante = (String) args[1];
            Log.i(TAG,"iniciando juego...");
            // Mandamos llamar a TableroActivity y le pasamos los parametros correspondientes.
            Intent intent = new Intent(ListaUsuarios.this, TableroActivity.class);
            intent.putExtra("idJugador",idContricante);
            intent.putExtra("nombreJugador",nombreContricante);
            intent.putExtra("miId",miIdUsuario);
            intent.putExtra("miNombre",miNombreUsuario);
            intent.putExtra("turno", false); // El que envia la petición cede el turno
            startActivity(intent);
            jugando=true;
        }
    };
    // El servidor nos avisa que nuestra petición de juego no pudo ser aceptada porque el otro usuario
    // está ocupado en otro juego.
    private Emitter.Listener onUsuarioOcupado = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // Ejecutamos en hilo principal
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Notificación Toast.
                    Toast.makeText(mContext,"Usuario ocupado!",Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
    /*
     * Método que actualiza el ListView cuando usuariosArray cambia.
     */
    private void notifyAdapter()
    {
        usuarioAdapter.notifyDataSetChanged();
    }
}