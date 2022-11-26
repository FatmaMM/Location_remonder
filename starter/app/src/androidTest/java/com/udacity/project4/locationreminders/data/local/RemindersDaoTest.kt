package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase
    private lateinit var dao: RemindersDao

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
        ).build()

        dao = database.reminderDao()
    }

    @After
    fun clearDatabase() {
        database.close()
    }

    @Test
    fun insertInDatabaseSuccessfully() = runBlockingTest {
        dao.saveReminder(reminderDTO)

        Assert.assertEquals(dao.getReminders().size, 1)
        Assert.assertEquals(dao.getReminders().get(0), reminderDTO)
    }

    @Test
    fun getFromDB() = runBlockingTest {
        dao.saveReminder(reminderDTO)

        val reminder = dao.getReminderById(reminderDTO.id)

        Assert.assertNotNull(reminder)
        Assert.assertEquals(reminder?.title, reminderDTO.title)
        Assert.assertEquals(reminder?.description, reminderDTO.description)
        Assert.assertEquals(reminder?.location, reminderDTO.location)
        Assert.assertEquals(reminder?.latitude, reminderDTO.latitude)
        Assert.assertEquals(reminder?.longitude, reminderDTO.longitude)
    }

    @Test
    fun deleteDB() = runBlockingTest {
        dao.deleteAllReminders()
        Assert.assertEquals(dao.getReminders().size, 0)
    }
}