package at.specure.util

import android.content.Context
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

object MarkwonBuilder {
    fun build(context: Context): Markwon {
        return Markwon.builder(context)
            .usePlugins(
                listOf(
                    CorePlugin.create(), HtmlPlugin.create(), ImagesPlugin.create(),
                    StrikethroughPlugin.create(), TablePlugin.create(context),
                    MarkwonThemePlugin(), LinkifyPlugin.create()
                )
            )
            .build()
    }
}