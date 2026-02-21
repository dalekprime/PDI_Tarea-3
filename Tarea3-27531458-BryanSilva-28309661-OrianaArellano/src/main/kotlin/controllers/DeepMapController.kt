package controllers

import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File

class DeepMapController(private val width: Int, private val height: Int) {

    private var fboId: Int = 0
    private var colorTextureId: Int = 0
    private var depthTextureId: Int = 0

    private var previewShaderProgramId: Int = 0
    private var depthShaderProgramId: Int = 0

    private var vaoId: Int = 0
    private var vboId: Int = 0
    private var vertexCount: Int = 0

    private var rotationX: Float = 0f
    private var rotationY: Float = 0f
    private var scale: Float = 1.0f
    private lateinit var verticesArray: FloatArray
    private var translateX: Float = 0f
    private var translateY: Float = 0f

    init {
        setupFBO()
        setupShaders()
        glEnable(GL_DEPTH_TEST)
    }

    private fun setupFBO() {
        fboId = glGenFramebuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, fboId)
        colorTextureId = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, colorTextureId)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width, height, 0, GL_RGBA, GL_FLOAT, 0L)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTextureId, 0)
        depthTextureId = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, depthTextureId)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureId, 0)
        glDrawBuffer(GL_COLOR_ATTACHMENT0)
        glReadBuffer(GL_COLOR_ATTACHMENT0)
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Fallo al crear el Framebuffer")
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    private fun setupShaders() {
        val vertexShaderSource = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform mat4 MVP;
            uniform mat4 MV; 
            out float distCamara;
            void main() {
                gl_Position = MVP * vec4(aPos, 1.0);
                // Distancia real desde la cámara al vértice
                distCamara = -(MV * vec4(aPos, 1.0)).z; 
            }
        """.trimIndent()
        val previewFragSource = """
            #version 330 core
            out vec4 FragColor;
            void main() {
                FragColor = vec4(0.2, 0.6, 1.0, 1.0); 
            }
        """.trimIndent()
        val depthFragSource = """
            #version 330 core
            in float distCamara;
            uniform float nearLimit;
            uniform float farLimit;
            out vec4 FragColor;
            void main() {
                // Interpola la distancia entre 0.0 y 1.0
                float factor = clamp((distCamara - nearLimit) / (farLimit - nearLimit), 0.0, 1.0);
                
                // Invertimos: Lo que está en el 'near' (factor 0.0) se vuelve BLANCO (1.0).
                factor = 1.0 - factor;
                
                // Pintamos el píxel en escala de grises
                FragColor = vec4(factor, factor, factor, 1.0);
            }
        """.trimIndent()
        val vs = compileShader(GL_VERTEX_SHADER, vertexShaderSource)
        val fsPreview = compileShader(GL_FRAGMENT_SHADER, previewFragSource)
        val fsDepth = compileShader(GL_FRAGMENT_SHADER, depthFragSource)
        previewShaderProgramId = glCreateProgram()
        glAttachShader(previewShaderProgramId, vs)
        glAttachShader(previewShaderProgramId, fsPreview)
        glLinkProgram(previewShaderProgramId)
        depthShaderProgramId = glCreateProgram()
        glAttachShader(depthShaderProgramId, vs)
        glAttachShader(depthShaderProgramId, fsDepth)
        glLinkProgram(depthShaderProgramId)
        glDeleteShader(vs)
        glDeleteShader(fsPreview)
        glDeleteShader(fsDepth)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shaderId = glCreateShader(type)
        glShaderSource(shaderId, source)
        glCompileShader(shaderId)
        val success = IntArray(1)
        glGetShaderiv(shaderId, GL_COMPILE_STATUS, success)
        if (success[0] == GL_FALSE) {
            throw RuntimeException("Error al compilar SHADER:\n${glGetShaderInfoLog(shaderId)}")
        }
        return shaderId
    }

    fun loadOBJ(filePath: String) {
        val verticesList = mutableListOf<Float>()
        val renderVertices = mutableListOf<Float>()
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        File(filePath).forEachLine { line ->
            val tokens = line.trim().split("\\s+".toRegex())
            //Ignorar líneas vacías o comentarios
            if (tokens.isEmpty() || tokens[0].isEmpty()) return@forEachLine
            when (tokens[0]) {
                "v" -> {
                    if (tokens.size >= 4) {
                        val x = tokens[1].toFloat()
                        val y = tokens[2].toFloat()
                        val z = tokens[3].toFloat()
                        verticesList.add(x)
                        verticesList.add(y)
                        verticesList.add(z)
                        if (x < minX) minX = x; if (x > maxX) maxX = x
                        if (y < minY) minY = y; if (y > maxY) maxY = y
                        if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
                    }
                }
                "f" -> {
                    if (tokens.size < 4) return@forEachLine
                    val v0Data = tokens[1].split("/")
                    val v0Idx = v0Data[0].toInt() - 1
                    for (i in 2 until tokens.size - 1) {
                        val v1Data = tokens[i].split("/")
                        val v1Idx = v1Data[0].toInt() - 1
                        val v2Data = tokens[i+1].split("/")
                        val v2Idx = v2Data[0].toInt() - 1
                        renderVertices.add(verticesList[v0Idx * 3])
                        renderVertices.add(verticesList[v0Idx * 3 + 1])
                        renderVertices.add(verticesList[v0Idx * 3 + 2])
                        renderVertices.add(verticesList[v1Idx * 3])
                        renderVertices.add(verticesList[v1Idx * 3 + 1])
                        renderVertices.add(verticesList[v1Idx * 3 + 2])
                        renderVertices.add(verticesList[v2Idx * 3])
                        renderVertices.add(verticesList[v2Idx * 3 + 1])
                        renderVertices.add(verticesList[v2Idx * 3 + 2])
                    }
                }
            }
        }
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val centerZ = (minZ + maxZ) / 2f
        for (i in renderVertices.indices step 3) {
            renderVertices[i]     -= centerX
            renderVertices[i + 1] -= centerY
            renderVertices[i + 2] -= centerZ
        }
        verticesArray = renderVertices.toFloatArray()
        vertexCount = verticesArray.size / 3
        if (vaoId != 0) glDeleteVertexArrays(vaoId)
        if (vboId != 0) glDeleteBuffers(vboId)
        vaoId = glGenVertexArrays()
        glBindVertexArray(vaoId)
        vboId = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        val buffer = BufferUtils.createFloatBuffer(verticesArray.size)
        buffer.put(verticesArray).flip()
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L)
        glEnableVertexAttribArray(0)
        glBindVertexArray(0)
    }

    fun updateTransform(rotX: Float, rotY: Float, newScale: Float, transX: Float = 0f, transY: Float = 0f) {
        this.rotationX = rotX
        this.rotationY = rotY
        this.scale = newScale
        this.translateX = transX
        this.translateY = transY
    }

    fun renderPreview(): Mat {
        glBindFramebuffer(GL_FRAMEBUFFER, fboId)
        glViewport(0, 0, width, height)
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glUseProgram(previewShaderProgramId)
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
        val projection = Matrix4f().perspective(Math.toRadians(45.0).toFloat(), width.toFloat() / height.toFloat(), 0.1f, 100.0f)
        val view = Matrix4f().lookAt(0f, 0f, 10f, 0f, 0f, 0f, 0f, 1f, 0f)
        val model = Matrix4f()
            .translate(translateX, translateY, 0f)
            .rotateX(rotationX)
            .rotateY(rotationY)
            .scale(scale)
        val mv = Matrix4f(view).mul(model)
        val mvp = Matrix4f(projection).mul(mv)
        val mvpLoc = glGetUniformLocation(previewShaderProgramId, "MVP")
        val matrixBuffer = MemoryUtil.memAllocFloat(16)
        mvp.get(matrixBuffer)
        glUniformMatrix4fv(mvpLoc, false, matrixBuffer)
        glBindVertexArray(vaoId)
        glDrawArrays(GL_TRIANGLES, 0, vertexCount)
        glBindVertexArray(0)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        val colorBuffer = BufferUtils.createByteBuffer(width * height * 3)
        glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, colorBuffer)
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        val byteArray = ByteArray(width * height * 3)
        colorBuffer.get(byteArray)
        val colorMat = Mat(height, width, CvType.CV_8UC3)
        colorMat.put(0, 0, byteArray)
        val flippedMat = Mat()
        Core.flip(colorMat, flippedMat, 0)
        val finalMat = Mat()
        Imgproc.cvtColor(flippedMat, finalMat, Imgproc.COLOR_RGB2BGR)
        colorMat.release()
        flippedMat.release()
        MemoryUtil.memFree(matrixBuffer)
        return finalMat
    }

    fun generateDepthMap(): Mat {
        glBindFramebuffer(GL_FRAMEBUFFER, fboId)
        glViewport(0, 0, width, height)
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glUseProgram(depthShaderProgramId)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        val cameraZ = 10f
        val projection = Matrix4f().perspective(Math.toRadians(45.0).toFloat(), width.toFloat() / height.toFloat(), 0.1f, 100.0f)
        val view = Matrix4f().lookAt(0f, 0f, cameraZ, 0f, 0f, 0f, 0f, 1f, 0f)
        val model = Matrix4f()
            .translate(translateX, translateY, 0f)
            .rotateX(rotationX)
            .rotateY(rotationY)
            .scale(scale)
        val mv = Matrix4f(view).mul(model)
        val mvp = Matrix4f(projection).mul(mv)
        val mvpLoc = glGetUniformLocation(depthShaderProgramId, "MVP")
        val mvLoc = glGetUniformLocation(depthShaderProgramId, "MV")
        val matrixBuffer = MemoryUtil.memAllocFloat(16)
        mvp.get(matrixBuffer)
        glUniformMatrix4fv(mvpLoc, false, matrixBuffer)
        mv.get(matrixBuffer)
        glUniformMatrix4fv(mvLoc, false, matrixBuffer)
        var nearLimit = Float.MAX_VALUE
        var farLimit = -Float.MAX_VALUE
        for (i in verticesArray.indices step 3) {
            val x = verticesArray[i]
            val y = verticesArray[i+1]
            val z = verticesArray[i+2]
            val viewZ = mv.m02() * x + mv.m12() * y + mv.m22() * z + mv.m32()
            val dist = -viewZ
            if (dist < nearLimit) nearLimit = dist
            if (dist > farLimit) farLimit = dist
        }
        if (farLimit - nearLimit < 0.0001f) {
            farLimit = nearLimit + 0.1f
        }
        glUniform1f(glGetUniformLocation(depthShaderProgramId, "nearLimit"), nearLimit)
        glUniform1f(glGetUniformLocation(depthShaderProgramId, "farLimit"), farLimit)
        glBindVertexArray(vaoId)
        glDrawArrays(GL_TRIANGLES, 0, vertexCount)
        glBindVertexArray(0)
        val colorBuffer = BufferUtils.createByteBuffer(width * height * 3)
        glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, colorBuffer)
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        val byteArray = ByteArray(width * height * 3)
        colorBuffer.get(byteArray)
        val colorMat = Mat(height, width, CvType.CV_8UC3)
        colorMat.put(0, 0, byteArray)
        val flippedMat = Mat()
        Core.flip(colorMat, flippedMat, 0)
        val grayMat = Mat()
        Imgproc.cvtColor(flippedMat, grayMat, Imgproc.COLOR_RGB2GRAY)
        colorMat.release()
        flippedMat.release()
        MemoryUtil.memFree(matrixBuffer)
        return grayMat
    }

    fun cleanup() {
        glDeleteFramebuffers(fboId)
        glDeleteTextures(colorTextureId)
        glDeleteTextures(depthTextureId)
        glDeleteVertexArrays(vaoId)
        glDeleteBuffers(vboId)
        glDeleteProgram(previewShaderProgramId)
        glDeleteProgram(depthShaderProgramId)
    }
}