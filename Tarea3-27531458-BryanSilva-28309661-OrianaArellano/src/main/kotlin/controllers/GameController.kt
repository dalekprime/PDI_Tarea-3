package controllers

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.util.Duration
import models.Stereogram
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import java.io.ByteArrayInputStream
import kotlin.math.max
import java.io.File
import javafx.animation.PauseTransition

class GameController(private val stereogramController: StereogramController) {

    data class GameLevel(
        val levelNum: Int,
        val stereogram: Stereogram,
        val answerOptions: List<String>,
    )

    private var levels = mutableListOf<GameLevel>()
    private var currentLevelIndex = 0
    private var totalPoints = 0
    private val maxTimeSeconds = 60

    // Variables del Temporizador
    private var timeline: Timeline? = null
    private var secondsElapsed = 0

    // Elementos de la Interfaz Gráfica
    private lateinit var lblGameLevel: Label
    private lateinit var lblGameScore: Label
    private lateinit var lblGameTime: Label
    private lateinit var imgGameStereogram: ImageView
    private lateinit var progressGame: ProgressBar
    private lateinit var btnStartGame: Button
    private lateinit var btnOption1: Button
    private lateinit var btnOption2: Button
    private lateinit var btnOption3: Button
    private lateinit var btnOption4: Button

    fun setUI(
        lblLevel: Label, lblScore: Label, lblTime: Label,
        imgView: ImageView, progBar: ProgressBar, btnStart: Button,
        btn1: Button, btn2: Button, btn3: Button, btn4: Button
    ) {
        this.lblGameLevel = lblLevel
        this.lblGameScore = lblScore
        this.lblGameTime = lblTime
        this.imgGameStereogram = imgView
        this.progressGame = progBar
        this.btnStartGame = btnStart
        this.btnOption1 = btn1
        this.btnOption2 = btn2
        this.btnOption3 = btn3
        this.btnOption4 = btn4

        setupEvents()
    }

    private fun setupEvents() {
        setOptionsVisible(false)

        btnStartGame.setOnAction { playGame() }

        val buttonOptions = listOf(btnOption1, btnOption2, btnOption3, btnOption4)
        for (button in buttonOptions) {
            button.setOnAction { event ->
                handleResponse((event.source as Button).text)
            }
        }
    }

    private fun playGame() {
        btnStartGame.isDisable = true
        currentLevelIndex = 0
        totalPoints = 0
        levels.clear()

        loadTestLevels()
        loadLevelOnScreen()
    }

    private fun loadLevelOnScreen() {
        if (currentLevelIndex < levels.size) {
            val actualLevel = levels[currentLevelIndex]

            lblGameLevel.text = "Nivel ${actualLevel.levelNum}"
            lblGameScore.text = "Puntos: $totalPoints"

            imgGameStereogram.image = matToJavaFXImage(actualLevel.stereogram.getStereogramMat())

            val options = actualLevel.answerOptions
            if (options.size >= 4) {
                btnOption1.text = options[0]
                btnOption2.text = options[1]
                btnOption3.text = options[2]
                btnOption4.text = options[3]
                setOptionsVisible(true)
            }
            startTimer()
        } else {
            endGame()
        }
    }

    private fun startTimer() {
        timeline?.stop()
        secondsElapsed = 0
        lblGameTime.text = "Tiempo: ${maxTimeSeconds}s"

        timeline = Timeline(KeyFrame(Duration.seconds(1.0), EventHandler {
            secondsElapsed++
            val timeRemaining = maxTimeSeconds - secondsElapsed
            lblGameTime.text = "Tiempo: ${timeRemaining}s"
            progressGame.progress = timeRemaining.toDouble() / maxTimeSeconds

            if (timeRemaining <= 0) {
                manageTimeOut()
            }
        }))
        timeline?.cycleCount = maxTimeSeconds
        timeline?.play()
    }

    private fun handleResponse(selectAnswer: String) {
        timeline?.stop()
        setOptionsVisible(false)

        val currentLevel = levels[currentLevelIndex]
        val isCorrect = (selectAnswer == currentLevel.stereogram.getName())

        if (isCorrect) {
            val timeRemaining = max(0, maxTimeSeconds - secondsElapsed)
            val levelScore = timeRemaining * currentLevel.levelNum
            totalPoints += levelScore
            println("¡Respuesta Correcta!")
        } else {
            println("Respuesta Incorrecta.")
        }
        imgGameStereogram.image = matToJavaFXImage(currentLevel.stereogram.getDeepMap())

        val pause = PauseTransition(Duration.seconds(2.5))
        pause.setOnFinished {
            currentLevelIndex++
            loadLevelOnScreen()
        }
        pause.play()
    }

    private fun manageTimeOut() {
        handleResponse("")
    }

    private fun endGame() {
        lblGameTime.text = "Tiempo: --"
        imgGameStereogram.image = null
        btnStartGame.text = "Jugar de Nuevo"
        btnStartGame.isDisable = false
        progressGame.progress = 0.0
        setOptionsVisible(false)

        // Diagnóstico de Agudeza Estereoscópica
        val diagnostico = when {
            totalPoints >= 2500 -> "Excelente"
            totalPoints >= 1500 -> "Buena"
            totalPoints >= 500  -> "Media"
            else                -> "Baja"
        }

        // Mostrar en la pantalla
        lblGameLevel.text = "TEST FINALIZADO - Agudeza: $diagnostico"
        lblGameScore.text = "Puntaje Final: $totalPoints"
    }

    private fun setOptionsVisible(visible: Boolean) {
        btnOption1.isVisible = visible
        btnOption2.isVisible = visible
        btnOption3.isVisible = visible
        btnOption4.isVisible = visible
    }

    private fun loadTestLevels() {
        val rutaProfundidad = "src/main/resources/mapasProfundidad/"
        val rutaTexturas = "src/main/resources/texturas/"

        fun loadLevelFromFile(
            numberLevel: Int,
            nameFile: String,
            nameFigure: String,
            patternWidth: Int,
            maxDepth: Int,
            tech: String,
            textureFile: String?,
            options: List<String>
        ) {
            val fileDepth = File(rutaProfundidad + nameFile)
            val depthMap = Imgcodecs.imread(fileDepth.absolutePath, Imgcodecs.IMREAD_GRAYSCALE)

            if (depthMap.empty()) {
                println("ERROR: No se pudo encontrar el mapa en: ${fileDepth.absolutePath}")
                return
            }

            val texturaMat: Mat? = if (tech == "SIS" && textureFile != null) {
                val fileTex = File(rutaTexturas + textureFile)
                val mat = Imgcodecs.imread(fileTex.absolutePath, Imgcodecs.IMREAD_COLOR)

                if (mat.empty()) {
                    println("ADVERTENCIA: No se encontró la textura en: ${fileTex.absolutePath}. Se usará ruido (RDS) por defecto.")
                    null
                } else {
                    mat
                }
            } else {
                null
            }

            val stereogramMat = stereogramController.generate(
                depthMap = depthMap,
                patternWidth = patternWidth,
                maxDepth = maxDepth,
                patternTexture = texturaMat
            )

            val stereogramObj = Stereogram(
                nameFigure,
                depthMap,
                stereogramMat,
                tech
            )

            levels.add(GameLevel(numberLevel, stereogramObj, options))
            println("Nivel $numberLevel cargado. Figura: $nameFigure | Técnica: $tech")
        }

        loadLevelFromFile(
            numberLevel = 1,
            nameFile = "images.png",
            nameFigure = "Círculo",
            patternWidth = 130,
            maxDepth = 50,
            tech = "SIS",
            textureFile = "rocks.jpg",
            options = listOf("Triángulo", "Círculo", "Cuadrado", "Estrella")
        )

        loadLevelFromFile(
            numberLevel = 2,
            nameFile = "cube.png",
            nameFigure = "Cubo",
            patternWidth = 130,
            maxDepth = 40,
            tech = "SIS",
            textureFile = "texture.jpg",
            options = listOf("Rectangulo", "Diamante", "Triángulo", "Cubo")
        )

        loadLevelFromFile(
            numberLevel = 3,
            nameFile = "heart.jpg",
            nameFigure = "Corazón",
            patternWidth = 130,
            maxDepth = 50,
            tech = "SIS",
            textureFile = "wind.jpg",
            options = listOf("Corazón", "Estrella", "Luna", "Sol")
        )

        loadLevelFromFile(
            numberLevel = 4,
            nameFile = "",
            nameFigure = "Vaso",
            patternWidth = 130,
            maxDepth = 50,
            tech = "SIS",
            textureFile = "roses.jpg",
            options = listOf("Vaso", "Plato", "Cucharilla", "Tetera")
        )

        loadLevelFromFile(
            numberLevel = 5,
            nameFile = "cube.png",
            nameFigure = "Cubo",
            patternWidth = 130,
            maxDepth = 50,
            tech = "RDS",
            textureFile = null,
            options = listOf("Cubo", "Triangulo", "Nariz", "Mano")
        )

        loadLevelFromFile(
            numberLevel = 6,
            nameFile = "5dots.jpg",
            nameFigure = "5 puntos",
            patternWidth = 130,
            maxDepth = 50,
            tech = "RDS",
            textureFile = null,
            options = listOf("3 puntos", "4 puntos", "5 puntos", "6 puntos")
        )

        loadLevelFromFile(
            numberLevel = 7,
            nameFile = "hallo.jpg",
            nameFigure = "HALLO",
            patternWidth = 100,
            maxDepth = 60,
            tech = "RDS",
            textureFile = null,
            options = listOf("HELLO", "HALLO", "HALLE", "HELLI")
        )

        loadLevelFromFile(
            numberLevel = 8,
            nameFile = "",
            nameFigure = "",
            patternWidth = 130,
            maxDepth = 50,
            tech = "RDS",
            textureFile = null,
            options = listOf("", "", "", "")
        )

        loadLevelFromFile(
            numberLevel = 9,
            nameFile = "",
            nameFigure = "",
            patternWidth = 130,
            maxDepth = 50,
            tech = "RDS",
            textureFile = null,
            options = listOf("", "", "", "")
        )

        loadLevelFromFile(
            numberLevel = 10,
            nameFile = "",
            nameFigure = "",
            patternWidth = 130,
            maxDepth = 50,
            tech = "RDS",
            textureFile = null,
            options = listOf("", "", "", "")
        )
    }

    private fun matToJavaFXImage(mat: Mat): Image {
        val buffer = MatOfByte()
        Imgcodecs.imencode(".png", mat, buffer)
        return Image(ByteArrayInputStream(buffer.toArray()))
    }
}