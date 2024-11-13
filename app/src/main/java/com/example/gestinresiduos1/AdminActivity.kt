package com.example.gestinresiduos1

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button


class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        findViewById<Button>(R.id.buttonDelay).setOnClickListener {
            sendNotification("Retrasos en la recolección")
        }

        findViewById<Button>(R.id.buttonChangeSchedule).setOnClickListener {
            sendNotification("Cambio de horario")
        }

        findViewById<Button>(R.id.buttonSuspendService).setOnClickListener {
            sendNotification("Suspensión del servicio")
        }

        findViewById<Button>(R.id.buttonModifyWaste).setOnClickListener {
            sendNotification("Modificación en el tipo de residuos")
        }
    }

    private fun sendNotification(message: String) {
        // Aquí se puede usar Firebase o NotificationReceiver para enviar la notificación.
        val intent = Intent(this, NotificationReceiver::class.java)
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }
}

