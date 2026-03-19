package layout

import com.example.lumikids.minigame.objectrecognition.model.GameObject
import kotlin.plus

class ObjectGameController(private val items: List<GameObject>) {

    var correctObject: GameObject? = null
        private set

    // --- NUEVO: Variable para recordar el último objeto ganador ---
    private var lastCorrectObject: GameObject? = null

    fun generateNewRound(): List<GameObject>? {
        // Validación de seguridad
        if (items.size < 3) return null

        // 1. Filtramos la lista para que el nuevo objeto correcto NUNCA sea el anterior
        val availableForCorrect = if (lastCorrectObject != null) {
            items.filter { it != lastCorrectObject }
        } else {
            items
        }

        // 2. Elegimos el NUEVO objeto correcto de esta lista limpia
        val newCorrectObject = availableForCorrect.random()

        // 3. Elegimos 2 opciones falsas de la lista original
        // (Asegurándonos de no agarrar el objeto correcto actual)
        val wrongOptions = items.filter { it != newCorrectObject }.shuffled().take(2)

        // 4. Juntamos el ganador con los 2 falsos y los revolvemos para que
        // la respuesta correcta cambie de lugar en la pantalla
        val finalOptions = (listOf(newCorrectObject) + wrongOptions).shuffled()

        // 5. Actualizamos las variables de nuestra clase
        correctObject = newCorrectObject
        lastCorrectObject = newCorrectObject // Guardamos en memoria para el próximo turno

        return finalOptions
    }

    fun isCorrect(selected: GameObject): Boolean {
        return selected == correctObject
    }
}