package edu.put.inf153931

import android.annotation.SuppressLint
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import java.util.Date

class SyncActivity : AppCompatActivity() {
    lateinit var dbHandler: MyDBHandler
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync)
        dbHandler = MyDBHandler(this, null, null, 1)
        updateLastSyncText()

        findViewById<Button>(R.id.sync_button).setOnClickListener {
            findViewById<TextView>(R.id.progress_text).visibility = TextView.VISIBLE
            findViewById<ProgressBar>(R.id.progress_bar).visibility = ProgressBar.VISIBLE
            sync()
        }

        findViewById<Button>(R.id.sync_button_collection).setOnClickListener {
            findViewById<TextView>(R.id.progress_text).visibility = TextView.VISIBLE
            findViewById<ProgressBar>(R.id.progress_bar).visibility = ProgressBar.VISIBLE
            sync(1)
        }

        findViewById<Button>(R.id.mainScreen_button).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.eraseData2).setOnClickListener {
            dbHandler.deleteAccountData()
            finishAffinity()
        }
    }

    private fun sync(extension: Int = 0) {
        fun downloadFile() {
            var urlString: String
            if (extension == 0) urlString =
                "https://boardgamegeek.com/xmlapi2/collection?username=${dbHandler.getUsername()}"
            else {
                urlString =
                    "https://boardgamegeek.com/xmlapi2/collection?username=${dbHandler.getUsername()}&subtype=boardgameexpansion"
            }
            Log.i("downloadFile", urlString)
            val xmlDirectory = File(filesDir, "xml")
            if (!xmlDirectory.exists()) {
                xmlDirectory.mkdir()
            }
            if (xmlDirectory.listFiles() != null) {
                for (file in xmlDirectory.listFiles()) {
                    file.delete()
                }
            }
            val xmlFile = File(xmlDirectory, "collection.xml")
            Log.i("downloadFile", xmlFile.toString())

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL(urlString)
                    val connection = url.openConnection()
                    connection.connect()
                    val input = BufferedInputStream(url.openStream())
                    val output = FileOutputStream(xmlFile)
                    val data = ByteArray(1024)
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        output.write(data, 0, count)
                    }
                    output.flush()
                    output.close()
                    input.close()

                    if (xmlFile.readText().contains("Please try again later for access.")) {
                        throw Exception("Spróbuj ponownie później.")
                    }
                    if (xmlFile.readText().contains("Invalid username specified")) {
                        throw Exception("Login użytkownika nie istnieje w BGG.")
                    }
                    if (xmlFile.readText().contains("<error>")) {
                        throw Exception("Wystąpił nieznany błąd.")
                    }

                    withContext(Dispatchers.Main) {
                        dbHandler.updateLastSync()
                        updateLastSyncText()
                        dbHandler.setModifiedSinceLastSync(true)


                        val xmlDirectory = File(filesDir, "xml")
                        val file = File(xmlDirectory, "collection.xml")
                        if (file.exists()) {
                            if (extension == 0) {
                                dbHandler.deleteCollectionData()
                            }

                            val factory = XmlPullParserFactory.newInstance()
                            factory.isNamespaceAware = true

                            val parser = factory.newPullParser()
                            parser.setInput(file.inputStream(), null)

                            var objectID: Long? = null
                            var name: String? = null
                            var yearPublished: Int? = null
                            var thumbnail: String? = null
                            var subtype: String? = null

                            var eventType = parser.eventType
                            while (eventType != XmlPullParser.END_DOCUMENT) {
                                when (eventType) {
                                    XmlPullParser.START_TAG -> {
                                        val tagName = parser.name
                                        when (tagName) {
                                            "item" -> {
                                                objectID =
                                                    parser.getAttributeValue(null, "objectid")
                                                        .toLong()
                                                subtype = parser.getAttributeValue(null, "subtype")
                                            }

                                            "name" -> {
                                                name = parser.nextText()
                                            }

                                            "yearpublished" -> {
                                                yearPublished = parser.nextText().toInt()
                                            }

                                            "thumbnail" -> {
                                                thumbnail = parser.nextText()
                                            }
                                        }
                                    }

                                    XmlPullParser.END_TAG -> {
                                        val tagName = parser.name
                                        when (tagName) {
                                            "item" -> {
                                                if (extension == 0 && objectID != null) dbHandler.saveGame(
                                                    objectID, name, yearPublished, thumbnail
                                                )
                                                else if (extension == 1 && objectID != null) dbHandler.makeExpansion(
                                                    objectID
                                                )
                                                objectID = null
                                                name = null
                                                yearPublished = null
                                                thumbnail = null
                                            }
                                        }
                                    }
                                }
                                eventType = parser.next()
                            }

                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SyncActivity, "$e", Toast.LENGTH_SHORT
                        ).show()
                    }
                    findViewById<TextView>(R.id.progress_text).visibility = TextView.INVISIBLE
                    findViewById<ProgressBar>(R.id.progress_bar).visibility = ProgressBar.INVISIBLE
                    Log.e("downloadFile", e.toString())
                    val incompleteFile = File(xmlDirectory, "collection.xml")
                    if (incompleteFile.exists()) {
                        incompleteFile.delete()
                    }
                }
            }
        }

        fun properSync() {
            downloadFile()

        }

        val lastSync = dbHandler.getLastSync()
        if (lastSync != null) {
            if (dbHandler.getModifiedSinceLastSync() || extension == 1) {
                if (ChronoUnit.DAYS.between(lastSync.toInstant(), Date().toInstant()) >= 1) {
                    properSync()
                } else {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Potwierdzenie synchronizacji")
                    if (extension == 0) builder.setMessage("Synchronizację wykonano mniej niż 24h temu. Czy na pewno chcesz ją wykonać?")
                    else builder.setMessage("Upewnij się, że wcześniej wykonałeś standardową synchronizację.")

                    builder.setPositiveButton("OK") { dialog, _ ->
                        properSync()
                        dialog.dismiss()
                    }

                    builder.setNegativeButton("ANULUJ") { dialog, _ ->
                        Toast.makeText(this, "Anulowano synchronizację", Toast.LENGTH_SHORT).show()
                        findViewById<TextView>(R.id.progress_text).visibility = TextView.INVISIBLE
                        findViewById<ProgressBar>(R.id.progress_bar).visibility =
                            ProgressBar.INVISIBLE
                        dialog.dismiss()
                    }

                    val alert = builder.create()
                    alert.show()
                }
            } else {
                Toast.makeText(this, "Brak zmian do synchronizacji", Toast.LENGTH_SHORT).show()
                findViewById<TextView>(R.id.progress_text).visibility = TextView.INVISIBLE
                findViewById<ProgressBar>(R.id.progress_bar).visibility = ProgressBar.INVISIBLE
            }
        } else {
            properSync()
        }
    }

    private fun updateLastSyncText() {
        val lastSync = dbHandler.getLastSync()
        if (lastSync != null) {
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
            findViewById<TextView>(R.id.last_sync).text = formatter.format(lastSync)
        } else {
            findViewById<TextView>(R.id.last_sync).text = "Brak (synchronizacja wymagana)"
        }
        findViewById<TextView>(R.id.progress_text).visibility = TextView.INVISIBLE
        findViewById<ProgressBar>(R.id.progress_bar).visibility = ProgressBar.INVISIBLE
    }
}