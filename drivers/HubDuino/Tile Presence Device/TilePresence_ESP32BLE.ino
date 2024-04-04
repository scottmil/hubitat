//******************************************************************************************
//  File: TilePresence_ESP32BLE.ino
//  Author: Scott Miller 
//
//  Summary:  TilePresence_ESP32WiFi implements - 2 x Presence device (reading digital input set by BLE scans)
//
//            The Arduino example app BLEScan can be used to discover the MAC address of your Tile, which has a serviceUID "feed". 
//
//            Compilation Notes:  1) Requires Espressif esp32 library version 2.0.14 or below.  Will not compile under
//                                version 3.*.  
//                                2) Set Arduino IDE Tools -> Partition Scheme -> Huge APP (3MB No OTA/1MB SPIFFS)
//                                3) Uses Adafruit Huzzah32 ESP32 Feather or equivalent
//
//  Change History:
//
//    Date        Who            What
//    ----        ---            ----
//    2021-11-19  Scott Miller   Original Creation
//    2022-01-19  Scott Miller   Reduced loop delay to 1 second

//******************************************************************************************
// ESP32_BLE Library 
//****************** ***********************************************************************
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>
#include <WiFi.h>
                                        
int scanTime = 5; //In seconds
BLEScan* pBLEScan;  
       
/* Include MAC_ADDRESS of the 2 Tiles to search for */
//std::string TILE_MAC_ADDRESS_1 = "c1:62:xx:xx:xx:xx";  //  Scott's billfold Tile Mac address
std::string TILE_MAC_ADDRESS_1 = "f7:1f:xx:xx:xx:xx";  //  Scott's single house key Tile MAC address
std::string TILE_MAC_ADDRESS_2 = "c3:4f:xx:xx:xx:xx";  //  Barb's house keys Tile MAC address


//*****************************************************************************************
//Huzzah ESP32 Pin Definitions 
//*****************************************************************************************

//Digital Pins
#define PIN_PRESENCE_OUT1         14  //Jumper output to ST_Anything_TilePresence_ESP32WiFi.ino PIN_PRESENCE_1 input on second ESP32
#define PIN_PRESENCE_OUT2         21  //Jumper output to ST_Anything_TilePresence_ESP32WiFi.ino PIN_PRESENCE_2 input on second ESP32

//*****************************************************************************************

bool isPresent1 = false;
bool isPresent2 = false;
int ble1NotFoundCount = 0;
int ble2NotFoundCount = 0;
int numLoops = 0;

//******************************************************************************************
//Arduino Setup() routine
//******************************************************************************************
void setup()
{   
  //Set and initialize pins to read result of BLE scan
  pinMode(PIN_PRESENCE_OUT1,OUTPUT);
  pinMode(PIN_PRESENCE_OUT2,OUTPUT);
  digitalWrite(PIN_PRESENCE_OUT1,HIGH); //Initialize to notpresent
  digitalWrite(PIN_PRESENCE_OUT2,HIGH); //Initialize to notpresent

  //Turn off unused Wifi
   WiFi.mode(WIFI_OFF);
  
  //******************************************************************************************
  //Setup Bluetooth Low Energy scan
  //Begin scanning for BLE devices
  Serial.begin(115200);
  BLEDevice::init("BLEScanner");
  pBLEScan = BLEDevice::getScan(); //create new scan
  pBLEScan->setActiveScan(true); //active scan uses more power, but get results faster
  //pBLEScan->setInterval(100);  
  //pBLEScan->setWindow(99);  // less or equal setInterval value
  pBLEScan->setInterval(40); //Apple 
  pBLEScan->setWindow(30);  // Apple
  //pBLEScan->setInterval(10000);  //last used
  //pBLEScan->setWindow(9999);  // less or equal setInterval value  last used
}
  
//******************************************************************************************
//Arduino Loop() routine
//******************************************************************************************
void loop() { 
   
    BLEScanResults foundDevices = pBLEScan->start(scanTime, false);
    Serial.print("Devices found: ");
    Serial.println(foundDevices.getCount());
    int i;
    bool ble1Found = false;
    bool ble2Found = false;
    for (i=0; i < foundDevices.getCount(); i++) {
      BLEAdvertisedDevice advertisedDevice = foundDevices.getDevice(i);
      //Serial.printf("Found Device: %s \n", advertisedDevice.toString().c_str());
      if (advertisedDevice.getAddress().toString() == TILE_MAC_ADDRESS_1)  {
        ble1Found= true;
        Serial.printf("BLE1 Advertised Device: %s \n", advertisedDevice.toString().c_str());
        ble1NotFoundCount = 0;
        if(!isPresent1) {
          isPresent1 = true;
          Serial.printf("BLE1 found; isPresent1 is true.\n");
          digitalWrite(PIN_PRESENCE_OUT1, LOW);
        } 
      }
      if (advertisedDevice.getAddress().toString() == TILE_MAC_ADDRESS_2)  {
        ble2Found= true;
        Serial.printf("BLE2 Advertised Device: %s \n", advertisedDevice.toString().c_str());
        ble2NotFoundCount = 0;
        if(!isPresent2) {
          isPresent2 = true;
          Serial.printf("BLE2 found; isPresent2 is true.\n");
          digitalWrite(PIN_PRESENCE_OUT2, LOW);
        } 
      }
    }

    /* Tile is considered not found after 10 consecutive "not found" scans */
    if(!ble1Found) {
      ble1NotFoundCount++;
      if(ble1NotFoundCount >= 10 ) {
        isPresent1 = false;
        Serial.printf("BLE1 not found; isPresent1 is false.\n");
        digitalWrite(PIN_PRESENCE_OUT1, HIGH);
        ble1NotFoundCount = 0;
       }
     } 
     if(!ble2Found) {
      ble2NotFoundCount++;
      if(ble2NotFoundCount >= 10 ) {
        isPresent2 = false;
        Serial.printf("BLE2 not found; isPresent2 is false.\n");
        digitalWrite(PIN_PRESENCE_OUT2, HIGH);
        ble2NotFoundCount = 0;
       }
     }      
  
   pBLEScan->clearResults();   // delete results fromBLEScan buffer to release memory
   
   delay(1000);
}
