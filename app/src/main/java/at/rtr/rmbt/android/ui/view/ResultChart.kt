package at.rtr.rmbt.android.ui.view

import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.TestResultGraphItemRecord

interface ResultChart {

    fun addResultGraphItems(graphItems: List<TestResultGraphItemRecord>?, networkType: NetworkTypeCompat)
}