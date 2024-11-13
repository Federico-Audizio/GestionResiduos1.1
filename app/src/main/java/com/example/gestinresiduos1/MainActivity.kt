package com.example.gestinresiduos1

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Calendar
import java.util.UUID
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var barrioSpinner: Spinner
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: DatabaseReference
    private val NOTIFICATION_PERMISSION_CODE = 1001

    private val servicio1 = mapOf(
        "lunes" to listOf("Bº Roca", "Bº Cotolengo"),
        "martes" to listOf("Bº Consolata", "Bº Chalet", "Bº 2 Hermanos", "Bº Roque Saenz Peña", "Bº Ciudad", "Bº 9 de Septiembre", "Bº Independencia"),
        "miércoles" to listOf("Bº La Florida", "Bº Hospital", "Bº San Francisco", "Bº PARQUE", "Bº San Cayetano", "Bº Corradi"),
        "jueves" to listOf("Bº Roca", "Bº Cotolengo"),
        "viernes" to listOf("Bº Consolata", "Bº Chalet", "Bº 2 Hermanos", "Bº Roque Saenz Peña", "Bº Ciudad", "Bº 9 de Septiembre", "Bº Independencia"),
        "sábado" to listOf("Bº La Florida", "Bº Hospital", "Bº San Francisco", "Bº PARQUE", "Bº San Cayetano", "Bº Corradi")
    )

    private val servicio2 = mapOf(
        "lunes" to listOf("Bº Catedral", "Bº Sarmiento"),
        "martes" to listOf("Bº Iturraspe", "Bº Vélez Sarsfield", "Bº Hernández", "Bº San Carlos", "Bº 20 de Junio"),
        "miércoles" to listOf("Bº La Milka", "Bº San Martin", "Bº Bouchard", "Bº Jardín"),
        "jueves" to listOf("Bº Catedral", "Bº Sarmiento"),
        "viernes" to listOf("Bº Iturraspe", "Bº Vélez Sarsfield", "Bº Hernández", "Bº San Carlos", "Bº 20 de Junio"),
        "sábado" to listOf("Bº La Milka", "Bº San Martin", "Bº Bouchard", "Bº Jardín")
    )

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
                    super.onCreate(savedInstanceState)
                    setContentView(R.layout.activity_main)

                    auth = FirebaseAuth.getInstance()

                    // Asegúrate de que el usuario esté autenticado
                    val user = auth.currentUser
                    if (user != null) {
                        val userEmail = user.email ?: return

                        barrioSpinner = findViewById(R.id.barrioSpinner)
                        val guardarButton: Button = findViewById(R.id.guardarButton)
                        sharedPreferences = getSharedPreferences("APP_PREFERENCES", Context.MODE_PRIVATE)
                        database = FirebaseDatabase.getInstance().reference

                        // Configurar Spinner de barrios
                        val barrios = servicio1.values.flatten() + servicio2.values.flatten()
                        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, barrios)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        barrioSpinner.adapter = adapter

                        // Crear canal de notificación si es necesario (Android 8+)
                        createNotificationChannel()

                        guardarButton.setOnClickListener {
                            val barrioSeleccionado = barrioSpinner.selectedItem.toString()
                            Log.d("MainActivity", "Botón Guardar presionado. Barrio seleccionado: $barrioSeleccionado")

                            // Guardar en Firestore con el correo como ID
                            val db = FirebaseFirestore.getInstance()
                            val usuarioData = hashMapOf("barrio" to barrioSeleccionado)

                            db.collection("usuarios").document(userEmail)
                                .set(usuarioData) // Usa .set() para sobrescribir el documento si ya existe
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Barrio guardado", Toast.LENGTH_SHORT).show()
                                    Log.d("Firebase", "Barrio guardado con éxito en Firebase para el usuario $userEmail")
                                    enviarNotificacionPrueba(barrioSeleccionado)
                                    programarNotificacion(barrioSeleccionado)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firebase", "Error al guardar en Firebase", e)
                                    Toast.makeText(this, "Error en el guardado", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
                        finish() // Finaliza la actividad si no hay usuario autenticado
                    }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default",
                "Notificaciones de Recolección",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canal para notificaciones de recolección de basura"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun programarNotificacion(barrio: String) {
        val servicio = when {
            servicio1.any { it.value.contains(barrio) } -> servicio1
            servicio2.any { it.value.contains(barrio) } -> servicio2
            else -> return
        }

        val diasDeRecoleccion = servicio.filter { it.value.contains(barrio) }.keys
        FirebaseMessaging.getInstance().subscribeToTopic(barrio).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                for (dia in diasDeRecoleccion) {
                    val diaDeNotificacion = obtenerDiaDeSemana(dia)
                    if (diaDeNotificacion != null) {
                        programarNotificacionParaElDia(barrio, diaDeNotificacion)
                    }
                }
            } else {
                Toast.makeText(this, "Error en la suscripción a notificaciones", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerDiaDeSemana(dia: String): Int? {
        return when (dia.toLowerCase()) {
            "lunes" -> Calendar.MONDAY
            "martes" -> Calendar.TUESDAY
            "miércoles" -> Calendar.WEDNESDAY
            "jueves" -> Calendar.THURSDAY
            "viernes" -> Calendar.FRIDAY
            "sábado" -> Calendar.SATURDAY
            else -> null
        }
    }

    private fun programarNotificacionParaElDia(barrio: String, diaDeNotificacion: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, diaDeNotificacion)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("barrio", barrio)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    // Este método se llama después de que el usuario responde a la solicitud de permiso
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, puedes proceder con las notificaciones
                Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show()
            } else {
                // Permiso denegado, muestra un mensaje o maneja el caso
                Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarNotificacionPrueba(barrio: String) {
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("barrio", barrio)
        }
        sendBroadcast(intent)
    }

}




