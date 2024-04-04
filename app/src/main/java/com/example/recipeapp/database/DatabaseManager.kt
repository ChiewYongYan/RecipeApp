package com.example.recipeapp.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.recipeapp.model.Recipe
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DatabaseManager(private var myContext : Context): SQLiteOpenHelper(myContext, "recipe-master",null, 1) {

    // object
    private var myDataBase: SQLiteDatabase? = null

    private val DATABASE_NAME = "recipe-master.sqlite"
    private var userSqlite = ""
    private var dbPath = ""

    override fun onCreate(db: SQLiteDatabase?) {
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

    //setup database
    fun createDatabase() {
        this.userSqlite = DATABASE_NAME

        // search file and create new one
        val dbExist = this.checkDataBase(this.userSqlite)
        if (dbExist) {
            // show database exists log
            Log.v("DB Exists", "db exists")

            // set db path
            dbPath = myContext.getDatabasePath(userSqlite).toString()
        } else {

            // set it to readable only
            this.readableDatabase
            try {
                // close db
                this.close()

                // copy database from assets to user device folder
                this.copyDatabase()

                // rename sqlite file
                //this.checkAndRenameDatabase(DATABASE_NAME, this.userSqlite)

            } catch (e: IOException) {
                throw Error("Error copying database")
            }
        }
    }

    private fun checkDataBase(dbName: String): Boolean {
        var checkDB = false
        try {
            val myPath = myContext.getDatabasePath(dbName).toString()
            val dbFile = File(myPath)
            checkDB = dbFile.exists()
        } catch (e: SQLiteException) {
            println("delete database file.")
        }
        return checkDB
    }

    private fun copyDatabase() {
        val outFileName = myContext.getDatabasePath(DATABASE_NAME).toString()

        val myOutput = FileOutputStream(outFileName)
        val myInput = myContext.assets.open(DATABASE_NAME)

        val buffer = ByteArray(1024)
        var length: Int = myInput.read(buffer)
        while ((length) > 0) {
            myOutput.write(buffer, 0, length)
            length = myInput.read(buffer)
        }
        myInput.close()
        myOutput.flush()
        myOutput.close()
    }

    fun openDatabase() {
        if (myDataBase != null) return

        // check dbPath is empty or not
        myDataBase = if (dbPath.isEmpty()){
            // concat username and sqlite
            this.userSqlite = DATABASE_NAME

            // set db path
            dbPath = myContext.getDatabasePath(userSqlite).toString()

            SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
        } else {
            SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
        }
    }

    fun closeEntireDb() {
        if (myDataBase != null) {
            myDataBase!!.close()
            myDataBase = null
        }
        super.close()
    }

    //table function
    fun getLastInsertedId(): Int {
        this.openDatabase()
        val db = myDataBase!!
        val query = "SELECT MAX(id) FROM recipe"
        val cursor = db.rawQuery(query, null)
        var lastId =1
        if (cursor != null && cursor.moveToFirst()) {
            lastId = cursor.getInt(0)
        }
        cursor.close()

        return lastId+1
    }

    fun getAllRecipe(whereQuery : String) : ArrayList<Recipe>{
        val recipeList = ArrayList<Recipe>()
        this.openDatabase()
        val db = myDataBase!!
        var selectQuery = "SELECT * FROM $RECIPE_TABLE $whereQuery"
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                val recipe = Recipe()
                recipe.id = cursor.getInt(0)
                recipe.imgPath = cursor.getString(1) ?: ""
                recipe.name = cursor.getString(2) ?:""
                recipe.type = cursor.getString(3) ?: ""
                recipe.ingredients = cursor.getString(4) ?: ""
                recipe.steps = cursor.getString(5) ?: ""

                recipeList.add(recipe)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return recipeList
    }

     fun insertRecipe(recipe : Recipe) : Boolean{
        this.openDatabase()
        val db = myDataBase!!
        db.beginTransaction()

        val contentValues = ContentValues()
        contentValues.put("id",recipe.id)
        contentValues.put("img_path",recipe.imgPath)
        contentValues.put("name", recipe.name)
        contentValues.put("type", recipe.type)
        contentValues.put("ingredients", recipe.ingredients)
        contentValues.put("steps", recipe.steps)
        db.insert(RECIPE_TABLE, null, contentValues)

        db.setTransactionSuccessful()
        db.endTransaction()
        return true
    }

    fun updateRecipe(recipeId: Int, updatedValues: ContentValues): Int {
        this.openDatabase()
        val db = myDataBase!!
        db.beginTransaction()

        // Define 'where' part of query.
        val selection = "id = ?"
        val selectionArgs = arrayOf(recipeId.toString())

        val count = db.update("recipe", updatedValues, selection, selectionArgs)
        db.setTransactionSuccessful()
        db.endTransaction()
        return count
    }

    fun deleteRecipe(id : Int): Int{
        this.openDatabase()
        val db = myDataBase!!
        db.beginTransaction()
        val selection = "id = ?"
        val selectionArgs = arrayOf(id.toString())
        val result = db.delete("recipe", selection, selectionArgs)
        db.setTransactionSuccessful()
        db.endTransaction()
        return result
    }

    companion object {
        private var instance: DatabaseManager? = null

        @Synchronized
        fun getInstance(ctx: Context): DatabaseManager {
            if (instance == null) instance = DatabaseManager(ctx.applicationContext)
            return instance!!
        }

        // table name
        private const val RECIPE_TABLE = "recipe"

    }
}