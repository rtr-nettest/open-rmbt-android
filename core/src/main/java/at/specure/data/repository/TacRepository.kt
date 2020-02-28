package at.specure.data.repository

import at.specure.data.entity.TacRecord
import kotlinx.coroutines.flow.Flow

interface TacRepository {

    fun saveTac(tacRecord: TacRecord)

    fun getTac(language: String): Flow<TacRecord?>
}