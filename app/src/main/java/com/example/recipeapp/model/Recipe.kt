package com.example.recipeapp.model

import java.io.Serializable

class Recipe : Serializable {
    var id = 0
    var name = ""
    var imgPath = ""
    var steps = ""
    var ingredients = ""
    var type = ""
}