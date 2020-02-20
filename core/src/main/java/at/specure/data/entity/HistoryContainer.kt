package at.specure.data.entity

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Relation

@Keep
class HistoryContainer {

    @Embedded
    lateinit var reference: HistoryReference

    @Relation(parentColumn = "uuid", entityColumn = "referenceUUID", entity = History::class)
    lateinit var items: List<History>
}
