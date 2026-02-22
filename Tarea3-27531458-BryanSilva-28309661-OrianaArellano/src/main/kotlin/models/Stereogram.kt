package models

import org.opencv.core.Mat

class Stereogram {
    private var name: String
    private lateinit var stereogram: Mat
    private lateinit var deepMap: Mat
    private var tech: String
    constructor(name: String, deepMap: Mat, stereogram: Mat, tech: String) {
        this.name = name
        this.stereogram = stereogram
        this.deepMap = deepMap
        this.tech = tech
    }
    constructor() {
        this.name = "Default"
        this.tech = "None"
    }

}
