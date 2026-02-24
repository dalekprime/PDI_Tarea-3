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

    private var timeline: Timeline? = null
    private var secondsElapsed = 0

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

            imgGameStereogram.image = matToJavaFXImage(actualLevel.stereogram.getStereogramMat()!!)

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

        imgGameStereogram.image = matToJavaFXImage(currentLevel.stereogram.getDeepMap()!!)

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

        val diagnostico = when {
            totalPoints >= 2500 -> "Excelente"
            totalPoints >= 1500 -> "Buena"
            totalPoints >= 500  -> "Media"
            else                -> "Baja"
        }

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
            eyeSep: Int,
            focalLen: Int,
            tech: String,
            textureFile: String?,
            options: List<String>
        ) {
            val stereogramObj = Stereogram(nameFigure, tech, eyeSep, focalLen)

            val fileDepth = File(rutaProfundidad + nameFile)
            val depthMap = Imgcodecs.imread(fileDepth.absolutePath, Imgcodecs.IMREAD_GRAYSCALE)

            if (depthMap.empty()) {
                println("ERROR: No se pudo encontrar el mapa en: ${fileDepth.absolutePath}")
                return
            }
            stereogramObj.setDeepMap(depthMap)

            if (tech == "TX" && textureFile != null) {
                val fileTex = File(rutaTexturas + textureFile)
                val mat = Imgcodecs.imread(fileTex.absolutePath, Imgcodecs.IMREAD_COLOR)

                if (mat.empty()) {
                    println("ADVERTENCIA: No se encontró la textura en: ${fileTex.absolutePath}. Se usará Random Dots (RD) por defecto.")
                    stereogramObj.setTech("RD")
                } else {
                    stereogramObj.setTexture(mat)
                }
            }

            val stereogramMat: Mat = when (stereogramObj.getTech()) {
                "TX" -> stereogramController.generateTextureStereogram(stereogramObj)
                "RD" -> stereogramController.generateRandomDotStereogram(stereogramObj)
                else -> stereogramController.generateRandomDotStereogram(stereogramObj)
            }

            stereogramObj.setStereogram(stereogramMat)

            levels.add(GameLevel(numberLevel, stereogramObj, options))
            println("Nivel $numberLevel cargado. Figura: $nameFigure | Técnica: ${stereogramObj.getTech()}")
        }

        loadLevelFromFile(
            numberLevel = 1,
            nameFile = "cube.png",
            nameFigure = "Cubo",
            eyeSep = 130,
            focalLen = 40,
            tech = "TX",
            textureFile = "texture.jpg",
            options = listOf("Rectangulo", "Diamante", "Triángulo", "Cubo")
        )

        loadLevelFromFile(
            numberLevel = 2,
            nameFile = "sombrero.jpg",
            nameFigure = "Sombrero",
            eyeSep = 130,
            focalLen = 50,
            tech = "TX",
            textureFile = "rocksb.jpg",
            options = listOf("Sombrero", "Plato", "Gorra", "Sol")
        )


        loadLevelFromFile(
            numberLevel = 3,
            nameFile = "manzana.jpg",
            nameFigure = "Manzana",
            eyeSep = 130,
            focalLen = 50,
            tech = "TX",
            textureFile = "texture2.jpg",
            options = listOf("Cambur", "Manzana", "Circulo", "Estrella")
        )

        loadLevelFromFile(
            numberLevel = 4,
            nameFile = "dona.png",
            nameFigure = "Dona",
            eyeSep = 130,
            focalLen = 50,
            tech = "TX",
            textureFile = "flor.jpg",
            options = listOf("Dona", "Plato", "Pizza", "Vaso")
        )

        loadLevelFromFile(
            numberLevel = 5,
            nameFile = "cerdo.png",
            nameFigure = "Cerdo",
            eyeSep = 130,
            focalLen = 50,
            tech = "TX",
            textureFile = "brillos.jpg",
            options = listOf("Cerdo", "Vaca", "Fenix", "Perro")
        )

        loadLevelFromFile(
            numberLevel = 6,
            nameFile = "rocket.png",
            nameFigure = "Cohete",
            eyeSep = 130,
            focalLen = 50,
            tech = "TX",
            textureFile = "water.jpg",
            options = listOf("Planeta", "Estrella", "Cohete", "Avión")
        )

        loadLevelFromFile(
            numberLevel = 7,
            nameFile = "calavera1.jpg",
            nameFigure = "Calavera",
            eyeSep = 130,
            focalLen = 60,
            tech = "TX",
            textureFile = "lineas.jpg",
            options = listOf("Calavera", "Cabeza", "Mano", "Pie")
        )

        loadLevelFromFile(
            numberLevel = 8,
            nameFile = "gato.png",
            nameFigure = "Gato",
            eyeSep = 130,
            focalLen = 50,
            tech = "RD",
            textureFile = null,
            options = listOf("Gato", "Pie", "Perro", "Ojo")
        )

        loadLevelFromFile(
            numberLevel = 9,
            nameFile = "g2.jpg",
            nameFigure = "Guitarra",
            eyeSep = 130,
            focalLen = 50,
            tech = "RD",
            textureFile = null,
            options = listOf("Bateria", "Guitarra", "Piano", "Flauta")
        )

        loadLevelFromFile(
            numberLevel = 10,
            nameFile = "arboles.png",
            nameFigure = "Arboles",
            eyeSep = 130,
            focalLen = 50,
            tech = "RD",
            textureFile = null,
            options = listOf("Flores", "Arboles", "Caballo", "Unicornio")
        )


    }

    private fun matToJavaFXImage(mat: Mat): Image {
        val buffer = MatOfByte()
        Imgcodecs.imencode(".png", mat, buffer)
        return Image(ByteArrayInputStream(buffer.toArray()))
    }
}
