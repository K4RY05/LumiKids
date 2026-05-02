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
import coil.ImageLoader
import coil.load
import coil.memory.MemoryCache
import coil.request.ImageRequest
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
    private lateinit var btnSpeak: ImageButton
    private lateinit var imageLoader: ImageLoader

    @Volatile private var mediaPlayer: MediaPlayer? = null
    private var repeatPlayer: MediaPlayer? = null
    private var validationRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // =========================
    // ESTADO CENTRAL — fuente única de verdad
    // Nunca leer sentenceBar.childCount para saber el stage
    // =========================
    private enum class Stage { PRONOUN, VERB, COMPLEMENT }

    private data class SentenceState(
        val pronoun: PecsItem? = null,
        val verb: PecsItem? = null,
        val complement: PecsItem? = null
    ) {
        val stage: Stage get() = when {
            pronoun == null -> Stage.PRONOUN
            verb == null    -> Stage.VERB
            else            -> Stage.COMPLEMENT
        }
        val count: Int get() = listOfNotNull(pronoun, verb, complement).size
        val isFull: Boolean get() = pronoun != null && verb != null && complement != null
        val isEmpty: Boolean get() = pronoun == null
    }

    private var sentence = SentenceState()
    private var isSelecting = false

    // =========================
    // DATA CLASS & MAPA
    // =========================
    data class ComplementData(val text: String, val imagePath: String, val soundCategory: String)

    private val complementMap = mapOf(
        "eat" to listOf(
            ComplementData("apple",      "food/apple.jpg",      "food"),
            ComplementData("banana",     "food/banana.jpg",     "food"),
            ComplementData("cookie",     "food/cookie.jpg",     "food"),
            ComplementData("sweetbread", "food/sweetbread.jpg", "food"),
            ComplementData("yogurt",     "food/yogurt.jpg",     "food"),
            ComplementData("sandwich",   "food/sandwich.jpg",   "food")
        ),
        "drink" to listOf(
            ComplementData("water", "food/water.jpg", "food"),
            ComplementData("juice", "food/juice.jpg", "food"),
            ComplementData("milk",  "food/milk.jpg",  "food")
        ),
        "run" to listOf(
            ComplementData("park",   "place/park.jpg",   "place"),
            ComplementData("patio",  "place/patio.jpg",  "place"),
            ComplementData("school", "place/school.jpg", "place"),
            ComplementData("street", "place/street.jpg", "place")
        ),
        "play" to listOf(
            ComplementData("car",     "games/car.jpg",     "games"),
            ComplementData("doll",    "games/doll.jpg",    "games"),
            ComplementData("ball",    "games/ball.jpg",    "games"),
            ComplementData("blocks",  "games/blocks.jpg",  "games"),
            ComplementData("bubbles", "games/bubbles.jpg", "games")
        ),
        "write" to listOf(
            ComplementData("letter",     "school/letter.jpg",     "school"),
            ComplementData("number",     "school/number.jpg",     "school"),
            ComplementData("whiteboard", "school/whiteboard.jpg", "school"),
            ComplementData("paper",      "school/paper.jpg",      "school")
        ),
        "cut" to listOf(
            ComplementData("paper",    "school/paper.jpg",    "school"),
            ComplementData("circle",   "school/circle.png",   "school"),
            ComplementData("square",   "school/square.png",   "school"),
            ComplementData("triangle", "school/triangle.png", "school")
        ),
        "paint" to listOf(
            ComplementData("paper",      "school/paper.jpg",      "school"),
            ComplementData("box",        "school/box.jpg",        "school"),
            ComplementData("whiteboard", "school/whiteboard.jpg", "school")
        ),
        "sleep" to listOf(
            ComplementData("bed",     "furniure/bed.jpg",     "furniure"),
            ComplementData("sofa",    "furniure/sofa.jpg",    "furniure"),
            ComplementData("pillow",  "furniure/pillow.jpg",  "furniure"),
            ComplementData("blanket", "furniure/blanket.png", "furniure")
        )
    )

    private fun imgUrl(path: String) = "${RetrofitClient.BASE_URL}img/$path?w=200"

    // =========================
    // onCreate
    // =========================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pecsboard)

        imageLoader = ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .okHttpClient { RetrofitClient.getClient(this) }
            .crossfade(true)
            .build()

        rvOptions   = findViewById(R.id.rvOptions)
        sentenceBar = findViewById(R.id.sentenceBar)
        btnSpeak    = findViewById(R.id.btnSpeak)
        btnSpeak.isEnabled = false
        btnSpeak.alpha = 0.5f

        // ── Botón speaker: reproduce la oración completa ──────────────────────
        btnSpeak.setOnClickListener {
            playSentenceAudio()
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener  { finish() }
        findViewById<ImageButton>(R.id.btnClear).setOnClickListener { removeLastItem() }

        rvOptions.layoutManager = GridLayoutManager(this, 3)

        clearBoardInDB {
            applyState(SentenceState())  // estado limpio
            prefetchAllComplements()
        }
    }

    // =========================
    // RENDERIZAR ESTADO → UI
    // Un solo lugar que sincroniza estado con vistas
    // =========================
    private fun applyState(newState: SentenceState) {
        sentence = newState
        Log.d(TAG, "applyState: stage=${sentence.stage} pronoun=${sentence.pronoun?.text} verb=${sentence.verb?.text} complement=${sentence.complement?.text}")

        // Reconstruir sentenceBar desde el estado, no al revés
        sentenceBar.removeAllViews()
        listOfNotNull(sentence.pronoun, sentence.verb, sentence.complement).forEach { item ->
            addViewToSentenceBar(item)
        }

        btnSpeak.isEnabled = false
        btnSpeak.alpha = 0.5f

        // Cargar opciones según el stage actual
        when (sentence.stage) {
            Stage.PRONOUN    -> loadPecs("pronoun")
            Stage.VERB       -> loadPecs("verb")
            Stage.COMPLEMENT -> loadComplements(sentence.verb!!.text.lowercase())
        }
    }

    // =========================
    // SELECCIÓN
    // =========================
    private fun onItemSelected(item: PecsItem) {
        if (isSelecting) return
        if (sentence.isFull) return

        val currentStage = sentence.stage
        isSelecting = true
        rvOptions.isEnabled = false
        rvOptions.alpha = 0.5f

        val audioName = getAudioName(item, currentStage)

        when (currentStage) {
            Stage.PRONOUN -> {
                val newState = sentence.copy(pronoun = item)
                sentence = newState
                saveSelectionToDB(item.id)
                addViewToSentenceBar(item)

                stopAllSounds()
                playSound("${RetrofitClient.BASE_URL_SOUNDS}pronoun/$audioName.mp3") {
                    loadPecs("verb")
                    if (!sentence.isEmpty) startRepeatingSound()
                }
            }

            Stage.VERB -> {
                val newState = sentence.copy(verb = item)
                sentence = newState
                saveSelectionToDB(item.id)
                addViewToSentenceBar(item)

                stopAllSounds()
                prefetchComplementsFor(item.text.lowercase())
                playSound("${RetrofitClient.BASE_URL_SOUNDS}verb/$audioName.mp3") {
                    loadComplements(sentence.verb!!.text.lowercase())
                    if (!sentence.isEmpty) startRepeatingSound()
                }
            }

            Stage.COMPLEMENT -> {
                val newState = sentence.copy(complement = item)
                sentence = newState
                saveSelectionToDB(item.id)
                addViewToSentenceBar(item)

                val verbText = sentence.verb?.text?.lowercase() ?: ""
                val isCorrect = complementMap[verbText]
                    ?.any { it.text.equals(item.text, ignoreCase = true) } ?: false

                stopAllSounds()
                rvOptions.adapter = PecsAdapter(emptyList()) {}
                unlockSelection()

                // ── Reproducir audio del complemento y luego la oración completa ──
                val compData = complementMap[verbText]?.find { it.text == audioName }
                    ?: complementMap.values.flatten().find { it.text == audioName }
                val complementSoundUrl = compData
                    ?.let { "${RetrofitClient.BASE_URL_SOUNDS}${it.soundCategory}/${it.text}.mp3" }
                    ?: ""

                if (complementSoundUrl.isNotEmpty()) {
                    playSound(complementSoundUrl) {
                        // Después del complemento, reproducir la oración completa (si es correcta)
                        if (isCorrect) {
                            playSentenceAudio {
                                handler.post { showValidationResult(true) }
                            }
                        } else {
                            handler.post { showValidationResult(false) }
                        }
                    }
                } else {
                    if (isCorrect) {
                        playSentenceAudio {
                            handler.post { showValidationResult(true) }
                        }
                    } else {
                        showValidationResult(false)
                    }
                }

                validationRunnable = Runnable {
                    showValidationResult(isCorrect)
                    validationRunnable = null
                }
                // Retraso ampliado para dar tiempo al audio del complemento + oración completa
                handler.postDelayed(validationRunnable!!, 3500)
            }
        }
    }

    private fun getAudioName(item: PecsItem, stage: Stage): String {
        return if (stage == Stage.PRONOUN) {
            when {
                item.imageUrl.contains("/she/")   -> "she"
                item.imageUrl.contains("/he/")    -> "he"
                item.imageUrl.contains("/women/") -> "women"
                item.imageUrl.contains("/men/")   -> "men"
                else -> item.text
            }
        } else item.text
    }

    // =========================
    // AUDIO — ORACIÓN COMPLETA
    // Carpeta: sounds/sentence/{pronoun}_{verb}_{complement}.mp3
    // Ejemplo: sounds/sentence/she_eat_apple.mp3
    // =========================

    /**
     * Construye la URL del audio de la oración completa.
     * Formato del archivo: {pronounAudio}_{verb}_{complement}.mp3
     * Carpeta en servidor: sounds/sentence/
     */
    private fun buildSentenceAudioUrl(): String {
        val pronoun    = sentence.pronoun    ?: return ""
        val verb       = sentence.verb       ?: return ""
        val complement = sentence.complement ?: return ""

        val pronounAudio    = getAudioName(pronoun, Stage.PRONOUN)
        val verbAudio       = verb.text.lowercase()
        val complementAudio = complement.text.lowercase()

        val url = "${RetrofitClient.BASE_URL_SOUNDS}sentence/${pronounAudio}_${verbAudio}_${complementAudio}.mp3"
        Log.d(TAG, "buildSentenceAudioUrl: $url")
        return url
    }

    /**
     * Reproduce el audio de la oración completa.
     * Se invoca:
     *   1. Automáticamente al validar la oración correcta (después del audio del complemento).
     *   2. Al pulsar el botón speaker (btnSpeak) cuando la oración está completa y es correcta.
     *
     * @param onComplete Callback opcional al terminar la reproducción.
     */
    private fun playSentenceAudio(onComplete: (() -> Unit)? = null) {
        val url = buildSentenceAudioUrl()
        if (url.isEmpty()) {
            Log.w(TAG, "playSentenceAudio: URL vacía, oración incompleta")
            onComplete?.invoke()
            return
        }
        Log.d(TAG, "playSentenceAudio: reproduciendo $url")
        stopAllSounds()
        playSound(url, onComplete)
    }

    // =========================
    // SENTENCE BAR — solo agrega vistas, no maneja estado
    // =========================
    private fun addViewToSentenceBar(item: PecsItem) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_sentence, sentenceBar, false)
        view.tag = item
        view.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        view.findViewById<ImageView>(R.id.imgSentence).load(item.imageUrl, imageLoader)

        view.setOnClickListener {
            if (isSelecting) return@setOnClickListener
            validationRunnable?.let { handler.removeCallbacks(it); validationRunnable = null }

            val newState = when (item) {
                sentence.pronoun    -> SentenceState()
                sentence.verb       -> sentence.copy(verb = null, complement = null)
                sentence.complement -> sentence.copy(complement = null)
                else                -> return@setOnClickListener
            }

            when (item) {
                sentence.pronoun -> {
                    sentence.pronoun?.let { deleteSelectionFromDB(it.id) }
                    sentence.verb?.let { deleteSelectionFromDB(it.id) }
                    sentence.complement?.let { deleteSelectionFromDB(it.id) }
                }
                sentence.verb -> {
                    sentence.verb?.let { deleteSelectionFromDB(it.id) }
                    sentence.complement?.let { deleteSelectionFromDB(it.id) }
                }
                sentence.complement -> deleteSelectionFromDB(item.id)
            }

            stopAllSounds()
            unlockSelection()
            applyState(newState)
        }

        sentenceBar.addView(view)
    }

    // =========================
    // API GET
    // =========================
    private fun loadPecs(category: String) {
        val userId = SessionManager(this).getUserId() ?: return
        val pronounFolder = when {
            category == "verb" -> getAudioName(
                sentence.pronoun ?: return, Stage.PRONOUN
            )
            else -> ""
        }
        val endpoint = if (category == "verb") "verb/$pronounFolder" else category
        val url = "${RetrofitClient.BASE_URL}api/pecs/board/$endpoint/$userId"

        activityScope.launch(Dispatchers.IO) {
            runCatching {
                val array = JSONArray(URL(url).readText())
                val list = (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    PecsItem(
                        obj.getInt("id"),
                        obj.getString("text"),
                        obj.getString("imageUrl"),
                        obj.optString("type", "")
                    )
                }.shuffled().take(3)

                prefetchItems(list)

                withContext(Dispatchers.Main) {
                    if (sentence.stage == if (category == "pronoun") Stage.PRONOUN else Stage.VERB) {
                        rvOptions.adapter = PecsAdapter(list) { onItemSelected(it) }
                    }
                    unlockSelection()
                }
            }.onFailure {
                Log.e(TAG, "loadPecs ERROR: ${it.message}")
                withContext(Dispatchers.Main) { unlockSelection() }
            }
        }
    }

    private fun loadComplements(verb: String) {
        if (sentence.stage != Stage.COMPLEMENT) { unlockSelection(); return }

        val correctList = complementMap[verb] ?: run { unlockSelection(); return }
        val correct   = correctList.random()
        val incorrect = complementMap.filterKeys { it != verb }.values.flatten().shuffled().take(2)
        val items = (listOf(correct) + incorrect).shuffled().mapIndexed { i, comp ->
            PecsItem(id = i, text = comp.text, imageUrl = imgUrl(comp.imagePath), type = "complement")
        }

        prefetchItems(items)
        rvOptions.adapter = PecsAdapter(items) { onItemSelected(it) }
        unlockSelection()
    }

    private fun unlockSelection() {
        isSelecting = false
        rvOptions.isEnabled = true
        rvOptions.alpha = 1.0f
    }

    // =========================
    // PRECARGA
    // =========================
    private fun prefetchAllComplements() {
        activityScope.launch(Dispatchers.IO) {
            complementMap.values.flatten().distinctBy { it.imagePath }.map { comp ->
                async {
                    val url = imgUrl(comp.imagePath)
                    runCatching {
                        imageLoader.execute(
                            ImageRequest.Builder(this@PecsBoardActivity)
                                .data(url).memoryCacheKey(url).diskCacheKey(url).build()
                        )
                    }
                }
            }.awaitAll()
            Log.d(TAG, "prefetchAllComplements: COMPLETO")
        }
    }

    private fun prefetchItems(items: List<PecsItem>) {
        items.forEach { item ->
            imageLoader.enqueue(
                ImageRequest.Builder(this)
                    .data(item.imageUrl).memoryCacheKey(item.imageUrl).diskCacheKey(item.imageUrl).build()
            )
        }
    }

    private fun prefetchComplementsFor(verb: String) {
        val allComps = (complementMap[verb] ?: return) +
                complementMap.filterKeys { it != verb }.values.flatten()
        activityScope.launch(Dispatchers.IO) {
            allComps.distinctBy { it.imagePath }.map { comp ->
                async {
                    val url = imgUrl(comp.imagePath)
                    runCatching {
                        imageLoader.execute(
                            ImageRequest.Builder(this@PecsBoardActivity)
                                .data(url).memoryCacheKey(url).diskCacheKey(url).build()
                        )
                    }
                }
            }.awaitAll()
        }
    }

    // =========================
    // AUDIO — GENERAL
    // =========================
    private fun stopAllSounds() {
        stopRepeatingSound()
        runCatching { mediaPlayer?.let { if (it.isPlaying) it.stop(); it.release() } }
        mediaPlayer = null
    }

    private fun playSound(url: String, onComplete: (() -> Unit)? = null) {
        try {
            val mp = MediaPlayer()
            mp.setDataSource(url)
            mp.setOnPreparedListener { it.start(); mediaPlayer = it }
            mp.setOnErrorListener { it, _, _ ->
                Log.e(TAG, "playSound onError: $url")
                runCatching { it.release() }
                if (mediaPlayer == it) mediaPlayer = null
                onComplete?.invoke(); true
            }
            mp.setOnCompletionListener {
                it.release()
                if (mediaPlayer == it) mediaPlayer = null
                onComplete?.invoke()
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "playSound EXCEPTION: ${e.message}")
            onComplete?.invoke()
        }
    }

    private val repeatSoundRunnable = object : Runnable {
        override fun run() {
            if (sentence.isEmpty) return
            if (mediaPlayer?.isPlaying == true) { handler.postDelayed(this, 5000); return }
            playRepeatSound()
            if (!sentence.isFull) handler.postDelayed(this, 5000)
        }
    }

    private fun playRepeatSound() {
        if (sentence.isEmpty) return

        val (item, category) = when {
            sentence.complement != null -> Pair(sentence.complement!!, "complement")
            sentence.verb != null       -> Pair(sentence.verb!!, "verb")
            else                        -> Pair(sentence.pronoun!!, "pronoun")
        }

        val audioName = getAudioName(item, when (category) {
            "pronoun" -> Stage.PRONOUN
            else      -> Stage.VERB
        })

        val soundUrl = when (category) {
            "pronoun"    -> "${RetrofitClient.BASE_URL_SOUNDS}pronoun/$audioName.mp3"
            "verb"       -> "${RetrofitClient.BASE_URL_SOUNDS}verb/$audioName.mp3"
            "complement" -> {
                val verbText = sentence.verb?.text?.lowercase() ?: ""
                complementMap[verbText]?.find { it.text == item.text }
                    ?.let { "${RetrofitClient.BASE_URL_SOUNDS}${it.soundCategory}/${it.text}.mp3" } ?: ""
            }
            else -> ""
        }
        if (soundUrl.isEmpty()) return

        try {
            val prev = repeatPlayer; repeatPlayer = null
            val rp = MediaPlayer()
            rp.setDataSource(soundUrl)
            rp.setOnPreparedListener { runCatching { prev?.release() }; it.start(); repeatPlayer = it }
            rp.setOnCompletionListener { it.release(); if (repeatPlayer == it) repeatPlayer = null }
            rp.setOnErrorListener { it, _, _ -> it.release(); if (repeatPlayer == it) repeatPlayer = null; true }
            repeatPlayer = rp
            rp.prepareAsync()
        } catch (e: Exception) { Log.e(TAG, "playRepeatSound EXCEPTION: ${e.message}") }
    }

    private fun startRepeatingSound() {
        handler.removeCallbacks(repeatSoundRunnable)
        handler.postDelayed(repeatSoundRunnable, 4000)
    }

    private fun stopRepeatingSound() {
        handler.removeCallbacks(repeatSoundRunnable)
        runCatching { repeatPlayer?.release() }
        repeatPlayer = null
    }

    private fun playLocalSound(resId: Int) {
        try {
            val prev = mediaPlayer; mediaPlayer = null
            runCatching { prev?.release() }
            val mp = MediaPlayer.create(this, resId) ?: return
            mediaPlayer = mp; mp.start()
        } catch (e: Exception) { Log.e(TAG, "playLocalSound ERROR: ${e.message}") }
    }

    // =========================
    // VALIDACIÓN
    // =========================
    private fun showValidationResult(isCorrect: Boolean) {
        val resultIcon = findViewById<ImageView>(R.id.resultIcon)
        if (isCorrect) {
            resultIcon.setImageResource(R.drawable.ic_correct)
            // El audio de la oración ya se reprodujo antes de llegar aquí;
            // solo reproducimos el sonido de victoria local.
            playLocalSound(R.raw.win)
            btnSpeak.isEnabled = true
            btnSpeak.alpha = 1.0f
        } else {
            resultIcon.setImageResource(R.drawable.ic_error)
            playLocalSound(R.raw.fail)
            btnSpeak.isEnabled = false
            btnSpeak.alpha = 0.5f
        }

        resultIcon.visibility = android.view.View.VISIBLE
        resultIcon.postDelayed({
            resultIcon.visibility = android.view.View.GONE
            if (!isCorrect) {
                val newState = sentence.copy(complement = null)
                sentence.complement?.let { deleteSelectionFromDB(it.id) }
                stopAllSounds()
                applyState(newState)
            }
        }, 2000)
    }

    // =========================
    // BORRAR ÚLTIMO ITEM (botón clear)
    // =========================
    private fun removeLastItem() {
        if (isSelecting) return
        validationRunnable?.let { handler.removeCallbacks(it); validationRunnable = null }

        val newState = when {
            sentence.complement != null -> {
                sentence.complement?.let { deleteSelectionFromDB(it.id) }
                sentence.copy(complement = null)
            }
            sentence.verb != null -> {
                sentence.verb?.let { deleteSelectionFromDB(it.id) }
                sentence.copy(verb = null)
            }
            sentence.pronoun != null -> {
                sentence.pronoun?.let { deleteSelectionFromDB(it.id) }
                SentenceState()
            }
            else -> return
        }

        stopAllSounds()
        unlockSelection()
        applyState(newState)
        btnSpeak.isEnabled = false
        btnSpeak.alpha = 0.5f
    }

    // =========================
    // API POST / DELETE
    // =========================
    private fun saveSelectionToDB(themeId: Int) {
        val userId = SessionManager(this).getUserId() ?: return
        activityScope.launch(Dispatchers.IO) {
            runCatching {
                val conn = (URL("${RetrofitClient.BASE_URL}api/pecs/board/select")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
                OutputStreamWriter(conn.outputStream).use {
                    it.write(JSONObject().apply {
                        put("ID_theme", themeId); put("ID_user", userId)
                    }.toString())
                }
                conn.responseCode
            }.onFailure { Log.e(TAG, "saveSelectionToDB ERROR: ${it.message}") }
        }
    }

    private fun deleteSelectionFromDB(themeId: Int) {
        val userId = SessionManager(this).getUserId() ?: return
        activityScope.launch(Dispatchers.IO) {
            runCatching {
                (URL("${RetrofitClient.BASE_URL}api/pecs/board/item/$userId/$themeId")
                    .openConnection() as HttpURLConnection)
                    .apply { requestMethod = "DELETE" }.responseCode
            }.onFailure { Log.e(TAG, "deleteSelectionFromDB ERROR: ${it.message}") }
        }
    }

    private fun clearBoardInDB(onSuccess: () -> Unit = {}) {
        val userId = SessionManager(this).getUserId() ?: return
        activityScope.launch(Dispatchers.IO) {
            runCatching {
                (URL("${RetrofitClient.BASE_URL}api/pecs/board/$userId")
                    .openConnection() as HttpURLConnection)
                    .apply { requestMethod = "DELETE" }.responseCode
            }.onFailure { Log.e(TAG, "clearBoardInDB ERROR: ${it.message}") }
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    // =========================
    // LIFECYCLE
    // =========================
    override fun onPause() {
        super.onPause()
        stopAllSounds()
    }

    override fun onResume() {
        super.onResume()
        if (!sentence.isEmpty) startRepeatingSound()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
        stopAllSounds()
        mediaPlayer = null
        imageLoader.shutdown()
    }
}