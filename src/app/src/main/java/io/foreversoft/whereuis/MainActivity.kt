package io.foreversoft.whereuis

import android.os.Bundle
import android.content.Intent
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Button
import android.view.View
import io.foreversoft.whereuis.model.SecurityInfo
import kotlinx.android.synthetic.main.activity_main.*
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.Scope
import org.blockstack.android.sdk.model.UserData
import org.blockstack.android.sdk.model.toBlockstackConfig
import net.kibotu.pgp.Pgp
import java.util.UUID
import java.security.SecureRandom
import com.google.gson.Gson
import org.blockstack.android.sdk.model.CryptoOptions
import org.blockstack.android.sdk.model.PutFileOptions

class MainActivity : AppCompatActivity() {

    private var _blockstackSession: BlockstackSession? = null
    private lateinit var textMessage: TextView
    private lateinit var testEncryptionButton: Button

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                textMessage.setText(R.string.title_home)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                textMessage.setText(R.string.title_dashboard)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                textMessage.setText(R.string.title_notifications)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }



    private fun onSignIn(userData: UserData) {
        userDataTextView.text = "Signed in as ${userData.profile?.name}"
        signInButton.isEnabled = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        textMessage = findViewById(R.id.message)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        val scopes = arrayOf(Scope.StoreWrite)
        val config = "https://whereuis.z19.web.core.windows.net"
            .toBlockstackConfig(scopes, "/redirect.html")

        _blockstackSession = BlockstackSession(this, config)

        signInButton.isEnabled = true

        signInButton.setOnClickListener { view: View ->
            blockstackSession().redirectUserToSignIn {
                // only called on error
            }
        }

        testEncryptionButton = findViewById(R.id.testEncryptionButton)

        testEncryptionButton.setOnClickListener {

            //


            val sr = SecureRandom()
            val pwd = "${sr.nextFloat()}_${sr.nextFloat()}"
            val keyGen = Pgp.generateKeyRingGenerator(pwd.toCharArray())
            val prk = Pgp.genPGPPrivKey(keyGen)
            val puk = Pgp.genPGPPublicKey(keyGen)

            val securityInfo = SecurityInfo(pwd, puk, prk)

            val securityInfoJson = Gson().toJson(securityInfo)

            blockstackSession().putFile("/whereuis/securityInfo.json", securityInfoJson,  PutFileOptions(true))

            val jsonEncrypted = blockstackSession().encryptContent(securityInfoJson, CryptoOptions())


            val enc = jsonEncrypted.value!!.json.toString()


            val dec = blockstackSession().decryptContent(enc, false, CryptoOptions())
            val secInfo = dec.value as String
            Log.i("", "")

//            val friendLocPath = UUID.randomUUID().toString().replace("-", "").toUpperCase()
//            val friendId = "myfriend01"
//            val encryptedFriendId = Pgp.encrypt(friendId)
//            val decryptedFriendId = Pgp.decrypt(encryptedFriendId.toString(), password)
        }

        if (intent?.action == Intent.ACTION_VIEW) {
            // handle the redirect from sign in
            handleAuthResponse(intent)
        }
    }

    private fun handleAuthResponse(intent: Intent) {
        val response = intent.dataString
        if (response != null) {
            val authResponseTokens = response.split(':')

            if (authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]

                blockstackSession().handlePendingSignIn(authResponse, { userData ->
                    if (userData.hasValue) {
                        // The user is now signed in!
                        runOnUiThread {
                            onSignIn(userData.value!!)
                        }
                    }
                })
            }
        }
    }

    fun blockstackSession() : BlockstackSession {
        val session = _blockstackSession
        if(session != null) {
            return session
        } else {
            throw IllegalStateException("No session.")
        }
    }
}
