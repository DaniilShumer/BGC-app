package edu.put.inf153931

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    val dbHandler = MyDBHandler(this, null, null, 1)
    val imagesDir = "images"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("MainActivity", "onCreate")
        dbHandler.deleteAccountData()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.syncView).setOnClickListener {
            startActivity(Intent(this, SyncActivity::class.java))
        }

        findViewById<Button>(R.id.eraseData).setOnClickListener {
            finishAffinity()
        }

        findViewById<Button>(R.id.gameList).setOnClickListener {
            val intent = Intent(this, GamesActivity::class.java)
            intent.putExtra("expansion", false)
            startActivity(intent)
        }

        findViewById<Button>(R.id.expansionsList).setOnClickListener {
            val intent = Intent(this, GamesActivity::class.java)
            intent.putExtra("expansion", true)
            startActivity(intent)
        }

        val imagesDir = File(applicationContext.filesDir, imagesDir)
        if (!imagesDir.exists()) {
            imagesDir.mkdir()
        } else{
            val images = imagesDir.listFiles()
            for (image in images) {
                image.delete()
            }
        }
    }

    override fun onResume() {
        Log.i("MainActivity", "onResume")
        super.onResume()

        if (!dbHandler.isAccountAdded()) {
            Log.i("MainActivity", "isAccountAdded: false")
            startActivity(Intent(this, UsernameActivity::class.java))
        } else if (dbHandler.getLastSync() == null) {
            Log.i("MainActivity", "getLastSync: null")
            startActivity(Intent(this, SyncActivity::class.java))
        } else {
            findViewById<TextView>(R.id.username).text = dbHandler.getUsername()
            findViewById<TextView>(R.id.numberOfGames).text =
                dbHandler.getNumberOfGames().toString()
            findViewById<TextView>(R.id.numberOfExpansions).text =
                dbHandler.getNumberOfExpansions().toString()
            findViewById<TextView>(R.id.lastSync).text =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(dbHandler.getLastSync())
        }
    }
}

class MyDBHandler(
    context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int
) : SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {
    companion object {
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "bgc.db"

        val TABLE_ACCOUNT = "account"
        val COLLECTION_TABLE = "collection"
        val PHOTOS_TABLE = "photos"

        val COLUMN_ID = "_id"

        val COLUMN_USERNAME = "username"
        val COLUMN_LAST_SYNC = "lastsync"
        val COLUMN_LIST_MODIFIED_SINCE_LAST_SYNC = "list_modified_since_last_sync"

        val COLUMN_TITLE = "title"
        val COLUMN_YEAR = "year"
        val COLUMN_THUMBNAIL_URL = "thumbnail_url"
        val COLUMN_GAMEID = "game_id"
        val COLUMN_EXPANSION = "expansion"

        val GAME_ID = "game_id"
        val PHOTO = "photo"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_ACCOUNT_TABLE =
            ("CREATE TABLE $TABLE_ACCOUNT($COLUMN_ID INTEGER PRIMARY KEY,$COLUMN_USERNAME TEXT,$COLUMN_LAST_SYNC DATETIME, $COLUMN_LIST_MODIFIED_SINCE_LAST_SYNC INTEGER)")
        db?.execSQL(CREATE_ACCOUNT_TABLE)

        val CREATE_COLLECTION_TABLE =
            ("CREATE TABLE $COLLECTION_TABLE($COLUMN_ID INTEGER PRIMARY KEY,$COLUMN_TITLE TEXT,$COLUMN_YEAR INTEGER, $COLUMN_THUMBNAIL_URL TEXT, $COLUMN_GAMEID INTEGER UNIQUE, $COLUMN_EXPANSION INTEGER)")
        db?.execSQL(CREATE_COLLECTION_TABLE)

        val CREATE_PHOTOS_TABLE =
            ("CREATE TABLE $PHOTOS_TABLE($COLUMN_ID INTEGER PRIMARY KEY,$GAME_ID INTEGER,$PHOTO TEXT)")
        db?.execSQL(CREATE_PHOTOS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_ACCOUNT")
        onCreate(db)
    }

    fun isAccountAdded(): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ACCOUNT", null)
        if (!cursor.moveToFirst()) return false
        if (cursor.count <= 0) {
            cursor.close()
            return false
        }
        cursor.close()
        return true
    }


    fun addAccount(username: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_USERNAME, username)
        values.put(COLUMN_LIST_MODIFIED_SINCE_LAST_SYNC, 0)
        db.insert(TABLE_ACCOUNT, null, values)
        db.close()
    }

    fun getUsername(): String? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ACCOUNT", null)
        if (!cursor.moveToFirst()) return null
        val username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
        cursor.close()
        return username
    }

    fun updateLastSync() {
        val db = this.writableDatabase
        val values = ContentValues()
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val dateString = dateFormat.format(currentDate)
        values.put(COLUMN_LAST_SYNC, dateString)
        db.update(TABLE_ACCOUNT, values, null, null)
        db.close()
    }

    fun getLastSync(): Date? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ACCOUNT", null)
        if (!cursor.moveToFirst()) return null
        val lastSync = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_SYNC))
        var date: Date? = null
        Log.i("MyDBHandler", "lastSync: $lastSync")
        if (lastSync != null) {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            date = format.parse(lastSync)
        }
        cursor.close()
        return date
    }

    fun getModifiedSinceLastSync(): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ACCOUNT", null)
        if (!cursor.moveToFirst()) return false
        val modified =
            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIST_MODIFIED_SINCE_LAST_SYNC))
        cursor.close()
        return modified != 0
    }

    fun setModifiedSinceLastSync(bool: Boolean) {
        val db = this.writableDatabase
        val values = ContentValues()
        val value = if (bool) 1 else 0
        values.put(COLUMN_LIST_MODIFIED_SINCE_LAST_SYNC, value)
        db.update(TABLE_ACCOUNT, values, null, null)
        db.close()
    }

    fun deleteAccountData() {
        val db = this.writableDatabase
        db.delete(TABLE_ACCOUNT, null, null)
        db.close()
    }

    fun deleteCollectionData() {
        val db = this.writableDatabase
        db.delete("collection", null, null)
        db.close()
    }

    fun saveGame(id: Long?, title: String?, year: Int?, thumbnailUrl: String?) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_GAMEID, id)
        values.put(COLUMN_TITLE, title)
        values.put(COLUMN_YEAR, year)
        values.put(COLUMN_THUMBNAIL_URL, thumbnailUrl)
        values.put(COLUMN_EXPANSION, 0)
        db.insert(COLLECTION_TABLE, null, values)
        db.close()
    }

    fun makeExpansion(gameID: Long?) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_EXPANSION, 1)
        db.update(COLLECTION_TABLE, values, "$COLUMN_GAMEID = ?", arrayOf(gameID.toString()))
        db.close()
    }

    fun getNumberOfGames(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $COLLECTION_TABLE WHERE expansion=0", null)
        if (!cursor.moveToFirst()) return 0
        val count = cursor.count
        cursor.close()
        return count
    }

    fun getNumberOfExpansions(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $COLLECTION_TABLE WHERE expansion=1", null)
        if (!cursor.moveToFirst()) return 0
        val count = cursor.count
        cursor.close()
        return count
    }

    fun getGames(expansion: Boolean = false, orderByTitle: Boolean = true): ArrayList<Game> {
        val db = this.readableDatabase
        val cursor = if (orderByTitle)
            db.rawQuery(
                "SELECT * FROM $COLLECTION_TABLE WHERE expansion=? ORDER BY $COLUMN_TITLE",
                arrayOf(if (expansion) "1" else "0"),
                null
            )
        else
            db.rawQuery(
                "SELECT * FROM $COLLECTION_TABLE WHERE expansion=? ORDER BY $COLUMN_YEAR",
                arrayOf(if (expansion) "1" else "0"),
                null
            )
        val games = ArrayList<Game>()
        if (!cursor.moveToFirst()) return games
        do {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GAMEID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
            val year = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_YEAR))
            val thumbnailUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_THUMBNAIL_URL))
            val game = Game(id, title, year, thumbnailUrl)
            games.add(game)
        } while (cursor.moveToNext())
        cursor.close()
        return games
    }

    fun getGame(game_id: Long): Game? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $COLLECTION_TABLE WHERE $COLUMN_GAMEID = ?",
            arrayOf(game_id.toString()),
            null
        )
        if (!cursor.moveToFirst()) return null
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GAMEID))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
        val year = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_YEAR))
        val thumbnailUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_THUMBNAIL_URL))
        val game = Game(id, title, year, thumbnailUrl)
        cursor.close()
        return game
    }
}