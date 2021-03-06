package cc.cryptopunks.ui.model.internal

import cc.cryptopunks.ui.model.*

internal fun UI.State.generateSelection(clicked: UI.Event.Clicked): List<UIData> =
    context.generateUIData(clicked.id, clicked.value)

internal fun UI.State.generateSelection(data: UI.Action.SelectData): List<UIData> =
    context.generateUIData(data.id, data.value)

internal fun UI.State.generateUIDataFromStack(): List<UIData> =
    context.generateUIData(stack)

private fun UI.Context.generateUIData(stack: List<UIView>): List<UIData> = stack
    .flatMap { view -> view.args.mapKeys { (key, _) -> view.source.params[key]!! }.toList() }
    .flatMap { (type, value) -> generateUIData(type.id, value) }


private fun UI.Context.generateUIData(id: String, value: Any): List<UIData> =
    when (value) {
        is Map<*, *> -> {
            val type = requireNotNull(types[id]) {
                "Cannot resolve type for id: $id"
            }
            UIData(type, value).unfoldComplexTypes()
        }
        else -> listOf(
            UIData(Service.Type(id), value)
        )
    }

private fun UIData.unfoldComplexTypes(): List<UIData> =
    type.properties.flatMap { (key, type) ->
        when {
            type.id.isBlank() -> emptyList()
            else -> when (
                val value = (value as JsonObject)[key]
            ) {
                null -> emptyList()
                else -> {
                    val data = UIData(type, value)
                    data.unfoldComplexTypes().plus(data)
                }
            }
        }
    } + this
