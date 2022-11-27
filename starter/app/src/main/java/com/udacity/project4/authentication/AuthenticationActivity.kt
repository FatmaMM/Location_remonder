package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.udacity.project4.databinding.ActivityAuthenticationBinding

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    lateinit var binding: ActivityAuthenticationBinding
    private val auth = FirebaseAuth.getInstance()
    private val loginInIntent: Intent = AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAvailableProviders(
            listOf(
                AuthUI.IdpConfig.GoogleBuilder().build(),
                AuthUI.IdpConfig.EmailBuilder().build()
            )
        )
        .setIsSmartLockEnabled(false)
        .setLogo(R.drawable.map)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth.currentUser?.let {
            navigateToRemindersActivity()
        } ?: run {
            binding = ActivityAuthenticationBinding.inflate(layoutInflater)
            setContentView(binding.root)
            binding.loginBtn.setOnClickListener { loginLauncher.launch(loginInIntent) }
        }
    }

    private fun navigateToRemindersActivity() {
        var intent = Intent(this, RemindersActivity::class.java)
        startActivity(intent)
        finish()
    }

    private val loginLauncher = registerForActivityResult(StartActivityForResult())
    { result: ActivityResult ->
        val idpResponse = IdpResponse.fromResultIntent(result.data)
        if (result.resultCode == Activity.RESULT_OK) {
            navigateToRemindersActivity()
        } else {
            idpResponse?.let {
                Toast.makeText(applicationContext, "log in unsuccessful", Toast.LENGTH_SHORT).show()
                navigateToRemindersActivity()
            } ?: run {
                Toast.makeText(
                    applicationContext,
                    idpResponse?.error?.localizedMessage.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}
