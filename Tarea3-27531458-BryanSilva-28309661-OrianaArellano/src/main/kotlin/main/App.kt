package main

import controllers.BasicViewController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*

class App : Application() {
    private lateinit var controller: BasicViewController

    override fun start(stage: Stage) {
        //Vincular OpenGL al hilo de JavaFX
        glfwMakeContextCurrent(offscreenWindow)
        GL.createCapabilities()
        println("OpenGL vinculado al hilo de JavaFX. Versión: ${glGetString(GL_VERSION)}")

        //Cargar tu interfaz
        val sceneLoader = FXMLLoader(App::class.java.getResource("/views/BasicView.fxml"))
        val scene = Scene(sceneLoader.load(), 1200.0, 800.0)
        controller = sceneLoader.getController()
        stage.title = "Tarea3-27531458-BryanSilva-28309661-OrianaArellano"
        stage.scene = scene
        stage.icons.add(Image("visuals/icon.png"))
        stage.show()
    }

    override fun stop() {
        //Limpiamos los recursos
        if (this::controller.isInitialized) {
            controller.cleanup()
        }
        //Destruir la ventana oculta de OpenGL al cerrar
        glfwDestroyWindow(offscreenWindow)
        glfwTerminate()
        super.stop()
    }

}

