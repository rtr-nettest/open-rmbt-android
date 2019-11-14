package at.rtr.rmbt.android.ui.view.curve

/**
 * Helper class describes the section of measurement curve. It contains data about count of squares, label, start & end angles and length of section curve
 */
class CurveSection(val count: Int, val text: String, val isUpside: Boolean, var startAngle: Float = 0f, var endAngle: Float = 0f, var length: Float = 0f)