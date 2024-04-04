package com.example.recipeapp

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.recipeapp.database.DatabaseManager
import com.example.recipeapp.model.Recipe
import java.io.File

class RecipeDetailActivity : AppCompatActivity() {

    //data
    private var recipe = Recipe()
    private lateinit var imageFile: File

    //view
    private lateinit var imageViewRecipe : ImageView
    private lateinit var recipeNameTxtView : TextView
    private lateinit var recipeTypeTxtView : TextView
    private lateinit var recipeIngredientTxtView : TextView
    private lateinit var recipeStepTxtView : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initObject()
        initView()
        initData()
    }

    /**
     * Initialization
     **/
    private fun initObject() {

    }

    private fun initView() {
        imageViewRecipe = findViewById(R.id.imageViewRecipe)
        recipeNameTxtView = findViewById(R.id.recipeNameTxtView)
        recipeTypeTxtView = findViewById(R.id.recipeTypeTxtView)
        recipeIngredientTxtView = findViewById(R.id.recipeIngredientTxtView)
        recipeStepTxtView = findViewById(R.id.recipeStepTxtView)
    }

    private fun initData() {
        // get intent value
        val intent = intent
        if(intent !=null && intent.hasExtra("recipe")){
            this.recipe = intent.extras!!.get("recipe") as Recipe

            //set data
            if(recipe.imgPath.isNotEmpty()){
                imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), recipe.imgPath)
                val imageUri = Uri.fromFile(imageFile)
                imageViewRecipe.setImageURI(imageUri)
            }
            else{
                imageViewRecipe.setImageResource(R.drawable.dummy_image)
            }


            this.recipeNameTxtView.setText(recipe.name)
            this.recipeTypeTxtView.setText(recipe.type)
            this.recipeIngredientTxtView.setText(recipe.ingredients)
            this.recipeStepTxtView.setText(recipe.steps)

            this.title = recipe.name
        }
    }

    /**
     * Navigation
     */
    override fun onBackPressed() {
        super.onBackPressed()
        this.finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}