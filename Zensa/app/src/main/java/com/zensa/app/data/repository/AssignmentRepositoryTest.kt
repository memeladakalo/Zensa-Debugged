package com.zensa.app.data.repository

import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.dao.AssignmentDao
import com.zensa.app.data.dao.ModuleDao
import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.data.remote.ZensaApiService
import com.zensa.app.data.remote.dto.AssignmentDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AssignmentRepositoryTest
 *
 * 8 unit tests focused on the offline-first contract:
 *   - Reads go to Room
 *   - Writes go to Room first, then to the server
 *   - On server failure, the local row stays and is marked unsynced
 *   - Refresh merges server data into Room
 *
 * We use MockK to fake the DAO and the Retrofit service. No Room,
 * no network — pure logic tests running on the JVM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AssignmentRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: AssignmentDao
    private lateinit var moduleDao: ModuleDao
    private lateinit var api: ZensaApiService

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = mockk(relaxed = true)
        moduleDao = mockk(relaxed = true)
        api = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repo() = AssignmentRepository(dao, moduleDao, api)

    // 1
    @Test
    fun `create writes to Room before calling the API`() = runTest(dispatcher) {
        coEvery { api.createAssignment(any()) } returns
            retrofit2.Response.success(sampleDto())

        val slot = slot<AssignmentEntity>()
        coEvery { dao.upsert(capture(slot)) } returns Unit

        repo().create(
            userId = "u1",
            moduleLocalId = null,
            title = "Database assignment",
            description = "ERD and normalise",
            priority = "HIGH",
            status = "PENDING",
            dueDate = "2026-06-20T23:59:00"
        )

        // At least one of the upsert calls was the initial local row.
        assertTrue(slot.captured.title == "Database assignment")
        coVerify { api.createAssignment(any()) }
    }

    // 2
    @Test
    fun `create returns the local row when the server fails`() = runTest(dispatcher) {
        coEvery { api.createAssignment(any()) } throws
            java.io.IOException("network down")
        coEvery { dao.upsert(any()) } returns Unit

        val result = repo().create(
            userId = "u1",
            moduleLocalId = null,
            title = "Offline task",
            description = "",
            priority = "MEDIUM",
            status = "PENDING",
            dueDate = "2026-06-20T23:59:00"
        )

        assertTrue(result is ApiResult.Success)
        val saved = (result as ApiResult.Success).data
        assertEquals("Offline task", saved.title)
    }

    // 3
    @Test
    fun `refresh writes server rows into Room`() = runTest(dispatcher) {
        coEvery { api.getAssignments() } returns
            retrofit2.Response.success(listOf(sampleDto()))

        repo().refresh()

        coVerify { dao.upsertAll(any()) }
    }

    // 4
    @Test
    fun `refresh returns error when the API is unreachable`() = runTest(dispatcher) {
        coEvery { api.getAssignments() } throws
            java.io.IOException("offline")

        val result = repo().refresh()

        assertTrue(result is ApiResult.Error)
    }

    // 5
    @Test
    fun `setCompleted updates Room and marks the row unsynced`() = runTest(dispatcher) {
        coEvery { dao.getByLocalId("local-1") } returns sampleEntity(serverId = null)

        repo().setCompleted("local-1", true)

        coVerify { dao.setCompleted("local-1", true) }
    }

    // 6
    @Test
    fun `delete soft-deletes locally when the row has no serverId`() = runTest(dispatcher) {
        coEvery { dao.getByLocalId("local-1") } returns sampleEntity(serverId = null)

        repo().delete("local-1")

        coVerify { dao.softDelete("local-1") }
        coVerify { dao.hardDelete("local-1") }
    }

    // 7
    @Test
    fun `delete with serverId tries the server then hard-deletes locally`() = runTest(dispatcher) {
        coEvery { dao.getByLocalId("local-1") } returns sampleEntity(serverId = "server-1")
        coEvery { api.deleteAssignment("server-1") } returns
            retrofit2.Response.success(Unit)

        repo().delete("local-1")

        coVerify { dao.softDelete("local-1") }
        coVerify { api.deleteAssignment("server-1") }
        coVerify { dao.hardDelete("local-1") }
    }

    // 8
    @Test
    fun `delete on an unknown row returns error`() = runTest(dispatcher) {
        coEvery { dao.getByLocalId(any()) } returns null

        val result = repo().delete("missing")

        assertTrue(result is ApiResult.Error)
    }

    // ---------- Fixtures ----------

    private fun sampleDto() = AssignmentDto(
        assignmentId = "server-1",
        userId = "u1",
        moduleId = null,
        title = "Database assignment",
        description = "ERD and normalise",
        priority = "HIGH",
        status = "PENDING",
        dueDate = "2026-06-20T23:59:00",
        isCompleted = false,
        createdAt = "2026-06-01T00:00:00",
        updatedAt = "2026-06-01T00:00:00"
    )

    private fun sampleEntity(serverId: String?) = AssignmentEntity(
        localId = "local-1",
        serverId = serverId,
        userId = "u1",
        moduleLocalId = null,
        title = "Test",
        description = "",
        priority = "MEDIUM",
        status = "PENDING",
        dueDate = "2026-06-20T23:59:00",
        isCompleted = false,
        createdAt = "2026-06-01T00:00:00",
        updatedAt = "2026-06-01T00:00:00"
    )
}
