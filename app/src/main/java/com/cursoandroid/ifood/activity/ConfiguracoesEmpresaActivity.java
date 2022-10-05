package com.cursoandroid.ifood.activity;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.cursoandroid.ifood.R;
import com.cursoandroid.ifood.helper.ConfiguracaoFirebase;
import com.cursoandroid.ifood.helper.UsuarioFirebase;
import com.cursoandroid.ifood.model.Empresa;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;

public class ConfiguracoesEmpresaActivity extends AppCompatActivity {

    private EditText editEmpresaNome, editEmpresaCategoria, editEmpresaTempo, editEmpresaTaxa;
    private CircleImageView imagePerfilEmpresa;
    private StorageReference storageReference;
    private String idUsuarioLogado;
    private String urlImagemSelecionada = "";
    private DatabaseReference firebaseRef;
    private AlertDialog dialog;

    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes_empresa);

        //Configuração da toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Configurações");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        inicializarComponentes();

        //Configurações iniciais
        storageReference = ConfiguracaoFirebase.getFirebaseStorage();
        idUsuarioLogado = UsuarioFirebase.getIdUsuario();
        firebaseRef = ConfiguracaoFirebase.getFirebase();

        registerForActivityResult();

        recuperarDadosEmpresa();

        imagePerfilEmpresa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (i.resolveActivity(getPackageManager()) != null){
                    activityResultLauncher.launch(i);
                }
            }
        });
    }

    public void recuperarDadosEmpresa(){

        DatabaseReference empresaRef = firebaseRef
                .child("empresas")
                .child(idUsuarioLogado);

        empresaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null){
                    Empresa empresa = snapshot.getValue(Empresa.class);
                    editEmpresaNome.setText(empresa.getNome());
                    editEmpresaCategoria.setText(empresa.getCategoria());
                    editEmpresaTempo.setText(empresa.getTempo());
                    editEmpresaTaxa.setText(empresa.getPrecoEntrega().toString());

                    urlImagemSelecionada = empresa.getUrlImagem();
                    if (urlImagemSelecionada != ""){
                        Picasso.get()
                                .load(urlImagemSelecionada)
                                .into(imagePerfilEmpresa);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void registerForActivityResult(){
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {

                        dialog = new SpotsDialog.Builder()
                                .setContext(ConfiguracoesEmpresaActivity.this)
                                .setCancelable(false)
                                .setMessage("Carregando imagem")
                                .build();
                        dialog.show();

                        if (result.getResultCode() == RESULT_OK){
                            Bitmap imagem;

                            Uri localImagem = null;
                            if (result.getData() != null) {
                                localImagem = result.getData().getData();
                            }

                            try {
                                imagem = MediaStore.Images.Media.getBitmap(getContentResolver(), localImagem);

                                if (imagem != null){
                                    imagePerfilEmpresa.setImageBitmap(imagem);

                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    imagem.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                                    byte[] dadosImagem = baos.toByteArray();

                                    final StorageReference imagemRef = storageReference
                                            .child("imagens")
                                            .child("empresas")
                                            .child(idUsuarioLogado + "jpeg");

                                    UploadTask uploadTask = imagemRef.putBytes(dadosImagem);
                                    uploadTask.addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            dialog.dismiss();
                                            exibirMensagem("Erro ao fazer upload da imagem!");
                                        }
                                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                            imagemRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Uri> task) {
                                                    urlImagemSelecionada = String.valueOf(task.getResult());

                                                }
                                            });

                                            dialog.dismiss();
                                            exibirMensagem("Sucesso ao fazer upload da imagem!");
                                        }
                                    });
                                }else {
                                    imagePerfilEmpresa.setImageResource(R.drawable.perfil);
                                    dialog.dismiss();
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    public void validarDadosEmpresa(View view){
        //Validar se os campos foram preenchidos
        String nome = editEmpresaNome.getText().toString();
        String categoria = editEmpresaCategoria.getText().toString();
        String tempo = editEmpresaTempo.getText().toString();
        String taxa = editEmpresaTaxa.getText().toString();

        if (!nome.isEmpty()){
            if (!categoria.isEmpty()){
                if (!tempo.isEmpty()){
                    if (!taxa.isEmpty()){

                        Empresa empresa = new Empresa();
                        empresa.setIdUsuario(idUsuarioLogado);
                        empresa.setNome(nome);
                        empresa.setCategoria(categoria);
                        empresa.setTempo(tempo);
                        empresa.setPrecoEntrega(Double.parseDouble(taxa));
                        empresa.setUrlImagem(urlImagemSelecionada);
                        empresa.salvar();

                        exibirMensagem("Dados salvos com sucesso!");
                        finish();

                    }else {
                        exibirMensagem("Preencha a taxa de entrega!");
                    }

                }else {
                    exibirMensagem("Preenca o tempo de entrega!");
                }

            }else {
                exibirMensagem("Preencha a categoria!");
            }

        }else {
            exibirMensagem("Preencha o nome!");
        }
    }

    private void exibirMensagem(String texto){
        Toast.makeText(this,
                texto, Toast.LENGTH_SHORT).show();
    }

    private void inicializarComponentes(){
        editEmpresaNome = findViewById(R.id.editEmpresaNome);
        editEmpresaCategoria = findViewById(R.id.editEmpresaCategoria);
        editEmpresaTaxa = findViewById(R.id.editEmpresaTaxa);
        editEmpresaTempo = findViewById(R.id.editEmpresaTempo);
        imagePerfilEmpresa = findViewById(R.id.imagePerfilEmpresa);
    }
}