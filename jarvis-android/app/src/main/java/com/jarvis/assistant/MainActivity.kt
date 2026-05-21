package com.jarvis.assistant

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jarvis.assistant.databinding.ActivityMainBinding
import com.jarvis.assistant.tts.ElevenLabsService
import com.jarvis.assistant.ui.ChatAdapter
import com.jarvis.assistant.ui.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()
    private val adapter = ChatAdapter()

    private var pulseAnimator: AnimatorSet? = null

    private val requestMic = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.startListening()
        else Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "JARVIS"

        binding.rvChat.apply {
            adapter = this@MainActivity.adapter
            layoutManager = LinearLayoutManager(this@MainActivity).also { it.stackFromEnd = true }
        }

        binding.btnMic.setOnClickListener {
            when (vm.state.value) {
                MainViewModel.State.IDLE     -> activateMic()
                MainViewModel.State.LISTENING -> vm.stopListening()
                MainViewModel.State.SPEAKING  -> vm.stopSpeaking()
                else                         -> Unit   // THINKING — ignore taps
            }
        }

        observeViewModel()
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        vm.state.observe(this) { state -> applyState(state) }

        vm.messages.observe(this) { msgs ->
            adapter.submitList(msgs.toList())
            if (msgs.isNotEmpty()) binding.rvChat.smoothScrollToPosition(msgs.size - 1)
        }

        vm.error.observe(this) { err ->
            err?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun applyState(state: MainViewModel.State) {
        stopPulse()
        binding.btnMic.alpha = 1f

        when (state) {
            MainViewModel.State.IDLE -> {
                binding.tvStatus.text = getString(R.string.status_idle)
                binding.statusDot.setBackgroundResource(R.drawable.dot_idle)
                binding.pulseRing.visibility = View.INVISIBLE
            }
            MainViewModel.State.LISTENING -> {
                binding.tvStatus.text = getString(R.string.status_listening)
                binding.statusDot.setBackgroundResource(R.drawable.dot_listening)
                binding.pulseRing.visibility = View.VISIBLE
                startPulse(binding.pulseRing)
            }
            MainViewModel.State.THINKING -> {
                binding.tvStatus.text = getString(R.string.status_thinking)
                binding.statusDot.setBackgroundResource(R.drawable.dot_thinking)
                binding.pulseRing.visibility = View.INVISIBLE
                binding.btnMic.alpha = 0.4f
            }
            MainViewModel.State.SPEAKING -> {
                binding.tvStatus.text = getString(R.string.status_speaking)
                binding.statusDot.setBackgroundResource(R.drawable.dot_speaking)
                binding.pulseRing.visibility = View.VISIBLE
                startPulse(binding.pulseRing)
            }
        }
    }

    // ── Pulse animation ───────────────────────────────────────────────────────

    private fun startPulse(target: View) {
        val sx = ObjectAnimator.ofFloat(target, "scaleX", 1f, 1.5f, 1f)
        val sy = ObjectAnimator.ofFloat(target, "scaleY", 1f, 1.5f, 1f)
        val al = ObjectAnimator.ofFloat(target, "alpha",  0.9f, 0f, 0.9f)
        pulseAnimator = AnimatorSet().apply {
            playTogether(sx, sy, al)
            duration = 1200
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        binding.pulseRing.alpha = 0.9f
        binding.pulseRing.scaleX = 1f
        binding.pulseRing.scaleY = 1f
    }

    // ── Mic permission ────────────────────────────────────────────────────────

    private fun activateMic() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            vm.startListening()
        } else {
            requestMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Options menu ──────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_voice -> { showVoicePicker(); true }
        R.id.action_clear -> { vm.clearConversation(); true }
        else              -> super.onOptionsItemSelected(item)
    }

    private fun showVoicePicker() {
        val labels = arrayOf("Adam — deep, authoritative", "Antoni — warm, friendly", "Rachel — clear, professional")
        val ids    = arrayOf(ElevenLabsService.VOICE_ADAM, ElevenLabsService.VOICE_ANTONI, ElevenLabsService.VOICE_RACHEL)
        AlertDialog.Builder(this)
            .setTitle("Select JARVIS Voice")
            .setItems(labels) { _, i ->
                vm.setVoice(ids[i])
                Toast.makeText(this, "Voice updated", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
