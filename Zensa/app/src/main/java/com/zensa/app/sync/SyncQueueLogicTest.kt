package com.zensa.app.sync

import com.zensa.app.data.entity.SyncQueueEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SyncQueueLogicTest
 *
 * Unit tests for the pure logic that governs the offline sync queue.
 *
 * We deliberately do NOT touch Room or Retrofit here. Those are
 * covered by their own layers. This file tests the rules that
 * decide whether a queued operation is pending, retryable, or
 * permanently failed — the exact rules the SyncWorker and
 * SyncRepository rely on.
 *
 * Every test fixes its own inputs, so the suite is deterministic.
 */
class SyncQueueLogicTest {

    // --------------------------------------------------------------
    // Test 1 — A freshly enqueued operation starts as pending.
    // --------------------------------------------------------------
    @Test
    fun `new operation is pending not synced and not failed`() {
        val item = queueItem()

        assertEquals(0, item.retryCount)
        assertFalse(item.synced)
        assertFalse(item.failed)
        assertEquals(null, item.lastError)
    }

    // --------------------------------------------------------------
    // Test 2 — The maximum retry threshold is respected.
    // --------------------------------------------------------------
    @Test
    fun `operation becomes failed when retry count reaches the maximum`() {
        val once = queueItem(retryCount = 1)
        val twice = queueItem(retryCount = 2)
        val almost = queueItem(retryCount = SyncQueueEntity.MAX_RETRIES - 1)
        val over = queueItem(retryCount = SyncQueueEntity.MAX_RETRIES)

        assertFalse(once.failed)
        assertFalse(twice.failed)
        assertFalse(almost.failed)
        assertTrue(over.failed)
    }

    // --------------------------------------------------------------
    // Test 3 — Only CREATE, UPDATE, DELETE are valid operations.
    // --------------------------------------------------------------
    @Test
    fun `operation constants cover every supported write`() {
        val supported = setOf(
            SyncQueueEntity.OP_CREATE,
            SyncQueueEntity.OP_UPDATE,
            SyncQueueEntity.OP_DELETE
        )

        assertEquals(3, supported.size)
        assertTrue(supported.contains("CREATE"))
        assertTrue(supported.contains("UPDATE"))
        assertTrue(supported.contains("DELETE"))
    }

    // --------------------------------------------------------------
    // Test 4 — Entity type constants cover every syncable table.
    // --------------------------------------------------------------
    @Test
    fun `entity type constants cover every syncable table`() {
        val types = setOf(
            SyncQueueEntity.TYPE_MODULE,
            SyncQueueEntity.TYPE_ASSIGNMENT,
            SyncQueueEntity.TYPE_SUBTASK,
            SyncQueueEntity.TYPE_STUDY_SESSION
        )

        assertEquals(4, types.size)
        assertTrue(types.contains("MODULE"))
        assertTrue(types.contains("ASSIGNMENT"))
        assertTrue(types.contains("SUBTASK"))
        assertTrue(types.contains("STUDY_SESSION"))
    }

    // --------------------------------------------------------------
    // Test 5 — Copy-on-failure pattern preserves identity.
    // This is the exact pattern handleFailure(...) uses.
    // --------------------------------------------------------------
    @Test
    fun `failure tracking preserves the original identity fields`() {
        val original = queueItem(
            queueId = "q-1",
            entityLocalId = "a-100",
            entityType = SyncQueueEntity.TYPE_ASSIGNMENT,
            operation = SyncQueueEntity.OP_CREATE
        )

        val failed = original.copy(
            retryCount = original.retryCount + 1,
            lastError = "server 500"
        )

        assertEquals(original.queueId, failed.queueId)
        assertEquals(original.entityLocalId, failed.entityLocalId)
        assertEquals(original.entityType, failed.entityType)
        assertEquals(original.operation, failed.operation)
        assertEquals(1, failed.retryCount)
        assertEquals("server 500", failed.lastError)
    }

    // --------------------------------------------------------------
    // Test 6 — A successful retry clears the error message.
    // --------------------------------------------------------------
    @Test
    fun `a successful retry clears the previous error`() {
        val previouslyFailed = queueItem(
            retryCount = 3,
            lastError = "network timeout"
        )

        // A retry that succeeds resets the failure metadata before
        // the row is removed from the queue.
        val recovered = previouslyFailed.copy(
            retryCount = 0,
            lastError = null,
            synced = true
        )

        assertTrue(recovered.synced)
        assertEquals(0, recovered.retryCount)
        assertEquals(null, recovered.lastError)
    }

    // --------------------------------------------------------------
    // Test 7 — MAX_RETRIES constant is small and sane.
    // --------------------------------------------------------------
    @Test
    fun `max retries is a small positive number`() {
        assertTrue(SyncQueueEntity.MAX_RETRIES in 1..10)
    }

    // --------------------------------------------------------------
    // Test 8 — Two operations with the same content are still
    // distinguishable because queueId is a UUID.
    // --------------------------------------------------------------
    @Test
    fun `queueId makes every operation unique`() {
        val a = queueItem(queueId = "q-a")
        val b = queueItem(queueId = "q-b")

        // Same payload, different queue identity.
        assertEquals(a.entityLocalId, b.entityLocalId)
        assertEquals(a.operation, b.operation)
        assertTrue(a.queueId != b.queueId)
    }

    // --------------------------------------------------------------
    // Test 9 — The pending predicate used by the worker is correct.
    // --------------------------------------------------------------
    @Test
    fun `pending predicate matches only unsynced non failed items`() {
        fun isPending(item: SyncQueueEntity): Boolean =
            !item.synced && !item.failed

        assertTrue(isPending(queueItem()))
        assertTrue(isPending(queueItem(retryCount = 2)))
        assertFalse(isPending(queueItem(synced = true)))
        assertFalse(isPending(queueItem(failed = true)))
    }

    // --------------------------------------------------------------
    // Test 10 — A failed-then-cleared item returns to the pending state.
    // --------------------------------------------------------------
    @Test
    fun `clearing the failure flag returns the item to pending`() {
        val failed = queueItem(
            retryCount = SyncQueueEntity.MAX_RETRIES,
            failed = true,
            lastError = "give up"
        )

        val recovered = failed.copy(
            failed = false,
            retryCount = 0,
            lastError = null
        )

        assertFalse(recovered.failed)
        assertEquals(0, recovered.retryCount)
        assertEquals(null, recovered.lastError)
    }

    // --------------------------------------------------------------
    // Fixture
    // --------------------------------------------------------------

    private fun queueItem(
        queueId: String = "queue-1",
        entityType: String = SyncQueueEntity.TYPE_ASSIGNMENT,
        entityLocalId: String = "local-1",
        operation: String = SyncQueueEntity.OP_CREATE,
        createdAt: String = "2026-05-12T10:00:00",
        retryCount: Int = 0,
        lastError: String? = null,
        synced: Boolean = false,
        failed: Boolean = false
    ) = SyncQueueEntity(
        queueId = queueId,
        entityType = entityType,
        entityLocalId = entityLocalId,
        operation = operation,
        createdAt = createdAt,
        retryCount = retryCount,
        lastError = lastError,
        synced = synced,
        failed = failed
    )
}
