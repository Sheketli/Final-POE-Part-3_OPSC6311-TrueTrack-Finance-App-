package com.example.truetrackfinance

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.truetrackfinance.data.repository.*
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthState
import com.example.truetrackfinance.util.SessionManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
    private val badgeRepository = mockk<BadgeRepository>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(userRepository, categoryRepository, badgeRepository, sessionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login success updates state`() = runTest {
        val user = mockk<com.example.truetrackfinance.data.db.entity.User>(relaxed = true) {
            every { id } returns 1L
        }
        coEvery { userRepository.login("test", "pass") } returns LoginResult.Success(user)
        
        viewModel.login("test", "pass")
        
        val state = viewModel.authState.getOrAwaitValue()
        assertEquals(AuthState.LoginSuccess(1L), state)
    }

    @Test
    fun `failed login attempts remaining`() = runTest {
        coEvery { userRepository.login("test", "wrong") } returns LoginResult.Failed(2)
        
        viewModel.login("test", "wrong")
        
        val state = viewModel.authState.getOrAwaitValue()
        assertEquals(AuthState.LoginFailed(2), state)
    }
}
