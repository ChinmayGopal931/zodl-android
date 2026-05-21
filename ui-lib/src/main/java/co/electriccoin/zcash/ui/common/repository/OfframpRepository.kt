package co.electriccoin.zcash.ui.common.repository

import co.electriccoin.zcash.ui.common.provider.OfframpCheckpointStorageProvider
import kotlinx.coroutines.flow.Flow
import xyz.justzappit.offramp.orchestrator.OfframpCheckpoint

/**
 * Owns the in-flight UPI offramp checkpoint. v1: at most one in-flight order at a time; the
 * progress VM saves on every status emission and clears on Completed.
 */
interface OfframpRepository {
    fun observeInFlight(): Flow<OfframpCheckpoint?>
    suspend fun getInFlight(): OfframpCheckpoint?
    suspend fun save(checkpoint: OfframpCheckpoint)
    suspend fun clear()
}

internal class OfframpRepositoryImpl(
    private val storage: OfframpCheckpointStorageProvider,
) : OfframpRepository {
    override fun observeInFlight(): Flow<OfframpCheckpoint?> = storage.observe()
    override suspend fun getInFlight(): OfframpCheckpoint? = storage.get()
    override suspend fun save(checkpoint: OfframpCheckpoint) = storage.store(checkpoint)
    override suspend fun clear() = storage.clear()
}
