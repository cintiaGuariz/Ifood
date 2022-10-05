package com.cursoandroid.ifood.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.cursoandroid.ifood.R;
import com.cursoandroid.ifood.helper.ConfiguracaoFirebase;
import com.cursoandroid.ifood.helper.UsuarioFirebase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class AutenticacaoActivity extends AppCompatActivity {

    private EditText campoEmail, campoSenha;
    private Switch tipoAcesso, tipoUsuario;
    private LinearLayout linearTipoUsuario;
    private Button buttonAcesso;
    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autenticacao);

        inicializarComponentes();
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        //autenticacao.signOut();

        //Verifica usuário logado
        verificaUsuarioLogado();

        tipoAcesso.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){//empresa
                    linearTipoUsuario.setVisibility(View.VISIBLE);
                }else {//usuario
                    linearTipoUsuario.setVisibility(View.GONE);
                }
            }
        });

        buttonAcesso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = campoEmail.getText().toString();
                String senha = campoSenha.getText().toString();

                if (!email.isEmpty()){
                    if (!senha.isEmpty()){

                        //Verifica o estado do switch
                        if (tipoAcesso.isChecked()){//Cadastro
                            autenticacao.createUserWithEmailAndPassword(
                                    email, senha
                            ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                  if (task.isSuccessful()){

                                      Toast.makeText(AutenticacaoActivity.this,
                                              "Cadastro feito com sucesso!",
                                              Toast.LENGTH_SHORT).show();

                                      String tipoUsuario = getTipoUsuario();
                                      UsuarioFirebase.atualiazarTipoUsuario(tipoUsuario);

                                      abrirTelaPrincipal(tipoUsuario);

                                  }else {
                                      String erroExcecao = "";

                                      try {
                                          throw task.getException();
                                      }catch (FirebaseAuthWeakPasswordException e){
                                          erroExcecao = "Digite uma senha mais forte!";
                                      }catch (FirebaseAuthInvalidCredentialsException e){
                                          erroExcecao = "Digite um email válido!";
                                      }catch (FirebaseAuthUserCollisionException e){
                                          erroExcecao = "Esta conta já foi cadastrada!";
                                      }catch (Exception e){
                                          erroExcecao = "Erro ao cadastrar: " + e.getMessage();
                                          e.printStackTrace();
                                      }

                                      Toast.makeText(AutenticacaoActivity.this,
                                              "Erro: " + erroExcecao,
                                              Toast.LENGTH_SHORT).show();
                                  }
                                }
                            });

                        }else {//Login
                            autenticacao.signInWithEmailAndPassword(
                                    email, senha
                            ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                  if (task.isSuccessful()){

                                      Toast.makeText(AutenticacaoActivity.this,
                                              "Logado com sucesso!",
                                              Toast.LENGTH_SHORT).show();

                                      String tipoUsuario = task.getResult().getUser().getDisplayName();
                                      abrirTelaPrincipal(tipoUsuario);

                                  }else {

                                      Toast.makeText(AutenticacaoActivity.this,
                                              "Erro ao fazer login " + task.getException(),
                                              Toast.LENGTH_LONG).show();
                                  }
                                }
                            });

                        }

                    }else {
                        Toast.makeText(AutenticacaoActivity.this,
                                "Por favor preencha a senha!",
                                Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(AutenticacaoActivity.this,
                            "Por favor preencha o email!",
                            Toast.LENGTH_SHORT).show();
                }

                email = "";
                senha = "";
            }
        });
    }

    private String getTipoUsuario(){
        return tipoUsuario.isChecked() ? "E" : "U";
    }

    private void verificaUsuarioLogado(){
        FirebaseUser usuarioAtual = autenticacao.getCurrentUser();

        if (usuarioAtual != null){
            String tipoUsuario = usuarioAtual.getDisplayName();
            abrirTelaPrincipal(tipoUsuario);
        }
    }

    private void abrirTelaPrincipal(String tipoUsuario){
        if (tipoUsuario.equals("E")){//empresa
            startActivity(new Intent(getApplicationContext(), EmpresaActivity.class));

        }else {//usuario
            startActivity(new Intent(getApplicationContext(), HomeActivity.class));
        }

    }

    private void inicializarComponentes(){
        campoEmail = findViewById(R.id.editCadastroEmail);
        campoSenha = findViewById(R.id.editCadastroSenha);
        tipoAcesso = findViewById(R.id.switchAcesso);
        tipoUsuario = findViewById(R.id.switchTipoUsuario);
        buttonAcesso = findViewById(R.id.buttonAcesso);
        linearTipoUsuario = findViewById(R.id.linearTipoUsuario);

    }
}