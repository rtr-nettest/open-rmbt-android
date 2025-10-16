package at.rtr.rmbt.android.ui.view

import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.TestResultGraphItemRecord

interface ResultChart {

    fun addServerResultGraphItems(graphItems: List<TestResultGraphItemRecord>?, networkType: NetworkTypeCompat)

    fun addLocalResultGraphItems(graphItems: List<TestResultGraphItemRecord>?, networkType: NetworkTypeCompat)
}