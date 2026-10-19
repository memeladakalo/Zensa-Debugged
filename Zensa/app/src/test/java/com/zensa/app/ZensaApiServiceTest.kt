package com.zensa.app

import com.zensa.app.data.remote.dto.LoginRequestDto
import com.zensa.app.data.remote.dto.RegisterRequestDto
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.zensa.app.data.remote.ZensaApiService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * ZensaApiServiceTest
 *
 * Verifies the Retrofit wiring and JSON contract with the Spring
 * Boot backend. Uses MockWebServer so the tests are hermetic — no
 * real server and no real network.
 *
 * These are JVM unit tests. MockWebServer provides a local fake
 * HTTP server, so no Android emulator is required.
 */
class ZensaApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ZensaApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ZensaApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // 1
    @Test
    fun registerPostsJsonAndParsesResponse() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "userId": "u-1",
                      "name": "Ada",
                      "email": "ada@zensa.app",
                      "message": "Registration successful"
                    }
                    """.trimIndent()
                )
        )

        val response = api.register(
            RegisterRequestDto("Ada", "ada@zensa.app", "password123")
        )

        assertTrue(response.isSuccessful)
        assertEquals("u-1", response.body()!!.userId)
        assertEquals("ada@zensa.app", response.body()!!.email)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/auth/register", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"email\":\"ada@zensa.app\""))
    }

    // 2
    @Test
    fun loginParsesJwtToken() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "token": "eyJhbGciOiJIUzI1NiJ9.fake",
                      "userId": "u-1",
                      "name": "Ada",
                      "email": "ada@zensa.app"
                    }
                    """.trimIndent()
                )
        )

        val response = api.login(LoginRequestDto("ada@zensa.app", "password123"))

        assertTrue(response.isSuccessful)
        assertNotNull(response.body()!!.token)
        assertTrue(response.body()!!.token.startsWith("eyJ"))
    }

    // 3
    @Test
    fun failedLoginReturns401AndNullBody() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":401,"message":"Incorrect email or password"}""")
        )

        val response = api.login(LoginRequestDto("ada@zensa.app", "wrong"))

        assertEquals(401, response.code())
        assertTrue(response.body() == null)
    }

    // 4
    @Test
    fun duplicateEmailReturns409WithMessage() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":409,"message":"An account with this email already exists"}""")
        )

        val response = api.register(
            RegisterRequestDto("Ada", "ada@zensa.app", "password123")
        )

        assertEquals(409, response.code())
        val raw = response.errorBody()!!.string()
        assertTrue(raw.contains("already exists"))
    }

    // 5
    // 5
    @Test
    fun getModulesParsesListResponse() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    [
                      {
                        "moduleId": "m1",
                        "userId": "u1",
                        "name": "Software Development"
                      }
                    ]
                    """.trimIndent()
                )
        )

        val response = api.getModules()

        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
        assertEquals(1, response.body()!!.size)
        assertEquals("m1", response.body()!![0].moduleId)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
    }
}