package com.example.smartfarmadvisor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartfarmadvisor.adapters.DailyForecastItem
import com.example.smartfarmadvisor.adapters.WeatherForecastAdapter
import com.example.smartfarmadvisor.data.FarmDatabase
import com.example.smartfarmadvisor.data.FarmEntity
import com.example.smartfarmadvisor.data.api.RetrofitClient
import com.example.smartfarmadvisor.databinding.ActivityDashboardBinding
import com.example.smartfarmadvisor.domain.IrrigationCalculator
import com.example.smartfarmadvisor.domain.RecommendationStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var forecastAdapter: WeatherForecastAdapter

    private var farmsList: List<FarmEntity> = emptyList()
    private var selectedFarm: FarmEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupUserInfo()
        setupForecastRecyclerView()
        setupListeners()
        observeFarms()
    }

    private fun setupUserInfo() {
        val user = auth.currentUser
        val farmerName = user?.displayName ?: "Farmer"
        val farmerEmail = user?.email ?: "No email associated"

        binding.txtWelcomeFarmer.text = "Welcome, $farmerName!"
        binding.txtFarmerEmail.text = farmerEmail
    }

    private fun setupForecastRecyclerView() {
        forecastAdapter = WeatherForecastAdapter()
        binding.rvForecast.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvForecast.adapter = forecastAdapter
    }

    private fun setupListeners() {
        binding.btnSignOut.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnAddFarm.setOnClickListener {
            startActivity(Intent(this, AddFarmActivity::class.java))
        }

        binding.btnViewFarms.setOnClickListener {
            startActivity(Intent(this, FarmListActivity::class.java))
        }

        binding.btnIrrigationHistory.setOnClickListener {
            startActivity(Intent(this, IrrigationHistoryActivity::class.java))
        }

        binding.btnRefreshWeather.setOnClickListener {
            selectedFarm?.let { fetchWeatherForFarm(it) } ?: run {
                Toast.makeText(this, "Please select or add a farm first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeFarms() {
        lifecycleScope.launch {
            FarmDatabase.getDatabase(this@DashboardActivity)
                .farmDao()
                .getAllFarms()
                .collectLatest { farms ->
                    farmsList = farms
                    binding.txtTotalFarms.text = "${farms.size} Farm${if (farms.size != 1) "s" else ""}"

                    if (farms.isEmpty()) {
                        binding.txtFarmCoordinates.text = "No farms registered yet. Click '+ Add Farm' to get started."
                        showDefaultEmptyWeatherState()
                    } else {
                        setupFarmSpinner(farms)
                    }
                }
        }
    }

    private fun setupFarmSpinner(farms: List<FarmEntity>) {
        val farmNames = farms.map { "${it.farmName} (${it.cropName})" }
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            farmNames
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerActiveFarm.adapter = spinnerAdapter

        val currentSelectedId = selectedFarm?.id
        val targetIndex = farms.indexOfFirst { it.id == currentSelectedId }.let { if (it >= 0) it else 0 }

        binding.spinnerActiveFarm.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in farms.indices) {
                    val farm = farms[position]
                    selectedFarm = farm
                    binding.txtFarmCoordinates.text = String.format(
                        Locale.US,
                        "Crop: %s  •  Lat: %.4f, Lon: %.4f",
                        farm.cropName,
                        farm.latitude,
                        farm.longitude
                    )
                    fetchWeatherForFarm(farm)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        if (farms.isNotEmpty()) {
            binding.spinnerActiveFarm.setSelection(targetIndex)
        }
    }

    private fun fetchWeatherForFarm(farm: FarmEntity) {
        binding.pbWeatherLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getWeatherForecast(
                    latitude = farm.latitude,
                    longitude = farm.longitude
                )

                binding.pbWeatherLoading.visibility = View.GONE

                val dailyTime = response.daily?.time ?: emptyList()
                val dailyEt0 = response.daily?.et0FaoEvapotranspiration ?: emptyList()
                val dailyRain = response.daily?.precipitationSum ?: emptyList()
                val dailyTemp = response.daily?.temperature2mMax ?: emptyList()
                val hourlyTime = response.hourly?.time ?: emptyList()
                val hourlySoil = response.hourly?.soilMoisture0To1cm ?: emptyList()

                val todayEt0 = dailyEt0.firstOrNull()
                val todayRainfall = dailyRain.firstOrNull()
                val todayMaxTemp = dailyTemp.firstOrNull()

                val currentHourPrefix = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.US).format(java.util.Date())
                var todaySoilMoisture: Double? = null

                val currentHourIdx = hourlyTime.indexOfFirst { it.startsWith(currentHourPrefix) }
                if (currentHourIdx != -1 && currentHourIdx < hourlySoil.size && hourlySoil[currentHourIdx] != null) {
                    todaySoilMoisture = hourlySoil[currentHourIdx]
                } else {
                    val todayValidSoil = (0 until minOf(24, hourlySoil.size)).mapNotNull { hourlySoil[it] }
                    if (todayValidSoil.isNotEmpty()) {
                        todaySoilMoisture = todayValidSoil.last()
                    }
                }

                binding.txtMetricTemp.text = todayMaxTemp?.let { String.format(Locale.US, "%.1f °C", it) } ?: "-- °C"
                binding.txtMetricRainfall.text = todayRainfall?.let { String.format(Locale.US, "%.1f mm", it) } ?: "-- mm"
                binding.txtMetricET0.text = todayEt0?.let { String.format(Locale.US, "%.1f mm", it) } ?: "-- mm"
                binding.txtMetricSoilMoisture.text = todaySoilMoisture?.let { String.format(Locale.US, "%.3f m³/m³", it) } ?: "-- m³/m³"

                val recommendation = IrrigationCalculator.calculateRecommendation(
                    et0 = todayEt0,
                    rainfall = todayRainfall,
                    soilMoisture = todaySoilMoisture
                )

                updateRecommendationCard(recommendation.status, recommendation.explanation)

                val forecastList = ArrayList<DailyForecastItem>()
                val daysCount = minOf(dailyTime.size, dailyEt0.size, dailyRain.size, dailyTemp.size)

                for (i in 0 until daysCount) {
                    forecastList.add(
                        DailyForecastItem(
                            date = dailyTime[i],
                            maxTemp = dailyTemp[i],
                            rainfall = dailyRain[i],
                            et0 = dailyEt0[i]
                        )
                    )
                }

                forecastAdapter.submitList(forecastList)

            } catch (e: Exception) {
                binding.pbWeatherLoading.visibility = View.GONE
                Toast.makeText(
                    this@DashboardActivity,
                    "Failed to load weather: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()

                updateRecommendationCard(
                    RecommendationStatus.INSUFFICIENT_DATA,
                    "Weather network request failed: ${e.localizedMessage}. Please check internet connection."
                )
            }
        }
    }

    private fun updateRecommendationCard(status: RecommendationStatus, explanation: String) {
        binding.txtRecommendationExplanation.text = explanation

        when (status) {
            RecommendationStatus.IRRIGATE -> {
                binding.cardRecommendation.setBackgroundResource(R.drawable.bg_recommendation_irrigate)
                binding.txtRecommendationBadge.text = "IRRIGATE"
                binding.txtRecommendationBadge.background = ContextCompat.getDrawable(this, R.drawable.bg_badge_irrigate)
                binding.txtRecommendationBadge.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }
            RecommendationStatus.NOT_NEEDED -> {
                binding.cardRecommendation.setBackgroundResource(R.drawable.bg_recommendation_not_needed)
                binding.txtRecommendationBadge.text = "NOT NEEDED"
                binding.txtRecommendationBadge.background = ContextCompat.getDrawable(this, R.drawable.bg_badge_not_needed)
                binding.txtRecommendationBadge.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }
            RecommendationStatus.INSUFFICIENT_DATA -> {
                binding.cardRecommendation.setBackgroundResource(R.drawable.bg_recommendation_unavailable)
                binding.txtRecommendationBadge.text = "INSUFFICIENT WEATHER DATA"
                binding.txtRecommendationBadge.background = ContextCompat.getDrawable(this, R.drawable.bg_badge_unavailable)
                binding.txtRecommendationBadge.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }
        }
    }

    private fun showDefaultEmptyWeatherState() {
        binding.txtMetricTemp.text = "-- °C"
        binding.txtMetricRainfall.text = "-- mm"
        binding.txtMetricET0.text = "-- mm"
        binding.txtMetricSoilMoisture.text = "-- m³/m³"
        updateRecommendationCard(
            RecommendationStatus.INSUFFICIENT_DATA,
            "No farm selected. Please register a farm to view live weather and irrigation recommendations."
        )
        forecastAdapter.submitList(emptyList())
    }
}
