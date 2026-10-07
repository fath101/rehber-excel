package com.example.rehberexcel

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var button: Button
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }
        button = Button(this).apply {
            text = "Rehberi Excel'e aktar"
            setOnClickListener { onExportClicked() }
        }
        status = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 16f
            setPadding(0, pad, 0, 0)
        }
        root.addView(button)
        root.addView(status)
        setContentView(root)
    }

    private fun onExportClicked() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            export()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQ_CONTACTS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CONTACTS && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            export()
        } else {
            status.text = "Rehber izni verilmedi."
        }
    }

    private fun export() {
        button.isEnabled = false
        status.text = "Aktarılıyor…"
        Thread {
            val message = try {
                val contacts = readContacts()
                if (contacts.isEmpty()) {
                    "Telefon numarası olan kişi bulunamadı."
                } else {
                    val fileName = saveToDownloads(contacts)
                    "${contacts.size} kayıt aktarıldı.\n\nDosya: İndirilenler/$fileName"
                }
            } catch (e: Exception) {
                "Hata: ${e.message}"
            }
            runOnUiThread {
                status.text = message
                button.isEnabled = true
            }
        }.start()
    }

    /** (Ad Soyad, Telefon) çiftleri; aynı kişi + aynı numara tekrarları atılır. */
    private fun readContacts(): List<List<String>> {
        val result = LinkedHashSet<Pair<String, String>>()
        contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER),
            null, null,
            "${Phone.DISPLAY_NAME} COLLATE LOCALIZED ASC"
        )?.use { c ->
            val nameIdx = c.getColumnIndexOrThrow(Phone.DISPLAY_NAME)
            val numIdx = c.getColumnIndexOrThrow(Phone.NUMBER)
            while (c.moveToNext()) {
                val name = c.getString(nameIdx)?.trim().orEmpty()
                val number = c.getString(numIdx)?.trim().orEmpty()
                if (number.isNotEmpty()) result.add(name to number)
            }
        }
        return result.map { listOf(it.first, it.second) }
    }

    private fun saveToDownloads(rows: List<List<String>>): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "rehber_$stamp.xlsx"

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, MIME_XLSX)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Dosya oluşturulamadı")

        contentResolver.openOutputStream(uri)?.use { out ->
            XlsxWriter.write(out, listOf("Ad Soyad", "Telefon"), rows)
        } ?: throw IllegalStateException("Dosya yazılamadı")

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        contentResolver.update(uri, values, null, null)
        return fileName
    }

    companion object {
        private const val REQ_CONTACTS = 1
        private const val MIME_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }
}
