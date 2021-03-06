package cc.cryptopunks.ui.proteus

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import cc.cryptopunks.ui.mapper.Jackson
import com.flipkart.android.proteus.*
import com.flipkart.android.proteus.value.DrawableValue
import com.flipkart.android.proteus.value.Layout
import com.flipkart.android.proteus.value.ObjectValue
import com.flipkart.android.proteus.value.Value

class DynamicView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onEvent: (String, Any) -> Unit = { _, _ -> }

    data class Update(
        val layout: Layout? = null,
        val layouts: Map<String, Layout> = emptyMap(),
        val data: ObjectValue? = null,
        val strings: List<String> = emptyList(),
    )

    operator fun invoke(
        update: Update
    ) = update(
        layout = update.layout,
        layouts = update.layouts,
        data = update.data
    )

    fun update(
        layout: Layout? = null,
        layouts: Map<String, Layout> = emptyMap(),
        data: ObjectValue? = null,
    ) {
        if (layouts.isNotEmpty()) this.layouts.apply {
            clear()
            plusAssign(layouts)
        }
        when {
            layout != null -> {
                removeAllViews()
                proteusView = proteusInflater.inflate(layout, data ?: ObjectValue())
                addView(proteusView.asView)
            }
            data != null ->
                proteusView.viewManager.update(data)
        }
    }

    private lateinit var proteusView: ProteusView

    private val proteusInflater by lazy { proteusContext.inflater }

    private val proteusContext by lazy {
        proteusComponent.proteus.createContextBuilder(context)
            .setLayoutManager(layoutManager)
            .setStyleManager(styleManager)
            .setImageLoader(imageLoader)
            .setCallback(proteusCallback)
            .build()
    }

    private var layouts: MutableMap<String, Layout> = mutableMapOf()

    private val layoutManager = object : LayoutManager() {
        override fun getLayouts() = this@DynamicView.layouts
    }

    private val styleManager = object : StyleManager() {
        private val styles: Styles = Styles()
        override fun getStyles() = styles
    }

    private val proteusCallback = object : ProteusLayoutInflater.Callback {

        override fun onUnknownViewType(
            context: ProteusContext,
            type: String,
            layout: Layout,
            data: ObjectValue,
            index: Int,
        ): ProteusView {
            TODO("Not yet implemented")
        }

        override fun onEvent(event: String, value: Value, view: ProteusView) {
            println("$event: $value")
            val id = view.viewManager.layout.extras!!.getAsString("layout")!!
            val json = proteusComponent.proteusTypeAdapterFactory.COMPILED_VALUE_TYPE_ADAPTER.toJson(value)
            val data = Jackson.jsonSlimMapper.readTree(json)
            onEvent(id, data)
        }
    }


    private val imageLoader = ProteusLayoutInflater
        .ImageLoader { view, url, callback: DrawableValue.AsyncCallback ->

        }
}
