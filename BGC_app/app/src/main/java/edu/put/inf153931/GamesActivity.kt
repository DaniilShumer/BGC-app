package edu.put.inf153931

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val requestSemaphore = java.util.concurrent.Semaphore(5)


class Game(id: Long, title: String?, year: Int?, thumbnailUrl: String?) {
    val ID: Long = id
    val name: String? = title
    val year: Int? = year
    val thumbnailURL: String? = thumbnailUrl
}

class GameRow(context: Context, game: Game, idx: Int, on: Int?) : TableRow(context) {
    var thumbnail_bmp: android.graphics.Bitmap? = null

    init {
        addView(TextView(context).apply {
            if (game.ID == -1L) {
                text = "lp."
            } else {
                text = on.toString()
            }
            layoutParams = LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            width = 200
        })
        addView(TextView(context).apply {
            if (game.ID == -1L) {
                text = "Tytuł"
            } else {
                text = game.name
            }
            layoutParams = LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            width = 500
        })
        addView(TextView(context).apply {
            if (game.ID == -1L) {
                text = "Rok"
            } else {
                text = game.year.toString()
            }
            layoutParams = LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            )
            width = 200
        })
        if (game.ID == -1L) {
            addView(TextView(context).apply {
                if (game.ID == -1L) {
                    text = "Miniaturka"
                } else {
                    text = game.thumbnailURL
                }
                layoutParams = LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                width = 500
            })
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                requestSemaphore.acquire()
                val url = game.thumbnailURL
                try {
                    val str = java.net.URL(url).openStream()
                    thumbnail_bmp = android.graphics.BitmapFactory.decodeStream(str)
                    val bmp =
                        android.graphics.Bitmap.createScaledBitmap(thumbnail_bmp!!, 300, 300, false)
                    requestSemaphore.release()
                    withContext(Dispatchers.Main) {
                        addView(ImageView(context).apply {
                            layoutParams = LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                            setImageBitmap(bmp)
                        })
                    }
                } catch (e: Exception) {
                    Log.e("GameRow", "Error while loading image: $e")
                }
            }
        }


        apply {
            LayoutParams(
                LayoutParams.WRAP_CONTENT
            )
            if (game.ID == -1L) {
                setBackgroundColor(0xFF80572C.toInt())
            } else {
                setBackgroundColor(if (idx % 2 == 0) 0xFF534E4E.toInt() else 0xFF644C32.toInt())
            }
            setPadding(20, 40, 20, 40)
            setOnClickListener {
                val intent = Intent(context, OneGameActivity::class.java)
                intent.putExtra("game_id", game.ID)
                intent.putExtra("thumbnail_bmp", thumbnail_bmp)
                context.startActivity(intent)
            }
        }
    }
}

class GamesActivity : AppCompatActivity() {
    lateinit var dbHandler: MyDBHandler
    var expantion: Boolean = false
    var sort_by_title: Boolean = true
    var gameList: ArrayList<Game> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        for (i in 1..5)
            requestSemaphore.release()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)
        dbHandler = MyDBHandler(this, null, null, 1)
        expantion = intent.getBooleanExtra("expansion", false)
        sort_by_title = intent.getBooleanExtra("sort_by_title", true)

        gameList = dbHandler.getGames(expansion = expantion, orderByTitle = sort_by_title)

        val tableLayout = TableLayout(this)

        tableLayout.apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT
            )
        }
        tableLayout.addView(GameRow(this, Game(-1, null, null, null), -1, null))
        var on = 1
        for (game in gameList) {
            tableLayout.addView(GameRow(this, game, gameList.indexOf(game), on))
            on++
        }

        findViewById<ScrollView>(R.id.scroll_view).addView(tableLayout)

        findViewById<Button>(R.id.sort_by_title).setOnClickListener {
            val intent = Intent(this, GamesActivity::class.java)
            intent.putExtra("expansion", expantion)
            intent.putExtra("sort_by_title", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        findViewById<Button>(R.id.sort_by_year).setOnClickListener {
            val intent = Intent(this, GamesActivity::class.java)
            intent.putExtra("expansion", expantion)
            intent.putExtra("sort_by_title", false)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        findViewById<Button>(R.id.main_screen).setOnClickListener {
            finish()
        }
    }

}