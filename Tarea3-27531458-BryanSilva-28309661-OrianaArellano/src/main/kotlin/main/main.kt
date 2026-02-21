package main

import javafx.application.Application
import nu.pattern.OpenCV
import org.opencv.core.Core
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil.NULL

fun main() {
    //Cargar OpenCV
    OpenCV.loadLocally()
    println("OpenCV cargado exitosamente. Versión: ${Core.VERSION}")
    //Cargar OpenGL
    initOpenGLOffscreen()
    //Lanzar JavaFX
    Application.launch(App::class.java)
}

fun initOpenGLOffscreen() {
    println("LWJGL cargado exitosamente. Versión: ${Version.getVersion()}")
    if (!glfwInit()) {
        throw IllegalStateException("No se pudo inicializar GLFW")
    }
    //Configurar el contexto
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
    //Crear una ventana
    val offscreenWindow = glfwCreateWindow(1, 1, "Offscreen Context", NULL, NULL)
    if (offscreenWindow == NULL) {
        throw RuntimeException("Fallo al crear el contexto de OpenGL")
    }
    glfwMakeContextCurrent(offscreenWindow)
    GL.createCapabilities()
    println("OpenGL cargado exitosamente. Versión: ${glGetString(GL_VERSION)}")
}