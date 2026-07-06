package com.example.data

import android.content.Context
import androidx.room.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun fromVideoClipList(value: List<VideoClip>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, VideoClip::class.java)
        return moshi.adapter<List<VideoClip>>(type).toJson(value)
    }

    @TypeConverter
    fun toVideoClipList(value: String?): List<VideoClip>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, VideoClip::class.java)
        return moshi.adapter<List<VideoClip>>(type).fromJson(value)
    }

    @TypeConverter
    fun fromAudioClipList(value: List<AudioClip>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, AudioClip::class.java)
        return moshi.adapter<List<AudioClip>>(type).toJson(value)
    }

    @TypeConverter
    fun toAudioClipList(value: String?): List<AudioClip>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, AudioClip::class.java)
        return moshi.adapter<List<AudioClip>>(type).fromJson(value)
    }

    @TypeConverter
    fun fromTextOverlayList(value: List<TextOverlay>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, TextOverlay::class.java)
        return moshi.adapter<List<TextOverlay>>(type).toJson(value)
    }

    @TypeConverter
    fun toTextOverlayList(value: String?): List<TextOverlay>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, TextOverlay::class.java)
        return moshi.adapter<List<TextOverlay>>(type).fromJson(value)
    }
}

@Dao
interface VideoProjectDao {
    @Query("SELECT * FROM video_projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<VideoProject>>

    @Query("SELECT * FROM video_projects WHERE id = :id")
    suspend fun getProjectById(id: Int): VideoProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProject): Long

    @Update
    suspend fun updateProject(project: VideoProject)

    @Delete
    suspend fun deleteProject(project: VideoProject)
}

@Database(entities = [VideoProject::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoProjectDao(): VideoProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "video_editor_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
