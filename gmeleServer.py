import socket
import threading
import time
import platform
import os # <-- Προσθήκη της βιβλιοθήκης os

# --- Ρυθμίσεις του Server ---
HOST = "0.0.0.0"  # Ο server "ακούει" σε όλες τις διαθέσιμες διεπαφές δικτύου
PORT = 41007      # Η πόρτα επικοινωνίας

# Dictionary αντιστοίχισης IP σε Hostname
"""
IP_TO_HOSTNAME_MAP = {
    "192.168.88.2": "PRPC01",
    "192.168.88.3": "PRPC02",
    "192.168.88.4": "PRPC03",
    "192.168.88.5": "PRPC04",
    "192.168.88.6": "PRPC05",
    "192.168.88.7": "PRPC06",
    "192.168.88.8": "PRPC07",
    "192.168.88.9": "PRPC08",
    "192.168.88.10": "PRPC09",
    "192.168.88.11": "PRPC10",
    "192.168.88.12": "PRPC11",
    "192.168.88.13": "PRPC12",
    "192.168.88.14": "PRPC13",
    "192.168.88.15": "PRPC14",
    "192.168.88.16": "PRPC15",
    "192.168.88.17": "PRPC16",
    "192.168.88.18": "PRPC17",
    "192.168.88.19": "PRPC18",
    "192.168.88.20": "PRPC19",
    "192.168.88.21": "PRPC20",
    "192.168.88.22": "PRPC21",
    "192.168.88.23": "PRPC22",
    "192.168.88.24": "PRPC23",
    "192.168.88.25": "PRPC24",
    "192.168.88.26": "PRPC25",
    "192.168.88.27": "PRPC26",
    "192.168.88.28": "PRPC27"
}
"""
IP_TO_HOSTNAME_MAP = {
    "192.168.1.63": "PRPC01"
}

def handle_client(conn, addr):
    """
    Διαχειρίζεται τη σύνδεση ενός μεμονωμένου client.
    Κάθε client που συνδέεται, εκτελεί αυτή τη συνάρτηση σε ένα δικό του, ξεχωριστό thread.
    conn: Το αντικείμενο socket της σύνδεσης με τον client.
    addr: Ένα tuple που περιέχει την IP διεύθυνση και την πόρτα του client.
    """
    server_ip = conn.getsockname()[0]
    hostname_pc = IP_TO_HOSTNAME_MAP.get(server_ip, "UNKNOWN_HOST")

    print(f"[NEW CONNECTION] {addr} connected to {server_ip} (impersonating {hostname_pc}).")

    try:
        with conn:
            data = conn.recv(1024)
            if not data:
                return

            command = data.decode('utf-8').strip()
            print(f"[{addr}] Received command for {hostname_pc}: '{command}'")

            response = ""
            if command == "Echo":
                os_name = "Windows10" if platform.system() == "Windows" else platform.system()
                response = f"{hostname_pc} - {os_name}"
                conn.sendall(response.encode('utf-8'))

            elif command == "Restart":
                response = f"{hostname_pc} - Rebooting..."
                conn.sendall(response.encode('utf-8'))
                conn.close() # Κλείνουμε τη σύνδεση πριν την εκτέλεση της εντολής
                print(f"Executing RESTART on {hostname_pc}")
                if platform.system() == "Windows":
                    os.system("shutdown /r /t 1") # Εντολή για επανεκκίνηση στα Windows
                else:
                    os.system("sudo reboot") # Εντολή για επανεκκίνηση σε Linux/macOS

            elif command == "Shutdown":
                response = f"{hostname_pc} - Shutting down..."
                conn.sendall(response.encode('utf-8'))
                conn.close() # Κλείνουμε τη σύνδεση πριν την εκτέλεση της εντολής
                print(f"Executing SHUTDOWN on {hostname_pc}")
                if platform.system() == "Windows":
                    os.system("shutdown /s /t 1") # Εντολή για τερματισμό στα Windows
                else:
                    os.system("sudo shutdown now") # Εντολή για τερματισμό σε Linux/macOS

            elif command == "Restore":
                initial_response = f"{hostname_pc} - Restoring..."
                conn.sendall(initial_response.encode('utf-8'))
                time.sleep(3)
                final_response = f"{hostname_pc} - Restored"
                conn.sendall(final_response.encode('utf-8'))
                response = f"{initial_response} -> {final_response}"

            else:
                response = f"{hostname_pc} - Unknown command: {command}"
                conn.sendall(response.encode('utf-8'))

            print(f"[{addr}] Sent response for {hostname_pc}: '{response}'")

    except Exception as e:
        print(f"[ERROR] An error occurred with {addr}: {e}")
    finally:
        print(f"[CONNECTION ENDED] {addr} disconnected from {hostname_pc}.")


def start_server():
    """
    Αρχικοποιεί και ξεκινά τον TCP server.
    """
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    try:
        server_socket.bind((HOST, PORT))
        server_socket.listen()
        print(f"[*] Server listening on {HOST}:{PORT}...")
        print(f"[*] Impersonating hosts: {list(IP_TO_HOSTNAME_MAP.values())}")

        while True:
            client_conn, client_addr = server_socket.accept()
            thread = threading.Thread(target=handle_client, args=(client_conn, client_addr))
            thread.start()
            print(f"[ACTIVE CONNECTIONS] {threading.active_count() - 1}")

    except OSError as e:
        print(f"[SERVER FAILED] Could not bind to port {PORT}. Error: {e}")
    except Exception as e:
        print(f"[ERROR] An unexpected server error occurred: {e}")
    finally:
        server_socket.close()
        print("[SERVER SHUTDOWN] Server socket closed.")


if __name__ == "__main__":
    start_server()