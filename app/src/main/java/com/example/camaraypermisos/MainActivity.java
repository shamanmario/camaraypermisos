package com.example.camaraypermisos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.CAMERA;

public class MainActivity extends AppCompatActivity {

    ImageView fotito;
    Button boton;

    private final String CARPETA_RAIZ = "misfotoscapturadas/";
    private final String RUTA_IMAGEN = CARPETA_RAIZ+"misfotos";
    private String path;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fotito = (ImageView) findViewById(R.id.fotito);
        boton = (Button) findViewById(R.id.boton);

        if(validaPermisos()){
            boton.setEnabled(true);
        }else{
            boton.setEnabled(false);
        }

    }

    private boolean validaPermisos() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;

        if((checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED)&&(checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED))
            return true;

        if((shouldShowRequestPermissionRationale(CAMERA))||(shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)))
            cargarDialogoRecomendacion();
        else
            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, CAMERA}, 100);
        return false;
    }

    private void cargarDialogoRecomendacion() {
        AlertDialog.Builder dialogo = new AlertDialog.Builder(MainActivity.this);
        dialogo.setTitle("Permisos desactivados");
        dialogo.setMessage("Debe aceptar los permisos para que la APP funcione correctamente");

        dialogo.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, CAMERA}, 100);
            }
        });
        dialogo.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode ==  100){
            if(grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                boton.setEnabled(true);
            } else {
                solicitarPermisosManual();
            }
        }
    }

    private void solicitarPermisosManual() {
        final CharSequence[] opciones = {"Si", "No"};
        final AlertDialog.Builder alerta = new AlertDialog.Builder(MainActivity.this);
        alerta.setTitle("¿Desea configurar permisos?");
        alerta.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which){
                    case 0:
                        Intent pedirPermiso = new Intent();
                        pedirPermiso.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        pedirPermiso.setData(uri);
                        startActivity(pedirPermiso);
                        break;
                    default:
                        Toast.makeText(getApplication(), "Permisos no otorgados", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                }
            }
        });

        alerta.show();

    }


    public void cargarFoto(View view){
        cargarImagen();
    }

    private void cargarImagen() {
        final CharSequence[] opciones = {"Tomar Foto con la cámara", "Sacar foto de la galería", "Cancelar"};
        final AlertDialog.Builder alertaOpciones = new AlertDialog.Builder(MainActivity.this);

        alertaOpciones.setTitle("Seleccione una opción:");
        alertaOpciones.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which){
                    case 0: //seleccionando tomar foto con la cámara
                        //Toast.makeText(getApplication(), "Tomando foto con la cámara", Toast.LENGTH_SHORT).show();
                        tomarFoto();
                        break;
                    case 1: //sacar foto de galería
                        Intent intentGaleria = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intentGaleria.setType("image/");
                        startActivityForResult(intentGaleria.createChooser(intentGaleria, "Seleccione una aplicación"), 10);
                        break;
                    default://cancelar
                        dialog.dismiss();

                }
            }
        });

        alertaOpciones.show();
    }

    private void tomarFoto() {
        String nombre_foto = "";
        File archivoImagen = new File(Environment.getExternalStorageDirectory(), RUTA_IMAGEN);
        boolean estaCreado = archivoImagen.exists();

        if(estaCreado){
            nombre_foto = (System.currentTimeMillis())/1000 + ".jpg";
        } else {
            estaCreado = archivoImagen.mkdirs();
        }

        this.path = Environment.getExternalStorageDirectory()+File.separator+RUTA_IMAGEN+File.separator+nombre_foto;

        File nuevaFoto = new File(this.path);

        Intent intentoCamara = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intentoCamara.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(nuevaFoto));

        startActivityForResult(intentoCamara, 20);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK){
            switch(requestCode){
                case 10: //si seleccionamos desde galería
                    Uri path = data.getData();
                    fotito.setImageURI(path);
                    break;

                case 20: //si tomamos la foto
                    MediaScannerConnection.scanFile(this, new String[]{this.path}, null, new OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ruta", "path: "+path);
                        }
                    });
                    //Toast.makeText(getApplication(), "recuperando foto de la cámara", Toast.LENGTH_SHORT).show();

                    Bitmap bitmap = BitmapFactory.decodeFile(this.path);
                    fotito.setImageBitmap(bitmap);
                    break;
            }
        }
    }
}