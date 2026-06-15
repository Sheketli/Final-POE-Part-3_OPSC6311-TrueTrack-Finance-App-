package com.example.truetrackfinance.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.truetrackfinance.R
import com.example.truetrackfinance.databinding.FragmentProfileBinding
import com.example.truetrackfinance.ui.viewmodel.ProfileViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * ProfileFragment displays user account details, stats, and navigation to sub-settings.
 */
@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // AuthViewModel for session handling and base user info
    private val authViewModel: AuthViewModel by viewModels()
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ProfileFragment", "onViewCreated: Initializing Profile screen")

        val userId = authViewModel.getActiveUserId()
        viewModel.initialise(userId)
        
        setupMenuRows()
        observeViewModel()
        setupClickListeners()
    }

    /**
     * Set up the custom menu items with icons and localized titles/subtitles.
     */
    private fun setupMenuRows() {
        binding.rowSettings.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_settings)
            tvMenuTitle.text = getString(R.string.settings)
            root.findViewById<TextView>(R.id.tvMenuSubtitle).text = "App preferences"
        }
        binding.rowCategories.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_wallet)
            tvMenuTitle.text = getString(R.string.categories)
            root.findViewById<TextView>(R.id.tvMenuSubtitle).text = "Manage spending categories"
        }
        binding.rowRecurring.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_sync)
            tvMenuTitle.text = "Recurring Transactions"
            tvMenuSubtitle.text = "Manage auto-logged expenses"
        }
        binding.rowExportCsv.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_export)
            tvMenuTitle.text = getString(R.string.export_csv)
            root.findViewById<TextView>(R.id.tvMenuSubtitle).text = "Download your data"
        }
    }

    /**
     * Binds user data and session stats to the UI.
     */
    private fun observeViewModel() {
        // Sync with Auth session to get real Full Name and Email
        authViewModel.getActiveUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                Log.v("ProfileFragment", "Updating user info: ${it.email}")
                // Sync display name and email fields
                binding.tvUsername.text = it.fullName
                binding.tvEmail.text = it.email

                // Extraction logic for User Initials (e.g., "Liam Dlamini" -> "LD")
                val initials = it.fullName.split(" ")
                    .filter { word -> word.isNotEmpty() }
                    .take(2)
                    .map { word -> word[0].uppercaseChar() }
                    .joinToString("")
                binding.tvProfileInitials.text = initials

                // Binding the user handle (@name) for the profile screen
                binding.tvProfileHandle.text = getString(R.string.handle_format, it.username.lowercase().replace(" ", "_"))
            }
        }
    }

    private fun setupClickListeners() {
        binding.rowSettings.root.setOnClickListener {
            Log.d("ProfileFragment", "Navigating to Settings")
            findNavController().navigate(R.id.action_profile_to_settings)
        }
        binding.rowCategories.root.setOnClickListener {
            Log.d("ProfileFragment", "Navigating to Categories")
            findNavController().navigate(R.id.action_profile_to_categories)
        }
        binding.rowRecurring.root.setOnClickListener {
            Log.d("ProfileFragment", "Navigating to Recurring Transactions")
            findNavController().navigate(R.id.action_profile_to_recurring)
        }
        binding.rowExportCsv.root.setOnClickListener {
            Log.d("ProfileFragment", "Starting CSV export")
            viewModel.exportToCsv(requireContext())
        }
        binding.btnViewAllBadges.setOnClickListener {
            Log.d("ProfileFragment", "Navigating to Achievements from badges card")
            findNavController().navigate(R.id.action_profile_to_achievements)
        }
        binding.btnLogout.setOnClickListener {
            Log.i("ProfileFragment", "User requested Sign Out")
            authViewModel.logout()
            startActivity(android.content.Intent(requireContext(), com.example.truetrackfinance.ui.AuthActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
