package xyz.justzappit.evm.rpc

class RpcException(
    val method: String,
    val code: Int?,
    message: String?,
    val raw: String,
) : RuntimeException(buildString {
    append("RPC ").append(method)
    if (code != null) append(" failed with code=").append(code)
    if (!message.isNullOrBlank()) append(": ").append(message)
})
