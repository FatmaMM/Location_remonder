package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    private lateinit var fakeData: FakeDataSource
    private lateinit var viewModel: RemindersListViewModel

    @ExperimentalCoroutinesApi
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    val instantTaskExecRule = InstantTaskExecutorRule()

    @Before
    fun createViewModel() = mainCoroutineRule.run {
        fakeData = FakeDataSource()
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeData)
    }

    @After
    fun clearDataSource() = runTest {
        fakeData.deleteAllReminders()
        stopKoin()
    }

    @Test
    fun testLoading() {
        mainCoroutineRule.pauseDispatcher()
        viewModel.loadReminders()
        Assert.assertEquals(viewModel.showLoading.getOrAwaitValue(), true)

        mainCoroutineRule.resumeDispatcher()
        Assert.assertEquals(viewModel.showLoading.getOrAwaitValue(), false)
    }

    @Test
    fun loadRemindersError() {
        fakeData.setError(true)
        viewModel.loadReminders()

        Assert.assertEquals(viewModel.showSnackBar.getOrAwaitValue(), "Error getting data")
        Assert.assertEquals(viewModel.showNoData.getOrAwaitValue() ,true)
    }

    @Test
    fun loadRemindersSuccess(){
        val reminder = ReminderDTO(
            "title",
            "test description",
            "test location",
            45.088,
            32.098877,
            90.toString()
        )
        fakeData.saveOneReminder(reminder)
        viewModel.loadReminders()
        Assert.assertNotNull(viewModel.remindersList.value?.get(0))
    }

}