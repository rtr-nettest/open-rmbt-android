package at.rtr.rmbt.android.ui

import at.bluesource.choicesdk.maps.common.MapFragment
import at.bluesource.choicesdk.maps.common.MapOptions

/**
 * MapFragment from Choice SDK has no empty constructor, which is needed by Android framework
 * while in restore savedInstance state
 */
class MapFragmentPatched @JvmOverloads constructor(options: MapOptions? = null) : MapFragment(options)