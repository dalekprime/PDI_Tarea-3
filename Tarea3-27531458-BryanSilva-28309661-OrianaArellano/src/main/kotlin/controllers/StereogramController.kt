package controllers

import models.Stereogram
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.random.Random
import org.opencv.core.Point
import org.opencv.calib3d.StereoSGBM


class StereogramController {
    fun generateRandomDotBase(width: Int, height: Int): Mat {
        val result = Mat(height, width, CvType.CV_8UC3)
        val resultData = ByteArray(width * height * 3)
        for (i in resultData.indices step 3) {
            resultData[i] = Random.nextInt(256).toByte()
            resultData[i + 1] = Random.nextInt(256).toByte()
            resultData[i + 2] = Random.nextInt(256).toByte()
        }
        result.put(0, 0, resultData)
        return result
    }

    fun generateRandomDotStereogram(stereogram: Stereogram): Mat {
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
        stereogram.getDeepMap()!!.get(0, 0, depthMapData)
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

    fun generateTextureStereogram(stereogram: Stereogram): Mat {
        val depthMap = stereogram.getDeepMap() ?: return Mat()
        val texture = stereogram.getTexture() ?: return Mat()
        val width = depthMap.cols()
        val height = depthMap.rows()
        val eyeSep = stereogram.getEyeSep()
        val focalLen = stereogram.getFocalLen()
        val texWidth = texture.cols()
        val texHeight = texture.rows()
        val result = Mat(height, width, CvType.CV_8UC3)
        val resultData = ByteArray(width * height * 3)
        val depthMapData = ByteArray(width * height)
        depthMap.get(0, 0, depthMapData)
        val baseData = ByteArray(texWidth * texHeight * 3)
        texture.get(0, 0, baseData)
        for (y in 0 until height) {
            val same = IntArray(width) { it }
            //Cálculo de Disparidad según Mapa de Profundidad
            for (x in 0 until width) {
                val z = depthMapData[y * width + x].toInt() and 0xFF
                val sep = eyeSep - (z * focalLen / 255.0)
                val left = Math.round(x - sep / 2.0).toInt()
                val right = Math.round(left + sep).toInt()
                if (left >= 0 && right < width) {
                    var rootLeft = left
                    while (same[rootLeft] != rootLeft) rootLeft = same[rootLeft]
                    var rootRight = right
                    while (same[rootRight] != rootRight) rootRight = same[rootRight]
                    if (rootLeft != rootRight) {
                        if (rootLeft < rootRight) same[rootRight] = rootLeft
                        else same[rootLeft] = rootRight
                    }
                }
            }
            //Escritura en la Imagen
            for (x in 0 until width) {
                var match = x
                while (same[match] != match) match = same[match]
                val patX = match % texWidth
                val patY = y % texHeight
                val target = (y * width + x) * 3
                val source = (patY * texWidth + patX) * 3
                resultData[target] = baseData[source]
                resultData[target + 1] = baseData[source + 1]
                resultData[target + 2] = baseData[source + 2]
            }
        }
        result.put(0, 0, resultData)
        return result
    }

    fun decodeStereogram(stereogramMat: Mat, eyeSep: Int, maxDepth: Int, windowSize: Int): Mat {
        if (stereogramMat.empty()) {
            println("Error: No hay estereograma cargado")
            return Mat()
        }
        val width = stereogramMat.cols()
        val height = stereogramMat.rows()
        //Trabajamos con Escala de Gris
        val grayMat = Mat()
        Imgproc.cvtColor(stereogramMat, grayMat, Imgproc.COLOR_BGR2GRAY)
        //Almacenan el mínimo error posible en la revision, y el desplazamiento óptimo
        val minErrorMat = Mat(height, width, CvType.CV_32F, Scalar(Float.MAX_VALUE.toDouble()))
        val bestMatchMat = Mat(height, width, CvType.CV_8U, Scalar(0.0))
        val minSearch = Math.max(1, eyeSep - maxDepth)
        val maxSearch = eyeSep
        val safeWindowSize = Math.max(1, windowSize)
        val blurSize = Size(safeWindowSize.toDouble(), safeWindowSize.toDouble())
        //Separamos la imagen en dos ventanas, Izquierda y derecha
        for (d in minSearch..maxSearch) {
            val roiOriginal = Rect(0, 0, width - d, height)
            val roiShifted = Rect(d, 0, width - d, height)
            val imgOriginal = Mat(grayMat, roiOriginal)
            val imgShifted = Mat(grayMat, roiShifted)
            val diff = Mat()
            Core.absdiff(imgOriginal, imgShifted, diff)
            diff.convertTo(diff, CvType.CV_32F)
            //Blur para comparar rápidamente las diferencias
            val diffBlurred = Mat()
            Imgproc.blur(diff, diffBlurred, blurSize)
            val mask = Mat()
            val minErrorRoi = Mat(minErrorMat, roiOriginal)
            Core.compare(diffBlurred, minErrorRoi, mask, Core.CMP_LT)
            diffBlurred.copyTo(minErrorRoi, mask)
            val bestMatchRoi = Mat(bestMatchMat, roiOriginal)
            val dMat = Mat(bestMatchRoi.size(), CvType.CV_8U, Scalar(d.toDouble()))
            dMat.copyTo(bestMatchRoi, mask)
            imgOriginal.release(); imgShifted.release(); diff.release()
            diffBlurred.release(); mask.release(); dMat.release()
        }
        val resultData = ByteArray(width * height)
        bestMatchMat.get(0, 0, resultData)
        val depthData = ByteArray(width * height)
        val range = Math.max(1, maxSearch - minSearch)
        for (i in resultData.indices) {
            val d = resultData[i].toInt() and 0xFF
            if (d > 0) {
                val depthValue = 255 - ((d - minSearch) * 255 / range)
                depthData[i] = depthValue.toByte()
            } else {
                depthData[i] = 0
            }
        }
        val depthMap = Mat(height, width, CvType.CV_8U)
        depthMap.put(0, 0, depthData)
        val cleanMap = Mat()
        Imgproc.medianBlur(depthMap, cleanMap, 5)
        Imgproc.equalizeHist(cleanMap, cleanMap)
        grayMat.release()
        minErrorMat.release()
        bestMatchMat.release()
        depthMap.release()
        return cleanMap
    }

    fun addHelperDots(baseMat: Mat, eyeSep: Int): Mat {
        val resultWithDots = baseMat.clone()
        val width = resultWithDots.width()
        val yPos = 20.0
        val dot1X = (width / 2.0) - (eyeSep / 2.0)
        val dot2X = (width / 2.0) + (eyeSep / 2.0)
        val radius = 10

        Imgproc.circle(resultWithDots, Point(dot1X, yPos), radius,
            Scalar(192.0, 192.0, 192.0), -1)

        Imgproc.circle(resultWithDots, Point(dot2X, yPos), radius,
            Scalar(255.0, 255.0, 255.0), -1)

        Imgproc.circle(resultWithDots, Point(dot1X, yPos), radius,
            Scalar(0.0, 0.0, 0.0), 2)

        Imgproc.circle(resultWithDots, Point(dot2X, yPos), radius,
            Scalar(0.0, 0.0, 0.0), 2)

        return resultWithDots
    }

    fun interpolate(stereogram: Stereogram, t:Float): Mat {
        val depthMap = stereogram.getDeepMap()
        val stereogramMat = stereogram.getStereogramMat()
        val width = depthMap!!.width()
        val height = depthMap.height()
        val result = Mat(height, width, CvType.CV_8UC3)
        val depthData = ByteArray(width * height)
        val stereogramData = ByteArray(width * height * 3)
        stereogramMat!!.get(0, 0, stereogramData)
        depthMap.get(0, 0, depthData)
        val resultData = ByteArray(width * height * 3)
        for (i in depthData.indices) {
            val z = depthData[i].toInt() and 0xFF
            val step = i * 3
            resultData[step] = (stereogramData[step] * t + (1 - t) * z).toInt().toByte()
            resultData[step+1] = (stereogramData[step+1] * t + (1 - t) * z).toInt().toByte()
            resultData[step+2] = (stereogramData[step+2] * t + (1 - t) * z).toInt().toByte()
        }
        result.put(0, 0, resultData)
        return result
    }
    fun decodeStereogramSGBM(stereogramMat: Mat, eyeSep: Int, maxDepth: Int, windowSize: Int): Mat {
        if (stereogramMat.empty()) {
            println("Error: No hay estereograma cargado")
            return Mat()
        }
        val grayMat = Mat()
        Imgproc.cvtColor(stereogramMat, grayMat, Imgproc.COLOR_BGR2GRAY)
        val minSearch = Math.max(0, eyeSep - maxDepth)
        var numDisparities = Math.max(16, eyeSep - minSearch)
        if (numDisparities % 16 != 0) {
            numDisparities += (16 - (numDisparities % 16))
        }
        var safeWindowSize = if (windowSize % 2 == 0) windowSize + 1 else windowSize
        safeWindowSize = Math.max(3, safeWindowSize)
        val sgbm = StereoSGBM.create(
            minSearch, numDisparities, safeWindowSize,
            8 * grayMat.channels() * safeWindowSize * safeWindowSize,
            32 * grayMat.channels() * safeWindowSize * safeWindowSize,
            1, 63, 10, 100, 1, StereoSGBM.MODE_SGBM
        )
        val dispMat = Mat()
        sgbm.compute(grayMat, grayMat, dispMat)
        val resultMat = Mat()
        Core.normalize(dispMat, resultMat, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)
        val cleanMat = Mat()
        Imgproc.medianBlur(resultMat, cleanMat, 5)
        Core.bitwise_not(cleanMat, cleanMat)
        grayMat.release()
        dispMat.release()
        resultMat.release()
        return cleanMat
    }

}