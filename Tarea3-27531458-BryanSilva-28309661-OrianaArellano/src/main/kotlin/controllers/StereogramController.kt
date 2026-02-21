package controllers

import org.opencv.core.CvType
import org.opencv.core.Mat
import kotlin.random.Random

class StereogramController {
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
}