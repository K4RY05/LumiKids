package com.example.lumikids.ui

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class PecsBoardActivity : BaseActivity() {

    private val TAG = "PECS_DEBUG"

    private val SOUND_VOLUME = 0.65f

    private lateinit var rvOptions: RecyclerView
    private lateinit var sentenceBar: LinearLayout
    private lateinit var btnSpeak: ImageButton
    private lateinit var imageLoader: ImageLoader


    private val isSelecting = AtomicBoolean(false)

    private val DEBOUNCE_MS = 600L
    private var lastSelectTime = 0L

    // ── ID de sesión de audio: cada sonido nuevo incrementa este contador.
    // Los callbacks comprueban que el ID no cambió antes de ejecutar lógica.
    // Esto evita que un audio "viejo" dispare acciones cuando ya fue cancelado.
    private val audioSession = AtomicInteger(0)

    private var mediaPlayer: MediaPlayer? = null
    private var repeatPlayer: MediaPlayer? = null
    private var validationRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    // =========================
    // ESTADO CENTRAL
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

        setupImmersiveMode()

        // Bajar el volumen del stream de música/media al nivel deseado para niños
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVol = (maxVol * SOUND_VOLUME).toInt().coerceAtLeast(1)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)

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

        btnSpeak.setOnClickListener { playSentenceAudio() }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener  { finish() }
        // Debounce también en el botón clear para clickers rápidos
        findViewById<ImageButton>(R.id.btnClear).setOnClickListener { removeLastItem() }

        rvOptions.layoutManager = GridLayoutManager(this, 3)

        clearBoardInDB {
            applyState(SentenceState())
            prefetchAllComplements()
        }
    }

    // =========================
    // RENDERIZAR ESTADO → UI
    // =========================
    private fun applyState(newState: SentenceState) {
        sentence = newState
        Log.d(TAG, "applyState: stage=${sentence.stage}")

        sentenceBar.removeAllViews()
        listOfNotNull(sentence.pronoun, sentence.verb, sentence.complement).forEach { addViewToSentenceBar(it) }

        btnSpeak.isEnabled = false
        btnSpeak.alpha = 0.5f

        when (sentence.stage) {
            Stage.PRONOUN    -> loadPecs("pronoun")
            Stage.VERB       -> loadPecs("verb")
            Stage.COMPLEMENT -> loadComplements(sentence.verb!!.text.lowercase())
        }
    }

    // =========================
    // SELECCIÓN — con debounce y protección multi-click
    // =========================
    private fun onItemSelected(item: PecsItem) {
        // ── Debounce: ignora clicks más rápidos que DEBOUNCE_MS ──────────────
        val now = System.currentTimeMillis()
        if (now - lastSelectTime < DEBOUNCE_MS) return
        lastSelectTime = now

        // ── Bloqueo atómico: solo un click activo a la vez ──────────────────
        if (!isSelecting.compareAndSet(false, true)) return
        if (sentence.isFull) { isSelecting.set(false); return }

        // Feedback visual inmediato para que el niño sepa que el click funcionó
        rvOptions.isEnabled = false
        rvOptions.alpha = 0.5f

        val currentStage = sentence.stage
        val audioName = getAudioName(item, currentStage)

        when (currentStage) {
            Stage.PRONOUN -> {
                sentence = sentence.copy(pronoun = item)
                saveSelectionToDB(item.id)
                addViewToSentenceBar(item)

                stopAllSounds()
                playSound("${RetrofitClient.BASE_URL_SOUNDS}pronoun/$audioName.mp3") {
                    loadPecs("verb")
                    if (!sentence.isEmpty) startRepeatingSound()
                }
            }

            Stage.VERB -> {
                sentence = sentence.copy(verb = item)
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
                sentence = sentence.copy(complement = item)
                saveSelectionToDB(item.id)
                addViewToSentenceBar(item)

                val verbText = sentence.verb?.text?.lowercase() ?: ""
                val isCorrect = complementMap[verbText]
                    ?.any { it.text.equals(item.text, ignoreCase = true) } ?: false

                stopAllSounds()
                // Limpiar el grid inmediatamente para evitar más selecciones
                rvOptions.adapter = PecsAdapter(emptyList()) {}
                unlockSelection()

                val compData = complementMap[verbText]?.find { it.text == audioName }
                    ?: complementMap.values.flatten().find { it.text == audioName }
                val complementSoundUrl = compData
                    ?.let { "${RetrofitClient.BASE_URL_SOUNDS}${it.soundCategory}/${it.text}.mp3" } ?: ""

                // Cancelar cualquier validación pendiente antes de programar una nueva
                cancelPendingValidation()

                if (complementSoundUrl.isNotEmpty()) {
                    playSound(complementSoundUrl) {
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
                        playSentenceAudio { handler.post { showValidationResult(true) } }
                    } else {
                        showValidationResult(false)
                    }
                }

                // Fallback: si el audio tarda más de lo esperado, mostrar resultado de todos modos
                scheduleValidationFallback(isCorrect, 5000L)
            }
        }
    }

    // =========================
    // VALIDACIÓN — helpers de timing
    // =========================
    private fun cancelPendingValidation() {
        validationRunnable?.let { handler.removeCallbacks(it) }
        validationRunnable = null
    }

    private fun scheduleValidationFallback(isCorrect: Boolean, delayMs: Long) {
        val r = Runnable {
            // Solo actúa si la validación aún no se ejecutó (resultIcon invisible)
            val resultIcon = findViewById<ImageView>(R.id.resultIcon)
            if (resultIcon.visibility != android.view.View.VISIBLE) {
                showValidationResult(isCorrect)
            }
            validationRunnable = null
        }
        validationRunnable = r
        handler.postDelayed(r, delayMs)
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
    // =========================
    private fun buildSentenceAudioUrl(): String {
        val pronoun    = sentence.pronoun    ?: return ""
        val verb       = sentence.verb       ?: return ""
        val complement = sentence.complement ?: return ""

        val pronounAudio    = getAudioName(pronoun, Stage.PRONOUN)
        val verbAudio       = verb.text.lowercase()
        val complementAudio = complement.text.lowercase()

        return "${RetrofitClient.BASE_URL_SOUNDS}sentence/${pronounAudio}_${verbAudio}_${complementAudio}.mp3"
    }

    private fun playSentenceAudio(onComplete: (() -> Unit)? = null) {
        val url = buildSentenceAudioUrl()
        if (url.isEmpty()) { onComplete?.invoke(); return }
        stopAllSounds()
        playSound(url, onComplete)
    }

    // =========================
    // SENTENCE BAR
    // =========================
    private fun addViewToSentenceBar(item: PecsItem) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_sentence, sentenceBar, false)
        view.tag = item
        view.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        view.findViewById<ImageView>(R.id.imgSentence).load(item.imageUrl, imageLoader)

        view.setOnClickListener {
            // Debounce en clicks de la sentenceBar también
            val now = System.currentTimeMillis()
            if (now - lastSelectTime < DEBOUNCE_MS) return@setOnClickListener
            lastSelectTime = now

            if (isSelecting.get()) return@setOnClickListener
            cancelPendingValidation()

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
            category == "verb" -> getAudioName(sentence.pronoun ?: return, Stage.PRONOUN)
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
                    val expectedStage = if (category == "pronoun") Stage.PRONOUN else Stage.VERB
                    if (sentence.stage == expectedStage) {
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
        isSelecting.set(false)
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
    // Uso de WeakReference en callbacks para evitar fugas de memoria.
    // El volumen se aplica con setVolume() en cada MediaPlayer.
    // =========================
    private fun stopAllSounds() {
        stopRepeatingSound()
        // Incrementar el ID de sesión invalida todos los callbacks pendientes
        audioSession.incrementAndGet()
        releasePlayer(mediaPlayer)
        mediaPlayer = null
    }

    private fun releasePlayer(mp: MediaPlayer?) {
        mp ?: return
        runCatching {
            if (mp.isPlaying) mp.stop()
            mp.release()
        }
    }

    /**
     * Reproduce un sonido desde URL.
     * - Volumen regulado por SOUND_VOLUME.
     * - El callback onComplete solo se ejecuta si el audioSession no cambió
     *   (es decir, si no se llamó stopAllSounds() mientras preparaba el audio).
     */
    private fun playSound(url: String, onComplete: (() -> Unit)? = null) {
        val sessionAtStart = audioSession.get()
        // WeakReference a la Activity para evitar fuga si se destruye antes de que prepare
        val weakThis = WeakReference(this)

        try {
            val mp = MediaPlayer()
            mp.setDataSource(url)
            mp.setVolume(SOUND_VOLUME, SOUND_VOLUME)

            mp.setOnPreparedListener { player ->
                val activity = weakThis.get() ?: run { player.release(); return@setOnPreparedListener }
                // Si el session cambió, este audio ya no es relevante
                if (activity.audioSession.get() != sessionAtStart) {
                    player.release()
                    return@setOnPreparedListener
                }
                player.start()
                activity.mediaPlayer = player
            }

            mp.setOnErrorListener { player, _, _ ->
                val activity = weakThis.get()
                runCatching { player.release() }
                if (activity != null && activity.mediaPlayer == player) activity.mediaPlayer = null
                if (activity != null && activity.audioSession.get() == sessionAtStart) {
                    activity.handler.post { onComplete?.invoke() }
                }
                true
            }

            mp.setOnCompletionListener { player ->
                val activity = weakThis.get()
                player.release()
                if (activity != null && activity.mediaPlayer == player) activity.mediaPlayer = null
                if (activity != null && activity.audioSession.get() == sessionAtStart) {
                    activity.handler.post { onComplete?.invoke() }
                }
            }

            mp.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "playSound EXCEPTION: ${e.message}")
            if (audioSession.get() == sessionAtStart) onComplete?.invoke()
        }
    }

    // =========================
    // AUDIO — REPETICIÓN
    // =========================
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
            val prev = repeatPlayer
            repeatPlayer = null
            val rp = MediaPlayer()
            rp.setDataSource(soundUrl)
            rp.setVolume(SOUND_VOLUME, SOUND_VOLUME)
            rp.setOnPreparedListener {
                runCatching { prev?.release() }
                it.start()
                repeatPlayer = it
            }
            rp.setOnCompletionListener { it.release(); if (repeatPlayer == it) repeatPlayer = null }
            rp.setOnErrorListener { it, _, _ ->
                runCatching { it.release() }
                if (repeatPlayer == it) repeatPlayer = null
                true
            }
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
        releasePlayer(repeatPlayer)
        repeatPlayer = null
    }

    private fun playLocalSound(resId: Int) {
        try {
            val prev = mediaPlayer
            mediaPlayer = null
            releasePlayer(prev)
            val mp = MediaPlayer.create(this, resId) ?: return
            mp.setVolume(SOUND_VOLUME, SOUND_VOLUME)
            mediaPlayer = mp
            mp.start()
        } catch (e: Exception) { Log.e(TAG, "playLocalSound ERROR: ${e.message}") }
    }

    // =========================
    // VALIDACIÓN
    // =========================
    private fun showValidationResult(isCorrect: Boolean) {
        // Si la validación ya fue ejecutada por el callback de audio, el fallback no repite
        cancelPendingValidation()

        val resultIcon = findViewById<ImageView>(R.id.resultIcon)
        if (isCorrect) {
            resultIcon.setImageResource(R.drawable.ic_correct)
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
        // Debounce para el botón clear también
        val now = System.currentTimeMillis()
        if (now - lastSelectTime < DEBOUNCE_MS) return
        lastSelectTime = now

        if (isSelecting.get()) return
        cancelPendingValidation()

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
        cancelPendingValidation()
        stopAllSounds()
        mediaPlayer = null
        imageLoader.shutdown()
    }
}