package com.juego.juegodemesa;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class GameActivity extends AppCompatActivity {

    private DatabaseReference gameRef;
    private String gameId;
    private String playerRole;

    private TextView player1TapsTextView;
    private TextView player2TapsTextView;
    private TextView timerTextView;
    private Button tapButton;
    private Button restartButton;
    private Button logoutButton;

    private CountDownTimer gameTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Inicializa los elementos de la UI
        player1TapsTextView = findViewById(R.id.player1Taps);
        player2TapsTextView = findViewById(R.id.player2Taps);
        timerTextView = findViewById(R.id.timerTextView);
        tapButton = findViewById(R.id.tapButton);
        restartButton = findViewById(R.id.restartButton);
        logoutButton = findViewById(R.id.logoutButton);

        tapButton.setEnabled(false); // Deshabilitado hasta que ambos jugadores estén listos

        // Obtiene el gameId y playerRole desde el Intent
        gameId = getIntent().getStringExtra("gameId");
        playerRole = getIntent().getStringExtra("playerRole");

        // Inicializa la referencia de Firebase Database usando gameId
        gameRef = FirebaseDatabase.getInstance().getReference("games").child(gameId);

        // Monitorea el estado de preparación de ambos jugadores
        gameRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean player1Ready = snapshot.child("player1").child("isReady").getValue(Boolean.class);
                Boolean player2Ready = snapshot.child("player2").child("isReady").getValue(Boolean.class);

                if (Boolean.TRUE.equals(player1Ready) && Boolean.TRUE.equals(player2Ready)) {
                    // Ambos jugadores están listos, cambia el estado del juego a "ready"
                    gameRef.child("gameStatus").setValue("ready");
                    gameRef.child("player1").child("isReady").setValue(false); // Restablece el estado de preparación
                    gameRef.child("player2").child("isReady").setValue(false); // Restablece el estado de preparación
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GameActivity", "Error al leer el estado de preparación", error.toException());
            }
        });

        // Monitorea el estado del juego y comienza cuando ambos jugadores estén listos
        gameRef.child("gameStatus").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String gameStatus = snapshot.getValue(String.class);
                if ("ready".equals(gameStatus)) {
                    // Ambos jugadores están en la sala y listos, comienza el juego
                    tapButton.setEnabled(true);
                    startGameTimer();
                } else {
                    // Si el juego está en estado "waiting", desactiva el botón y reinicia el temporizador
                    tapButton.setEnabled(false);
                    if (gameTimer != null) {
                        gameTimer.cancel();
                        timerTextView.setText("Esperando a ambos jugadores...");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GameActivity", "Error al leer el estado del juego", error.toException());
            }
        });

        // Configura el listener de tapButton para incrementar los taps
        tapButton.setOnClickListener(v -> incrementTaps());

        // Listener para el botón de reiniciar partida
        restartButton.setOnClickListener(v -> restartGame());

        // Listener para el botón de cerrar sesión
        logoutButton.setOnClickListener(v -> logout());

        // Listener para cambios en el juego en tiempo real
        gameRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer player1Taps = snapshot.child("player1").child("taps").getValue(Integer.class);
                Integer player2Taps = snapshot.child("player2").child("taps").getValue(Integer.class);

                if (player1Taps != null) {
                    player1TapsTextView.setText("Jugador 1: " + player1Taps + " Taps");
                }
                if (player2Taps != null) {
                    player2TapsTextView.setText("Jugador 2: " + player2Taps + " Taps");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GameActivity", "Error en Firebase Database", error.toException());
            }
        });
    }

    private void startGameTimer() {
        gameTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTextView.setText("Tiempo restante: " + millisUntilFinished / 1000 + " s");
            }

            @Override
            public void onFinish() {
                timerTextView.setText("Tiempo terminado!");
                tapButton.setEnabled(false);
                determineWinner();
            }
        }.start();
    }

    private void incrementTaps() {
        String playerKey = playerRole.equals("player1") ? "player1" : "player2";

        DatabaseReference playerTapsRef = gameRef.child(playerKey).child("taps");

        playerTapsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer currentTaps = snapshot.getValue(Integer.class);
                if (currentTaps == null) {
                    currentTaps = 0;
                }
                playerTapsRef.setValue(currentTaps + 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GameActivity", "Error al incrementar taps", error.toException());
            }
        });
    }

    private void restartGame() {
        // Reinicia el contador de taps y el estado de preparación del jugador actual
        gameRef.child("player1").child("taps").setValue(0);
        gameRef.child("player2").child("taps").setValue(0);
        gameRef.child(playerRole).child("isReady").setValue(true); // Marca al jugador actual como listo
        gameRef.child("gameStatus").setValue("waiting");

        // Detiene el temporizador si está corriendo
        if (gameTimer != null) {
            gameTimer.cancel();
            timerTextView.setText("Esperando a ambos jugadores...");
        }
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(GameActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void determineWinner() {
        gameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer player1Taps = snapshot.child("player1").child("taps").getValue(Integer.class);
                Integer player2Taps = snapshot.child("player2").child("taps").getValue(Integer.class);

                String result;
                if (player1Taps != null && player2Taps != null) {
                    if (player1Taps > player2Taps) {
                        result = "Jugador 1 gana!";
                    } else if (player1Taps < player2Taps) {
                        result = "Jugador 2 gana!";
                    } else {
                        result = "¡Es un empate!";
                    }
                } else {
                    result = "No se pudieron obtener los resultados.";
                }

                Toast.makeText(GameActivity.this, result, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GameActivity", "Error al determinar el ganador", error.toException());
            }
        });
    }
}
