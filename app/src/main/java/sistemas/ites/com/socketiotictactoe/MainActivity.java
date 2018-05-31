package sistemas.ites.com.socketiotictactoe;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/*
 * MainActivity.java
 *
 * Solicita al usuario un nombre de usuario y manda a llamar a ListaUsuarios.java
 *
 */
public class MainActivity extends AppCompatActivity {

    private EditText txtUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtUsuario = (EditText) findViewById(R.id.txtUsuario);

        ((Button)findViewById(R.id.btnIniciar)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usuario = txtUsuario.getText().toString();
                Intent intent = new Intent(MainActivity.this, ListaUsuarios.class);
                intent.putExtra("usuario",usuario);
                startActivity(intent);
                finish();
            }
        });
    }
}
