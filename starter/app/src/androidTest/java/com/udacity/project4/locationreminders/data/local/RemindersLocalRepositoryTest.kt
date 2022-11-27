package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var database: RemindersDatabase
    private lateinit var repo: RemindersLocalRepository

    private val reminderDTO = ReminderDTO(
        title = "title",
        description = "description",
        location = "location",
        latitude = 30.29838,
        longitude = 27.9847,
    )

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        repo =
            RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun clearDatabase() {
        database.close()
    }

    @Test
    fun insertInDatabaseSuccessfully() = runBlocking {
        repo.saveReminder(reminderDTO)
        val result = repo.getReminders() as Result.Success
        Assert.assertEquals(result.data.size, 1)
        Assert.assertEquals(result.data.get(0), reminderDTO)
    }

    @Test
    fun getFromRepo() = runBlocking {
        repo.saveReminder(reminderDTO)

        val reminder = repo.getReminder(reminderDTO.id) as Result.Success

        Assert.assertNotNull(reminder)
        Assert.assertEquals(reminder.data.title, reminderDTO.title)
        Assert.assertEquals(reminder.data.description, reminderDTO.description)
        Assert.assertEquals(reminder.data.location, reminderDTO.location)
        Assert.assertEquals(reminder.data.latitude, reminderDTO.latitude)
        Assert.assertEquals(reminder.data.longitude, reminderDTO.longitude)
    }

    @Test
    fun getFromRepoWithError() = runBlocking {
        val reminderError = repo.getReminder(23.toString()) as Result.Error
        Assert.assertNotNull(reminderError)
        Assert.assertEquals(reminderError.message, "Reminder not found!")
    }

    @Test
    fun deleteDataFromRepo() = runBlocking {
        repo.deleteAllReminders()
        val reminder = repo.getReminders() as Result.Success
        Assert.assertEquals(reminder.data.size, 0)
    }

}