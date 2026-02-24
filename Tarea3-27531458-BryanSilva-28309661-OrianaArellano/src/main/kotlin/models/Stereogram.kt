package models

import org.opencv.core.Mat

class Stereogram {
    private var name: String
    private var stereogram: Mat?
    private var deepMap: Mat?
    private var texture: Mat?
    private var tech: String
    private var eyeSep: Int
    private var focalLen: Int
    constructor(name: String, tech: String, eyeSep: Int, focalLen: Int) {
        this.name = name
        this.stereogram = null
        this.deepMap = null
        this.texture = null
        this.tech = tech
        this.eyeSep = eyeSep
        this.focalLen = focalLen
    }
    constructor() {
        this.name = "Default"
        this.stereogram = null
        this.deepMap = null
        this.texture = null
        this.tech = "RD"
        this.eyeSep = 130
        this.focalLen = 30
    }
    //Getters
    fun getName(): String { return name }
    fun getStereogramMat(): Mat? { return stereogram }
    fun getDeepMap(): Mat? { return deepMap }
    fun getTexture(): Mat? { return texture }
    fun getTech(): String { return tech }
    fun getEyeSep(): Int{ return eyeSep }
    fun getFocalLen(): Int { return  focalLen }
    //Setters
    fun setName(name: String) {this.name = name}
    fun setStereogram(stereogram: Mat) {this.stereogram = stereogram}
    fun setDeepMap(deepMap: Mat) {this.deepMap = deepMap}
    fun setTexture(texture: Mat) {this.texture = texture}
    fun setTech(tech: String) {this.tech = tech}
    fun setEyeSep(eyeSep: Int) {this.eyeSep = eyeSep}
    fun setFocalLen(focalLen: Int) {this.focalLen = focalLen}
}
