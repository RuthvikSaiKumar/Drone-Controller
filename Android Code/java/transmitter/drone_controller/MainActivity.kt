package transmitter.drone_controller

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import transmitter.drone_controller.ui.theme.Drone_ControllerTheme
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.roundToInt
import kotlin.reflect.KFunction1

class MainActivity : ComponentActivity() {
    private val udpServerIp = "192.168.0.0"
    private val udpServerPort = 5005
    private lateinit var socket: DatagramSocket
    private lateinit var serverAddress: InetAddress

    private var ch1: Int = 0 // Roll
    private var ch2: Int = 0 // Pitch
    private var ch3: Int = 0 // Throttle
    private var ch4: Int = 0 // Yaw
    private var ch5: Int = 0 // Switch 1
    private var ch6: Int = 0 // Switch 2

    private var udpJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            Drone_ControllerTheme {
                ControllerScreen(
                    setChannelValues = ::setChannelValues,
                    reconnect = ::reconnectUDPServer
                )
            }
        }

        try {
            socket = DatagramSocket()
            serverAddress = InetAddress.getByName(udpServerIp)
            sendDataToUDPServer()
        } catch (e: Exception) {
            Log.e("UDP", "Error initializing UDP socket", e)
        }
    }

    private fun sendDataToUDPServer() {
        udpJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (true) {
                    val message = "$ch1,$ch2,$ch3,$ch4,$ch5,$ch6"
                    val data = message.toByteArray()
                    val packet = DatagramPacket(data, data.size, serverAddress, udpServerPort)
                    socket.send(packet)
                    kotlinx.coroutines.delay(20)
                }
            } catch (e: Exception) {
                Log.e("UDP", "Error sending UDP packet", e)
            }
        }
    }

    private fun setChannelValues(
        ch1: Int,
        ch2: Int,
        ch3: Int,
        ch4: Int,
        ch5: Int,
        ch6: Int
    ) {
        this.ch1 = ch1
        this.ch2 = ch2
        this.ch3 = ch3
        this.ch4 = ch4
        this.ch5 = ch5
        this.ch6 = ch6
    }

    private fun reconnectUDPServer(newIpAddress: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                udpJob?.cancelAndJoin()

                if (!socket.isClosed) {
                    socket.close()
                }

                socket = DatagramSocket()
                serverAddress = InetAddress.getByName(newIpAddress)

                sendDataToUDPServer()
            } catch (e: Exception) {
                Log.e("UDP", "Error reconnecting to UDP server", e)
            }
        }
    }
}

@Composable
fun ControllerScreen(
    setChannelValues: (Int, Int, Int, Int, Int, Int) -> Unit,
    reconnect: KFunction1<String, Unit>
) {
    var roll by remember { mutableFloatStateOf(0.5f) }
    var pitch by remember { mutableFloatStateOf(0.5f) }
    var throttle by remember { mutableFloatStateOf(0f) }
    var yaw by remember { mutableFloatStateOf(0.5f) }

    var toggle1 by remember { mutableStateOf(false) }
    var toggle2 by remember { mutableStateOf(false) }

    var ipAddress by remember { mutableStateOf("192.168.0.0") }

    LaunchedEffect(roll, pitch, throttle, yaw, toggle1, toggle2) {
        // Map float values (0 to 1) to integer range (1000 to 2000) for PWM signals
        val ch1 = (roll * 1000).roundToInt() // Roll (1000-2000)
        val ch2 = (pitch * 1000).roundToInt() // Pitch (1000-2000)
        val ch3 = (throttle * 1000).roundToInt() // Throttle (1000-2000)
        val ch4 = (yaw * 1000).roundToInt() // Yaw (1000-2000)
        val ch5 = if (toggle1) 2000 else 1000 // Switch 1
        val ch6 = if (toggle2) 2000 else 1000 // Switch 2

        Log.d(
            "ControllerScreen",
            "ch1: $ch1, ch2: $ch2, ch3: $ch3, ch4: $ch4, ch5: $ch5, ch6: $ch6"
        )

        setChannelValues(ch1, ch2, ch3, ch4, ch5, ch6)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Switch(
                checked = toggle1,
                onCheckedChange = { toggle1 = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.Green,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray
                ),
            )
            OutlinedTextField(
                value = ipAddress,
                onValueChange = {
                    ipAddress = it
                    reconnect(ipAddress)
                },
                label = { Text("IP Address", color = Color.White) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                ),
                modifier = Modifier
                    .width(200.dp)
            )
            Switch(
                checked = toggle2,
                onCheckedChange = { toggle2 = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.Green,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray
                ),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Joystick (Throttle & Yaw)
            Joystick(
                onPositionChanged = { x, y ->
                    throttle = 1f - y
                    yaw = x
                },
                isNonCenteringYAxis = true // The Y-axis (throttle) does not self-center
            )

// Right Joystick (Pitch & Roll)
            Joystick(
                onPositionChanged = { x, y ->
                    roll = x
                    pitch = 1f - y
                },
                isNonCenteringYAxis = false // The Y-axis (pitch) self-centers
            )
        }
    }
}

@SuppressLint("ReturnFromAwaitPointerEventScope")
@Composable
fun Joystick(
    onPositionChanged: (Float, Float) -> Unit,
    // This is now `isNonCenteringYAxis`, to make it more explicit
    isNonCenteringYAxis: Boolean = false
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var thumbPosition by remember { mutableStateOf(Offset(0f, 0f)) }

    val thumbRadius = with(LocalDensity.current) { 40.dp.toPx() }
    val baseRadius = with(LocalDensity.current) { 105.dp.toPx() }

    Box(
        modifier = Modifier
            .size(300.dp)
            .onSizeChanged { size = it }
            .clip(CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .pointerInput(isNonCenteringYAxis) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pos = event.changes.firstOrNull()?.position
                        val isPressed = event.changes.first().pressed

                        if (pos != null && isPressed) {
                            val x = pos.x - size.width / 2
                            val y = pos.y - size.height / 2
                            val distance = Offset(x, y).getDistance()

                            // Clamp the thumb position to the base circle
                            val newX = if (distance > baseRadius) {
                                x / distance * baseRadius
                            } else {
                                x
                            }
                            val newY = if (distance > baseRadius) {
                                y / distance * baseRadius
                            } else {
                                y
                            }

                            // The Y axis only remains non-centering if `isNonCenteringYAxis` is true
                            val finalY = if (isNonCenteringYAxis) y.coerceIn(
                                -baseRadius,
                                baseRadius
                            ) else newY

                            thumbPosition = Offset(newX, finalY)

                            val normalizedX = (newX / baseRadius).coerceIn(-1f, 1f)
                            val normalizedY = (finalY / baseRadius).coerceIn(-1f, 1f)

                            val mappedX = (normalizedX + 1) / 2
                            val mappedY = (normalizedY + 1) / 2

                            onPositionChanged(mappedX, mappedY)
                        } else if (!isPressed) {
                            // Reset X axis to center for both sticks when finger is lifted
                            thumbPosition = if (isNonCenteringYAxis) {
                                Offset(0f, thumbPosition.y)
                            } else {
                                Offset(0f, 0f)
                            }
                            // Call onPositionChanged with centered values
                            val newX = 0.5f
                            val newY =
                                if (isNonCenteringYAxis) (thumbPosition.y / baseRadius + 1) / 2 else 0.5f
                            onPositionChanged(newX, newY)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(
                    thumbPosition.x + size.width / 2,
                    thumbPosition.y + size.height / 2
                )
            )
        }
    }
}