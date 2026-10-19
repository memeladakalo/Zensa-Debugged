package com.zensa.app.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zensa.app.data.local.ZensaDatabase
import com.zensa.app.data.entity.ModuleEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ModuleDaoTest — Room in-memory tests for ModuleDao.
 *
 * Runs on an emulator. Uses an in-memory database so each test
 * starts from a clean slate and nothing touches the real
 * zensa.db on the device.
 */
@RunWith(AndroidJUnit4::class)
class ModuleDaoTest {

    private lateinit var db: ZensaDatabase
    private lateinit var dao: ModuleDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ZensaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.moduleDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // 1
    @Test
    fun insertAndReadBack() = runBlocking {
        val module = module(localId = "m1", code = "CSC301", name = "Software Engineering")
        dao.upsert(module)

        val stored = dao.getByLocalId("m1")
        assertNotNull(stored)
        assertEquals("CSC301", stored!!.moduleCode)
    }

    // 2
    @Test
    fun observeForUserEmitsInsertedRows() = runBlocking {
        dao.upsert(module(localId = "m1", code = "CSC301", name = "Software Eng"))

        val list = dao.observeForUser("u1").first()
        assertEquals(1, list.size)
        assertEquals("CSC301", list.first().moduleCode)
    }

    // 3
    @Test
    fun softDeleteHidesFromUserQuery() = runBlocking {
        dao.upsert(module(localId = "m1", code = "CSC301", name = "Software Eng"))
        dao.softDelete("m1")

        val list = dao.observeForUser("u1").first()
        assertTrue(list.isEmpty())
    }

    // 4
    @Test
    fun softDeleteMarksRowUnsynced() = runBlocking {
        dao.upsert(module(localId = "m1", code = "CSC301", name = "Software Eng"))
        dao.softDelete("m1")

        val stored = dao.getByLocalId("m1")
        assertNotNull(stored)
        assertTrue(stored!!.isDeleted)
        assertTrue(!stored.isSynced)
    }

    // 5
    @Test
    fun markSyncedFlipsFlag() = runBlocking {
        dao.upsert(module(localId = "m1", code = "CSC301", name = "Software Eng"))
        dao.markSynced("m1")

        val stored = dao.getByLocalId("m1")
        assertTrue(stored!!.isSynced)
    }

    // 6
    @Test
    fun getUnsyncedOnlyReturnsPendingRows() = runBlocking {
        dao.upsert(module(localId = "m1", code = "A", name = "A"))
        dao.upsert(module(localId = "m2", code = "B", name = "B"))
        dao.markSynced("m1")

        val pending = dao.getUnsynced()
        assertEquals(1, pending.size)
        assertEquals("m2", pending.first().localId)
    }

    // 7
    @Test
    fun upsertReplacesExistingRow() = runBlocking {
        dao.upsert(module(localId = "m1", code = "CSC301", name = "Old Name"))
        dao.upsert(module(localId = "m1", code = "CSC301", name = "New Name"))

        val stored = dao.getByLocalId("m1")
        assertEquals("New Name", stored!!.name)
    }

    // 8
    @Test
    fun hardDeleteRemovesRow() = runBlocking {
        dao.upsert(module(localId = "m1", code = "CSC301", name = "Software Eng"))
        dao.hardDelete("m1")
        assertNull(dao.getByLocalId("m1"))
    }

    // 9
    @Test
    fun getByServerIdFindsCorrectRow() = runBlocking {
        dao.upsert(
            module(localId = "m1", code = "CSC301", name = "Software Eng")
                .copy(serverId = "server-x")
        )

        val stored = dao.getByServerId("server-x")
        assertNotNull(stored)
        assertEquals("m1", stored!!.localId)
    }

    // 10
    @Test
    fun observeForUserOrdersByCreatedAtDescending() = runBlocking {
        dao.upsert(
            module(localId = "m1", code = "A", name = "A")
                .copy(createdAt = "2026-06-01T10:00:00")
        )
        dao.upsert(
            module(localId = "m2", code = "B", name = "B")
                .copy(createdAt = "2026-06-02T10:00:00")
        )

        val list = dao.observeForUser("u1").first()
        assertEquals("m2", list.first().localId)
    }

    // ---------- Fixture ----------

    private fun module(localId: String, code: String, name: String) = ModuleEntity(
        localId = localId,
        serverId = null,
        userId = "u1",
        moduleCode = code,
        name = name,
        description = "",
        colour = "#6A4BAF",
        createdAt = "2026-06-01T10:00:00",
        updatedAt = "2026-06-01T10:00:00",
        isSynced = false,
        isDeleted = false
    )
}
