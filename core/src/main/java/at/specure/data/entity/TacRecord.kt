package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables

@Entity(tableName = Tables.TAC)
data class TacRecord(

    /**
     * language code of the terms and conditions
     */
    @PrimaryKey
    val language: String,

    /**
     * version of the saved terms and conditions
     */
    val version: Int,

    /**
     * HTML content of the terms and conditions
     */
    val content: String
)