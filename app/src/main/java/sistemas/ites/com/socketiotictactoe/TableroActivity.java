package sistemas.ites.com.socketiotictactoe;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

/**
 * Created by Jacob Green on 28/05/18.
 */

/*
 * TableroActivity.java
 *
 * Muestra un tablero sencillo (muy sencillo) de 3x3 botones.
 *
 * Incluye el manejo de socket io para la comunicación entre los jugadores.
 */
public class TableroActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "TableroActivity";

    // Arreglo de 9 botones (para no manejar referencias individuales)
    private Button[] botones = new Button[9];
    private Button btnReiniciar;
    private TextView txtTitulo;
    private TextView txtTurno;

    // Declaración de variables referentes al juego
    private boolean miTurno = false;
    private String caracter = "X";
    private String color = "#000000"; // default negro
    private String colorRaya = "#0EAE09"; // verde
    private int totalTurnos = 0;
    private int tamTablero = 3; // tablero de 3x3
    private int[] tablero = new int[tamTablero*tamTablero];
    private int[] indicesFCD = new int[] { 0, 3, 6, 0, 1, 2, 0, 2 }; // indices de inicio de cada fila, columna y diagonal
    private int[] incrIndicesFCD  = new int[] { 1, 1, 1, 3, 3, 3, 4, 2 }; // incremento para cada elemento de la fila, columna y diagonal

    // Usuarios: actual y contricante
    private Usuario miUsuario;
    private Usuario contricante;

    // Obtener instancia de socket io
    private Socket mSocket;
    {
        try {
            // nos devolverá el mismo socket (misma instancia) que en la activity anterior
            // dado que la conexión sigue abierta
            mSocket = IO.socket(ListaUsuarios.SERVER);
        } catch (URISyntaxException e) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tablero_layout);

        // Registrar listeners para eventos socket io
        // onMovimiento sucederá cuando el servidor nos informe que el contricante ha realizado
        // un movimiento sobre el tablero
        mSocket.on("movimiento",onMovimiento);
        // onReiniciar sucederá cuando el servidor nos informe que el contricante ha reiniciado el juego
        mSocket.on("reiniciar juego", onReiniciar);
        // onJuegoAbandonado sucederá cuando el servidor nos informe que el contricante abandonó el juego
        mSocket.on("juego abandonado", onJuegoAbandonado);
        // no debería cumplirse puesto que la activity anterior sigue corriendo
        if (!mSocket.connected())
        {
            Log.i(TAG,"conectando ...");
            mSocket.connect();
        }

        txtTitulo = (TextView) findViewById(R.id.textView);
        txtTurno = (TextView) findViewById(R.id.txtTurno);

        // obtenemos info del contricante enviada por el activity anterior (ListaUsuarios)
        if (getIntent().hasExtra("idJugador") && getIntent().hasExtra("nombreJugador"))
        {
            contricante = new Usuario(getIntent().getStringExtra("idJugador"),getIntent().getStringExtra("nombreJugador"));
        }
        // obtenemos info del usuario que ejecuta esta aplicacion
        if (getIntent().hasExtra("miId") && getIntent().hasExtra("miNombre"))
        {
            miUsuario = new Usuario(getIntent().getStringExtra("miId"),getIntent().getStringExtra("miNombre"));
        }
        // checamos si el turno inicial es mio o del contrario
        if (getIntent().hasExtra("turno"))
        {
            miTurno = getIntent().getBooleanExtra("turno",false);
            if (!miTurno)
            {
                caracter = "O";
                txtTurno.setText("Turno: " + contricante.getNombre());
            } else {
                txtTurno.setText("Tu turno " + miUsuario.getNombre());
            }
        }
        txtTitulo.setText("Jugando contra: " + contricante.getNombre());

        // Inicializar botones
        botones[0] = (Button) findViewById(R.id.btn0);
        botones[1] = (Button) findViewById(R.id.btn1);
        botones[2] = (Button) findViewById(R.id.btn2);
        botones[3] = (Button) findViewById(R.id.btn3);
        botones[4] = (Button) findViewById(R.id.btn4);
        botones[5] = (Button) findViewById(R.id.btn5);
        botones[6] = (Button) findViewById(R.id.btn6);
        botones[7] = (Button) findViewById(R.id.btn7);
        botones[8] = (Button) findViewById(R.id.btn8);

        botones[0].setTag("0");
        botones[1].setTag("1");
        botones[2].setTag("2");
        botones[3].setTag("3");
        botones[4].setTag("4");
        botones[5].setTag("5");
        botones[6].setTag("6");
        botones[7].setTag("7");
        botones[8].setTag("8");

        botones[0].setOnClickListener(this);
        botones[1].setOnClickListener(this);
        botones[2].setOnClickListener(this);
        botones[3].setOnClickListener(this);
        botones[4].setOnClickListener(this);
        botones[5].setOnClickListener(this);
        botones[6].setOnClickListener(this);
        botones[7].setOnClickListener(this);
        botones[8].setOnClickListener(this);

        // Botón reiniciar
        btnReiniciar = (Button) findViewById(R.id.btnReset);
        btnReiniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reiniciarJuego();
                // Informamos al contricante que deseamos reiniciar el juego
                mSocket.emit("reiniciar juego", contricante.getId());
            }
        });

        // Inicializamos el arreglo tablero
        initTablero();
    }
    @Override
    public void onPause()
    {
        super.onPause();
        // Avisamos al contricante que estamos abandonando el juego
        mSocket.emit("juego abandonado", contricante.getId());
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onClick(View view) {

        /*
         * Utilizamos un solo listener para manejar el evento onClick de los 9 botones.
         * Solo respondemos a dicho evento si es el turno del usuario que dió click y además,
         * el tablero no está lleno.
         */
        if (totalTurnos < tablero.length && miTurno)
        {

            String t = (String) view.getTag();
            if (t != null)
            {
                int tag = Integer.parseInt(t);
                // Si tag está entre 0 y 8 quiere decir que se dio click en alguno de los botones del tablero
                if (tag >= 0 && tag <= 8)
                {
                    totalTurnos++;
                    setButtonView(view, tag);
                }
            }
        }
    }

    private void setButtonView(View view, int btnId)
    {
        // Mandamos 'pintar' una X o una O en el botón y lo deshabilitamos.
        Button button = (Button)view;
        button.setText(caracter);
        button.setTextColor(Color.parseColor(color));
        button.setTypeface(button.getTypeface(), Typeface.BOLD);
        button.setEnabled(false);

        // Ponemos un 1 en el arreglo tablero para indicar la posición elegida
        tablero[btnId] = 1;
        // notificar al otro jugador sobre el movimiento que realizamos
        mSocket.emit("movimiento",contricante.getId(),String.valueOf(btnId),caracter);
        // Checamos si hay ganador, empate o el juego continua
        int[] r = calificarTablero();
        notificarGanador(r);
    }

    // Eventos de socket io
    /*
     * El servidor nos avisa que el contricante realizó un movimiento.
     * args[0] contiene la posición del botón pulsado por el contricante.
     * args[1] contiene el texto "X" o "O" que debe ir en esa posición.
     */
    private Emitter.Listener onMovimiento = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            final int botonID = Integer.parseInt((String)args[0]);
            final String c = (String) args[1];
            if (botonID >= 0 && botonID <= 8)
            {
                // Ejecutamos en hilo principal
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Actualizamos el tablero
                        botones[botonID].setText(c);
                        botones[botonID].setTextColor(Color.parseColor(color));
                        botones[botonID].setTypeface(botones[botonID].getTypeface(), Typeface.BOLD);
                        botones[botonID].setEnabled(false);
                        tablero[botonID] = -1;
                        totalTurnos++;
                        int[] r = calificarTablero();
                        notificarGanador(r);
                    }
                });

            }
        }
    };
    // El servidor nos avisa que el contricante decidió reiniciar el juego
    private Emitter.Listener onReiniciar = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // Ejecutamos en hilo principal
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // También reiniciamos el juego
                    reiniciarJuego();
                }
            });
        }
    };
    // El servidor nos avisa que el contricante ha abandonado el juego
    private Emitter.Listener onJuegoAbandonado = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // Ejecutamos en hilo principal
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Informamos que el contricante ha abandonado el juego
                    txtTitulo.setText(contricante.getNombre() + " ha abandonado el juego ...");
                    txtTurno.setText("");
                }
            });
        }
    };
    // Cambia el turno
    private void cambiarTurno()
    {
        miTurno=!miTurno;
        if (miTurno)
        {
            txtTurno.setText("Tu turno " + miUsuario.getNombre());
        } else {
            txtTurno.setText("Turno: " + contricante.getNombre());
        }
    }
    // Inicializar tablero en ceros
    private void initTablero()
    {
        for (int i=0;i<tablero.length;i++)
        {
            tablero[i]=0;
        }
    }

    /*
     * Checar si hay ganador
     *
     * Regresa en resul[0]:
     *  0 -> empate
     *  1 -> gana el usuario actual
     * -1 -> gana el contricante
     *  2 -> no determinado
     *
     *  Regresa en resul[1]:
     *  El indice donde inicia la raya a pintar.
     */
    private int[] calificarTablero() {
        int[] resul = {2,-1};
        for (int i = 0; i < indicesFCD.length; i++) {
            int sum = 0;
            for (int j = 0; j < tamTablero; j++) {
                sum += tablero[indicesFCD[i] + j * incrIndicesFCD[i]];
            }
            if (Math.abs(sum) == tamTablero) {
                totalTurnos=9;
                resul[0] = sum / tamTablero;
                resul[1] = i;
                return resul;
            }
        }
        if (totalTurnos == tablero.length)
        {
            resul[0] = 0; // empate
            return resul;
        }
        return resul; // no determinado, el juego continua
    }
    // Muestra un texto con el resultado del juego y en caso de haber ganador pone la "raya" en otro color
    private void notificarGanador(int[] r)
    {
        switch (r[0])
        {
            case 1:
                txtTurno.setText("Juego finalizado ... Ganaste!!!" );
                dibujarRaya(r[1]);
                break;
            case -1:
                txtTurno.setText("Juego finalizado ... Perdiste!!!");
                dibujarRaya(r[1]);
                break;
            case 0:
                txtTurno.setText("Juego finalizado ... Empate!!!");
                break;
            default:
                cambiarTurno();
                break;
        }
    }
    // Pone la fila, columna o diagonal de botones correspondienten el color dado por "colorRaya"
    private void dibujarRaya(int i)
    {
        for (int j = 0; j < tamTablero; j++) {
            botones[indicesFCD[i] + j * incrIndicesFCD[i]].setTextColor(Color.parseColor(colorRaya));
        }
    }
    // Resetea el tablero y todas las variables relacionadas
    private void reiniciarJuego()
    {
        totalTurnos = 0;
        initTablero();
        botones[0].setText("");
        botones[1].setText("");
        botones[2].setText("");
        botones[3].setText("");
        botones[4].setText("");
        botones[5].setText("");
        botones[6].setText("");
        botones[7].setText("");
        botones[8].setText("");

        botones[0].setEnabled(true);
        botones[1].setEnabled(true);
        botones[2].setEnabled(true);
        botones[3].setEnabled(true);
        botones[4].setEnabled(true);
        botones[5].setEnabled(true);
        botones[6].setEnabled(true);
        botones[7].setEnabled(true);
        botones[8].setEnabled(true);

        cambiarTurno();
    }
}