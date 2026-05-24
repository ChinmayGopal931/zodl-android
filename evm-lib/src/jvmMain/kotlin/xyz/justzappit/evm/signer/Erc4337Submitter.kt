package xyz.justzappit.evm.signer

import kotlinx.coroutines.delay
import xyz.justzappit.evm.abi.AbiAddress
import xyz.justzappit.evm.abi.AbiEncoder
import xyz.justzappit.evm.abi.AbiUint
import xyz.justzappit.evm.abi.keccak256
import xyz.justzappit.evm.hd.EvmKey
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.rpc.BundlerClient
import xyz.justzappit.evm.rpc.TransactionReceipt
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.ChainId
import xyz.justzappit.evm.types.TxHash
import xyz.justzappit.evm.types.Wei
import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.evm.util.padLeftToWord
import java.math.BigInteger

/**
 * Sends each `{to, value, data}` as a gas-sponsored ERC-4337 v0.6 UserOperation. The owner key
 * signs locally (self-custody); the bundler (Pimlico) relays and its verifying paymaster pays. The
 * first op for an undeployed account carries the factory initCode (lazy deploy); subsequent ops
 * carry none.
 *
 * The returned [TxHash] is the userOpHash; [awaitReceipt] resolves it to the mined transaction
 * receipt via the bundler, whose inner logs are identical to a normal tx — so downstream log
 * parsing is unchanged from the EOA path.
 */
class Erc4337Submitter(
    private val rpc: BaseRpcClient,
    private val bundler: BundlerClient,
    private val entryPoint: Address,
    private val accountFactory: Address,
    private val owner: EvmKey,
    private val smartAccount: Address,
    private val chainId: ChainId,
    private val gasLimitBufferPercent: Int = DEFAULT_GAS_BUFFER_PCT,
    private val receiptTimeoutMs: Long = DEFAULT_RECEIPT_TIMEOUT_MS,
    private val receiptPollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
) : TxSubmitter {
    /**
     * Local nonce cursor. After a successful `eth_sendUserOperation`, the next sequential nonce is
     * deterministically `cursor + 1` — no RPC read needed. Sidesteps the cross-RPC race on
     * fast-block chains where Pimlico's simulator (which validates the nonce at sponsorship time)
     * has already advanced past what our node RPC reports, causing AA25 on back-to-back UserOps.
     * Null = uninitialized; populated by the first RPC read and incremented locally thereafter.
     */
    private var nonceCursor: BigInteger? = null

    override suspend fun sendTransaction(to: Address, value: Wei, data: ByteArray): TxHash {
        val initCode =
            if (rpc.ethGetCode(smartAccount).isEmpty()) {
                ThirdwebSmartAccount.initCode(accountFactory, owner.address)
            } else {
                ByteArray(0)
            }
        val gasPrice = bundler.getUserOperationGasPrice()
        val nonce = nonceCursor ?: entryPointNonce().also { nonceCursor = it }

        val draft =
            UserOperationV06(
                sender = smartAccount,
                nonce = nonce,
                initCode = initCode,
                callData = ThirdwebSmartAccount.executeCalldata(to, value, data),
                callGasLimit = BigInteger.ZERO,
                verificationGasLimit = BigInteger.ZERO,
                preVerificationGas = BigInteger.ZERO,
                maxFeePerGas = hexToBig(gasPrice.maxFeePerGas),
                maxPriorityFeePerGas = hexToBig(gasPrice.maxPriorityFeePerGas),
                paymasterAndData = ByteArray(0),
                signature = DUMMY_SIGNATURE,
            )

        // ERC-7677: estimate with a paymaster stub (so the estimate covers paymaster validation),
        // then request the real sponsorship. Stub and final paymasterAndData share a length, so the
        // gas estimate stays valid.
        val stubbed = draft.copy(paymasterAndData = bundler.getPaymasterStubData(draft).paymasterAndData.hexToBytes())
        val estimate = bundler.estimateUserOperationGas(stubbed)
        val withGas =
            stubbed.copy(
                callGasLimit = hexToBig(estimate.callGasLimit).buffered(),
                verificationGasLimit = hexToBig(estimate.verificationGasLimit).buffered(),
                preVerificationGas = hexToBig(estimate.preVerificationGas).buffered(),
            )

        val sponsored =
            withGas.copy(
                paymasterAndData = bundler.sponsorUserOperation(withGas).paymasterAndData.hexToBytes(),
            )
        val signed = sponsored.copy(signature = signOwner(sponsored.userOpHash(entryPoint, chainId)))
        val txHash = bundler.sendUserOperation(signed)
        // Bundler accepted the op for this nonce — advance the cursor now so the next call doesn't
        // re-read a possibly-lagging node RPC. The optimistic advance is necessary for back-to-back
        // sendTransactions where neither caller has awaited a receipt yet (otherwise both would get
        // the same nonce). If a receipt later comes back !success, awaitReceipt resets the cursor
        // to force a re-read — the previous "orchestrator stops on revert" assumption was true for
        // run()/resume() (each builds a fresh submitter) but NOT for bridgeFundsBackToZec, which
        // reuses one submitter across cancelOrder + USDC.transfer. A failed cancelOrder used to
        // leave the cursor advanced past the actual on-chain nonce, AA25-storming the next op.
        nonceCursor = nonce + BigInteger.ONE
        return txHash
    }

    override suspend fun awaitReceipt(txHash: TxHash): TransactionReceipt {
        val deadline = System.currentTimeMillis() + receiptTimeoutMs
        while (System.currentTimeMillis() < deadline) {
            bundler.getUserOperationReceipt(txHash)?.let { receipt ->
                if (!receipt.success) {
                    // The UserOp landed on-chain but its inner call reverted. The cursor that
                    // sendTransaction optimistically advanced is now ahead of EntryPoint's actual
                    // nonce. Reset so the next sendTransaction re-reads from the node and picks up
                    // the real value, instead of replaying AA25 retries against a stale cursor.
                    nonceCursor = null
                }
                return receipt
            }
            delay(receiptPollIntervalMs)
        }
        // The bundler hasn't surfaced a receipt — we can't tell whether the op landed or got
        // dropped, so the cursor's relationship to actual on-chain state is undefined. Reset so the
        // next sendTransaction re-reads, rather than risk an AA25 storm on a stale cursor when
        // the op silently never settled.
        nonceCursor = null
        val minutes = receiptTimeoutMs / 60_000
        error(
            "Bundler did not return a receipt for userOp ${txHash.hex} after ${minutes}m. " +
                "The operation may still confirm on-chain — check the explorer before retrying.",
        )
    }

    /**
     * EntryPoint.getNonce(sender, key=0): the next sequential nonce; 0 for a counterfactual account.
     * Read once per submitter instance — the cursor takes over after the first UserOp lands. Uses
     * the node RPC because Pimlico's bundler endpoint does not serve `eth_call`.
     */
    private suspend fun entryPointNonce(): BigInteger {
        val ret =
            rpc.ethCall(
                to = entryPoint,
                data =
                    AbiEncoder.encodeFunctionCall(
                        "getNonce(address,uint192)",
                        listOf(AbiAddress(smartAccount), AbiUint(BigInteger.ZERO)),
                    ),
            )
        return if (ret.isEmpty()) BigInteger.ZERO else BigInteger(1, ret)
    }

    /** thirdweb's prebuilt Account contract validates the owner's ECDSA signature over the EIP-191-prefixed userOpHash. */
    private fun signOwner(userOpHash: ByteArray): ByteArray {
        val ethHash = keccak256(EIP191_PREFIX + userOpHash)
        return encodeSignature(EcdsaSigner.sign(ethHash, BigInteger(1, owner.privateKey)))
    }

    private fun BigInteger.buffered(): BigInteger =
        this * BigInteger.valueOf(100L + gasLimitBufferPercent) / BigInteger.valueOf(100L)

    companion object {
        private const val DEFAULT_GAS_BUFFER_PCT = 15
        private const val DEFAULT_RECEIPT_TIMEOUT_MS = 300_000L
        private const val DEFAULT_POLL_INTERVAL_MS = 2_000L
        private const val V_OFFSET = 27
        private const val EIP191_BYTE: Byte = 0x19

        private val EIP191_PREFIX =
            byteArrayOf(EIP191_BYTE) + "Ethereum Signed Message:\n32".toByteArray(Charsets.US_ASCII)

        // A structurally valid (canonical, low-s) throwaway signature for gas estimation, before the
        // real userOpHash is known. Recovers to some address, not the owner — fine, estimation does
        // not enforce the signature, it only needs the right length so ECDSA.recover doesn't revert.
        private val DUMMY_SIGNATURE: ByteArray =
            encodeSignature(EcdsaSigner.sign(keccak256("estimate".toByteArray()), BigInteger.ONE))

        private fun encodeSignature(sig: EcdsaSignature): ByteArray =
            sig.r.toByteArray().padLeftToWord() + sig.s.toByteArray().padLeftToWord() +
                byteArrayOf((sig.yParity + V_OFFSET).toByte())

        private fun hexToBig(hex: String): BigInteger =
            hex.removePrefix("0x").let { if (it.isEmpty()) BigInteger.ZERO else BigInteger(it, 16) }
    }
}
