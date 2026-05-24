package xyz.justzappit.evm.abi

import org.bouncycastle.crypto.digests.KeccakDigest

fun keccak256(data: ByteArray): ByteArray {
    val d = KeccakDigest(KECCAK_BITS)
    d.update(data, 0, data.size)
    return ByteArray(d.digestSize).also { d.doFinal(it, 0) }
}

private const val KECCAK_BITS = 256
