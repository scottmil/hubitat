# hubitat
Repository for Hubitat home automation code
Includes:
1. Aeotec Siren 6 driver migrated from SmartThings to Hubitat and originally developed by krlaframboise.
2. Eufy HomeBase and Eufy HomeBase Camera drivers to work in conjunction with ioBroker and Eufy ioBroker.eufy-client 
adapter created by @bropat and available here:  https://github.com/bropat/ioBroker.eufy-security.
ioBroker can be installed on a Raspberry Pi, and instructions are available here: https://www.iobroker.net/#en/intro  You can use ioBroker Blockly/Javascript scripts to detect changes in Eufy ioBroker objects and send REST GET URLs to control Hubitat using the Hubitat Maker API:
 <img width="1542" alt="ioBroker Eufy scripts" src="https://github.com/user-attachments/assets/ee5ba664-44cf-4609-b0eb-65f9b08408af">
To use REST set/get with Hubitat, install the ioBroker simple-api adapter.  To create Blockly/Javascript scripts, install the ioBroker javascript adapter. 
3. Additional drivers for HubDuino, a driver framework for Hubitat created by Daniel Ogorchock.  The Hubduino Tile Presence device utilizes two Adafruit Huzzah32 ESP32 boards; one board senses two Tiles using Bluetooth and their MAC addresses using the Arduino code TilePresence_ESP32BLE.ino. The outputs of this board are inputs to a second board that runs the Hubduino code ST_Anything_TilePresence_ESP32WiFi.ino.  I constructed two Tile Presence devices and installed them in different locations in my home to ensure they can sense my Tile devices.
