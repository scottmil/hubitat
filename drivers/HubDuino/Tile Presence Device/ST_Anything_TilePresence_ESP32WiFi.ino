//******************************************************************************************
//  File: ST_Anything_TilePresence_ESP32WiFi.ino
//  Authors: Scott Miller adapted from code by Dan G Ogorchock & Daniel J Ogorchock (Father and Son)
//
//  Summary:  This Arduino Sketch, along with the ST_Anything library and the revised SmartThings 
//            library, demonstrates the ability of an Adafruit Huzzah32 to 
//            implement a Tile presence sensor for integration into SmartThings/Hubitat.
//            The ST_Anything library takes care of all of the work to schedule device updates
//            as well as all communications with the Adafruint Huzzah32's WiFi.
//
//            ST_Anything_TilePresence_ESP32WiFi implements the following ST Capability
//              - 2 x Presence device (reading digital input set by BLE scans)
//
//            The Arduino example app BLEScan can be used to discover the MAC address of your Tile, which has a serviceUID "feed". 
//            ***Compiled sketch for ESP32 Dev Module with No OTA partition to increase memory size.
//       
//            Compilation Notes:  1) Requires Espressif esp32 library version 2.0.14 or below.  Will not compile under
//                                version 3.*.  
//                                2) Set Arduino IDE Tools -> Partition Scheme -> Huge APP (3MB No OTA/1MB SPIFFS)
//                                3) Uses Adafruit Huzzah32 ESP32 Feather or equivalent
//                                   
//
//  Change History:
//
//    Date        Who            What
//    ----        ---            ----
//    2019-10-12  Dan Ogorchock  Original Creation
//    2020-02-10  Scott Miller   Modified for sensing Tiles using Bluetooth Low Energy
//    2020-03-15  Scott Miller   Added second presence sensor
//    2020-07-02  Scott Miller   Increased Bluetooth power
//    2020-09-23  Scott Miller   Swapped Scott's house key for billfold Tile, removed transmit power, increased loop delay from 2 to 5 seconds
//    2021-11-19  Scott Miller   Separated BLE scan ESP32 from Wifi ESP32 so Wifi and BLE antenna not shared on a single device
//    2022-01-19  Scott Miller   Configured pull-up resistors for inputs
//******************************************************************************************
//******************************************************************************************
// SmartThings Library for ESP32WiFi
//******************************************************************************************
#include <SmartThingsESP32WiFi.h>

//******************************************************************************************
// ST_Anything Library 
//******************************************************************************************
#include <Constants.h>       //Constants.h is designed to be modified by the end user to adjust behavior of the ST_Anything library
#include <Device.h>          //Generic Device Class, inherited by Sensor and Executor classes
#include <Sensor.h>          //Generic Sensor Class, typically provides data to ST Cloud (e.g. Temperature, Motion, etc...)
#include <InterruptSensor.h> //Generic Interrupt "Sensor" Class, waits for change of state on digital input 
#include <PollingSensor.h>   //Generic Polling "Sensor" Class, polls Arduino pins periodically
#include <Everything.h>      //Master Brain of ST_Anything library that ties everything together and performs ST Shield communications

#include <IS_Presence.h>     //Implements a Presence Sensor (IS) to monitor the status of a digital input pin

// Required to disable BLE
#include <esp_bt.h>
#include <esp_bt_main.h>

//****************************************************************************************************************************
//Huzzah ESP32 Pin Definitions 
//****************************************************************************************************************************

//Digital Pins
#define PIN_PRESENCE_1            13  //SmartThings Capability "Presence Sensor" input pin
#define PIN_PRESENCE_2            15  //SmartThings Capability "Presence Sensor" input pin

//Jumper Tile_Presence_ESP32BLE.ino scan PIN_PRESENCE_OUT1 output to PIN_PRESENCE_1 input 
//Jumper Tile_Presence_ESP32BLE.ino scan PIN_PRESENCE_OUT2 output to PIN_PRESENCE_2 input

//******************************************************************************************
//ESP32 WiFi Information
//******************************************************************************************
String str_ssid     = "your ssid;                                 //  <---You must edit this line!
String str_password = "password;                          //  <---You must edit this line!
IPAddress ip(192, 168, 0, 146);           // Device IP Address      //  <---You must edit this line!  Garage
//IPAddress ip(192, 168, 0, 124);           // Device IP Address      //  <---You must edit this line! Dining
String hostName = "Garage";
IPAddress gateway(192, 168, 0, 1);       //router gateway          //  <---You must edit this line!
IPAddress subnet(255, 255, 255, 0);   //LAN subnet mask         //  <---You must edit this line!
IPAddress dnsserver(8,8,8,8);         //DNS server              //  <---You must edit this line!
const unsigned int serverPort = 8090; // port to run the http server on

// Hubitat Hub Information
IPAddress hubIp(192, 168, 0, 153);    // hubitat hub ip         //  <---You must edit this line!
const unsigned int hubPort = 39501;   // hubitat hub port

bool isPresent1 = false;
bool isPresent2 = false;
int numLoops = 0;

//******************************************************************************************
//Arduino Setup() routine
//******************************************************************************************
void setup()
{   
  //Set and initialize pins to read result of BLE scan
  pinMode(PIN_PRESENCE_1,INPUT_PULLUP);
  pinMode(PIN_PRESENCE_2,INPUT_PULLUP);

  //Required to disable BLE
  esp_bluedroid_disable();
  esp_bluedroid_deinit();
  esp_bt_controller_disable();
  esp_bt_controller_deinit();
  
  //******************************************************************************************
  //Declare each Device that is attached to the Arduino
  //  Notes: - For details on each device's constructor arguments below, please refer to the 
  //           corresponding header (.h) and program (.cpp) files.
  //         - The name assigned to each device (1st argument below) must match the Groovy
  //           Device Handler names.  
  //         - The new Composite Device Handler is comprised of a Parent DH and various Child
  //           DH's.  The names used below MUST not be changed for the Automatic Creation of
  //           child devices to work properly.  Simply increment the number by +1 for each duplicate
  //           device (e.g. presence1, presence2, presence3, etc...)  You can rename the Child Devices
  //           in the ST Phone Application or change the Hubitat Device Label
  //******************************************************************************************
  
  //Interrupt Sensors 
  static st::IS_Presence            sensor1(F("presence1"), PIN_PRESENCE_1, LOW, true, 1);
  static st::IS_Presence            sensor2(F("presence2"), PIN_PRESENCE_2, LOW, true, 1);
 
  //*****************************************************************************
  //  Configure debug print output from each main class 
  //***************************************************************************** 
  st::Everything::debug=false;
  st::Device::debug=false;
  st::PollingSensor::debug=false;
  st::InterruptSensor::debug=false;

  //*****************************************************************************
  //Initialize the "Everything" Class
  //*****************************************************************************

  //Create the SmartThings ESP32WiFi Communications Object
    //STATIC IP Assignment - Recommended
    //st::Everything::SmartThing = new st::SmartThingsESP32WiFi(str_ssid, str_password, ip, gateway, subnet, dnsserver, serverPort, hubIp, hubPort, st::receiveSmartString, "Dining Tile");
 
    //DHCP IP Assigment - Must set your router's DHCP server to provice a static IP address for this device's MAC address
    st::Everything::SmartThing = new st::SmartThingsESP32WiFi(str_ssid, str_password, serverPort, hubIp, hubPort, st::receiveSmartString, hostName);

  //Run the Everything class' init() routine which establishes WiFi communications with SmartThings Hub
  st::Everything::init();
  
  //*****************************************************************************
  //Add each sensor to the "Everything" Class
  //*****************************************************************************
  st::Everything::addSensor(&sensor1);
  st::Everything::addSensor(&sensor2);
      
  //*****************************************************************************
  //Initialize each of the devices which were added to the Everything Class
  //*****************************************************************************
  st::Everything::initDevices();
}
  
//******************************************************************************************
//Arduino Loop() routine
//******************************************************************************************
void loop() { 
   
   //*****************************************************************************
   //Execute the Everything run method which takes care of "Everything"
   //*****************************************************************************
   if (numLoops >= 5) {        // execute st::Everything::run() every 5 loops
      st::Everything::run();
      numLoops = 0;
   } else {
      numLoops++;
   }
}
