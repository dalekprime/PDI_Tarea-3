package main

import javafx.application.Application
import nu.pattern.OpenCV
import org.opencv.core.Core

fun main() {
    OpenCV.loadLocally()
    println("OpenCV cargado exitosamente. Versión: ${Core.VERSION}")
    Application.launch(App::class.java)
}
