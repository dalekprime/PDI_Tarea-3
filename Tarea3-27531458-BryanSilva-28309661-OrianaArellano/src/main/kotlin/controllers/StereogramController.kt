package controllers

import models.Stereogram
import org.opencv.core.CvType
import org.opencv.core.Mat
import kotlin.random.Random
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class StereogramController {
    fun generateRandomDotBase(width: Int, height: Int): Mat{
        val result = Mat(height, width, CvType.CV_8UC3)
        val resultData = ByteArray(width * height * 3)
        for (i in resultData.indices step 3) {
            resultData[i] = Random.nextInt(256).toByte()
            resultData[i+1] = Random.nextInt(256).toByte()
            resultData[i+2] = Random.nextInt(256).toByte()
        }
        result.put(0, 0, resultData)
        return result
    }

    fun generateRandomDotStereogram(stereogram: Stereogram): Mat{
        if (stereogram.getDeepMap() == null) {
            println("No hay Mapa de Profundidad")
            return Mat()
        }
        val width = stereogram.getDeepMap()!!.width()
        val height = stereogram.getDeepMap()!!.height()
        val eyeSep = stereogram.getEyeSep()
        val focalLen = stereogram.getFocalLen()
        val result = Mat(height, width, CvType.CV_8UC3)
        val resultData = ByteArray(width * height * 3)
        val depthMapData = ByteArray(width * height)
        stereogram.getDeepMap()!!.get(0,0, depthMapData)
        val base = generateRandomDotBase(width, height)
        val baseData = ByteArray(base.width() * base.height() * 3)
        base.get(0, 0, baseData)
        for (y in 0 until height) {
            val same = IntArray(width)
            for (x in 0 until width) {
                same[x] = x
            }
            //Cálculo de Disparidad según Mapa de Profundidad
            for (x in 0 until width) {
                val z = depthMapData[y * width + x].toInt() and 0xFF
                val separation = eyeSep - (z * focalLen / 255)
                val left = x - (separation / 2)
                val right = x + (separation / 2)
                if (left >= 0 && right < width) {
                    if (same[right] > left) {
                        same[right] = left
                    }
                }
            }
            //Escritura en la Imagen
            for (x in 0 until width) {
                var match = same[x]
                while (same[match] != match) {
                    match = same[match]
                }
                val target = (y * width + x) * 3
                val source = (y * width + match) * 3
                resultData[target] = baseData[source]
                resultData[target + 1] = baseData[source + 1]
                resultData[target + 2] = baseData[source + 2]
            }

        }
        result.put(0, 0, resultData)
        return result
    }

    fun generateTextureStereogram(stereogram: Stereogram): Mat{
        if (stereogram.getDeepMap() == null) {
            println("No hay Mapa de Profundidad")
            return Mat()
        }
        if (stereogram.getTexture() == null) {
            println("No hay Textura Cargada")
            return Mat()
        }
        val width = stereogram.getDeepMap()!!.width()
        val height = stereogram.getDeepMap()!!.height()
        val eyeSep = stereogram.getEyeSep()
        val focalLen = stereogram.getFocalLen()
        val texWidth = stereogram.getTexture()!!.width()
        val texHeight = stereogram.getTexture()!!.height()
        val result = Mat(height, width, CvType.CV_8UC3)
        val resultData = ByteArray(width * height * 3)
        val depthMapData = ByteArray(width * height)
        stereogram.getDeepMap()!!.get(0,0, depthMapData)
        val baseData = ByteArray(texWidth * texHeight * 3)
        stereogram.getTexture()!!.get(0, 0, baseData)
        for (y in 0 until height) {
            val same = IntArray(width)
            for (x in 0 until width) {
                same[x] = x
            }
            //Cálculo de Disparidad según Mapa de Profundidad
            for (x in 0 until width) {
                val z = depthMapData[y * width + x].toInt() and 0xFF
                val separation = eyeSep - (z * focalLen / 255)
                val left = x - (separation / 2)
                val right = x + (separation / 2)
                if (left >= 0 && right < width) {
                    if (same[right] > left) {
                        same[right] = left
                    }
                }
            }
            //Escritura en la Imagen
            for (x in 0 until width) {
                var match = same[x]
                while (same[match] != match) {
                    match = same[match]
                }
                //Módulo por si acaso la imagen es más pequeña que el mapa de profundidad
                val patX = match % texWidth
                val patY = y % texHeight
                val target = (y * width + x) * 3
                val source = (patY * texWidth + patX) * 3
                resultData[target]     = baseData[source]
                resultData[target + 1] = baseData[source + 1]
                resultData[target + 2] = baseData[source + 2]
            }
        }
        result.put(0, 0, resultData)
        return result
    }
    fun generate(depthMap: Mat, patternWidth: Int, maxDepth: Int, patternTexture: Mat? = null): Mat {
        val width = depthMap.cols()
        val height = depthMap.rows()
        val result = Mat(height, width, CvType.CV_8UC3)
        val depthData = ByteArray(width * height)
        depthMap.get(0, 0, depthData)
        val resultData = ByteArray(width * height * 3)
        var textureData: ByteArray? = null
        var texWidth = 0
        var texHeight = 0
        if (patternTexture != null && !patternTexture.empty()) {
            texWidth = patternTexture.cols()
            texHeight = patternTexture.rows()
            textureData = ByteArray(texWidth * texHeight * patternTexture.channels())
            patternTexture.get(0, 0, textureData)
        }
        for (y in 0 until height) {
            val same = IntArray(width) { it }
            for (x in 0 until width) {
                val z = depthData[y * width + x].toInt() and 0xFF
                val separation = patternWidth - (z * maxDepth / 255)
                val left = x - (separation / 2)
                val right = left + separation
                if (left >= 0 && right < width) {
                    var l = left
                    while (same[l] != l) l = same[l]
                    var r = right
                    while (same[r] != r) r = same[r]
                    if (l != r) {
                        if (l < r) same[r] = l else same[l] = r
                    }
                }
            }
            val rowColors = IntArray(width)
            for (x in 0 until width) {
                var root = x
                while (same[root] != root) root = same[root]
                if (root == x) {
                    if (textureData != null && patternTexture != null) {
                        val texX = x % texWidth
                        val texY = y % texHeight
                        val texChannels = patternTexture.channels()
                        val texIdx = (texY * texWidth + texX) * texChannels
                        if (texIdx + 2 < textureData.size) {
                            val b = textureData[texIdx].toInt() and 0xFF
                            val g = textureData[texIdx + 1].toInt() and 0xFF
                            val r = textureData[texIdx + 2].toInt() and 0xFF
                            rowColors[x] = (r shl 16) or (g shl 8) or b
                        }
                    } else {
                        val r = Random.nextInt(256)
                        val g = Random.nextInt(256)
                        val b = Random.nextInt(256)
                        rowColors[x] = (r shl 16) or (g shl 8) or b
                    }
                } else {
                    rowColors[x] = rowColors[root]
                }
                val color = rowColors[x]
                val idx = (y * width + x) * 3
                resultData[idx] = (color and 0xFF).toByte()
                resultData[idx + 1] = ((color shr 8) and 0xFF).toByte()
                resultData[idx + 2] = ((color shr 16) and 0xFF).toByte()
            }
        }
        result.put(0, 0, resultData)
        return result
    }

    fun addHelperDots(baseMat: Mat, eyeSep: Int): Mat {
        val resultWithDots = baseMat.clone()
        val width = resultWithDots.width()

        val yPos = 20.0
        val dot1X = (width / 2.0) - (eyeSep / 2.0)
        val dot2X = (width / 2.0) + (eyeSep / 2.0)
        val radius = 10

        Imgproc.circle(resultWithDots, Point(dot1X, yPos), radius, Scalar(192.0, 192.0, 192.0), -1)
        Imgproc.circle(resultWithDots, Point(dot2X, yPos), radius, Scalar(255.0, 255.0, 255.0), -1)
        Imgproc.circle(resultWithDots, Point(dot1X, yPos), radius, Scalar(0.0, 0.0, 0.0), 2)
        Imgproc.circle(resultWithDots, Point(dot2X, yPos), radius, Scalar(0.0, 0.0, 0.0), 2)

        return resultWithDots
    }
}