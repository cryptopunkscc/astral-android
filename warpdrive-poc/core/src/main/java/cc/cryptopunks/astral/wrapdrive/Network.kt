package cc.cryptopunks.astral.wrapdrive

import cc.cryptopunks.astral.gson.GsonCoder
import cc.cryptopunks.astral.tcp.astralTcpNetwork

val network = astralTcpNetwork(GsonCoder())
