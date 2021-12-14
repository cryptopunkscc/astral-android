package cc.cryptopunks.ui.poc.widget

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.cryptopunks.ui.poc.R
import cc.cryptopunks.ui.poc.databinding.CommandItemBinding
import cc.cryptopunks.ui.poc.model.*
import cc.cryptopunks.ui.poc.model.helper.output
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import splitties.views.imageDrawable


data class CommandView(
    val activity: AppCompatActivity,
    val container: ViewGroup = activity.findViewById(R.id.commandView),
    val toolbar: Toolbar = container(R.id.toolbar),
    val dynamicView: DataView = DataView(container(R.id.dataView)),
    val commandLayout: ViewGroup = container(R.id.selectedCommandContainer),
    val commandView: ViewGroup = container(R.id.selectedCommandLayout),
    val recyclerView: RecyclerView = container(R.id.optionsRecyclerView),
    val inputView: EditText = container(R.id.inputEditText),
    val actionButton: View = container(R.id.actionButton),
) : CoroutineScope by MainScope() {

    val cmdDrawable: Drawable = ShapeTextDrawable("$")

    val returnDrawable: Drawable = ResourcesCompat.getDrawable(
        container.resources,
        R.drawable.baseline_keyboard_return_white_18dp,
        container.context.theme
    )!!

    val optionsAdapter = OptionsAdapter()

    val commandBinding = CommandItemBinding.bind(commandView)

    init {

        activity.setSupportActionBar(toolbar)

        recyclerView.apply {
            val linearLayoutManager = LinearLayoutManager(context).apply {
                reverseLayout = true
//            stackFromEnd = true
            }
            val dividerItemDecoration = DividerItemDecoration(
                recyclerView.context,
                linearLayoutManager.orientation
            )

            layoutManager = linearLayoutManager
            adapter = optionsAdapter
            addItemDecoration(dividerItemDecoration)
        }

    }
}

operator fun <T : View> View.invoke(@IdRes id: Int): T =
    findViewById(id)

fun ComponentActivity.backEventsFlow(): Flow<Unit> = callbackFlow {
    val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            trySend(Unit)
        }
    }
    onBackPressedDispatcher.addCallback(onBackPressedCallback)
    awaitClose {
        onBackPressedCallback.remove()
    }
}

fun EditText.textChangesFlow(): Flow<String?> = callbackFlow {
    val watcher = doOnTextChanged { text, _, _, _ ->
        trySend(text?.toString())
    }
    awaitClose {
        removeTextChangedListener(watcher)
    }
}

private fun FloatingActionButton.setIcon(drawable: Drawable? = null) {
    when (drawable) {
        is ShapeTextDrawable -> {
            foreground = drawable
            imageDrawable = null
        }
        else -> {
            foreground = null
            imageDrawable = drawable
        }
    }
}

fun CommandView.uiEvents(): Flow<UI.Event> = channelFlow {

    operator fun UI.Event.unaryPlus() = trySend(this)

    actionButton.setOnClickListener {
        +UI.Event.Action
    }

    dynamicView.onEvent = { event ->
        +event
    }

    launch {
        optionsAdapter.clicks.collect { view ->
            when (val item = view.tag) {
                is UIMethod -> +UI.Event.Method(item.method)
                is String -> +UI.Event.Clicked("string", item)
                is Boolean -> +UI.Event.Clicked("boolean", item)
            }
        }
    }

    launch {
        inputView.textChangesFlow().collect { text ->
            +UI.Event.Text(text)
        }
    }

    launch {
        activity.backEventsFlow().collect {
            +UI.Event.Back
        }
    }
}


fun UI.Change.update(view: CommandView): UI.Change =
    apply { output.map(UIMessage::output).toSet().forEach { output -> view.update(state, output) } }

private fun CommandView.update(state: UI.State, output: UI.Output) {
    when (output) {

        UI.Element.Text -> {
            if (inputView.text.isNotEmpty() && state.text.isBlank())
                inputView.text = null
        }

        UI.Element.Methods -> {
            if (state.method == null) {
                inputView.hint = "find command by name or param"
                optionsAdapter.items = state.methods
            }
        }

        UI.Element.Param -> state.param?.run {
            when (
                val resolver = resolvers.minOrNull()
            ) {
                is UIResolver.Data -> {
                    inputView.hint = "select data from list"
                }
                is UIResolver.Option -> {
                    optionsAdapter.items = resolver.list
                    inputView.hint = "select option"
                }
                is UIResolver.Input -> when (resolver.type) {
                    Service.Type.bool -> {
                        optionsAdapter.items = listOf(false, true)
                        inputView.hint = "select option"
                    }
                    else -> {
                        inputView.hint = when (resolver.type) {
                            Service.Type.str -> "type text"
                            Service.Type.int -> "type integer"
                            Service.Type.num -> "type number"
                            else -> inputView.hint
                        }
                    }
                }
            }

            inputView.hint = "provide $name"
        }

        UI.Element.Method,
        is UI.Element.Args -> {
            state.method?.let { method ->
                commandBinding.set(
                    method,
                    state.args.mapValues { (_, value) -> value.toString() }
                )
            }
        }

        UI.Element.Display -> state.display.let { display ->
            val displayPanel = UIDisplay.Panel in display
            val displayInput = UIDisplay.Input in display
            val displayCommand = UIDisplay.Method in display
            val displayData = UIDisplay.Data in display
            inputView.isVisible = displayInput
            recyclerView.isVisible = displayPanel
            commandLayout.isVisible = displayCommand
            dynamicView.isVisible = displayData
        }

        UI.Element.Ready -> {
            if (state.isReady)
                inputView.hint = "Tap FAB to execute."
        }

        is UI.Element.Stack -> {
            if (state.stack.isNotEmpty()) {
                val view = state.stack.last()
                coroutineContext.cancelChildren()
                launch {
                    dynamicView.update(
                        layout = state.context.layouts[view.source.id]!!,
                        updates = view.data()
                    )
                }
            }
            activity.supportActionBar!!.title = state.stack.lastOrNull()?.source?.id
        }

        UI.Action.Exit -> {
            activity.finish()
        }

        else -> Unit
    }
}
