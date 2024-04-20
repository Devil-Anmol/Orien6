package com.example.orien6

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.layout.FlowColumnScopeInstance.align
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
//import androidx.navigation.NavController
//import androidx.navigation.NavHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
//import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null
    private lateinit var database: SensorDatabase
    private lateinit var sensorDataDao: SensorDataDao
    private var isStoringData = false
    private var isDrawingGraphs = false
    private var sensorDataList = emptyList<SensorData>()

    private var orientationX by mutableStateOf(0f)
    private var orientationY by mutableStateOf(0f)
    private var orientationZ by mutableStateOf(0f)

    private var rotationMatrix = FloatArray(9)
    private var orientationAngles = FloatArray(3)
    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        database = SensorDatabase.getDatabase(this)
        sensorDataDao = database.sensorDataDao()

        setContent {
            AppContent()
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometerSensor?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
        magnetometerSensor?.also { magnetometer ->
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }

        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // Convert radians to degrees
        val x = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val y = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        val z = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        orientationX = x;
        orientationY = y;
        orientationZ = z;

        // Insert data to the database if storing is enabled
        if (isStoringData) {
            val timestamp = System.currentTimeMillis()
            insertSensorData(SensorData(timestamp, x, y, z))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Ignoring for this example
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                // Handle unreliable accuracy
            }
        }
    }

    private fun insertSensorData(sensorData: SensorData) {
        CoroutineScope(Dispatchers.IO).launch {
            sensorDataDao.insertSensorData(sensorData)
        }
    }

    private fun loadSensorData() {
        CoroutineScope(Dispatchers.IO).launch {
            sensorDataList = sensorDataDao.getAllSensorData()
            isDrawingGraphs = true
        }
    }

    @Composable
    fun AppContent() {
        val context = LocalContext.current

        val navController = rememberNavController()

        NavHost(navController, startDestination = "home") {
            composable("home") {
                HomeScreen(navController)
            }
            composable("graph") {
                GraphScreen(navController)
            }
        }
    }

    @Composable
    fun HomeScreen(navController: NavController){
        val context = LocalContext.current
        MaterialTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column (
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ){

                    Row(
                        modifier = Modifier
                            .aspectRatio(0.4f)
                            .padding(16.dp),
//                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        BarColumn("X", orientationX, Color.Cyan)
                        BarColumn("Y", orientationY, Color.Green)
                        BarColumn("Z", orientationZ, Color.Red)
                    }
                    Column(
                        modifier = Modifier
                            .aspectRatio(0.4f)
                            .padding(top = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { isStoringData = true },
                            enabled = !isStoringData
                        ) {
                            Text("Start Storing Data")
                        }

                        Button(
                            onClick = { isStoringData = false },
                            enabled = isStoringData
                        ) {
                            Text("Stop Storing Data")
                        }

                        Button(
                            onClick = { clearDatabase() }
                        ) {
                            Text("Clear Database")
                        }

                        Button(
                            onClick = { downloadDataAsTextFile(context) }
                        ) {
                            Text("Download Data as .txt")
                        }

                        Button(
                            onClick = { navController.navigate("graph") }
                        ) {
                            Text("View Graphs")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BarColumn(label: String, value: Float, X: Color) {
        var t = "%.5f".format(value).toDouble();
        Column(
            modifier = Modifier.width(100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label)
            Text(text = "$t")
            VerticalBar(value = value,X)
        }
    }

    @Composable
    fun VerticalBar(value: Float, X: Color) {
        val maxValue = 20f // Max value for the bar
        val adjustedValue = min(max(value, -maxValue), maxValue) // Clamp the value between -20 and 20
        val percentage = (adjustedValue / maxValue) * 100 // Convert to percentage

        val color = calculateBarColor(value,X)

        Box(
            modifier = Modifier
                .size(20.dp, 150.dp)
                .background(color = Color.Transparent, shape = RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
                val barHeight = size.height * (percentage / 100)
                rotate(180f) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            })
        }
    }

    private fun calculateBarColor(value: Float, X : Color): Color {
        val maxValue = 20f
        val adjustedValue = min(max(value, -maxValue), maxValue) // Clamp the value between -20 and 20
        val percentage = (adjustedValue / maxValue) * 100 // Convert to percentage

        return when {
            percentage >= 0 -> X
            percentage < 0 -> X
            else -> Color.Transparent
        }
    }

    private fun clearDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            sensorDataDao.deleteAllSensorData()
        }
    }

    private fun getList(context: Context) : List<SensorData> {
        CoroutineScope(Dispatchers.IO).launch {
            val sensorDataList = sensorDataDao.getAllSensorData()
        }
        return sensorDataList;
    }

    private fun downloadDataAsTextFile(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val sensorDataList = sensorDataDao.getAllSensorData()
            val textContent = StringBuilder()
            for (sensorData in sensorDataList) {
                textContent.append("${sensorData.timestamp},${sensorData.x},${sensorData.y},${sensorData.z}\n")
            }
            val fileName = "sensor_data.txt"
            val externalDir = context.getExternalFilesDir(null)
            Log.d("hii","$externalDir")
            val file = File(externalDir, fileName)
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(textContent.toString().toByteArray())
            fileOutputStream.close()
        }
    }

    @Composable
    fun GraphScreen(navController: NavController) {
        var sensorDataList by remember { mutableStateOf(emptyList<SensorData>()) }
        isStoringData = false

        LaunchedEffect(key1 = true) {
            // Fetch data from the database
            sensorDataList = sensorDataDao.getAllSensorData()
        }

        OrientationGraph(sensorDataList)
    }
}

@Composable
fun OrientationGraph(sensorDataList: List<SensorData>) {
//    val orientationHistory by viewModel.orientationHistory.collectAsState()
    val data = sensorDataList
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Orientation History", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Graph for X-axis
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize(),
                onDraw = {
                    drawLineGraph(data, Color.Red, this) { it.x}
                }
            )
            Text(text = "X-Axis",
                modifier = Modifier
                    .align(Alignment.TopCenter))

        }

        Spacer(modifier = Modifier.height(25.dp))

        // Graph for Y-axis
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize(),
                onDraw = {
                    drawLineGraph(data, Color.Green, this) { it.y }
                }
            )
            Text(text = "Y-Axis",
                modifier = Modifier
                    .align(Alignment.TopCenter))
        }

        Spacer(modifier = Modifier.height(25.dp))

        // Graph for Z-axis
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize(),
                onDraw = {
                    drawLineGraph(data, Color.Blue, this) { it.z}
                }
            )
            Text(
                text = "Z-Axis",
                modifier = Modifier
                    .align(Alignment.TopCenter)
            )

        }
    }
}

private fun drawLineGraph(
    data: List<SensorData>,
    color: Color,
    scope: DrawScope,
    getValue: (SensorData) -> Float
) {
    scope.apply {
        val maxX = data.size.toFloat()
        val maxY = 390.0f
        // Draw background grid
        val stepX = size.width / maxX
        val stepY = size.height / 10
        drawGrid(stepX, stepY, color = Color.LightGray)

        val path = Path()

        data.forEachIndexed { index, point ->
            val x = index * stepX
            val y = size.height / 2 - getValue(point) * (size.height / maxY) // Adjust y-coordinate

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw axis lines
        drawLine(start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), color = Color.Black, strokeWidth = 2f) // Draw x-axis at center

        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))
    }
}

private fun DrawScope.drawGrid(stepX: Float, stepY: Float, color: Color) {
    val maxX = size.width
    val maxY = size.height
    val startY = size.height / 2 // Adjust y-coordinate for the center axis

    repeat((maxY / stepY).toInt()) { i ->
        drawLine(
            color = color,
            start = Offset(0f, startY - (i - 5) * stepY), // Adjust y-coordinate
            end = Offset(maxX, startY - (i - 5) * stepY), // Adjust y-coordinate
            strokeWidth = 0.5f
        )
    }

    repeat((maxX / stepX).toInt()) { i ->
        drawLine(
            color = color,
            start = Offset(i * stepX, 0f),
            end = Offset(i * stepX, maxY),
            strokeWidth = 0.5f
        )
    }
}
