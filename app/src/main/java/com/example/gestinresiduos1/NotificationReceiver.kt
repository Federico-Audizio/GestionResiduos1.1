package com.example.gestinresiduos1

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("message") ?: "Notificación sin mensaje" // Obtiene el mensaje enviado
        val barrio = intent.getStringExtra("barrio") ?: "Barrio no especificado" // Obtiene el barrio si es necesario

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            // Crea el intent para abrir la actividad al tocar la notificación
            val resultIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Construye la notificación
            val notification = NotificationCompat.Builder(context, "YOUR_CHANNEL_ID")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Notificación de recolección")
                .setContentText("$message en $barrio.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(System.currentTimeMillis().toInt(), notification) // ID único
        }
    }
}


