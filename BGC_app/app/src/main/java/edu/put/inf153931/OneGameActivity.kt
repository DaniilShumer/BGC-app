package edu.put.inf153931

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
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
import kotlin.random.Random

class OneGameActivity : AppCompatActivity() {
    var game: Game? = null
    var thumbnail_bmp: android.graphics.Bitmap? = null
    lateinit var dbHandler: MyDBHandler
    lateinit var imageView: ImageView
    var description: String? = null
    val imagesDir = "images"
    var image: File? = null
    lateinit var linearView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_one_game)

        val game_id = intent.getLongExtra("game_id", -1)
        dbHandler = MyDBHandler(this, null, null, 1)
        game = dbHandler.getGame(game_id)
        thumbnail_bmp = intent.getParcelableExtra("thumbnail_bmp")
        linearView = findViewById(R.id.linearView)

        val imDir = File(applicationContext.filesDir, imagesDir)
//        imDir.mkdir()
        for (f in imDir.listFiles()) {
            if (f.name.startsWith(game_id.toString())) addImageToGallery(f)
        }

        findViewById<ImageView>(R.id.thumbnail).setImageBitmap(thumbnail_bmp)
        findViewById<ImageView>(R.id.thumbnail).setOnClickListener {
            val intent = Intent(this, FullActivity::class.java)
            intent.putExtra("thumbnail_bmp", thumbnail_bmp)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.title).text = game?.name + " \n\n(" + game?.year + ")"

        downloadFile()

        val button = findViewById<Button>(R.id.doPhoto)
        val resultLauncher: ActivityResultLauncher<Uri> =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                Log.i("doPhoto", "success: $success")
                Log.i("doPhoto", "imageUri: ${image!!.absolutePath}")
                if (success && image != null) {
                    addImageToGallery(image!!)
                }
            }
        button.setOnClickListener {
            val imageUri = initUri(game_id)
            resultLauncher.launch(imageUri)

        }
    }

    private fun addImageToGallery(f: File) {
        linearView.addView(ImageView(this).apply {
            setImageBitmap(
                android.graphics.Bitmap.createScaledBitmap(
                    BitmapFactory.decodeFile(
                        f.path
                    ), 500, 500, false
                )
            )
            setOnClickListener{
                val builder = AlertDialog.Builder(this@OneGameActivity)
                builder.setTitle("Potwierdzenie usunięcia")
                builder.setMessage("Czy na pewno chcesz usunąć zdjecia.")

                builder.setPositiveButton("OK") { dialog, _ ->
                    f.delete()
                    dialog.dismiss()
                    recreate()
                }

                builder.setNegativeButton("ANULUJ") { dialog, _ ->
                    dialog.dismiss()
                }

                val alert = builder.create()
                alert.show()
            }
        })
    }


    private fun initUri(gameID: Long): Uri {
        val imagesDir = File(applicationContext.filesDir, imagesDir)
        image = File(imagesDir, "${gameID}_${gameID + Random.nextInt()}.jpg")
        Log.i("initUri", image!!.absolutePath)

        return FileProvider.getUriForFile(
            applicationContext, "com.example.inf153931.provider", image!!
        )
    }

    fun downloadFile() {
        var urlString: String =
            "https://www.boardgamegeek.com/xmlapi2/thing?id=" + game?.ID + "&stats=1"
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
        val xmlFile = File(xmlDirectory, "game.xml")
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
                    throw Exception("Dane na serwerze są przygotowywane. Spróbuj ponownie za kilka sekund.")
                }
                if (xmlFile.readText().contains("Invalid username specified")) {
                    throw Exception("Taki użytkownik nie istnieje w bazie BGC.")
                }
                if (xmlFile.readText().contains("<error>")) {
                    throw Exception("Wystąpił nieznany błąd. Spróbuj ponownie później.")
                }

                withContext(Dispatchers.Main) {
                    val xmlDirectory = File(filesDir, "xml")
                    val file = File(xmlDirectory, "game.xml")
                    if (file.exists()) {
                        // parse XML file
                        val factory = XmlPullParserFactory.newInstance()
                        factory.isNamespaceAware = true
                        val parser = factory.newPullParser()
                        parser.setInput(file.inputStream(), null)
                        var eventType = parser.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    val tagName = parser.name
                                    when (tagName) {
                                        "description" -> {
                                            description = parser.nextText()
                                            Log.i("description", description.toString())
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
                        applicationContext, e.toString(), Toast.LENGTH_LONG
                    ).show()
                }
                val incompleteFile = File(xmlDirectory, "game.xml")
                if (incompleteFile.exists()) {
                    incompleteFile.delete()
                }
            }
            findViewById<TextView>(R.id.description).text = description?.substring(0, 300) + "..."
        }
    }

}