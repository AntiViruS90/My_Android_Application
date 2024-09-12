package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.content.SharedPreferences
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit



class ResetWorker(context: Context, params: WorkerParameters): Worker(context, params) {
    override fun doWork(): Result {
        val sharedPreferences = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putInt("wordLimit", 300)
        editor.apply()

        return Result.success()
    }
}

class MainActivity : AppCompatActivity() {

    private var currentWord = ""
    private var score = 0
    private var wordLimit = 300
    private val wordsPerExtension = 100
    private lateinit var wordsList: List<String>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val resetWorkRequest = PeriodicWorkRequestBuilder<ResetWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueue(resetWorkRequest)

        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        score = sharedPreferences.getInt("score", 0)
        wordLimit = sharedPreferences.getInt("wordLimit", 300)

        val wordTextView = findViewById<TextView>(R.id.wordTextView)
        val inputEditText = findViewById<EditText>(R.id.inputEditText)
        val checkButton = findViewById<Button>(R.id.checkButton)
        val scoreTextView = findViewById<TextView>(R.id.scoreTextView)
        val wordLimitTextView = findViewById<TextView>(R.id.wordLimitTextView)

        wordsList = loadWordsFromFile()

        fun getRandomWord(): String {
            return wordsList.random()
        }

        fun updateWord() {
            currentWord = getRandomWord()
            wordTextView.text = currentWord
        }

        @SuppressLint("SetTextI18n")
        fun updateScore() {
            scoreTextView.text = "Баллы: $score"
            val editor = getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
            editor.putInt("score", score)
            editor.apply()
        }

        fun updateWordLimit() {
            wordLimitTextView.text = "Осталось слов: $wordLimit"

            val editor = getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
            editor.putInt("wordLimit", wordLimit)
            editor.apply()
        }

        fun showLimitDialog() {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Лимит исчерпан!")
            builder.setMessage("Ты потратил дневной лимит. Желаешь пополнить лимит за 50 баллов?")

            builder.setPositiveButton("Extend") { _, _ ->
                if (score >= 50) {
                    score -= 50
                    wordLimit += wordsPerExtension
                    updateScore()
                    updateWordLimit()
                    Toast.makeText(this, "Лимит пополнен на $wordsPerExtension слов!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Недостаточно баллов!", Toast.LENGTH_SHORT).show()
                }

            }

            builder.setNegativeButton("Нет, спасибо") { _, _ ->
                Toast.makeText(this, "Лимит не пополнен!", Toast.LENGTH_SHORT).show()
                checkButton.isEnabled = false
            }

            builder.show()
        }

        checkButton.setOnClickListener {
            val userInput = inputEditText.text.toString()
            if (userInput == currentWord) {
                score += 1
            } else {
                score -= 1
            }

            wordLimit -= 1

            updateScore()
            updateWordLimit()
            inputEditText.text.clear()

            if (wordLimit == 0) {
                showLimitDialog()
            } else {
                updateWord()
            }
        }

        updateWord()
        updateScore()
        updateWordLimit()
    }

    private fun loadWordsFromFile(): List<String> {

        val inputStream = resources.openRawResource(R.raw.words)
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.readLines()
    }

}