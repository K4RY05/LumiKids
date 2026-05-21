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
import coil.disk.DiskCache
import coil.load
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.size.Size
import com.example.lumikids.R
import com.example.lumikids.model.PecsItem
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.utils.PecsAdapter
import com.example.lumikids.utils.SessionManager
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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

    private val audioSession = AtomicInteger(0)

    private var mediaPlayer: MediaPlayer? = null
    private var repeatPlayer: MediaPlayer? = null
    private var validationRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // =========================
    // IMMERSIVE MODE
    // =========================
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
            ComplementData("yard",   "place/yard.jpg",   "place"),
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
            ComplementData("whiteboard", "school/whiteboard.jpg", "school")
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

    private fun imgUrl(path: String) = "${RetrofitClient.BASE_URL}img/$path?w=512&q=90"

    // =========================
    // onCreate
    // =========================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pecsboard)

        setupImmersiveMode()

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVol = (maxVol * SOUND_VOLUME).toInt().coerceAtLeast(1)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
        Log.d(TAG, "onCreate: volumen seteado a $targetVol / $maxVol")

        imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.35)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .okHttpClient { RetrofitClient.httpClient }
            .crossfade(true)
            .build()
        Log.d(TAG, "onCreate: ImageLoader creado (memoria=35%, disco=100MB)")

        rvOptions   = findViewById(R.id.rvOptions)
        sentenceBar = findViewById(R.id.sentenceBar)
        btnSpeak    = findViewById(R.id.btnSpeak)
        btnSpeak.isEnabled = false
        btnSpeak.alpha = 0.3f

        btnSpeak.setOnClickListener {
            Log.d(TAG, "btnSpeak: click → playSentenceAudio")
            playSentenceAudio()
        }
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            Log.d(TAG, "btnBack: click → finish")
            finish()
        }
        findViewById<ImageButton>(R.id.btnClear).setOnClickListener {
            Log.d(TAG, "btnClear: click → removeLastItem")
            removeLastItem()
        }

        rvOptions.layoutManager = GridLayoutManager(this, 3)

        clearBoardInDB {
            Log.d(TAG, "onCreate: board limpiado en DB → applyState inicial")
            applyState(SentenceState())
            prefetchAllComplements()
        }
    }

    // =========================
    // RENDERIZAR ESTADO → UI
    // =========================
    private fun applyState(newState: SentenceState) {
        sentence = newState
        Log.d(TAG, "applyState: stage=${sentence.stage} pronoun=${sentence.pronoun?.text} verb=${sentence.verb?.text} complement=${sentence.complement?.text}")

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
        val now = System.currentTimeMillis()
        if (now - lastSelectTime < DEBOUNCE_MS) {
            Log.w(TAG, "onItemSelected: debounce bloqueó click en '${item.text}'")
            return
        }
        lastSelectTime = now

        if (!isSelecting.compareAndSet(false, true)) {
            Log.w(TAG, "onItemSelected: isSelecting bloqueó click en '${item.text}'")
            return
        }
        if (sentence.isFull) {
            Log.w(TAG, "onItemSelected: sentence llena, ignorando '${item.text}'")
            isSelecting.set(false)
            return
        }

        rvOptions.isEnabled = false
        rvOptions.alpha = 0.5f

        val currentStage = sentence.stage
        val audioName = getAudioName(item, currentStage)
        Log.d(TAG, "onItemSelected: stage=$currentStage item='${item.text}' audioName='$audioName'")

        when (currentStage) {
            Stage.PRONOUN -> {
                sentence = sentence.copy(pronoun = item)
                saveSelectionToDB(item.id)
                addViewToSentenceBar(item)

                stopAllSounds()
                val url = "${RetrofitClient.BASE_URL_SOUNDS}pronoun/$audioName.mp3"
                Log.d(TAG, "PRONOUN: reproduciendo '$url'")
                playSound(url) {
                    Log.d(TAG, "PRONOUN: audio terminado → loadPecs(verb)")
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
                val url = "${RetrofitClient.BASE_URL_SOUNDS}verb/$audioName.mp3"
                Log.d(TAG, "VERB: reproduciendo '$url'")
                playSound(url) {
                    Log.d(TAG, "VERB: audio terminado → loadComplements(${sentence.verb?.text?.lowercase()})")
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
                Log.d(TAG, "COMPLEMENT: seleccionado='${item.text}' verbText='$verbText' isCorrect=$isCorrect")

                stopAllSounds()
                rvOptions.adapter = PecsAdapter(emptyList(), imageLoader) {}
                unlockSelection()

                val compData = complementMap[verbText]?.find { it.text == audioName }
                    ?: complementMap.values.flatten().find { it.text == audioName }
                val complementSoundUrl = compData
                    ?.let { "${RetrofitClient.BASE_URL_SOUNDS}${it.soundCategory}/${it.text}.mp3" } ?: ""
                Log.d(TAG, "COMPLEMENT: soundUrl='$complementSoundUrl'")

                cancelPendingValidation()

                if (complementSoundUrl.isNotEmpty()) {
                    playSound(complementSoundUrl) {
                        if (isCorrect) {
                            Log.d(TAG, "COMPLEMENT: correcto → playSentenceAudio")
                            playSentenceAudio {
                                handler.post { showValidationResult(true) }
                            }
                        } else {
                            Log.d(TAG, "COMPLEMENT: incorrecto → showValidationResult(false)")
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

                scheduleValidationFallback(isCorrect, 5000L)
            }
        }
    }

    // =========================
    // VALIDACIÓN — helpers de timing
    // =========================
    private fun cancelPendingValidation() {
        validationRunnable?.let {
            handler.removeCallbacks(it)
            Log.d(TAG, "cancelPendingValidation: runnable cancelado")
        }
        validationRunnable = null
    }

    private fun scheduleValidationFallback(isCorrect: Boolean, delayMs: Long) {
        val r = Runnable {
            val resultIcon = findViewById<ImageView>(R.id.resultIcon)
            if (resultIcon.visibility != android.view.View.VISIBLE) {
                Log.w(TAG, "scheduleValidationFallback: fallback disparado tras ${delayMs}ms isCorrect=$isCorrect")
                showValidationResult(isCorrect)
            }
            validationRunnable = null
        }
        validationRunnable = r
        handler.postDelayed(r, delayMs)
        Log.d(TAG, "scheduleValidationFallback: programado en ${delayMs}ms")
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

        val url = "${RetrofitClient.BASE_URL_SOUNDS}sentence/${pronounAudio}_${verbAudio}_${complementAudio}.mp3"
        Log.d(TAG, "buildSentenceAudioUrl: $url")
        return url
    }

    private fun playSentenceAudio(onComplete: (() -> Unit)? = null) {
        val url = buildSentenceAudioUrl()
        if (url.isEmpty()) { Log.w(TAG, "playSentenceAudio: url vacía"); onComplete?.invoke(); return }
        stopAllSounds()
        Log.d(TAG, "playSentenceAudio: reproduciendo '$url'")
        playSound(url, onComplete)
    }

    // =========================
    // SENTENCE BAR
    // =========================
    private fun addViewToSentenceBar(item: PecsItem) {
        Log.d(TAG, "addViewToSentenceBar: '${item.text}' → ${item.imageUrl}")
        val view = LayoutInflater.from(this).inflate(R.layout.item_sentence, sentenceBar, false)
        view.tag = item
        view.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)

        view.findViewById<ImageView>(R.id.imgSentence).load(item.imageUrl, imageLoader) {
            size(Size.ORIGINAL)
            allowHardware(true)
            memoryCacheKey(item.imageUrl)
            diskCacheKey(item.imageUrl)
            listener(
                onSuccess = { _, result ->
                    Log.d(TAG, "  [SENTENCE BAR OK] '${item.text}' dataSource=${result.dataSource}")
                },
                onError = { _, result ->
                    Log.e(TAG, "  [SENTENCE BAR ERROR] '${item.text}' → ${result.throwable?.message}")
                }
            )
        }

        view.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastSelectTime < DEBOUNCE_MS) return@setOnClickListener
            lastSelectTime = now
            if (isSelecting.get()) return@setOnClickListener

            Log.d(TAG, "sentenceBar click: '${item.text}'")
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
        val userId = SessionManager(this).getUserId() ?: run {
            Log.e(TAG, "loadPecs: userId es null, abortando")
            return
        }
        val pronounFolder = when {
            category == "verb" -> getAudioName(sentence.pronoun ?: run {
                Log.e(TAG, "loadPecs: pronoun es null al cargar verb")
                return
            }, Stage.PRONOUN)
            else -> ""
        }
        val endpoint = if (category == "verb") "verb/$pronounFolder" else category
        val url = "${RetrofitClient.BASE_URL}api/pecs/board/$endpoint/$userId"
        Log.d(TAG, "loadPecs: category='$category' url='$url'")

        activityScope.launch(Dispatchers.IO) {
            runCatching {
                val raw = URL(url).readText()
                Log.d(TAG, "loadPecs: respuesta (${raw.length} chars) = ${raw.take(200)}")

                val array = JSONArray(raw)
                Log.d(TAG, "loadPecs: total items en JSON = ${array.length()}")

                val list = (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    PecsItem(
                        obj.getInt("id"),
                        obj.getString("text"),
                        obj.getString("imageUrl"),
                        obj.optString("type", "")
                    )
                }.shuffled().take(3)

                Log.d(TAG, "loadPecs: items seleccionados = ${list.map { "'${it.text}' → ${it.imageUrl}" }}")
                prefetchItems(list)

                withContext(Dispatchers.Main) {
                    val expectedStage = if (category == "pronoun") Stage.PRONOUN else Stage.VERB
                    Log.d(TAG, "loadPecs [Main]: sentence.stage=${sentence.stage} expectedStage=$expectedStage")

                    if (sentence.stage == expectedStage) {
                        Log.d(TAG, "loadPecs: asignando adapter con ${list.size} items")
                        rvOptions.adapter = PecsAdapter(list, imageLoader) { onItemSelected(it) }
                    } else {
                        Log.w(TAG, "loadPecs: stage cambió mientras cargaba → adapter NO asignado")
                    }
                    unlockSelection()
                }
            }.onFailure {
                Log.e(TAG, "loadPecs ERROR: ${it::class.simpleName} → ${it.message}")
                withContext(Dispatchers.Main) { unlockSelection() }
            }
        }
    }

    private fun loadComplements(verb: String) {
        Log.d(TAG, "loadComplements: verb='$verb' stage=${sentence.stage}")

        if (sentence.stage != Stage.COMPLEMENT) {
            Log.w(TAG, "loadComplements: stage incorrecto (${sentence.stage}), abortando")
            unlockSelection()
            return
        }

        val correctList = complementMap[verb] ?: run {
            Log.e(TAG, "loadComplements: verb '$verb' no encontrado en complementMap")
            unlockSelection()
            return
        }

        val correct = correctList.random()

        // distinctBy imagePath garantiza que ningún incorrecto repita imagen (ni entre sí ni con el correcto)
        val incorrect = complementMap
            .filterKeys { it != verb }
            .values
            .flatten()
            .filter { it.imagePath != correct.imagePath }
            .distinctBy { it.imagePath }
            .shuffled()
            .take(2)

        if (incorrect.size < 2) {
            Log.w(TAG, "loadComplements: solo ${incorrect.size} incorrectos únicos disponibles")
        }

        val items = (listOf(correct) + incorrect).shuffled().mapIndexed { i, comp ->
            PecsItem(id = i, text = comp.text, imageUrl = imgUrl(comp.imagePath), type = "complement")
        }

        Log.d(TAG, "loadComplements: correcto='${correct.text}' incorrectos=${incorrect.map { it.text }}")
        Log.d(TAG, "loadComplements: items finales = ${items.map { "'${it.text}' → ${it.imageUrl}" }}")

        prefetchItems(items)
        rvOptions.adapter = PecsAdapter(items, imageLoader) { onItemSelected(it) }
        unlockSelection()
    }

    private fun unlockSelection() {
        isSelecting.set(false)
        rvOptions.isEnabled = true
        rvOptions.alpha = 1.0f
        Log.d(TAG, "unlockSelection: rvOptions habilitado")
    }

    // =========================
    // PRECARGA
    // =========================
    private fun prefetchAllComplements() {
        Log.d(TAG, "prefetchAllComplements: iniciando precarga de ${complementMap.values.flatten().distinctBy { it.imagePath }.size} imágenes únicas")
        activityScope.launch(Dispatchers.IO) {
            complementMap.values.flatten().distinctBy { it.imagePath }.map { comp ->
                async {
                    val url = imgUrl(comp.imagePath)
                    runCatching {
                        imageLoader.execute(
                            ImageRequest.Builder(this@PecsBoardActivity)
                                .data(url)
                                .size(Size.ORIGINAL)
                                .allowHardware(true)
                                .memoryCacheKey(url)
                                .diskCacheKey(url)
                                .build()
                        )
                        Log.d(TAG, "  [PREFETCH OK] ${comp.imagePath}")
                    }.onFailure {
                        Log.e(TAG, "  [PREFETCH ERROR] ${comp.imagePath} → ${it.message}")
                    }
                }
            }.awaitAll()
            Log.d(TAG, "prefetchAllComplements: precarga completa")
        }
    }

    private fun prefetchItems(items: List<PecsItem>) {
        Log.d(TAG, "prefetchItems: ${items.size} items")
        items.forEach { item ->
            imageLoader.enqueue(
                ImageRequest.Builder(this)
                    .data(item.imageUrl)
                    .size(Size.ORIGINAL)
                    .allowHardware(true)
                    .memoryCacheKey(item.imageUrl)
                    .diskCacheKey(item.imageUrl)
                    .listener(
                        onSuccess = { _, result ->
                            Log.d(TAG, "  [PREFETCH ITEM OK] '${item.text}' dataSource=${result.dataSource}")
                        },
                        onError = { _, result ->
                            Log.e(TAG, "  [PREFETCH ITEM ERROR] '${item.text}' → ${result.throwable?.message}")
                        }
                    )
                    .build()
            )
        }
    }

    private fun prefetchComplementsFor(verb: String) {
        val allComps = (complementMap[verb] ?: return) +
                complementMap.filterKeys { it != verb }.values.flatten()
        Log.d(TAG, "prefetchComplementsFor: verb='$verb' → ${allComps.distinctBy { it.imagePath }.size} imágenes únicas")
        activityScope.launch(Dispatchers.IO) {
            allComps.distinctBy { it.imagePath }.map { comp ->
                async {
                    val url = imgUrl(comp.imagePath)
                    runCatching {
                        imageLoader.execute(
                            ImageRequest.Builder(this@PecsBoardActivity)
                                .data(url)
                                .size(Size.ORIGINAL)
                                .allowHardware(true)
                                .memoryCacheKey(url)
                                .diskCacheKey(url)
                                .build()
                        )
                        Log.d(TAG, "  [PREFETCH VERB OK] ${comp.imagePath}")
                    }.onFailure {
                        Log.e(TAG, "  [PREFETCH VERB ERROR] ${comp.imagePath} → ${it.message}")
                    }
                }
            }.awaitAll()
        }
    }

    // =========================
    // AUDIO — GENERAL
    // =========================
    private fun stopAllSounds() {
        Log.d(TAG, "stopAllSounds: liberando audio session=${audioSession.get()}")
        stopRepeatingSound()
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

    private fun playSound(url: String, onComplete: (() -> Unit)? = null) {
        val sessionAtStart = audioSession.get()
        val weakThis = WeakReference(this)
        Log.d(TAG, "playSound: session=$sessionAtStart url='$url'")

        try {
            val mp = MediaPlayer()
            mp.setDataSource(url)
            mp.setVolume(SOUND_VOLUME, SOUND_VOLUME)

            mp.setOnPreparedListener { player ->
                val activity = weakThis.get() ?: run { player.release(); return@setOnPreparedListener }
                if (activity.audioSession.get() != sessionAtStart) {
                    Log.w(TAG, "playSound: session cambió (${activity.audioSession.get()} != $sessionAtStart), descartando")
                    player.release()
                    return@setOnPreparedListener
                }
                Log.d(TAG, "playSound: prepared → start '$url'")
                player.start()
                activity.mediaPlayer = player
            }

            mp.setOnErrorListener { player, what, extra ->
                Log.e(TAG, "playSound: ERROR what=$what extra=$extra url='$url'")
                val activity = weakThis.get()
                runCatching { player.release() }
                if (activity != null && activity.mediaPlayer == player) activity.mediaPlayer = null
                if (activity != null && activity.audioSession.get() == sessionAtStart) {
                    activity.handler.post { onComplete?.invoke() }
                }
                true
            }

            mp.setOnCompletionListener { player ->
                Log.d(TAG, "playSound: completion '$url'")
                val activity = weakThis.get()
                player.release()
                if (activity != null && activity.mediaPlayer == player) activity.mediaPlayer = null
                if (activity != null && activity.audioSession.get() == sessionAtStart) {
                    activity.handler.post { onComplete?.invoke() }
                }
            }

            mp.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "playSound EXCEPTION: ${e.message} url='$url'")
            if (audioSession.get() == sessionAtStart) onComplete?.invoke()
        }
    }

    // =========================
    // AUDIO — REPETICIÓN
    // =========================
    private val repeatSoundRunnable = object : Runnable {
        override fun run() {
            if (sentence.isEmpty) return
            if (mediaPlayer?.isPlaying == true) {
                Log.d(TAG, "repeatSoundRunnable: mediaPlayer activo, reintento en 5s")
                handler.postDelayed(this, 5000)
                return
            }
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

        if (soundUrl.isEmpty()) {
            Log.w(TAG, "playRepeatSound: soundUrl vacío para '${item.text}'")
            return
        }
        Log.d(TAG, "playRepeatSound: category='$category' url='$soundUrl'")

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
            rp.setOnErrorListener { it, what, extra ->
                Log.e(TAG, "playRepeatSound ERROR: what=$what extra=$extra")
                runCatching { it.release() }
                if (repeatPlayer == it) repeatPlayer = null
                true
            }
            repeatPlayer = rp
            rp.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "playRepeatSound EXCEPTION: ${e.message}")
        }
    }

    private fun startRepeatingSound() {
        Log.d(TAG, "startRepeatingSound: programando en 4s")
        handler.removeCallbacks(repeatSoundRunnable)
        handler.postDelayed(repeatSoundRunnable, 4000)
    }

    private fun stopRepeatingSound() {
        handler.removeCallbacks(repeatSoundRunnable)
        releasePlayer(repeatPlayer)
        repeatPlayer = null
        Log.d(TAG, "stopRepeatingSound: detenido")
    }

    private fun playLocalSound(resId: Int) {
        Log.d(TAG, "playLocalSound: resId=$resId")
        try {
            val prev = mediaPlayer
            mediaPlayer = null
            releasePlayer(prev)
            val mp = MediaPlayer.create(this, resId) ?: run {
                Log.e(TAG, "playLocalSound: MediaPlayer.create devolvió null para resId=$resId")
                return
            }
            mp.setVolume(SOUND_VOLUME, SOUND_VOLUME)
            mediaPlayer = mp
            mp.start()
        } catch (e: Exception) {
            Log.e(TAG, "playLocalSound ERROR: ${e.message}")
        }
    }

    // =========================
    // VALIDACIÓN
    // =========================
    private fun showValidationResult(isCorrect: Boolean) {
        Log.d(TAG, "showValidationResult: isCorrect=$isCorrect")
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
                Log.d(TAG, "showValidationResult: incorrecto → volver a COMPLEMENT stage")
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
        val now = System.currentTimeMillis()
        if (now - lastSelectTime < DEBOUNCE_MS) return
        lastSelectTime = now

        if (isSelecting.get()) return
        cancelPendingValidation()

        val newState = when {
            sentence.complement != null -> {
                Log.d(TAG, "removeLastItem: eliminando complement '${sentence.complement?.text}'")
                sentence.complement?.let { deleteSelectionFromDB(it.id) }
                sentence.copy(complement = null)
            }
            sentence.verb != null -> {
                Log.d(TAG, "removeLastItem: eliminando verb '${sentence.verb?.text}'")
                sentence.verb?.let { deleteSelectionFromDB(it.id) }
                sentence.copy(verb = null)
            }
            sentence.pronoun != null -> {
                Log.d(TAG, "removeLastItem: eliminando pronoun '${sentence.pronoun?.text}'")
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
        Log.d(TAG, "saveSelectionToDB: themeId=$themeId userId=$userId")
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
                val code = conn.responseCode
                Log.d(TAG, "saveSelectionToDB: responseCode=$code themeId=$themeId")
            }.onFailure { Log.e(TAG, "saveSelectionToDB ERROR: ${it.message}") }
        }
    }

    private fun deleteSelectionFromDB(themeId: Int) {
        val userId = SessionManager(this).getUserId() ?: return
        Log.d(TAG, "deleteSelectionFromDB: themeId=$themeId userId=$userId")
        activityScope.launch(Dispatchers.IO) {
            runCatching {
                val code = (URL("${RetrofitClient.BASE_URL}api/pecs/board/item/$userId/$themeId")
                    .openConnection() as HttpURLConnection)
                    .apply { requestMethod = "DELETE" }.responseCode
                Log.d(TAG, "deleteSelectionFromDB: responseCode=$code themeId=$themeId")
            }.onFailure { Log.e(TAG, "deleteSelectionFromDB ERROR: ${it.message}") }
        }
    }

    private fun clearBoardInDB(onSuccess: () -> Unit = {}) {
        val userId = SessionManager(this).getUserId() ?: return
        Log.d(TAG, "clearBoardInDB: userId=$userId")
        activityScope.launch(Dispatchers.IO) {
            runCatching {
                val code = (URL("${RetrofitClient.BASE_URL}api/pecs/board/$userId")
                    .openConnection() as HttpURLConnection)
                    .apply { requestMethod = "DELETE" }.responseCode
                Log.d(TAG, "clearBoardInDB: responseCode=$code")
            }.onFailure { Log.e(TAG, "clearBoardInDB ERROR: ${it.message}") }
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    // =========================
    // LIFECYCLE
    // =========================
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: stopAllSounds")
        stopAllSounds()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: sentence.isEmpty=${sentence.isEmpty}")
        if (!sentence.isEmpty) startRepeatingSound()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: limpiando recursos")
        activityScope.cancel()
        cancelPendingValidation()
        stopAllSounds()
        mediaPlayer = null
        imageLoader.shutdown()
    }
}