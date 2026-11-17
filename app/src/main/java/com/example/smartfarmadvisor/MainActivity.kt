package com.example.smartfarmadvisor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.smartfarmadvisor.databinding.ActivityMainBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnGuestSignIn.setOnClickListener {
            navigateToDashboard()
        }

        binding.btnSignOut.setOnClickListener {
            auth.signOut()
            showSignedOut()
        }

        if (auth.currentUser != null) {
            navigateToDashboard()
        } else {
            showSignedOut()
        }
    }

    private fun signInWithGoogle() {
        binding.txtStatus.text = "Opening Google sign-in..."

        lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = request
                )

                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    val firebaseCredential = GoogleAuthProvider.getCredential(
                        googleCredential.idToken,
                        null
                    )

                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this@MainActivity) { task ->
                            if (task.isSuccessful) {
                                navigateToDashboard()
                            } else {
                                binding.txtStatus.text =
                                    "Login failed: ${task.exception?.localizedMessage}"
                            }
                        }
                } else {
                    binding.txtStatus.text = "Google account not selected."
                }

            } catch (e: GetCredentialCancellationException) {
                binding.txtStatus.text = "Sign-in cancelled."
            } catch (e: NoCredentialException) {
                binding.txtStatus.text =
                    "No Google account found on this device/emulator. Please add a Google account in device Settings -> Accounts or sign in to Google Play."
            } catch (e: GetCredentialException) {
                binding.txtStatus.text =
                    "Credential error: ${e.message}\n(Make sure a Google account is logged into device/emulator Settings)"
            } catch (e: Exception) {
                binding.txtStatus.text =
                    "Sign-in error: ${e.localizedMessage ?: "Please try again."}"
            }
        }
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun showSignedOut() {
        binding.txtStatus.text = "Sign in to manage your farm"
        binding.btnGoogleSignIn.visibility = android.view.View.VISIBLE
        binding.btnSignOut.visibility = android.view.View.GONE
    }
}
