package cc.cryptopunks.ui.service.base

import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

interface Rpc {

    abstract class Api {
        val methods: List<KClass<*>> by lazy { rpcCommands() }
        val title: String get() = javaClass.name
    }

    interface Command

    abstract class Listen<T> : Command {
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        val result: T? = null
    }
}

private fun Any.rpcCommands(): List<KClass<*>> =
    this::class.nestedClasses.filter { type -> type.isSubclassOf(Rpc.Command::class) }

data class ListUpdate<T>(
    val init: List<T> = emptyList(),
    val add: List<T> = emptyList(),
    val remove: List<T> = emptyList(),
)
