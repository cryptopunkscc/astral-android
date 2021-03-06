package cc.cryptopunks.ui.model

object UI {

    sealed interface Event : Input {
        object Init : Event
        object Action : Event
        object Back : Event
        data class Configure(val config: Map<String, Any>) : Event
        data class Clicked(val id: String, val value: Any) : Event
        data class Text(val value: String? = null) : Event
        data class Method(val method: Service.Method) : Event
    }

    sealed interface Action : Input, Output, Message {
        object Init : Action
        data class AddContext(val context: Context) : Action
        data class SetMethod(val method: Service.Method) : Action
        data class SetArg(val key: String, val value: Any) : Action
        data class SetArgs(val args: UIArgs) : Action
        data class SetText(val text: String) : Action
        data class SetSelection(val data: List<UIData>) : Action
        data class SelectData(val id: String, val value: Any) : Action
        data class Configure(val config: Map<String, Any>) : Action
        object DropView : Action
        object DropMethod : Action
        object DropArg : Action

        // internal
        object NextParam : Action
        object CalculateMethods : Action
        object InferInputMethod : Action
        object SelectMethod : Action
        object SelectArg : Action
        object SwitchDisplay : Action
        object SwitchMode : Action
        object Execute : Action
        object Exit : Action
    }

    sealed interface Element<T> : Output {
        val defaultValue: T

        object Repo : UIElement<Service.Repo>()
        object Context : UIElement<UI.Context>(Context())
        object Config : UIElement<UIConfig>(UIConfig())
        object Stack : UIElement<List<UIView>>(emptyList())
        object Display : UIElement<Set<UIDisplay>>(setOf(UIDisplay.Panel))
        object Mode : UIElement<UIMode>(UIMode.Command)
        object Method : UIElement<Service.Method?>(null)
        object Methods : UIElement<List<UIMethod>>(emptyList())
        object Args : UIElement<UIArgs>(emptyMap())
        object Param : UIElement<UIParam?>(null)
        object Selection : UIElement<List<UIData>>(emptyList())
        object Ready : UIElement<Boolean>(false)
        object Text : UIElement<String>("")
    }

    data class Update<E : Element<T>, T>(val element: E, val value: T) : Message

    class State(elements: UIElements = emptyMap()) : UIState(elements) {
        val repo by +Element.Repo
        val context by +Element.Context
        val config by +Element.Config
        val methods by +Element.Methods
        val stack by +Element.Stack
        val mode by +Element.Mode
        val method by +Element.Method
        val args by +Element.Args
        val selection by +Element.Selection
        val text by +Element.Text
        val display by +Element.Display
        val param by +Element.Param
        val isReady by +Element.Ready

        companion object
    }

    data class Context(
        val methods: Map<String, Service.Method> = emptyMap(),
        val types: Map<String, Service.Type> = emptyMap(),
        val layouts: Map<String, UILayout> = emptyMap(),
        val resolvers: Map<String, Iterable<UIResolver>> = emptyMap(),
    ) {
        companion object
    }

    data class Change(val state: State, val output: List<Message> = emptyList())

    sealed interface Input
    sealed interface Output
    sealed interface Message
}
