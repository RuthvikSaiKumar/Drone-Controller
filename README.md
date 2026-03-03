# Drone-Controller
Android app that turns a smartphone into a dual-joystick controller and sends inputs over UDP to a Python backend. The backend emulates a virtual Xbox gamepad on the laptop, allowing simulators to recognize the phone as a standard controller. Built to solve hardware controller shortages during drone training sessions.

---
## Controller App Layout

<img width="1600" height="720" alt="image" src="https://github.com/user-attachments/assets/58510a37-df19-41c1-a058-fb8fc411e1d2" />

The application interface is designed to closely replicate a standard dual-stick drone transmitter.

The layout features two large on-screen joysticks:

* **Left Joystick**

  * Vertical axis controls **throttle**
  * Throttle does **not self-center**, mimicking real drone controllers
  * Horizontal axis controls **yaw** and is self-centering

* **Right Joystick**

  * Controls **pitch** (vertical axis)
  * Controls **roll** (horizontal axis)
  * Both axes are self-centering

At the top center of the interface, there is an **IP address input field** used to enter the laptop’s IP address for establishing the UDP connection.

Two **toggle buttons** are placed at the top left and top right of the screen. These are currently placeholders and do not have any implemented functionality.

---

## Requirements & Setup

### 1. Android Application

1. Navigate to the **Releases** section of this repository.
2. Download the latest `.apk` file.
3. Transfer and install the APK on your Android device.

   * Enable installation from unknown sources if prompted.

---

### 2. Windows Host Setup

The laptop must emulate a virtual Xbox controller. This requires the ViGEmBus driver.

1. Download and install **ViGEmBus Driver** from:
   [https://vigembus.com/](https://vigembus.com/)
2. Restart the system after installation if prompted.

---

### 3. Running the Emulation Script (Windows)

There are two ways to run the controller emulation backend:

#### Option A: Using Prebuilt Executable (Recommended for Quick Setup)

1. Download the `.exe` file from the **Releases** section.
2. Open Command Prompt in the folder containing the executable.
3. Run:

```
controller_emulator.exe
```

---

#### Option B: Running from Source

1. Download the Python source files along with `requirements.txt`.
2. Ensure Python 3.x is installed on your system.
3. Open Command Prompt in the project directory.
4. Install required dependencies:

```
pip install -r requirements.txt
```

5. Run the script:

```
python drone_controller.py
```

---

### 4. Network Configuration

* Ensure the Android device and laptop are connected to the same network.
* When the backend script starts, it **prints** the IP address that should be entered into the Android app to publish UDP packets. Use the displayed IP in the app’s IP field.

#### For Best Latency

For improved responsiveness, connect the laptop directly to the mobile hotspot created by the Android device. This reduces routing delay and improves stability.

#### USB Tethering (Wired Connection)

If using USB tethering between the phone and laptop, **do not** copy the IP address printed by the script, instead:

1. Enable USB tethering on the Android device.
2. Open Command Prompt on Windows.
3. Run:

```
ipconfig
```

4. Locate the IPv4 address listed under the **Ethernet adapter** section.
5. Enter that IPv4 address into the Android app.

---

Once connected successfully, the system will emulate a virtual Xbox controller recognized by compatible simulator software.
The script will be printing continuously the number of packets its receiving per second. This is useful when its disconnected since there would be no live output.

---

### Warning & Limitations

This software is intended for academic use only. It is a basic hobby project shared in the working state in which it was originally developed. It has not been extensively tested across different systems, and bugs or unexpected behavior may occur.

#### Known Issue

If the phone remains locked for an extended time, the app may internally reset. The IP field might revert to its placeholder value even though the previous connection is still active in the background.

Do not re-enter the IP immediately, as this can result in duplicate UDP signals being sent.

Instead, fully close the app and reopen it before reconnecting.
