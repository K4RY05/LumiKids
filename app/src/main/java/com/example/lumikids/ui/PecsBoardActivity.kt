package com.example.lumikids.ui

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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

    // Player principal: para sonidos de selección
    @Volatile
    private var mediaPlayer: MediaPlayer? = null

    private var repeatPlayer: MediaPlayer? = null

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

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
            ComplementData("yogurt", "food/yogurt.jpg", "food"),
            ComplementData("sandwich", "food/sandwich.jpg", "food")
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
            ComplementData("circle", "school/circle.png", "school"),
            ComplementData("square", "school/square.png", "school"),
            ComplementData("triangle", "school/triangle.png", "school")
        ),

        "paint" to listOf(
            ComplementData("paper", "school/paper.jpg", "school"),
            ComplementData("box", "school/box.jpg", "school"),
            ComplementData("whiteboard", "school/whiteboard.jpg", "school")
        ),

        "sleep" to listOf(
            ComplementData("bed", "furniure/bed.jpg", "furniure"),
            ComplementData("sofa", "furniure/sofa.jpg", "furniure"),
            ComplementData("pillow", "furniure/pillow.jpg", "furniure"),
            ComplementData("blanket", "furniure/blanket.png", "furniure")
        ),
    )

    // =========================
    // onCreate
    // =========================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pecsboard)

        Log.d(TAG, "=== onCreate START ===")

        rvOptions = findViewById(R.id.rvOptions)
        sentenceBar = findViewById(R.id.sentenceBar)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnClear).setOnClickListener { removeLastItem() }

        rvOptions.layoutManager = GridLayoutManager(this, 3)

        Log.d(TAG, "onCreate: limpiando board en DB antes de iniciar")
        clearBoardInDB {
            resetToPronouns()
        }

        Log.d(TAG, "=== onCreate END ===")
    }

    // =========================
    // SELECCIÓN
    // =========================
    private fun onItemSelected(item: PecsItem) {
        val stageAtSelection = currentStage

        if (sentenceBar.childCount >= 3) return

        addToSentenceBar(item)
        saveSelectionToDB(item.id)

        val audioName = getAudioName(item, stageAtSelection)

        // --- LÓGICA DE AUDIO ---
        val soundUrl = when (stageAtSelection) {
            "complement" -> {
                val comp = complementMap[selectedVerb]?.find { it.text == audioName }
                if (comp == null) "" else "${RetrofitClient.BASE_URL_SOUNDS}${comp.soundCategory}/${comp.text}.mp3"
            }
            else -> "${RetrofitClient.BASE_URL_SOUNDS}$stageAtSelection/$audioName.mp3"
        }

        if (soundUrl.isNotEmpty()) {
            playSound(soundUrl)
        }


        when (stageAtSelection) {
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

                val isCorrect = complementMap[selectedVerb]?.any {
                    it.text.equals(item.text, ignoreCase = true)
                } ?: false

                Log.d(TAG, "Validación: item=${item.text} verbo=$selectedVerb correcto=$isCorrect")

                val compData = complementMap[selectedVerb]?.find { it.text == audioName }
                    ?: complementMap.values.flatten().find { it.text == audioName }

                val soundUrl = if (compData != null) {
                    "${RetrofitClient.BASE_URL_SOUNDS}${compData.soundCategory}/${compData.text}.mp3"
                } else ""
                stopRepeatingSound()

                if (soundUrl.isNotEmpty()) {
                    playSound(soundUrl)
                }
                handler.postDelayed({
                    showValidationResult(isCorrect, resultIcon)
                }, 1100)
            }
        }
    }

    private fun getAudioName(item: PecsItem, stage: String): String {
        return if (stage == "pronoun") {
            val name = when {
                item.imageUrl.contains("/she/") -> "she"
                item.imageUrl.contains("/he/") -> "he"
                item.imageUrl.contains("/women/") -> "women"
                item.imageUrl.contains("/men/") -> "men"
                else -> item.text
            }
            Log.d(TAG, "  getAudioName (pronoun): imageUrl=${item.imageUrl} -> '$name'")
            name
        } else {
            Log.d(TAG, "  getAudioName ($stage): '${item.text}'")
            item.text
        }
    }

    // =========================
    // SENTENCE BAR
    // =========================
    private fun addToSentenceBar(item: PecsItem) {

        Log.d(TAG, "addToSentenceBar: ${item.text}")

        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_sentence, sentenceBar, false)

        view.tag = item

        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        view.layoutParams = params

        val img = view.findViewById<ImageView>(R.id.imgSentence)
        img.load(item.imageUrl)

        view.setOnClickListener {
            val index = sentenceBar.indexOfChild(view)
            Log.d(TAG, "sentenceBar click: eliminando index=$index item=${item.text}")
            sentenceBar.removeView(view)
            deleteSelectionFromDB(item.id)
            handleDeletion(index)

            if (sentenceBar.childCount == 0) {
                Log.d(TAG, "sentenceBar vacío tras click: deteniendo sonido repetido")
                stopRepeatingSound()
            }
        }

        sentenceBar.addView(view)

        if (sentenceBar.childCount == 1) {
            Log.d(TAG, "Primer item en sentenceBar: iniciando sonido repetido")
            startRepeatingSound()
        }
    }

    // =========================
    // API GET
    // =========================
    private fun loadPecs(category: String) {

        val userId = SessionManager(this).getUserId() ?: run {
            Log.e(TAG, "loadPecs: userId es null, abortando")
            return
        }

        val endpoint = if (category == "verb") "verb/$selectedPronounFolder" else category
        val url = "${RetrofitClient.BASE_URL}api/pecs/board/$endpoint/$userId"

        Log.d(TAG, "loadPecs: category=$category | GET $url")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = URL(url).readText()
                Log.d(TAG, "loadPecs: respuesta recibida (${json.length} chars)")

                val array = JSONArray(json)
                Log.d(TAG, "loadPecs: total items = ${array.length()}")

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

                Log.d(TAG, "loadPecs: mostrando ${finalList.size} items:")
                finalList.forEach { Log.d(TAG, "  - ${it.text} | ${it.imageUrl}") }

                withContext(Dispatchers.Main) {
                    rvOptions.adapter = PecsAdapter(finalList) { onItemSelected(it) }
                }

            } catch (e: Exception) {
                Log.e(TAG, "loadPecs ERROR: ${e.javaClass.simpleName} - ${e.message}")
            }
        }
    }

    // =========================
    // COMPLEMENTOS
    // =========================
    private fun loadComplements(verb: String) {

        Log.d(TAG, "loadComplements: verb=$verb")

        val correctList = complementMap[verb] ?: run {
            Log.e(TAG, "loadComplements: verbo '$verb' no encontrado en complementMap")
            Log.e(TAG, "loadComplements: claves disponibles = ${complementMap.keys}")
            return
        }

        if (correctList.isEmpty()) {
            Log.e(TAG, "loadComplements: lista vacía para verbo '$verb'")
            return
        }

        val correct = correctList.random()
        Log.d(TAG, "loadComplements: correcto = ${correct.text}")

        val allIncorrect = complementMap.filterKeys { it != verb }.values.flatten()
        val incorrect = allIncorrect.shuffled().take(2)
        Log.d(TAG, "loadComplements: incorrectos = ${incorrect.map { it.text }}")

        val finalList = (listOf(correct) + incorrect).shuffled()

        val list = finalList.mapIndexed { index, comp ->
            val imageUrl = "${RetrofitClient.BASE_URL}images/${comp.imagePath}"
            Log.d(TAG, "  COMPLEMENTO[$index] text=${comp.text} | img=$imageUrl | soundCat=${comp.soundCategory}")
            PecsItem(id = index, text = comp.text, imageUrl = imageUrl, type = "complement")
        }

        rvOptions.adapter = PecsAdapter(list) { onItemSelected(it) }
        Log.d(TAG, "loadComplements: adapter seteado con ${list.size} items")
    }

    // =========================
    // AUDIO PRINCIPAL
    // =========================
    private fun playSound(url: String, onComplete: (() -> Unit)? = null) {
        Log.d(TAG, "playSound: $url")
        try {
            val previousPlayer = mediaPlayer
            mediaPlayer = null

            val newPlayer = MediaPlayer()
            newPlayer.setDataSource(url)

            newPlayer.setOnPreparedListener { mp ->
                Log.d(TAG, "playSound: onPrepared")
                try { previousPlayer?.release() } catch (e: Exception) {
                    Log.w(TAG, "playSound: error liberando anterior: ${e.message}")
                }
                mp.start()
                mediaPlayer = mp
            }

            newPlayer.setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "playSound ERROR: $what en $url")
                try { mp.release() } catch (e: Exception) {}
                if (mediaPlayer == mp) mediaPlayer = null
                true
            }

            newPlayer.setOnCompletionListener { mp ->
                Log.d(TAG, "playSound: completado")
                mp.release()
                if (mediaPlayer == mp) mediaPlayer = null
                onComplete?.invoke()
            }

            newPlayer.prepareAsync()

        } catch (e: Exception) {
            Log.e(TAG, "playSound EXCEPTION: ${e.message}")
        }
    }
    // =========================
    // AUDIO REPETIDO
    // =========================
    private val repeatSoundRunnable = object : Runnable {
        override fun run() {
            val count = sentenceBar.childCount
            Log.d(TAG, "repeatSoundRunnable: tick | childCount=$count")

            if (count >= 3) {
                Log.d(TAG, "Máximo alcanzado (complemento). Ejecutando sonido final y deteniendo repetición.")
                playRepeatSound()
                return
            }
            playRepeatSound()
            handler.postDelayed(this, 5000)
        }
    }

    private fun playRepeatSound() {
        val count = sentenceBar.childCount
        if (count == 0) {
            Log.w(TAG, "playRepeatSound: sentenceBar vacío")
            return
        }

        val lastView = sentenceBar.getChildAt(count - 1)
        val item = lastView.tag as? PecsItem ?: run {
            Log.e(TAG, "playRepeatSound: tag no es PecsItem")
            return
        }

        val audioName = if (count == 1) {
            when {
                item.imageUrl.contains("/she/") -> "she"
                item.imageUrl.contains("/he/") -> "he"
                item.imageUrl.contains("/women/") -> "women"
                item.imageUrl.contains("/men/") -> "men"
                else -> item.text.lowercase().trim()
            }
        } else {
            item.text.lowercase().trim()
        }

        Log.d(TAG, "playRepeatSound: count=$count | audioName=$audioName")

        val soundUrl = when (count) {
            1 -> "${RetrofitClient.BASE_URL_SOUNDS}pronoun/$audioName.mp3"
            2 -> "${RetrofitClient.BASE_URL_SOUNDS}verb/$audioName.mp3"
            else -> {
                val comp = complementMap[selectedVerb]?.find { it.text == audioName }
                if (comp != null) {
                    "${RetrofitClient.BASE_URL_SOUNDS}${comp.soundCategory}/${comp.text}.mp3"
                } else {
                    Log.e(TAG, "playRepeatSound: complemento '$audioName' no encontrado")
                    ""
                }
            }
        }

        if (soundUrl.isEmpty()) return

        try {
            val previousRepeat = repeatPlayer
            repeatPlayer = null

            val rp = MediaPlayer()
            rp.setDataSource(soundUrl)

            rp.setOnPreparedListener { mp ->
                Log.d(TAG, "playRepeatSound: OK -> Reproduciendo $audioName")
                try { previousRepeat?.release() } catch (e: Exception) {}
                mp.start()
                repeatPlayer = mp
            }

            rp.setOnCompletionListener { mp ->
                mp.release()
                if (repeatPlayer == mp) repeatPlayer = null
            }

            rp.setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "playRepeatSound ERROR: $audioName no encontrado en $soundUrl")
                mp.release()
                if (repeatPlayer == mp) repeatPlayer = null
                true
            }

            repeatPlayer = rp
            rp.prepareAsync()

        } catch (e: Exception) {
            Log.e(TAG, "playRepeatSound EXCEPTION: ${e.message}")
        }
    }

    private fun startRepeatingSound() {
        Log.d(TAG, "startRepeatingSound: iniciando handler (delay 4000ms)")
        handler.removeCallbacks(repeatSoundRunnable)
        // FIX: postDelayed en lugar de post para no solaparse con el sonido de selección
        handler.postDelayed(repeatSoundRunnable, 4000)
    }

    private fun stopRepeatingSound() {
        Log.d(TAG, "stopRepeatingSound: deteniendo handler y liberando repeatPlayer")
        handler.removeCallbacks(repeatSoundRunnable)
        try { repeatPlayer?.release() } catch (e: Exception) {
            Log.w(TAG, "stopRepeatingSound: error liberando repeatPlayer: ${e.message}")
        }
        repeatPlayer = null
    }

    // =========================
    // SONIDO LOCAL
    // =========================
    private fun playLocalSound(resId: Int) {
        Log.d(TAG, "playLocalSound: resId=$resId")
        try {
            val previous = mediaPlayer
            mediaPlayer = null
            try { previous?.release() } catch (e: Exception) {
                Log.w(TAG, "playLocalSound: error liberando player anterior: ${e.message}")
            }
            val mp = MediaPlayer.create(this, resId)
            if (mp == null) {
                Log.e(TAG, "playLocalSound: MediaPlayer.create() retornó null para resId=$resId")
                return
            }
            mediaPlayer = mp
            mp.setOnCompletionListener {
                Log.d(TAG, "playLocalSound: completado resId=$resId")
            }
            mp.start()
            Log.d(TAG, "playLocalSound: reproducción iniciada")
        } catch (e: Exception) {
            Log.e(TAG, "playLocalSound ERROR: ${e.message}")
        }
    }

    // =========================
    // API POST
    // =========================
    private fun saveSelectionToDB(themeId: Int) {

        val userId = SessionManager(this).getUserId() ?: run {
            Log.e(TAG, "saveSelectionToDB: userId null, abortando")
            return
        }

        Log.d(TAG, "saveSelectionToDB: themeId=$themeId userId=$userId")

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

                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                val code = conn.responseCode
                Log.d(TAG, "saveSelectionToDB: responseCode=$code")

            } catch (e: Exception) {
                Log.e(TAG, "saveSelectionToDB ERROR: ${e.message}")
            }
        }
    }

    private fun deleteSelectionFromDB(themeId: Int) {

        val userId = SessionManager(this).getUserId() ?: run {
            Log.e(TAG, "deleteSelectionFromDB: userId null, abortando")
            return
        }

        Log.d(TAG, "deleteSelectionFromDB: themeId=$themeId userId=$userId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("${RetrofitClient.BASE_URL}api/pecs/board/item/$userId/$themeId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                }
                val code = conn.responseCode
                Log.d(TAG, "deleteSelectionFromDB: responseCode=$code")
            } catch (e: Exception) {
                Log.e(TAG, "deleteSelectionFromDB ERROR: ${e.message}")
            }
        }
    }

    private fun clearBoardInDB(onSuccess: () -> Unit = {}) {

        val userId = SessionManager(this).getUserId() ?: run {
            Log.e(TAG, "clearBoardInDB: userId null, abortando")
            return
        }

        Log.d(TAG, "clearBoardInDB: userId=$userId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("${RetrofitClient.BASE_URL}api/pecs/board/$userId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                }
                val code = conn.responseCode
                Log.d(TAG, "clearBoardInDB: responseCode=$code")

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "clearBoardInDB: llamando onSuccess()")
                    onSuccess()
                }

            } catch (e: Exception) {
                Log.e(TAG, "clearBoardInDB ERROR: ${e.message}")
            }
        }
    }

    // =========================
    // RESET
    // =========================
    private fun removeLastItem() {
        val count = sentenceBar.childCount
        Log.d(TAG, "removeLastItem: childCount=$count")

        if (count == 0) {
            Log.w(TAG, "removeLastItem: sentenceBar vacío, nada que borrar")
            return
        }

        val lastIndex = count - 1
        val lastView = sentenceBar.getChildAt(lastIndex)
        val item = lastView.tag as? PecsItem ?: run {
            Log.e(TAG, "removeLastItem: tag no es PecsItem en index $lastIndex")
            return
        }

        Log.d(TAG, "removeLastItem: eliminando '${item.text}' en index=$lastIndex")
        sentenceBar.removeViewAt(lastIndex)
        deleteSelectionFromDB(item.id)
        handleDeletion(lastIndex)

        if (sentenceBar.childCount == 0) {
            Log.d(TAG, "removeLastItem: sentenceBar vacío, deteniendo sonido repetido")
            stopRepeatingSound()
        }
    }

    private fun handleDeletion(index: Int) {
        Log.d(TAG, "handleDeletion: index=$index | currentStage=$currentStage")
        if (sentenceBar.childCount > 0 && sentenceBar.childCount < 3) {
            startRepeatingSound()
        }

        when (index) {
            0 -> resetToPronouns()
            1 -> {
                currentStage = "verb"
                loadPecs("verb")
            }
            2 -> {
                currentStage = "complement"
                loadComplements(selectedVerb)
            }
        }
    }

    private fun resetToPronouns() {
        Log.d(TAG, "resetToPronouns: limpiando todo y volviendo a pronoun")
        currentStage = "pronoun"
        selectedPronounFolder = ""
        selectedVerb = ""
        sentenceBar.removeAllViews()
        loadPecs("pronoun")
    }

    // =========================
    // VALIDACIÓN
    // =========================
    private fun showValidationResult(isCorrect: Boolean, resultIcon: ImageView) {
        Log.d(TAG, "showValidationResult: isCorrect=$isCorrect")

        if (isCorrect) {
            resultIcon.setImageResource(R.drawable.ic_correct)
            playLocalSound(R.raw.win)
        } else {
            resultIcon.setImageResource(R.drawable.ic_error)
            playLocalSound(R.raw.fail)
        }

        resultIcon.visibility = android.view.View.VISIBLE

        resultIcon.postDelayed({
            resultIcon.visibility = android.view.View.GONE
            if (!isCorrect) {
                Log.d(TAG, "showValidationResult: respuesta incorrecta -> removeLastItem()")
                removeLastItem()
            }
        }, 2000)
    }

    // =========================
    // LIFECYCLE
    // =========================
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "=== onPause: deteniendo sonido y handler ===")
        stopRepeatingSound()
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    Log.d(TAG, "onPause: MediaPlayer pausado")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onPause ERROR al pausar MediaPlayer: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "=== onResume: childCount=${sentenceBar.childCount} ===")
        if (sentenceBar.childCount > 0) {
            Log.d(TAG, "onResume: reanudando sonido repetido")
            startRepeatingSound()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "=== onDestroy: liberando recursos ===")
        stopRepeatingSound()
        try {
            mediaPlayer?.release()
            Log.d(TAG, "onDestroy: MediaPlayer liberado")
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy ERROR al liberar MediaPlayer: ${e.message}")
        }
        mediaPlayer = null
    }
}