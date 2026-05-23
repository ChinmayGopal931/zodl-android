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
import java.math.BigInteger

/**
 * Sends each `{to, value, data}` as a gas-sponsored ERC-4337 v0.6 UserOperation. The owner key
 * signs locally (self-custody); thirdweb's bundler relays and its paymaster pays. The first op for
 * an undeployed account carries the factory initCode (lazy deploy); subsequent ops carry none.
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

    override suspend fun sendTransaction(to: Address, value: Wei, data: ByteArray): TxHash {
        val initCode = if (rpc.ethGetCode(smartAccount).isEmpty()) {
            ThirdwebSmartAccount.initCode(accountFactory, owner.address)
        } else {
            ByteArray(0)
        }
        val gasPrice = bundler.getUserOperationGasPrice()

        val draft = UserOperationV06(
            sender = smartAccount,
            nonce = entryPointNonce(),
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
        val withGas = stubbed.copy(
            callGasLimit = hexToBig(estimate.callGasLimit).buffered(),
            verificationGasLimit = hexToBig(estimate.verificationGasLimit).buffered(),
            preVerificationGas = hexToBig(estimate.preVerificationGas).buffered(),
        )

        val sponsored = withGas.copy(
            paymasterAndData = bundler.sponsorUserOperation(withGas).paymasterAndData.hexToBytes(),
        )
        val signed = sponsored.copy(signature = signOwner(sponsored.userOpHash(entryPoint, chainId)))
        return bundler.sendUserOperation(signed)
    }

    override suspend fun awaitReceipt(txHash: TxHash): TransactionReceipt {
        val deadline = System.currentTimeMillis() + receiptTimeoutMs
        while (System.currentTimeMillis() < deadline) {
            bundler.getUserOperationReceipt(txHash)?.let { return it }
            delay(receiptPollIntervalMs)
        }
        val minutes = receiptTimeoutMs / 60_000
        error(
            "Bundler did not return a receipt for userOp ${txHash.hex} after ${minutes}m. " +
                "The operation may still confirm on-chain — check the explorer before retrying.",
        )
    }

    /** EntryPoint.getNonce(sender, key=0): the next sequential nonce; 0 for a counterfactual account. */
    private suspend fun entryPointNonce(): BigInteger {
        val ret = rpc.ethCall(
            to = entryPoint,
            data = AbiEncoder.encodeFunctionCall(
                "getNonce(address,uint192)",
                listOf(AbiAddress(smartAccount), AbiUint(BigInteger.ZERO)),
            ),
        )
        return if (ret.isEmpty()) BigInteger.ZERO else BigInteger(1, ret)
    }

    /** thirdweb's Account validates the owner's ECDSA signature over the EIP-191-prefixed userOpHash. */
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
        private const val FIELD_BYTES = 32
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
            padTo32(sig.r.toByteArray()) + padTo32(sig.s.toByteArray()) +
                byteArrayOf((sig.yParity + V_OFFSET).toByte())

        private fun padTo32(b: ByteArray): ByteArray = when {
            b.size == FIELD_BYTES -> b
            b.size > FIELD_BYTES -> b.copyOfRange(b.size - FIELD_BYTES, b.size)
            else -> ByteArray(FIELD_BYTES).also { System.arraycopy(b, 0, it, FIELD_BYTES - b.size, b.size) }
        }

        private fun hexToBig(hex: String): BigInteger =
            hex.removePrefix("0x").let { if (it.isEmpty()) BigInteger.ZERO else BigInteger(it, 16) }
    }
}
