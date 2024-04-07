package edu.put.inf153931

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class UsernameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_username)
        findViewById<Button>(R.id.button_saveAccount).setOnClickListener {
            saveAccount()
        }
    }

    fun saveAccount() {
        val dbHandler = MyDBHandler(this, null, null, 1)
        val username = findViewById<EditText>(R.id.editText_username).text.toString()
        if (username != "") {
            dbHandler.addAccount(username)
            Toast.makeText(this, "Zapisano konto", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Nie podano nazwy użytkownika", Toast.LENGTH_SHORT).show()
        }
    }
}