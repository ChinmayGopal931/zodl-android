package xyz.justzappit.evm.hd

import xyz.justzappit.evm.util.toHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class EvmKeyDerivationTest {
    @Test
    fun `canonical abandon mnemonic produces well-known account 0 address`() {
        // Industry-wide BIP-44 Ethereum vector. Same address is produced by MetaMask,
        // Ledger, Trezor, ethers, viem, web3j against this mnemonic at m/44'/60'/0'/0/0.
        val key = EvmKeyDerivation.derive(MNEMONIC, accountIndex = 0)
        assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", key.address)
    }

    @Test
    fun `canonical abandon mnemonic produces well-known private key`() {
        val key = EvmKeyDerivation.derive(MNEMONIC, accountIndex = 0)
        assertEquals(
            "1ab42cc412b618bdea3a599e3c9bae199ebf030895b039e9db1e30dafb12b727",
            key.privateKey.toHex(),
        )
    }

    @Test
    fun `account index 1 differs from 0`() {
        val a = EvmKeyDerivation.derive(MNEMONIC, accountIndex = 0)
        val b = EvmKeyDerivation.derive(MNEMONIC, accountIndex = 1)
        assertNotEquals(a.address, b.address)
        assertNotEquals(a.privateKey.toHex(), b.privateKey.toHex())
    }

    @Test
    fun `address is EIP-55 checksummed`() {
        val key = EvmKeyDerivation.derive(MNEMONIC, accountIndex = 0)
        assertTrue(key.address.startsWith("0x"))
        assertEquals(42, key.address.length)
        // The canonical vector has mixed case; pure-lowercase output would be a bug.
        assertNotEquals(key.address, key.address.lowercase())
    }

    @Test
    fun `publicKey is 64 bytes (X plus Y, no 04 prefix)`() {
        val key = EvmKeyDerivation.derive(MNEMONIC, accountIndex = 0)
        assertEquals(64, key.publicKey.size)
    }

    @Test
    fun `passphrase changes derived key`() {
        val a = EvmKeyDerivation.derive(MNEMONIC, passphrase = "")
        val b = EvmKeyDerivation.derive(MNEMONIC, passphrase = "TREZOR")
        assertNotEquals(a.address, b.address)
    }

    @Test
    fun `negative account index is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            EvmKeyDerivation.derive(MNEMONIC, accountIndex = -1)
        }
    }

    @Test
    fun `fromPrivateKey round-trips through derive`() {
        val derived = EvmKeyDerivation.derive(MNEMONIC, accountIndex = 0)
        val rebuilt = EvmKeyDerivation.fromPrivateKey(derived.privateKey)
        assertEquals(derived.address, rebuilt.address)
        assertTrue(derived.publicKey.contentEquals(rebuilt.publicKey))
    }

    @Test
    fun `private key must be 32 bytes`() {
        assertFailsWith<IllegalArgumentException> {
            EvmKeyDerivation.fromPrivateKey(ByteArray(31))
        }
        assertFailsWith<IllegalArgumentException> {
            EvmKeyDerivation.fromPrivateKey(ByteArray(33))
        }
    }

    @Test
    fun `zero private key is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            EvmKeyDerivation.fromPrivateKey(ByteArray(32))
        }
    }

    companion object {
        const val MNEMONIC =
            "abandon abandon abandon abandon abandon abandon " +
                "abandon abandon abandon abandon abandon about"
    }
}
