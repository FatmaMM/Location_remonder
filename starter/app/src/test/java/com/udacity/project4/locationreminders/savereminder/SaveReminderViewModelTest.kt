package com.udacity.project4.locationreminders.savereminder

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.MainCoroutineRule
import com.udacity.project4.locationreminders.data.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    private lateinit var fakeData: FakeDataSource
    private lateinit var viewModel: SaveReminderViewModel

    @ExperimentalCoroutinesApi
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    val instantTaskExecRule = InstantTaskExecutorRule()

    @Before
    fun createViewModel() = mainCoroutineRule.run {
        fakeData = FakeDataSource()
        viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), fakeData)
    }

    @After
    fun clearDataSource() = runTest {
        fakeData.deleteAllReminders()
        stopKoin()
    }

    @Test
    fun validateAndSaveReminderOnNullValues() {
        val reminderDataItem =
            ReminderDataItem(null, null, null, null, null, null.toString())
        viewModel.validateAndSaveReminder(reminderDataItem)
        assertEquals(viewModel.validateEnteredData(reminderDataItem), false)
        assertEquals(viewModel.showSnackBarInt.getOrAwaitValue(), R.string.err_enter_title)
    }

    @Test
    fun testLoading() {
        mainCoroutineRule.pauseDispatcher()
        val reminderDataItem =
            ReminderDataItem("title", "esc", "test location", 47.06543, 27.57545353, 98.toString())

        viewModel.validateAndSaveReminder(reminderDataItem)
        assertEquals(viewModel.showLoading.getOrAwaitValue(), true)

        mainCoroutineRule.resumeDispatcher()
        assertEquals(viewModel.showLoading.getOrAwaitValue(), false)
    }

    @Test
    fun validateAndSaveReminderValidItemSucceeds() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val reminderDataItem =
            ReminderDataItem("title", "esc", "test location", 47.06543, 27.57545353, 98.toString())
        viewModel.validateAndSaveReminder(reminderDataItem)
        assertEquals(viewModel.validateEnteredData(reminderDataItem), true)
        assertEquals(viewModel.showToast.getOrAwaitValue(), app.getString(R.string.reminder_saved))
    }


    @Test
    fun validateAndSaveReminderItemWithoutLocation() {
        val reminderDataItem =
            ReminderDataItem("title", "esc", null, null, null, 98.toString())
        viewModel.validateAndSaveReminder(reminderDataItem)
        assertEquals(viewModel.validateEnteredData(reminderDataItem), false)
        assertEquals(viewModel.showSnackBarInt.getOrAwaitValue(), R.string.err_select_location)
    }
}