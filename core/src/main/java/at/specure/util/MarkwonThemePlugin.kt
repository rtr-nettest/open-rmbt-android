package at.specure.util

import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.core.MarkwonTheme

class MarkwonThemePlugin : AbstractMarkwonPlugin() {
    override fun configureTheme(builder: MarkwonTheme.Builder) {
        super.configureTheme(builder)
        builder
            .bulletWidth(14)
            .headingBreakHeight(0)
    }
}