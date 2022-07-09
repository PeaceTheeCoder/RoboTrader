package com.theecoder.robotrader.network.db

import android.content.Context
import androidx.room.*
import com.theecoder.robotrader.network.module.Licence
import com.theecoder.robotrader.network.module.Owner
import com.theecoder.robotrader.network.module.Sicence
import com.theecoder.robotrader.network.module.Symbol

import org.json.JSONObject
import androidx.sqlite.db.SupportSQLiteDatabase

import androidx.room.migration.Migration




@Database(
    entities = [Licence::class, Sicence::class, Symbol::class ],
    version = 2,
    exportSchema = false
)
@TypeConverters(OwnerTypeConverter::class)
abstract class LicenceDB : RoomDatabase() {

    abstract fun getLicenceDao(): LicenceDao

    companion object{
        @Volatile
        private var instance: LicenceDB? =null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance?: synchronized(LOCK){
            instance?: createDatabase(context).also { instance = it }
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE symbols "
                            + "ADD COLUMN platform TEXT"
                )
            }
        }

        private fun createDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                LicenceDB::class.java,
                "licences_db"
            ).addMigrations(MIGRATION_1_2).build()
    }

}
class OwnerTypeConverter {
    @TypeConverter
    fun fromOwner(owner: Owner): String {
        return JSONObject().apply {
            put("name", owner.name)
            put("email", owner.email)
            put("phone", owner.phone)
            put("logo", owner.logo)
        }.toString()
    }

    @TypeConverter
    fun toOwner(owner: String): Owner {
        val json = JSONObject(owner)
        return Owner(
            json.get("email") as String,
            json.get("name") as String,
            json.get("phone") as String,
            json.get("logo") as String
        )
    }
}