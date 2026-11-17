package com.example.smartfarmadvisor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartfarmadvisor.adapters.FarmAdapter
import com.example.smartfarmadvisor.data.FarmDatabase
import com.example.smartfarmadvisor.data.FarmEntity
import com.example.smartfarmadvisor.databinding.ActivityFarmListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FarmListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFarmListBinding
    private lateinit var farmAdapter: FarmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFarmListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnAddFarm.setOnClickListener {
            startActivity(Intent(this, AddFarmActivity::class.java))
        }

        observeFarms()
    }

    private fun setupRecyclerView() {
        farmAdapter = FarmAdapter(
            onDeleteClick = { farm ->
                showDeleteConfirmationDialog(farm)
            },
            onItemClick = { farm ->
                finish()
            }
        )
        binding.rvFarms.layoutManager = LinearLayoutManager(this)
        binding.rvFarms.adapter = farmAdapter
    }

    private fun observeFarms() {
        lifecycleScope.launch {
            FarmDatabase.getDatabase(this@FarmListActivity)
                .farmDao()
                .getAllFarms()
                .collectLatest { farms ->
                    farmAdapter.submitList(farms)
                    if (farms.isEmpty()) {
                        binding.txtEmptyFarms.visibility = View.VISIBLE
                        binding.rvFarms.visibility = View.GONE
                    } else {
                        binding.txtEmptyFarms.visibility = View.GONE
                        binding.rvFarms.visibility = View.VISIBLE
                    }
                }
        }
    }

    private fun showDeleteConfirmationDialog(farm: FarmEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Farm")
            .setMessage("Are you sure you want to delete '${farm.farmName}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    FarmDatabase.getDatabase(this@FarmListActivity)
                        .farmDao()
                        .deleteFarm(farm.id)
                    Toast.makeText(this@FarmListActivity, "Farm deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
