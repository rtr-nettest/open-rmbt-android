package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables

@Entity(tableName = Tables.TAC)
data class TacRecord(

    /**
     * url of the terms and conditions
     */
    @PrimaryKey
    val url: String,

    /**
     * HTML content of the terms and conditions
     */
    val content: String
)