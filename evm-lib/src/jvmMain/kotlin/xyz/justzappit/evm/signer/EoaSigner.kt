package xyz.justzappit.evm.signer

import kotlinx.coroutines.delay
import xyz.justzappit.evm.hd.EvmKey
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.rpc.TransactionReceipt
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.ChainId
import xyz.justzappit.evm.types.Gas
import xyz.justzappit.evm.types.TxHash
import xyz.justzappit.evm.types.Wei
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
        value: Wei = Wei.ZERO,
        data: ByteArray = byteArrayOf(),
        gasLimitOverride: Gas? = null,
    ): TxHash {
        val nonce = rpc.ethGetTransactionCount(account.address, blockTag = "pending")
        val tip: Wei = rpc.ethMaxPriorityFeePerGas()
        val block = rpc.ethGetBlockByNumber(blockTag = "latest")
        val baseFee: Wei = block.baseFee
            ?: error("baseFeePerGas missing in latest block — chain may be pre-EIP-1559")
        val maxFee: Wei = baseFee * baseFeeMultiplier + tip
        val gasLimit = gasLimitOverride
            ?: rpc.ethEstimateGas(account.address, to, value, data)
                .times(BigInteger.valueOf(100L + gasLimitBufferPercent))
                .div(BigInteger.valueOf(100L))

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
        txHash: TxHash,
        timeoutMs: Long = DEFAULT_RECEIPT_TIMEOUT_MS,
        pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
    ): TransactionReceipt {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            rpc.ethGetTransactionReceipt(txHash)?.let { return it }
            delay(pollIntervalMs)
        }
        error("Timed out after ${timeoutMs}ms waiting for receipt of ${txHash.hex}")
    }

    companion object {
        private const val DEFAULT_BASE_FEE_MULTIPLIER = 2
        private const val DEFAULT_GAS_BUFFER_PCT = 20
        private const val DEFAULT_RECEIPT_TIMEOUT_MS = 120_000L
        private const val DEFAULT_POLL_INTERVAL_MS = 2_000L
    }
}
