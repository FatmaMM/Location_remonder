package com.udacity.project4

import android.app.Application
import android.content.Context
import android.os.Build
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.*
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.junit.After

@RunWith(AndroidJUnit4::class)
@LargeTest
class RemindersActivityTest : AutoCloseKoinTest() {

    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private lateinit var repo: ReminderDataSource
    private lateinit var decorView: View

    @get:Rule
    val activityRule = ActivityScenarioRule(RemindersActivity::class.java)


    @Before
    fun init() {
        stopKoin()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    getApplicationContext(),
                    get() as ReminderDataSource

                )
            }

            single {
                SaveReminderViewModel(
                    getApplicationContext(),
                    get() as ReminderDataSource
                )
            }

            single<ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(getApplicationContext()) }
            single { (locationManger: MLocationHelper) ->
                LocationHelperManger(
                    activity = getApplicationContext(),
                    locationManager = locationManger
                )
            }
            single { (context: Context, mPermissionHelper: PermissionHelper) ->
                PermissionHelperManager(
                    activity = getApplicationContext(),
                    mPermissionHelper = mPermissionHelper
                )
            }
        }

        //declare a new koin module
        startKoin {
            androidContext(getApplicationContext())
            modules(listOf(myModule))
        }

        //Get our real repository
        repo = get()

        //clear the data to start fresh
        runBlocking {
            repo.deleteAllReminders()
        }
        activityRule.scenario.onActivity { activity ->
            decorView = activity.window.decorView
        }
    }

    @Before
    fun registerIdlingResource(): Unit = IdlingRegistry.getInstance().run {
        register(EspressoIdlingResource.countingIdlingResource)
        register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource(): Unit = IdlingRegistry.getInstance().run {
        unregister(EspressoIdlingResource.countingIdlingResource)
        unregister(dataBindingIdlingResource)
    }

    @Test
    fun launchActivityWithData() {
        val reminderDTO = ReminderDTO(
            "Title",
            "description",
            "location",
            30.2134,
            27.432,
            90.toString()
        )

        runBlocking {
            repo.saveReminder(reminderDTO)
        }

        val activityScenarioLauncher = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenarioLauncher)

        onView(withText(reminderDTO.title)).check(matches(isDisplayed()))
        onView(withText(reminderDTO.description)).check(matches(isDisplayed()))
        onView(withText(reminderDTO.location)).check(matches(isDisplayed()))
        activityScenarioLauncher.close()
    }

    @Test
    fun createReminderAndBack() {
        val activityScenarioLauncher = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenarioLauncher)

        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())

        onView(withId(R.id.nav_host_fragment)).perform(click())
        onView(withId(R.id.save_btn)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("description"))
        closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withText("title")).check(matches(isDisplayed()))
        onView(withText("description")).check(matches(isDisplayed()))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            onView(withText(endsWith(getApplicationContext<Application>().getString(R.string.reminder_saved)))).inRoot(
                withDecorView(not(`is`(decorView)))
            )
                .check(matches(isDisplayed()))
        }
        activityScenarioLauncher.close()
    }

}
