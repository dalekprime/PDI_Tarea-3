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
import javafx.scene.control.Spinner
import javafx.scene.control.ToggleGroup
import models.Stereogram
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class BasicViewController {

    //Creation
    @FXML private lateinit var btnLoadObj: Button
    @FXML private lateinit var btnLoadDepth: Button
    @FXML private lateinit var btnCaptureDepth: Button
    @FXML private lateinit var btnGenerate: Button
    @FXML private lateinit var btnLoadTexture: Button
    @FXML private lateinit var btnDownloadDepth: Button
    @FXML private lateinit var btnDownloadStereogram: Button
    @FXML private lateinit var depthImageView: ImageView
    @FXML private lateinit var techGroup: ToggleGroup
    @FXML private lateinit var sliderEyeSep: Slider
    @FXML private lateinit var sliderFocalLen: Slider
    @FXML private lateinit var sliderInterpolation: Slider
    @FXML private lateinit var lblFocalLenVal: Label
    @FXML private lateinit var lblEyeSepVal: Label
    //Inversion
    @FXML private lateinit var btnInvert: Button
    @FXML private lateinit var btnDownloadDepthInv: Button
    @FXML private lateinit var btnSBGM: RadioButton
    @FXML private lateinit var depthImageViewInvOriginal: ImageView
    @FXML private lateinit var depthImageViewInverted: ImageView
    @FXML private lateinit var sliderPattern: Slider
    @FXML private lateinit var sliderFocalLenInv: Slider
    @FXML private lateinit var sliderEyeSepInv: Slider
    private lateinit var originalInverted: Mat
    private lateinit var actualInverted: Mat
    private var patternSize: Int = 15
    private var focalLenInv: Int = 130
    private var eyeSepInv: Int = 30
    private var stereogramToInvert: Mat? = null
    @FXML private lateinit var lblPatternVal: Label
    @FXML private lateinit var lblFocalLenInvVal: Label
    @FXML private lateinit var lblEyeSepInvVal: Label
    private var invAlgorithm: Int = 0
    //Morfología
    @FXML private lateinit var imgStructuring: ImageView
    private lateinit var morphoKernel: Mat
    @FXML private lateinit var btnErode: Button
    @FXML private lateinit var btnDilate: Button
    @FXML private lateinit var btnOpen: Button
    @FXML private lateinit var btnClose: Button
    @FXML private lateinit var btnMorphoOriginal: Button
    @FXML private lateinit var spnMorpho: Spinner <Int>
    @FXML private lateinit var morphKernelGroup: ToggleGroup
    private var actualShapeMorpho: Int = 0
    private var actualSizeMorpho: Int = 3
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

    private lateinit var depthMapController: DepthMapController
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
    private var interpolationFactor: Float = 0.0f

    @FXML
    fun initialize() {
        imgStructuring.isSmooth = false
        //Crea un Estereograma Vacío
        actualStereogram = Stereogram()

        depthMapController = DepthMapController(600, 500)
        stereogramController = StereogramController()
        gameController = GameController(stereogramController)

        //Botones
        btnLoadObj.setOnAction { openAndLoadObj() }
        btnLoadDepth.setOnAction { loadDepthImage() }
        btnCaptureDepth.setOnAction { generateDeepMap() }
        btnGenerate.setOnAction {generateStereogram()}
        btnLoadTexture.setOnAction {readImage()}
        btnToggleDots.setOnAction { toggleHelperDots() }
        btnDownloadDepth.setOnAction { downloadDepth() }
        btnDownloadStereogram.setOnAction { downloadStereogram() }
        btnErode.setOnAction { erode() }
        btnDilate.setOnAction { dilate() }
        btnOpen.setOnAction { open() }
        btnClose.setOnAction { close() }
        btnMorphoOriginal.setOnAction {showOriginalInverted()}
        btnDownloadDepthInv.setOnAction {downloadDepthInv()}
        btnSBGM.selectedProperty().addListener { _, _, newValue ->
                invAlgorithm = when (invAlgorithm) {
                    0 -> 1
                    1 -> 0
                    else -> 0
                }
                invert()
        }
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
                lblEyeSepVal.text = newValue.toInt().toString()
                actualStereogram.setEyeSep(newValue.toInt())
                generateStereogram()
            }
        }
        sliderFocalLen.valueProperty().addListener { _, _, newValue ->
            if(newValue != null) {
                lblFocalLenVal.text = newValue.toInt().toString()
                actualStereogram.setFocalLen(newValue.toInt())
                generateStereogram()
            }
        }
        sliderPattern.valueProperty().addListener { _, _, newValue ->
            if(newValue != null) {
                lblPatternVal.text = newValue.toInt().toString()
                patternSize = newValue.toInt()
                invert()
            }
        }
        sliderFocalLenInv.valueProperty().addListener { _, _, newValue ->
            if(newValue != null) {
                lblFocalLenInvVal.text = newValue.toInt().toString()
                focalLenInv = newValue.toInt()
                invert()
            }
        }
        sliderEyeSepInv.valueProperty().addListener { _, _, newValue ->
            if(newValue != null) {
                lblEyeSepInvVal.text = newValue.toInt().toString()
                eyeSepInv = newValue.toInt()
                invert()
            }
        }
        sliderInterpolation.valueProperty().addListener { _, _, newValue ->
            if(newValue != null) {
                interpolationFactor = newValue.toFloat()
                interpolate()
            }
        }
        morphKernelGroup.selectedToggleProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                val op = newValue as RadioButton
                actualShapeMorpho = when (op.text) {
                    "Rect" -> 0
                    "Cruz" -> 1
                    "Elipse" -> 2
                    else -> 0
                }
                updateKernelPreview()
            }
        }
        val valueFactory = javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(3, 50, 3, 2)
        spnMorpho.valueFactory = valueFactory
        spnMorpho.valueProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                actualSizeMorpho = newValue
                updateKernelPreview()
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
                depthMapController.updateTransform(rotX, rotY, actualScale, transX, transY)
                updatePreview()
            }
        }
        //Rotación
        depthImageView.setOnScroll { event ->
            if (objLoaded) {
                val zoomFactor = if (event.deltaY > 0) 1.1f else 0.9f
                actualScale *= zoomFactor
                depthMapController.updateTransform(rotX, rotY, actualScale)
                updatePreview()
            }
        }
        gameController.setUI(
            lblGameLevel, lblGameScore, lblGameTime,
            imgGameStereogram, progressGame, btnStartGame,
            btnOption1, btnOption2, btnOption3, btnOption4,
            btnHintDots
        )
    }

    private fun openAndLoadObj() {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Archivos OBJ", "*.obj"))
        fileChooser.initialDirectory = File(System.getProperty("user.dir") + "/src/main/resources")
        val file = fileChooser.showOpenDialog(btnLoadObj.scene.window)
        if (file != null) {
            depthMapController.loadOBJ(file.absolutePath)
            rotX = 0f
            rotY = 0f
            transX = 0f
            transY = 0f
            depthMapController.updateTransform(rotX, rotY, 1.0f)
            objLoaded = true
            btnCaptureDepth.isDisable = false
            btnGenerate.isDisable = false
            btnDownloadDepth.isDisable = false
            sliderEyeSep.value = 130.0
            sliderFocalLen.value = 30.0
            updatePreview()
        }
    }

    private fun loadDepthImage(){
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Archivos de Imagen", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        )
        fileChooser.initialDirectory = File(System.getProperty("user.dir") + "/src/main/resources")
        val file = fileChooser.showOpenDialog(btnLoadDepth.scene.window)
        if (file != null) {
            val imageMat = Imgcodecs.imread(file.absolutePath, Imgcodecs.IMREAD_GRAYSCALE)
            if (imageMat.empty()) {
                println("Error: El archivo seleccionado no es una imagen válida")
            } else {
                objLoaded = true
                btnGenerate.isDisable = false
                sliderEyeSep.value = 130.0
                sliderFocalLen.value = 30.0
                actualStereogram.setDeepMap(imageMat)
            }
        }
    }

    //Wireframe
    private fun updatePreview() {
        val previewMat = depthMapController.renderPreview()
        depthImageView.image = matToJavaFXImage(previewMat)
        val depthMap = depthMapController.generateDepthMap()
        actualStereogram.setDeepMap(depthMap)
    }

    //Muestra el mapa de profundidad
    private fun generateDeepMap() {
        if (objLoaded) {
            val depthMap = depthMapController.generateDepthMap()
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
        btnDownloadStereogram.isDisable = false
        sliderEyeSep.isDisable = false
        sliderFocalLen.isDisable = false
        sliderInterpolation.isDisable = false
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
        if (this::depthMapController.isInitialized) {
            depthMapController.cleanup()
        }
    }

    fun matToJavaFXImage(mat: Mat): Image {
        val buffer = MatOfByte()
        Imgcodecs.imencode(".png", mat, buffer)
        return Image(ByteArrayInputStream(buffer.toArray()))
    }

    private fun readImage(){
        val fileChooser = FileChooser()
        fileChooser.title = "Seleccionar Imagen Patrón"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Archivos de Imagen", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        )
        fileChooser.initialDirectory = File(System.getProperty("user.dir") + "/src/main/resources")
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

    private fun readImage2(){
        val fileChooser = FileChooser()
        fileChooser.title = "Seleccionar Imagen Patrón"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Archivos de Imagen", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        )
        fileChooser.initialDirectory = File(System.getProperty("user.dir") + "/src/main/resources")
        val file: File? = fileChooser.showOpenDialog(btnLoadObj.scene.window)
        if (file != null) {
            val imageMat = Imgcodecs.imread(file.absolutePath)
            if (imageMat.empty()) {
                println("Error: El archivo seleccionado no es una imagen válida")
            } else {
                sliderPattern.isDisable = false
                sliderEyeSepInv.isDisable = false
                sliderFocalLenInv.isDisable = false
                btnErode.isDisable = false
                btnDilate.isDisable = false
                btnOpen.isDisable = false
                btnClose.isDisable = false
                btnMorphoOriginal.isDisable = false
                btnDownloadDepthInv.isDisable = false
                btnSBGM.isDisable = false
                spnMorpho.isDisable = false
                sliderPattern.value = 15.0
                sliderEyeSepInv.value = 130.0
                sliderFocalLenInv.value = 30.0
                stereogramToInvert = imageMat
                depthImageViewInvOriginal.image = matToJavaFXImage(imageMat)
                val image = when (invAlgorithm){
                    0 -> stereogramController.decodeStereogram(stereogramToInvert!!, eyeSepInv, focalLenInv, patternSize)
                    1 -> stereogramController.decodeStereogramSGBM(stereogramToInvert!!, eyeSepInv, focalLenInv, patternSize)
                    else -> stereogramController.decodeStereogram(stereogramToInvert!!, eyeSepInv, focalLenInv, patternSize)
                }
                actualInverted = image
                originalInverted = image
                depthImageViewInverted.image = matToJavaFXImage(image)
            }
        }
    }

    private fun invert(){
        stereogramToInvert?: return
        val image = when (invAlgorithm){
            0 -> stereogramController.decodeStereogram(stereogramToInvert!!, eyeSepInv, focalLenInv, patternSize)
            1 -> stereogramController.decodeStereogramSGBM(stereogramToInvert!!, eyeSepInv, focalLenInv, patternSize)
            else -> stereogramController.decodeStereogram(stereogramToInvert!!, eyeSepInv, focalLenInv, patternSize)
        }
        depthImageViewInverted.image = matToJavaFXImage(image)
        actualInverted = image
        originalInverted = image
    }

    private fun downloadDepth(){
        actualStereogram.getDeepMap()?: return
        saveStandard(actualStereogram.getDeepMap()!!, "png")
    }

    private fun downloadStereogram(){
        actualStereogram.getStereogramMat()?: return
        saveStandard(actualStereogram.getStereogramMat()!!, "png")
    }

    private fun downloadDepthInv(){
        saveStandard(actualInverted, "png")
    }

    private fun saveStandard(imageMat: Mat, ext: String) {
        val fileChooser = FileChooser()
        fileChooser.title = "Guardar Imagen $ext"
        fileChooser.initialFileName = "defaultName.$ext"
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

    private fun interpolate(){
       val result =  stereogramController.interpolate(actualStereogram, interpolationFactor)
        depthImageView.image = matToJavaFXImage(result)
    }


    fun erode() {
        updateKernelPreview()
        val result = applyMorphology(actualInverted, Imgproc.MORPH_ERODE, morphoKernel)
        actualInverted = result
        depthImageViewInverted.image = matToJavaFXImage(actualInverted)
    }

    fun dilate() {
        updateKernelPreview()
        val result = applyMorphology(actualInverted, Imgproc.MORPH_DILATE, morphoKernel)
        actualInverted = result
        depthImageViewInverted.image = matToJavaFXImage(actualInverted)
    }

    fun open() {
        updateKernelPreview()
        val result = applyMorphology(actualInverted, Imgproc.MORPH_OPEN, morphoKernel)
        actualInverted = result
        depthImageViewInverted.image = matToJavaFXImage(actualInverted)
    }

    fun close() {
        updateKernelPreview()
        val result = applyMorphology(actualInverted, Imgproc.MORPH_CLOSE, morphoKernel)
        actualInverted = result
        depthImageViewInverted.image = matToJavaFXImage(actualInverted)
    }

    private fun applyMorphology(depthMap: Mat, operation: Int, kernel: Mat): Mat {
        val dest = Mat()
        Imgproc.morphologyEx(depthMap, dest, operation, kernel)
        return dest
    }

    fun createStructuringElement(shapeType: Int, size: Int): Mat {
        val shape = when (shapeType) {
            0 -> Imgproc.MORPH_RECT
            1 -> Imgproc.MORPH_CROSS
            2 -> Imgproc.MORPH_ELLIPSE
            else -> Imgproc.MORPH_RECT
        }
        val finalSize = if (size % 2 == 0) size + 1 else size
        return Imgproc.getStructuringElement(shape, Size(finalSize.toDouble(), finalSize.toDouble()))
    }

    private fun updateKernelPreview() {
        morphoKernel = createStructuringElement(actualShapeMorpho, actualSizeMorpho)
        val displayKernel = Mat()
        org.opencv.core.Core.multiply(morphoKernel, org.opencv.core.Scalar(255.0), displayKernel)
        val resizedKernel = Mat()
        Imgproc.resize(
            displayKernel,
            resizedKernel,
            Size(50.0, 50.0),
            0.0,
            0.0,
            Imgproc.INTER_NEAREST
        )
        imgStructuring.image = matToJavaFXImage(resizedKernel)
        displayKernel.release()
        resizedKernel.release()
    }

    private fun showOriginalInverted(){
        depthImageViewInverted.image = matToJavaFXImage(originalInverted)
        actualInverted = originalInverted
    }
}