package com.example.recipeapp

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.database.DatabaseManager
import com.example.recipeapp.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

const val END_EDIT_CREATE_REQUEST = 1

class MainActivity : AppCompatActivity() {

    //object
    private lateinit var db : DatabaseManager

    //data
    private var recipeList = ArrayList<Recipe>()
    private var whereQuery = ""
    var recipeTypeList = ArrayList<String>()
    private var filterBy = ""

    //view
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var recipeRecView : RecyclerView


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == END_EDIT_CREATE_REQUEST) {
            if (resultCode == RESULT_OK) {
                getRecipeData()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initObject()
        initData()
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
        db.closeEntireDb()
    }

    /**
     * Initialization
     **/
    private fun initObject() {
        this.db = DatabaseManager.getInstance(this)

        //devices db will be cerate at data/com.example.recipeapp/database
        this.db.createDatabase()
    }

    private fun initView() {
        this.recipeRecView = findViewById(R.id.recipeRecView)

        // init adapter
        this.recipeAdapter = RecipeAdapter()

        // init recycle view
        this.recipeRecView.layoutManager = LinearLayoutManager(this)
        this.recipeRecView.itemAnimator = DefaultItemAnimator()
        this.recipeRecView.adapter = this.recipeAdapter
    }

    private fun initData() {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val selectedRecipeType = sharedPreferences.getString("selectedRecipeType", "")

        if(selectedRecipeType != "" && selectedRecipeType != null){
            filterBy = selectedRecipeType
        }

        val resourcesArray = resources.getStringArray(R.array.recipe_types)
        this.recipeTypeList = ArrayList(resourcesArray.toList())
        this.recipeTypeList.add(0, "All")

        getRecipeData()
    }

    /**
     * Data
     **/
    private fun getRecipeData(){

        if(filterBy != "" && filterBy != "All"){
            whereQuery = "WHERE type = '${filterBy}'"
        }
        else whereQuery = ""

        // show progress dialog
        val pg = ProgressDialog(this@MainActivity)
        pg.setCancelable(false)
        pg.setCanceledOnTouchOutside(false)
        pg.setMessage("Loading....")
        pg.show()

        // generate data in background
        GlobalScope.launch(Dispatchers.Main) {
            // delay
            delay(200)

            withContext(Dispatchers.IO) {
                recipeList.clear()
                recipeList = db.getAllRecipe(whereQuery)
            }

            runOnUiThread {
                recipeAdapter.notifyDataSetChanged()
            }

            // dismiss progress dialog
            pg.dismiss()
        }
    }

    private fun deleteRecipe(recipe : Recipe){
        db.deleteRecipe(recipe.id)
        getRecipeData()
    }


    /**
     *Recycle view adapter
     */
    inner class RecipeAdapter : RecyclerView.Adapter<RecipeViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recipe_list, parent, false)
            return RecipeViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
            var recipe = recipeList[position]

            holder.recipeImgView.setImageResource(R.drawable.dummy_image)
            if(recipe.imgPath.isNotEmpty()){
                val imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), recipe.imgPath)
                val imageUri = Uri.fromFile(imageFile)
                holder.recipeImgView.setImageURI(imageUri)
            }

            holder.recipeNameTxtView.text = recipe.name
            holder.recipeIngredientTxtView.text = recipe.ingredients
            holder.recipeStepsTxtView.text = recipe.steps

            holder.editButton.setOnClickListener {
                val myIntent = Intent(this@MainActivity, CreateEditRecipeActivity::class.java)
                myIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                myIntent.putExtra("recipe",recipe)
                startActivityForResult(myIntent, END_EDIT_CREATE_REQUEST)
            }

            holder.deleteButton.setOnClickListener {
                deleteRecipe(recipe)
            }

            holder.vm.setOnClickListener {
                val myIntent = Intent(this@MainActivity, RecipeDetailActivity::class.java)
                myIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                myIntent.putExtra("recipe",recipe)
                startActivityForResult(myIntent, END_EDIT_CREATE_REQUEST)
            }
        }

        override fun getItemCount(): Int { return recipeList.size }
    }

    /**
     *Recycle view holder
     */
    class RecipeViewHolder(var vm: View) : RecyclerView.ViewHolder(vm) {
        var recipeImgView = vm.findViewById(R.id.recipeImgView) as ImageView
        var recipeNameTxtView = vm.findViewById(R.id.recipeNameTxtView) as TextView
        var recipeIngredientTxtView = vm.findViewById(R.id.recipeIngredientTxtView) as TextView
        var recipeStepsTxtView = vm.findViewById(R.id.recipeStepsTxtView) as TextView
        var editButton = vm.findViewById(R.id.editButton) as Button
        var deleteButton = vm.findViewById(R.id.deleteButton) as Button
    }

    /**
     *Option Menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add -> {
                val myIntent = Intent(this, CreateEditRecipeActivity::class.java)
                myIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivityForResult(myIntent, END_EDIT_CREATE_REQUEST)
            }
            R.id.action_filter -> {
                val optionsArray = recipeTypeList.toTypedArray<CharSequence>()

                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setTitle("Filter By Recipe Type")
                alertDialogBuilder.setItems(optionsArray) { dialog, which ->
                    filterBy = recipeTypeList[which]

                    //save to pref
                    val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putString("filterBy", filterBy).apply()

                    getRecipeData()
                }
                val alertDialog = alertDialogBuilder.create()
                alertDialog.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}