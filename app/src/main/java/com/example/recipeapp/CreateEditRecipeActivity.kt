package com.example.recipeapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.recipeapp.database.DatabaseManager
import com.example.recipeapp.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class CreateEditRecipeActivity : AppCompatActivity() {

    //object
    private lateinit var db : DatabaseManager

    //data
    private var recipe = Recipe()
    private var type = 0
    private var recipeTypeList = ArrayList<String>()
    private val REQUEST_PERMISSION_CODE = 101
    private val REQUEST_GALLERY_CODE = 102
    private var isUpdate = false
    private lateinit var imageFile: File
    private var isDelete = false

    //view
    private lateinit var imageViewRecipe : ImageView
    private lateinit var editTextRecipeName : EditText
    private lateinit var editTextIngredients : EditText
    private lateinit var editTextSteps : EditText
    private lateinit var createOrUpdateBtn : Button
    private lateinit var addImageBtn : Button
    private lateinit var recipeTypeSpinner : Spinner


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GALLERY_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImage: Uri? = data.data
            selectedImage?.let {
                imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${recipe.id}.jpg")
                contentResolver.openInputStream(selectedImage).use { inputStream ->
                    imageFile.outputStream().use { outputStream ->
                        inputStream?.copyTo(outputStream)
                        recipe.imgPath="${recipe.id}.jpg"
                    }
                }
                imageViewRecipe.setImageURI(selectedImage)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_recipe)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        this.title = "Create New Recipe"

        this.initObject()
        this.initView()
        this.initData()
    }

    /**
     * Initialization
     **/
    private fun initObject() {
        this.db = DatabaseManager.getInstance(this)
    }

    private fun initView() {
        this.imageViewRecipe = findViewById(R.id.imageViewRecipe)
        this.editTextRecipeName = findViewById(R.id.editTextRecipeName)
        this.editTextIngredients = findViewById(R.id.editTextIngredients)
        this.editTextSteps = findViewById(R.id.editTextSteps)
        this.createOrUpdateBtn = findViewById(R.id.createOrUpdateBtn)
        this.addImageBtn = findViewById(R.id.addImageBtn)

        //spinner
        this.recipeTypeSpinner = findViewById(R.id.recipeTypeSpinner)

        ArrayAdapter.createFromResource(
            this,
            R.array.recipe_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            recipeTypeSpinner.adapter = adapter
        }

        setupOnclick()
    }

    private fun initData() {
        val resourcesArray = resources.getStringArray(R.array.recipe_types)
        this.recipeTypeList = ArrayList(resourcesArray.toList())

        // get intent value
        val intent = intent
        if(intent !=null && intent.hasExtra("recipe")){
            this.recipe = intent.extras!!.get("recipe") as Recipe

            //set data
            imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), recipe.imgPath)
            val imageUri = Uri.fromFile(imageFile)
            imageViewRecipe.setImageURI(imageUri)

            this.editTextRecipeName.setText(recipe.name)
            this.editTextIngredients.setText(recipe.ingredients)
            this.editTextSteps.setText(recipe.steps)

            //get type position
            var count = 0
            for( t in recipeTypeList){
                if(t == recipe.type){
                    type = count
                    break
                }
                count += 1
            }

            recipeTypeSpinner.setSelection(type)

            this.createOrUpdateBtn.setText("Update")
            isUpdate = true
        }
        else{
            recipe.id = db.getLastInsertedId()
        }
    }

    private fun setupOnclick(){
        this.createOrUpdateBtn.setOnClickListener {
            if(isUpdate) updateRecipeData()
            else insertRecipeData()
        }

        this.addImageBtn.setOnClickListener {
            showOptionsDialog()
        }

        recipeTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                type = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    /**
     * Image Function
     **/
    private fun showOptionsDialog() {
        val options = arrayOf("Upload Image", "Delete Image")
        AlertDialog.Builder(this)
            .setTitle("Choose Action")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        if (hasStoragePermission()) {
                            openGallery()
                        } else {
                            requestStoragePermission()
                        }
                    }
                    1 -> {
                        isDelete = true
                        imageViewRecipe.setImageResource(R.drawable.dummy_image)
                    }
                }
            }
            .show()
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        var permission : Array<String>
        if (Build.VERSION.SDK_INT >= 33) {
            permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        }
        else {
            permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        ActivityCompat.requestPermissions(
            this,
           permission,
            REQUEST_PERMISSION_CODE
        )
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, REQUEST_GALLERY_CODE)
    }

    private fun deleteImage() {
        if (imageFile.exists()) {
            recipe.imgPath = ""
            imageFile.delete()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            }
        }
    }


    /**
     * Image Function
     **/

    private fun getDataFromEditText(){
        recipe.steps = editTextSteps.text.toString()
        recipe.name = editTextRecipeName.text.toString()
        recipe.ingredients = editTextIngredients.text.toString()
        recipe.type = recipeTypeList[type]
    }

    private fun insertRecipeData(){
        // show progress dialog
        val pg = ProgressDialog(this@CreateEditRecipeActivity)
        pg.setCancelable(false)
        pg.setCanceledOnTouchOutside(false)
        pg.setMessage("Loading....")
        pg.show()

        // generate data in background
        GlobalScope.launch(Dispatchers.Main) {
            // delay
            delay(200)

            withContext(Dispatchers.IO) {

                getDataFromEditText()
                db.insertRecipe(recipe)
            }

            // dismiss progress dialog
            pg.dismiss()
        }

        val returnIntent = Intent()
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    private fun updateRecipeData(){
        // show progress dialog
        val pg = ProgressDialog(this@CreateEditRecipeActivity)
        pg.setCancelable(false)
        pg.setCanceledOnTouchOutside(false)
        pg.setMessage("Loading....")
        pg.show()

        // generate data in background
        GlobalScope.launch(Dispatchers.Main) {
            // delay
            delay(200)

            withContext(Dispatchers.IO) {

                if(isDelete) deleteImage()
                getDataFromEditText()

                val updatedValues = ContentValues().apply {
                    put("img_path", recipe.imgPath)
                    put("name", recipe.name)
                    put("type", recipe.type)
                    put("ingredients",recipe.ingredients)
                    put("steps",recipe.steps)
                }

                db.updateRecipe(recipe.id,updatedValues)
            }

            // dismiss progress dialog
            pg.dismiss()
        }
        val returnIntent = Intent()
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
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