#!/usr/bin/env python3
"""Install a theme JSON through ADB without modifying firmware or SD contents."""
import argparse
import base64
import json
import subprocess
from pathlib import Path

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("theme", type=Path)
    parser.add_argument("--serial", required=True, help="ADB serial of the intended Y1")
    args = parser.parse_args()
    data = args.theme.read_bytes()
    if len(data) > 65536:
        parser.error("theme.json exceeds 64 KiB")
    obj = json.loads(data)
    if obj.get("schema") != 1 or not obj.get("id"):
        parser.error("theme schema or id is invalid")
    command = ["adb"]
    if args.serial:
        command += ["-s", args.serial]
    model = subprocess.check_output(command + ["shell", "getprop", "ro.product.model"], text=True).strip()
    if model != "Y1":
        parser.error(f"connected device is {model!r}, not Y1")
    encoded = base64.b64encode(data).decode("ascii")
    subprocess.run(command + ["shell", "am", "broadcast", "-n",
        "com.kroot.y1themes/.ThemeInstallReceiver", "-a",
        "com.kroot.y1themes.INSTALL_THEME", "--es", "data", encoded], check=True)
    print("Open Y1 Theme Player > テーマ and select", obj["name"])

if __name__ == "__main__":
    main()
