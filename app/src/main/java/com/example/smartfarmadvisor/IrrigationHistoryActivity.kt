package com.example.smartfarmadvisor

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartfarmadvisor.adapters.IrrigationAdapter
import com.example.smartfarmadvisor.data.FarmDatabase
import com.example.smartfarmadvisor.data.FarmEntity
import com.example.smartfarmadvisor.data.IrrigationEntity
import com.example.smartfarmadvisor.databinding.ActivityIrrigationHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class IrrigationHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIrrigationHistoryBinding
    private lateinit var adapter: IrrigationAdapter
    private var farmList: List<FarmEntity> = emptyList()
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIrrigationHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupDatePicker()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSaveIrrigation.setOnClickListener {
            saveIrrigationRecord()
        }

        loadFarms()
        observeIrrigationHistory()
    }

    private fun setupRecyclerView() {
        adapter = IrrigationAdapter(
            onDeleteClick = { record ->
                showDeleteConfirmation(record)
            }
        )
        binding.rvIrrigationHistory.layoutManager = LinearLayoutManager(this)
        binding.rvIrrigationHistory.adapter = adapter
    }

    private fun setupDatePicker() {
        updateDateField()

        binding.etDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateDateField()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateField() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        binding.etDate.setText(sdf.format(calendar.time))
    }

    private var historyJob: kotlinx.coroutines.Job? = null
    private var selectedFilterFarmId: Int? = null

    private fun loadFarms() {
        lifecycleScope.launch {
            FarmDatabase.getDatabase(this@IrrigationHistoryActivity)
                .farmDao()
                .getAllFarms()
                .collectLatest { farms ->
                    farmList = farms
                    val farmNames = if (farms.isEmpty()) {
                        listOf("No farms registered")
                    } else {
                        farms.map { "${it.farmName} (${it.cropName})" }
                    }

                    val spinnerAdapter = ArrayAdapter(
                        this@IrrigationHistoryActivity,
                        android.R.layout.simple_spinner_item,
                        farmNames
                    )
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerFarm.adapter = spinnerAdapter

                    val filterOptions = listOf("All Farms") + farms.map { it.farmName }
                    val filterAdapter = ArrayAdapter(
                        this@IrrigationHistoryActivity,
                        android.R.layout.simple_spinner_item,
                        filterOptions
                    )
                    filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerFilterFarm.adapter = filterAdapter

                    binding.spinnerFilterFarm.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                            selectedFilterFarmId = if (position > 0 && position - 1 < farms.size) {
                                farms[position - 1].id
                            } else {
                                null
                            }
                            observeIrrigationHistory()
                        }

                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    }
                }
        }
    }

    private fun observeIrrigationHistory() {
        historyJob?.cancel()
        historyJob = lifecycleScope.launch {
            val dao = FarmDatabase.getDatabase(this@IrrigationHistoryActivity).irrigationDao()
            val flow = selectedFilterFarmId?.let { dao.getIrrigationHistoryForFarm(it) } ?: dao.getAllIrrigationHistory()

            flow.collectLatest { records ->
                adapter.submitList(records)
                if (records.isEmpty()) {
                    binding.txtEmptyHistory.visibility = View.VISIBLE
                    binding.rvIrrigationHistory.visibility = View.GONE
                } else {
                    binding.txtEmptyHistory.visibility = View.GONE
                    binding.rvIrrigationHistory.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun saveIrrigationRecord() {
        if (farmList.isEmpty()) {
            Toast.makeText(this, "Please add a farm first", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedIndex = binding.spinnerFarm.selectedItemPosition
        if (selectedIndex < 0 || selectedIndex >= farmList.size) {
            Toast.makeText(this, "Please select a valid farm", Toast.LENGTH_SHORT).show()
            return
        }

        val farm = farmList[selectedIndex]
        val dateStr = binding.etDate.text.toString().trim()
        val durationStr = binding.etDuration.text.toString().trim()

        if (durationStr.isEmpty()) {
            binding.etDuration.error = "Enter duration in minutes"
            return
        }

        val durationMins = durationStr.toIntOrNull()
        if (durationMins == null || durationMins <= 0) {
            binding.etDuration.error = "Enter valid duration > 0"
            return
        }

        val entity = IrrigationEntity(
            farmId = farm.id,
            farmName = farm.farmName,
            date = dateStr,
            durationMinutes = durationMins,
            timestamp = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            FarmDatabase.getDatabase(this@IrrigationHistoryActivity)
                .irrigationDao()
                .insertIrrigation(entity)

            binding.etDuration.text.clear()
            Toast.makeText(this@IrrigationHistoryActivity, "Irrigation session recorded!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation(record: IrrigationEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Record")
            .setMessage("Delete irrigation record for ${record.farmName} on ${record.date}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    FarmDatabase.getDatabase(this@IrrigationHistoryActivity)
                        .irrigationDao()
                        .deleteIrrigation(record.id)
                    Toast.makeText(this@IrrigationHistoryActivity, "Record deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
