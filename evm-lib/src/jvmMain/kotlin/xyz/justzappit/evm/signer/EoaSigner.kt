package xyz.justzappit.evm.signer

import kotlinx.coroutines.delay
import xyz.justzappit.evm.hd.EvmKey
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.rpc.TransactionReceipt
import xyz.justzappit.evm.rpc.hexToBigInteger
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.ChainId
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger

class EoaSigner(
    private val rpc: BaseRpcClient,
    private val chainId: ChainId,
    private val account: EvmKey,
    private val baseFeeMultiplier: Int = DEFAULT_BASE_FEE_MULTIPLIER,
    private val gasLimitBufferPercent: Int = DEFAULT_GAS_BUFFER_PCT,
) {
    suspend fun sendTransaction(
        to: Address,
        value: BigInteger = BigInteger.ZERO,
        data: ByteArray = byteArrayOf(),
        gasLimitOverride: BigInteger? = null,
    ): String {
        val nonce = rpc.ethGetTransactionCount(account.address, blockTag = "pending")
        val tip = rpc.ethMaxPriorityFeePerGas()
        val block = rpc.ethGetBlockByNumber(blockTag = "latest")
        val baseFee = block.baseFeePerGas?.let { hexToBigInteger(it) }
            ?: error("baseFeePerGas missing in latest block — chain may be pre-EIP-1559")
        val maxFee = baseFee.multiply(BigInteger.valueOf(baseFeeMultiplier.toLong())).add(tip)
        val gasLimit = gasLimitOverride
            ?: rpc.ethEstimateGas(account.address, to, value, data)
                .multiply(BigInteger.valueOf(100L + gasLimitBufferPercent))
                .divide(BigInteger.valueOf(100L))

        val tx = Eip1559Tx(
            chainId = chainId,
            nonce = nonce,
            maxPriorityFeePerGas = tip,
            maxFeePerGas = maxFee,
            gasLimit = gasLimit,
            to = to,
            value = value,
            data = data,
        )
        val sig = EcdsaSigner.sign(tx.signingHash(), BigInteger(1, account.privateKey))
        return rpc.ethSendRawTransaction("0x" + tx.encodeSigned(sig).toHex())
    }

    suspend fun awaitReceipt(
        txHash: String,
        timeoutMs: Long = DEFAULT_RECEIPT_TIMEOUT_MS,
        pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
    ): TransactionReceipt {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            rpc.ethGetTransactionReceipt(txHash)?.let { return it }
            delay(pollIntervalMs)
        }
        error("Timed out after ${timeoutMs}ms waiting for receipt of $txHash")
    }

    companion object {
        private const val DEFAULT_BASE_FEE_MULTIPLIER = 2
        private const val DEFAULT_GAS_BUFFER_PCT = 20
        private const val DEFAULT_RECEIPT_TIMEOUT_MS = 120_000L
        private const val DEFAULT_POLL_INTERVAL_MS = 2_000L
    }
}
