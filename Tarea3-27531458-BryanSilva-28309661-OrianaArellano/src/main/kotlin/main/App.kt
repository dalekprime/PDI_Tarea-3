package main

import controllers.BasicViewController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class App : Application() {
    override fun start(stage: Stage) {
        val sceneLoader = FXMLLoader(App::class.java.getResource("/views/BasicView.fxml"))
        val scene = Scene(sceneLoader.load(), 1200.0, 800.0)
        val controller: BasicViewController = sceneLoader.getController()
        stage.title = "Tarea3-27531458-BryanSilva-28309661-OrianaArellano"
        stage.scene = scene
        stage.icons.add(Image("visuals/icon.png"))
        stage.show()
    }
}