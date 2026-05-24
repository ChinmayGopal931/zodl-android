package xyz.justzappit.evm.abi

import xyz.justzappit.evm.util.padLeftToWord
import java.io.ByteArrayOutputStream
import java.math.BigInteger

object AbiEncoder {
    fun encode(args: List<AbiArg>): ByteArray {
        val staticSize = args.size * WORD
        val out = ByteArrayOutputStream()
        var dynOffset = staticSize

        for (arg in args) {
            if (arg.isDynamic) {
                out.write(BigInteger.valueOf(dynOffset.toLong()).toByteArray().padLeftToWord())
                dynOffset += arg.tail().size
            } else {
                out.write(arg.head())
            }
        }
        for (arg in args) {
            if (arg.isDynamic) out.write(arg.tail())
        }
        return out.toByteArray()
    }

    fun encodeFunctionCall(canonicalSignature: String, args: List<AbiArg>): ByteArray =
        FunctionSelector.compute(canonicalSignature) + encode(args)

    fun bytes32String(s: String): AbiBytes32 {
        val data = s.toByteArray(Charsets.UTF_8)
        require(data.size <= WORD) { "string too long for bytes32 (${data.size} bytes): '$s'" }
        return AbiBytes32(data + ByteArray(WORD - data.size))
    }
}
