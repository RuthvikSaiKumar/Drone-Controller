import socket
import time
import vgamepad as vg
import threading
import psutil


# print system ip address
# hostname = socket.gethostname()
# ip_address = socket.gethostbyname(hostname)
# print(f"IP Address: {ip_address}")


def get_wlan_ip():
    interfaces = psutil.net_if_addrs()
    # Look for common Wi-Fi interface names
    for interface_name, snics in interfaces.items():
        if "wi-fi" in interface_name.lower() or "wlan" in interface_name.lower():
            for snic in snics:
                if snic.family == socket.AF_INET:
                    return snic.address
    return "WLAN IP not found"


print(f"WLAN IP: {get_wlan_ip()}")

UDP_IP = "0.0.0.0"  # Listen on all network interfaces
UDP_PORT = 5005

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 72)
sock.bind((UDP_IP, UDP_PORT))

print(f"Listening on {UDP_IP}:{UDP_PORT}")

# Initialize the virtual gamepad
gamepad = vg.VX360Gamepad()

A = vg.XUSB_BUTTON.XUSB_GAMEPAD_A
B = vg.XUSB_BUTTON.XUSB_GAMEPAD_B
X = vg.XUSB_BUTTON.XUSB_GAMEPAD_X
Y = vg.XUSB_BUTTON.XUSB_GAMEPAD_Y

# D-pad
L = vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT
R = vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT
U = vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP
D = vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN

# joysticks
left_joystick = gamepad.left_joystick_float
right_joystick = gamepad.right_joystick_float

# l1 and r1
L1 = vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER
R1 = vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER

# l2 and r2
L2 = gamepad.left_trigger_float
R2 = gamepad.right_trigger_float

# Shared data between threads
data_shared = {
    'ch1': 0.0,
    'ch2': 0.0,
    'ch3': 0.0,
    'ch4': 0.0,
    'ch5': False,
    'ch6': False
}


def listen_to_port():
    packet_count = 0
    start_time = time.time()
    while True:
        # todo: when modifying to transfer more data, make sure to change the buffer size
        data, addr = sock.recvfrom(128)

        # print(data.decode('utf-8').strip().split(','))
        ch1, ch2, ch3, ch4, ch5, ch6 = map(int, data.decode('utf-8').strip().split(','))
        # convert 0 to 1000 to -1.0 to 1.0, use a mapping function
        data_shared['ch1'] = (ch1 - 500.0) / 500.0
        data_shared['ch2'] = (ch2 - 500.0) / 500.0
        data_shared['ch3'] = (ch3 - 500.0) / 500.0
        data_shared['ch4'] = (ch4 - 500.0) / 500.0
        data_shared['ch5'] = ch5 == 2000
        data_shared['ch6'] = ch6 == 2000
        # Track packet count and calculate packets per second
        packet_count += 1
        elapsed_time = time.time() - start_time
        if elapsed_time >= 1.0:
            print(f"Packets received per second: {packet_count}")
            packet_count = 0
            start_time = time.time()


def execute_instructions():
    while True:
        ch1 = data_shared['ch1']
        ch2 = data_shared['ch2']
        ch3 = data_shared['ch3']
        ch4 = data_shared['ch4']

        right_joystick(ch1, ch2)
        left_joystick(ch4, ch3)

        gamepad.update()


# Create and start the threads
thread1 = threading.Thread(target=listen_to_port)
thread2 = threading.Thread(target=execute_instructions)

thread1.start()
thread2.start()

# Keep the main thread alive
# try:
#     while True:
#         time.sleep(1)
# except KeyboardInterrupt:
#     print("Exiting...")
