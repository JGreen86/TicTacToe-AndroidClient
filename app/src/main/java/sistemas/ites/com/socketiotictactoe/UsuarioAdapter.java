package sistemas.ites.com.socketiotictactoe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Jacob Green on 29/05/18.
 */

public class UsuarioAdapter extends ArrayAdapter {

    private Context mContext;
    private ArrayList<Usuario> items;

    public UsuarioAdapter(Context context, int resource) {
        super(context, resource);
    }

    public UsuarioAdapter(Context context, int resource, ArrayList<Usuario> items) {
        super(context, resource, items);
        this.mContext = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.row_user, null);
        }

        Usuario p = (Usuario) getItem(position);
        if(p != null){
            TextView txtName = (TextView) v.findViewById(R.id.txtName);
            //TextView txtMac = (TextView) v.findViewById(R.id.txtMac);

            txtName.setText(p.getNombre());

            //txtMac.setText(p.deviceAddress);
        }

        return v;
    }

    @Override
    public Usuario getItem(int position){
        return items.get(position);
    }

}