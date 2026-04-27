package com.example.lumikids.ui

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.lumikids.R
import com.example.lumikids.model.PecsItem
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.utils.PecsAdapter
import com.example.lumikids.utils.SessionManager
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class PecsBoardActivity : AppCompatActivity() {

    private val TAG = "PECS_DEBUG"

    private lateinit var rvOptions: RecyclerView
    private lateinit var sentenceBar: LinearLayout

    private var currentStage = "pronoun"
    private var selectedPronounFolder = ""
    private var selectedVerb = ""

    private var mediaPlayer: MediaPlayer? = null

    // =========================
    // DATA CLASS
    // =========================
    data class ComplementData(
        val text: String,
        val imagePath: String,
        val soundCategory: String
    )

    // =========================
    // MAPA
    // =========================
    private val complementMap = mapOf(

        "eat" to listOf(
            ComplementData("apple", "food/apple.jpg", "food"),
            ComplementData("banana", "food/banana.jpg", "food"),
            ComplementData("cookie", "food/cookie.jpg", "food"),
            ComplementData("sweetbread", "food/sweetbread.jpg", "food"),
            ComplementData("yogurt", "food/yogurt.jpg", "food")
        ),

        "drink" to listOf(
            ComplementData("water", "food/water.jpg", "food"),
            ComplementData("juice", "food/juice.jpg", "food"),
            ComplementData("milk", "food/milk.jpg", "food")
        ),

        "run" to listOf(
            ComplementData("park", "place/park.jpg", "place"),
            ComplementData("patio", "place/patio.jpg", "place"),
            ComplementData("school", "place/school.jpg", "place"),
            ComplementData("street", "place/street.jpg", "place")
        ),

        "walk" to listOf(
            ComplementData("park", "place/park.jpg", "place"),
            ComplementData("school", "place/school.jpg", "place"),
            ComplementData("street", "place/street.jpg", "place"),
            ComplementData("home", "place/home.jpg", "place"),
            ComplementData("patio", "place/patio.jpg", "place")
        ),

        "play" to listOf(
            ComplementData("car", "games/car.jpg", "games"),
            ComplementData("doll", "games/doll.jpg", "games"),
            ComplementData("ball", "games/ball.jpg", "games"),
            ComplementData("blocks", "games/blocks.jpg", "games"),
            ComplementData("bubbles", "games/bubbles.jpg", "games")
        ),

        "write" to listOf(
            ComplementData("letter", "school/letter.jpg", "school"),
            ComplementData("number", "school/number.jpg", "school"),
            ComplementData("whiteboard", "school/whiteboard.jpg", "school")
        ),

        "cut" to listOf(
            ComplementData("paper", "school/paper.jpg", "school"),
            ComplementData("circle", "school/circle.jpg", "school"),
            ComplementData("square", "school/square.jpg", "school"),
            ComplementData("triangle", "school/triangle.jpg", "school")
        ),

        "paint" to listOf(
            ComplementData("paper", "school/paper.jpg", "school"),
            ComplementData("box", "school/box.jpg", "school"),
            ComplementData("face", "personal_hygiene/face.jpg", "personal_hygiene")
        ),
        "sleep" to listOf(
            ComplementData("bed", "furniure/bed.jpg", "furniure"),
            ComplementData("sofa", "furniure/sofa.jpg", "furniure"),
            ComplementData("pillow", "furniure/pillow.jpg", "furniure"),
            ComplementData("blanket", "furniure/blanket.jpg", "furniure")
        ),

        "listen" to listOf(
            ComplementData("radio", "music/radio.jpg", "music"),
            ComplementData("song", "music/song.jpg", "music"),
            ComplementData("drum", "music/drum.jpg", "music"),
            ComplementData("bell", "music/bell.jpg", "music")
        )
    )

    // =========================
    // onCreate
    // =========================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pecsboard)

        rvOptions = findViewById(R.id.rvOptions)
        sentenceBar = findViewById(R.id.sentenceBar)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnClear).setOnClickListener { removeLastItem() }

        rvOptions.layoutManager = GridLayoutManager(this, 3)

        clearBoardInDB {
            resetToPronouns()
        }
    }

    // =========================
    // SELECCIÓN
    // =========================
    private fun onItemSelected(item: PecsItem) {

        Log.d(TAG, "Seleccion: ${item.text} Stage: $currentStage")

        if (sentenceBar.childCount >= 3) return

        addToSentenceBar(item)
        saveSelectionToDB(item.id)

        val audioName = getAudioName(item)

        val soundUrl = if (currentStage == "complement") {

            val comp = complementMap[selectedVerb]
                ?.find { it.text == audioName }

            if (comp == null) {
                Log.e(TAG, "No encontrado complemento: $audioName")
                ""
            } else {
                "${RetrofitClient.BASE_URL_SOUNDS}${comp.soundCategory}/${comp.text}.mp3"
            }

        } else {
            "${RetrofitClient.BASE_URL_SOUNDS}$currentStage/$audioName.mp3"
        }

        Log.d(TAG, "Audio URL: $soundUrl")

        if (soundUrl.isNotEmpty()) playSound(soundUrl)

        when (currentStage) {

            "pronoun" -> {
                selectedPronounFolder = audioName
                currentStage = "verb"
                loadPecs("verb")
            }

            "verb" -> {
                selectedVerb = item.text.lowercase()
                currentStage = "complement"
                loadComplements(selectedVerb)
            }

            "complement" -> {
                val resultIcon = findViewById<ImageView>(R.id.resultIcon)
                val isCorrect = complementMap[selectedVerb]?.any { it.text == item.text } ?: false

                showValidationResult(isCorrect, resultIcon)

               /* if (isCorrect) {
                    //Toast.makeText(this, "¡Oración completa y correcta!", Toast.LENGTH_SHORT).show()
                } else {
                   // Toast.makeText(this, "Intenta de nuevo", Toast.LENGTH_SHORT).show()
                }*/
            }
        }
    }

    private fun getAudioName(item: PecsItem): String {
        return if (currentStage == "pronoun") {
            when {
                item.imageUrl.contains("/she/") -> "she"
                item.imageUrl.contains("/he/") -> "he"
                item.imageUrl.contains("/women/") -> "women"
                item.imageUrl.contains("/men/") -> "men"
                else -> item.text
            }
        } else item.text
    }

    // =========================
    // SENTENCE BAR
    // =========================
    private fun addToSentenceBar(item: PecsItem) {

        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_sentence, sentenceBar, false)

        view.tag = item

        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        view.layoutParams = params

        val img = view.findViewById<ImageView>(R.id.imgSentence)
        img.load(item.imageUrl)

        view.setOnClickListener {
            val index = sentenceBar.indexOfChild(view)
            sentenceBar.removeView(view)
            deleteSelectionFromDB(item.id)
            handleDeletion(index)
        }

        sentenceBar.addView(view)
    }

    // =========================
    // API
    // =========================
    private fun loadPecs(category: String) {

        val userId = SessionManager(this).getUserId() ?: return
        val endpoint = if (category == "verb") "verb/$selectedPronounFolder" else category
        val url = "${RetrofitClient.BASE_URL}api/pecs/board/$endpoint/$userId"

        Log.d(TAG, "GET: $url")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = URL(url).readText()
                val array = JSONArray(json)
                val list = mutableListOf<PecsItem>()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)

                    list.add(
                        PecsItem(
                            obj.getInt("id"),
                            obj.getString("text"),
                            obj.getString("imageUrl"),
                            obj.optString("type", "")
                        )
                    )
                }
                list.shuffle()

                val finalList = list.take(3)
                withContext(Dispatchers.Main) {
                    rvOptions.adapter = PecsAdapter(finalList) { onItemSelected(it) }
                }

            } catch (e: Exception) {
                Log.e(TAG, "ERROR API: ${e.message}")
            }
        }
    }

    // =========================
    // COMPLEMENTOS
    // =========================
    private fun loadComplements(verb: String) {

        val correctList = complementMap[verb] ?: emptyList()

        if (correctList.isEmpty()) {
            Log.e(TAG, "No hay complementos para verbo: $verb")
            return
        }


        val correct = correctList.random()


        val allIncorrect = complementMap
            .filterKeys { it != verb }
            .values
            .flatten()
        val incorrect = allIncorrect.shuffled().take(2)
        val finalList = (listOf(correct) + incorrect).shuffled()

        val list = finalList.mapIndexed { index, comp ->

            val imageUrl = "${RetrofitClient.BASE_URL}images/${comp.imagePath}"

            Log.d(TAG, "COMPLEMENTO → ${comp.text}")
            Log.d(TAG, "IMG → $imageUrl")

            PecsItem(
                id = index,
                text = comp.text,
                imageUrl = imageUrl,
                type = "complement"
            )
        }

        rvOptions.adapter = PecsAdapter(list) { onItemSelected(it) }
    }
    // =========================
    // AUDIO
    // =========================
    private fun playSound(url: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { start() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ERROR AUDIO: ${e.message}")
        }
    }

    // =========================
    // API POST
    // =========================
    private fun saveSelectionToDB(themeId: Int) {

        val userId = SessionManager(this).getUserId() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("${RetrofitClient.BASE_URL}api/pecs/board/select")

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("ID_theme", themeId)
                    put("ID_user", userId)
                }

                OutputStreamWriter(conn.outputStream).use {
                    it.write(json.toString())
                }

                conn.responseCode

            } catch (e: Exception) {
                Log.e(TAG, "ERROR POST: ${e.message}")
            }
        }
    }

    private fun deleteSelectionFromDB(themeId: Int) {
        val userId = SessionManager(this).getUserId() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("${RetrofitClient.BASE_URL}api/pecs/board/item/$userId/$themeId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                }
                conn.responseCode
            } catch (e: Exception) {
                Log.e(TAG, "ERROR DELETE: ${e.message}")
            }
        }
    }

    private fun clearBoardInDB(onSuccess: () -> Unit = {}) {
        val userId = SessionManager(this).getUserId() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("${RetrofitClient.BASE_URL}api/pecs/board/$userId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                }
                conn.responseCode

                withContext(Dispatchers.Main) {
                    onSuccess()
                }

            } catch (e: Exception) {
                Log.e(TAG, "ERROR CLEAR: ${e.message}")
            }
        }
    }

    private fun removeLastItem() {
        val count = sentenceBar.childCount
        if (count == 0) return

        val lastIndex = count - 1
        val lastView = sentenceBar.getChildAt(lastIndex)
        val item = lastView.tag as? PecsItem ?: return

        sentenceBar.removeViewAt(lastIndex)
        deleteSelectionFromDB(item.id)
        handleDeletion(lastIndex)
    }

    private fun handleDeletion(index: Int) {
        when (index) {
            0 -> resetToPronouns()
            1 -> {
                currentStage = "verb"
                sentenceBar.removeViews(1, sentenceBar.childCount - 1)
                loadPecs("verb")
            }
            2 -> {
                currentStage = "complement"
                loadComplements(selectedVerb)
            }
        }
    }

    private fun resetToPronouns() {
        currentStage = "pronoun"
        selectedPronounFolder = ""
        selectedVerb = ""
        sentenceBar.removeAllViews()
        loadPecs("pronoun")
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
    private fun showValidationResult(isCorrect: Boolean, resultIcon: ImageView) {
        // 1. Configurar imagen y audio
        if (isCorrect) {
            resultIcon.setImageResource(R.drawable.ic_correct)
            // Usando tu lógica de sonidos (asegúrate de tener la clase gameAudio o usar mediaPlayer)
            playLocalSound(R.raw.win)
        } else {
            resultIcon.setImageResource(R.drawable.ic_error)
            playLocalSound(R.raw.fail)
        }

        // 2. Mostrar el icono
        resultIcon.visibility = android.view.View.VISIBLE

        // 3. Ocultarlo automáticamente después de 2 segundos
        resultIcon.postDelayed({
            resultIcon.visibility = android.view.View.GONE

            // Si falló, quizás quieras limpiar el último item para que el niño lo intente de nuevo
            if (!isCorrect) {
                removeLastItem()
            }
        }, 2000)
    }

    private fun playLocalSound(resId: Int) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing local sound: ${e.message}")
        }
    }
}