package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    private var error = false

    var remindersData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()


    fun setError(value: Boolean) {
        this.error = value
    }
     fun saveOneReminder(reminder: ReminderDTO) {
        remindersData.put(reminder.id, reminder)
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (error) {
            Result.Error("Error getting data")
        } else
            Result.Success(remindersData.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersData.put(reminder.id, reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        remindersData[id]?.let {
            return Result.Success(it)
        } ?: run {
            return Result.Error("Could not find Reminder")
        }
    }

    override suspend fun deleteAllReminders() {
        remindersData.clear()
    }


}