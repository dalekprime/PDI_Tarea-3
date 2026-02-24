package controllers

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.stage.FileChooser
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import java.io.ByteArrayInputStream
import java.io.File
import javafx.animation.Timeline
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.RadioButton
import javafx.scene.control.Slider
import javafx.scene.control.ToggleGroup
import models.Stereogram

class BasicViewController {

    //Creation
    @FXML private lateinit var btnLoadObj: Button
    @FXML private lateinit var btnCaptureDepth: Button
    @FXML private lateinit var btnGenerate: Button
    @FXML private lateinit var btnLoadTexture: Button
    @FXML private lateinit var depthImageView: ImageView
    @FXML private lateinit var techGroup: ToggleGroup
    @FXML private lateinit var sliderEyeSep: Slider
    @FXML private lateinit var sliderFocalLen: Slider
    //Inversion
    @FXML private lateinit var btnInvert: Button
    @FXML private lateinit var depthImageViewInvOriginal: ImageView
    @FXML private lateinit var depthImageViewInverted: ImageView
    @FXML private lateinit var sliderPattern: Slider
    @FXML private lateinit var sliderFocalLenInv: Slider
    @FXML private lateinit var sliderEyeSepInv: Slider
    private var patternSize: Int = 0
    private var focalLenInv: Int = 0
    private var eyeSepInv: Int = 0
    private var stereogramToInvert: Mat? = null
    //Game
    @FXML private lateinit var lblGameLevel: Label
    @FXML private lateinit var lblGameScore: Label
    @FXML private lateinit var lblGameTime: Label
    @FXML private lateinit var imgGameStereogram: ImageView
    @FXML private lateinit var btnOption1: Button
    @FXML private lateinit var btnOption2: Button
    @FXML private lateinit var btnOption3: Button
    @FXML private lateinit var btnOption4: Button
    @FXML private lateinit var btnStartGame: Button
    @FXML private lateinit var progressGame: ProgressBar
    @FXML private lateinit var btnToggleDots: Button
    @FXML private lateinit var btnHintDots: Button

    private lateinit var deepMapController: DeepMapController
    private lateinit var stereogramController: StereogramController
    private lateinit var gameController: GameController
    private lateinit var actualStereogram: Stereogram

    private var rotX = 0f
    private var rotY = 0f
    private var transX = 0f
    private var transY = 0f
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var objLoaded = false

    private var actualScale = 1.0f

    private var timeline: Timeline? = null
    private var secondsElapsed = 0
    private val maxTimeSeconds = 60
    private var isShowingDots = false

    @FXML
    fun initialize() {
        //Crea un Estereograma Vacío
        actualStereogram = Stereogram()

        deepMapController = DeepMapController(600, 500)
        stereogramController = StereogramController()
        gameController = GameController(stereogramController)

        //Botones
        btnLoadObj.setOnAction { openAndLoadObj() }
        btnCaptureDepth.setOnAction { generateDeepMap() }
        btnGenerate.setOnAction {generateStereogram()}
        btnLoadTexture.setOnAction {readImage()}
        btnToggleDots.setOnAction { toggleHelperDots() }

        btnInvert.setOnAction {readImage2()}
        techGroup.selectedToggleProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                val op = newValue as RadioButton
                when (op.text) {
                    "Puntos Aleatorios" -> actualStereogram.setTech("RD")
                    "Imagen de Patrón" -> actualStereogram.setTech("TX")
                }
            }
        }
        sliderEyeSep.valueProperty().addListener { _, _, newValue ->
            if(newValue != null) {
                actualStereogram.setEyeSep(newValue.toInt())
                generateStereogram()
            }
        }
        sliderFocalLen.valueProperty().addListener { _, _, newValue ->
            if(newValue != null) {
                actualStereogram.setFocalLen(newValue.toInt())
                generateStereogram()
            }
        }
        sliderPattern.valueProperty().addListener { _, _, newValue ->
            if(newValue != null) {
                patternSize = newValue.toInt()
                invert()
            }
        }
        sliderFocalLenInv.valueProperty().addListener { _, _, newValue ->
            if(newValue != null) {
                focalLenInv = newValue.toInt()
                invert()
            }
        }
        sliderEyeSepInv.valueProperty().addListener { _, _, newValue ->
            if(newValue != null) {
                eyeSepInv = newValue.toInt()
                invert()
            }
        }
        //Eventos del mouse
        depthImageView.setOnMousePressed { event ->
            lastMouseX = event.sceneX
            lastMouseY = event.sceneY
        }

        depthImageView.setOnMouseDragged { event ->
            if (objLoaded) {
                val deltaX = (event.sceneX - lastMouseX).toFloat()
                val deltaY = (event.sceneY - lastMouseY).toFloat()

                if (event.button == MouseButton.PRIMARY) {
                    //Rotar
                    rotY += deltaX * 0.01f
                    rotX += deltaY * 0.01f
                } else if (event.button == MouseButton.SECONDARY) {
                    //Desplazar
                    transX += deltaX * 0.02f
                    transY -= deltaY * 0.02f
                }

                lastMouseX = event.sceneX
                lastMouseY = event.sceneY
                deepMapController.updateTransform(rotX, rotY, actualScale, transX, transY)
                updatePreview()
            }
        }
        //Rotación
        depthImageView.setOnScroll { event ->
            if (objLoaded) {
                val zoomFactor = if (event.deltaY > 0) 1.1f else 0.9f
                actualScale *= zoomFactor
                deepMapController.updateTransform(rotX, rotY, actualScale)
                updatePreview()
            }
        }
        /*gameController.setUI(
            lblGameLevel, lblGameScore, lblGameTime,
            imgGameStereogram, progressGame, btnStartGame,
            btnOption1, btnOption2, btnOption3, btnOption4,
            btnHintDots
        )


    }

    private fun openAndLoadObj() {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Archivos OBJ", "*.obj"))
        val file = fileChooser.showOpenDialog(btnLoadTexture.scene.window)
        if (file != null) {
            deepMapController.loadOBJ(file.absolutePath)
            rotX = 0f
            rotY = 0f
            transX = 0f
            transY = 0f
            deepMapController.updateTransform(rotX, rotY, 1.0f)
            objLoaded = true
            btnCaptureDepth.isDisable = false
            btnGenerate.isDisable = false
            sliderEyeSep.value = 130.0
            sliderFocalLen.value = 30.0
            updatePreview()
        }
    }

    //Wireframe
    private fun updatePreview() {
        val previewMat = deepMapController.renderPreview()
        depthImageView.image = matToJavaFXImage(previewMat)
        val depthMap = deepMapController.generateDepthMap()
        actualStereogram.setDeepMap(depthMap)
    }

    //Muestra el mapa de profundidad
    private fun generateDeepMap() {
        if (objLoaded) {
            val depthMap = deepMapController.generateDepthMap()
            depthImageView.image = matToJavaFXImage(depthMap)
            actualStereogram.setDeepMap(depthMap)
            println("Mostrando el mapa de profundidad")
        }
    }

    private fun generateStereogram() {
        val stereogramMat: Mat = when (actualStereogram.getTech()) {
            "RD" -> stereogramController.generateRandomDotStereogram(actualStereogram)
            "TX" -> {
                if (actualStereogram.getTexture() == null) {
                    println("Se debe Elegir una Textura")
                    return
                }
                stereogramController.generateTextureStereogram(actualStereogram)}
            //Random Dots por defecto. Por ninguna razón
            else -> stereogramController.generateRandomDotStereogram(actualStereogram)
        }
        actualStereogram.setStereogram(stereogramMat)
        depthImageView.image = matToJavaFXImage(stereogramMat)
        isShowingDots = false
        btnToggleDots.text = "Mostrar Puntos"
        btnToggleDots.isDisable = false
        sliderEyeSep.isDisable = false
        sliderFocalLen.isDisable = false
        //saveStandard(stereogramMat, "png")
    }

    private fun toggleHelperDots() {
        val baseMat = actualStereogram.getStereogramMat() ?: return
        isShowingDots = !isShowingDots

        if (isShowingDots) {
            btnToggleDots.text = "Ocultar Puntos"
            //El controlador dibuja los puntos sobre una copia
            val matWithDots = stereogramController.addHelperDots(baseMat, actualStereogram.getEyeSep())
            depthImageView.image = matToJavaFXImage(matWithDots)
        } else {
            btnToggleDots.text = "Mostrar Puntos"
            depthImageView.image = matToJavaFXImage(baseMat)
        }
    }

    //Varios
    fun cleanup() {
        if (this::deepMapController.isInitialized) {
            deepMapController.cleanup()
        }
    }

    fun matToJavaFXImage(mat: Mat): Image {
        val buffer = MatOfByte()
        Imgcodecs.imencode(".png", mat, buffer)
        return Image(ByteArrayInputStream(buffer.toArray()))
    }

    fun readImage(){
        val fileChooser = FileChooser()
        fileChooser.title = "Seleccionar Imagen Patrón"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Archivos de Imagen", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        )
        val file: File? = fileChooser.showOpenDialog(btnLoadObj.scene.window)
        if (file != null) {
            val imageMat = Imgcodecs.imread(file.absolutePath)
            if (imageMat.empty()) {
                println("Error: El archivo seleccionado no es una imagen válida")
            } else {
                actualStereogram.setTexture(imageMat)
            }
        }
    }

    fun readImage2(){
        val fileChooser = FileChooser()
        fileChooser.title = "Seleccionar Imagen Patrón"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Archivos de Imagen", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        )
        val file: File? = fileChooser.showOpenDialog(btnLoadObj.scene.window)
        if (file != null) {
            val imageMat = Imgcodecs.imread(file.absolutePath)
            if (imageMat.empty()) {
                println("Error: El archivo seleccionado no es una imagen válida")
            } else {
                sliderPattern.isDisable = false
                sliderEyeSepInv.isDisable = false
                sliderFocalLenInv.isDisable = false
                sliderPattern.value = 11.0
                sliderEyeSepInv.value = 130.0
                sliderFocalLenInv.value = 30.0
                stereogramToInvert = imageMat
                depthImageViewInvOriginal.image = matToJavaFXImage(imageMat)
                val image = stereogramController.decodeStereogram(imageMat, eyeSepInv, focalLenInv, patternSize)
                depthImageViewInverted.image = matToJavaFXImage(image)
            }
        }
    }

    fun invert(){
        stereogramToInvert?: return
        val image = stereogramController.decodeStereogram(stereogramToInvert!!, eyeSepInv, focalLenInv, patternSize)
        depthImageViewInverted.image = matToJavaFXImage(image)
    }

    private fun saveStandard(imageMat: Mat, ext: String) {
        val fileChooser = FileChooser()
        fileChooser.title = "Guardar Imagen $ext"
        fileChooser.initialFileName = "imagen_editada.$ext"
        fileChooser.initialDirectory = File(System.getProperty("user.dir"))
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter(ext.uppercase(), "*.$ext"))
        val file = fileChooser.showSaveDialog(null) ?: return
        try {
            val success = Imgcodecs.imwrite(file.absolutePath, imageMat)
            if (success) println("Guardado $ext exitoso: ${file.name}")
            else println("Error interno de OpenCV al guardar")
        } catch (e: Exception) {
            println("Error al guardar: ${e.message}")
        }
    }
}