package com.juego.juegodemesa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var gameRef: DatabaseReference
    private var playerRole: String = "player2" // Rol por defecto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        gameRef = FirebaseDatabase.getInstance().getReference("games")

        val startGameButton = findViewById<Button>(R.id.startGameButton)
        startGameButton.setOnClickListener { joinOrCreateGame() }
    }

    private fun joinOrCreateGame() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Por favor, inicia sesión primero", Toast.LENGTH_SHORT).show()
            return
        }

        // Intenta unirse a una sala abierta donde player2 sea null y gameStatus sea "waiting"
        gameRef.orderByChild("player2/uid").equalTo(null).limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Hay una sala disponible, unirse como player2
                        for (gameSnapshot in snapshot.children) {
                            val gameId = gameSnapshot.key
                            if (gameId != null) {
                                playerRole = "player2"
                                gameRef.child(gameId).child("player2").child("uid").setValue(currentUser.uid)
                                gameRef.child(gameId).child("gameStatus").setValue("ready") // Cambia el estado a "ready"

                                // Inicia GameActivity y pasa gameId y playerRole
                                val intent = Intent(this@MainActivity, GameActivity::class.java)
                                intent.putExtra("gameId", gameId)
                                intent.putExtra("playerRole", playerRole)
                                startActivity(intent)
                            }
                            break
                        }
                    } else {
                        // No hay salas disponibles, crea una nueva y conviértete en player1
                        createNewGame()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error al acceder a la base de datos", Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "Error al acceder a Firebase: ${error.message}")
                }
            })
    }

    private fun createNewGame() {
        val currentUser = auth.currentUser ?: return

        val gameId = gameRef.push().key // Genera un ID único para el juego
        if (gameId != null) {
            playerRole = "player1"
            val gameData = hashMapOf(
                "player1" to hashMapOf(
                    "uid" to currentUser.uid,
                    "taps" to 0
                ),
                "player2" to hashMapOf(
                    "uid" to null,
                    "taps" to 0
                ),
                "gameStatus" to "waiting" // Estado inicial del juego
            )
            gameRef.child(gameId).setValue(gameData).addOnSuccessListener {
                val intent = Intent(this@MainActivity, GameActivity::class.java)
                intent.putExtra("gameId", gameId)
                intent.putExtra("playerRole", playerRole)
                startActivity(intent)
            }.addOnFailureListener {
                Toast.makeText(this, "Error al crear el juego", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error al crear el juego: ${it.message}")
            }
        } else {
            Toast.makeText(this, "Error: No se pudo generar el ID del juego.", Toast.LENGTH_SHORT).show()
        }
    }
}
