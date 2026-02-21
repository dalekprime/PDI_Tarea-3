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

class BasicViewController {

    @FXML private lateinit var btnLoadObj: Button
    @FXML private lateinit var btnCaptureDepth: Button
    @FXML private lateinit var btnGenerate: Button
    @FXML private lateinit var depthImageView: ImageView

    private lateinit var deepMapController: DeepMapController
    private lateinit var stereogramController: StereogramController

    private var rotX = 0f
    private var rotY = 0f
    private var transX = 0f
    private var transY = 0f
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var objLoaded = false

    private var escalaActual = 1.0f

    @FXML
    fun initialize() {
        deepMapController = DeepMapController(600, 500)
        stereogramController = StereogramController()
        //Botón para cargar
        btnLoadObj.setOnAction { openAndLoadObj() }

        //Botón para ver el mapa de profundidad
        btnCaptureDepth.setOnAction { generarMapaFinal() }

        btnGenerate.setOnAction {generarEstereograma()}

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
                deepMapController.updateTransform(rotX, rotY, escalaActual, transX, transY)
                actualizarPrevisualizacion()
            }
        }
        //Rotación
        depthImageView.setOnScroll { event ->
            if (objLoaded) {
                val zoomFactor = if (event.deltaY > 0) 1.1f else 0.9f
                escalaActual *= zoomFactor
                deepMapController.updateTransform(rotX, rotY, escalaActual)
                actualizarPrevisualizacion()
            }
        }
    }

    private fun openAndLoadObj() {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Archivos OBJ", "*.obj"))
        val file = fileChooser.showOpenDialog(btnLoadObj.scene.window)
        if (file != null) {
            deepMapController.loadOBJ(file.absolutePath)
            rotX = 0f
            rotY = 0f
            transX = 0f
            transY = 0f
            deepMapController.updateTransform(rotX, rotY, 1.0f)
            objLoaded = true
            btnCaptureDepth.isDisable = false
            actualizarPrevisualizacion()
        }
    }

    //Wireframe
    private fun actualizarPrevisualizacion() {
        val previewMat = deepMapController.renderPreview()
        depthImageView.image = matToJavaFXImage(previewMat)
    }

    //Muestra el mapa de profundidad
    private fun generarMapaFinal() {
        if (objLoaded) {
            val depthMat = deepMapController.generateDepthMap()
            depthImageView.image = matToJavaFXImage(depthMat)
            println("Mostrando el mapa de profundidad final.")
        }
    }

    private fun generarEstereograma() {
        if (!objLoaded) return
        val depthMap = deepMapController.generateDepthMap()
        val anchoPatron = 120
        val profundidadMax = 30 
        val stereogramMat = stereogramController.generate(depthMap, anchoPatron, profundidadMax, null)
        depthImageView.image = matToJavaFXImage(stereogramMat)
        depthMap.release()
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
}