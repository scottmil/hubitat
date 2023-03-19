/**
 *  Child Sound Sensor
 *
 *  Copyright 2017 Daniel Ogorchock
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 *    2017-04-10  Dan Ogorchock  Original Creation
 *    2017-08-23  Allan (vseven) Added a generateEvent routine that gets info from the parent device.  This routine runs each time the value is updated which can lead to other modifications of the device.
 *    2018-06-02  Dan Ogorchock  Revised/Simplified for Hubitat Composite Driver Model
 *    2018-09-22  Dan Ogorchock  Added preference for debug logging
 *    2019-07-01  Dan Ogorchock  Added importUrl
 *    2022-04-20  Scott Miller   Modified for Sound Sensor
 *    2022-05-08  Scott Miller   Added descriptionText logging
 *    2023-03-19  Scott Miller   Removed custom attribute "lastUpdated" and set namespace to "ogiewon" so parent will function properly.
 *
 * 
 */
 metadata {
	definition (
        name: "Child Sound Sensor",
        namespace: "ogiewon",
        importUrl: "https://github.com/scottmil/hubitat/tree/main/drivers/HubDuino/child-soundsensor.groovy",
        author: "Scott Miller"
    ) {
		capability "SoundSensor"
		capability "Sensor"
	}

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}

}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}


def parse(String description) {
    if (logEnable) log.debug "parse(${description}) called"
	def parts = description.split(" ")
    def name  = parts.length>0?parts[0].trim():null
    def value = parts.length>1?parts[1].trim():null
    if (name && value) {    
    	// Update device
        if (value != "detected") {value = "not detected"};  
        if (txtEnable && value == "detected"){
           log.info "${device.displayName} ${name} is ${value}."
        }
        sendEvent(name: name, value: value)
    }
    else {
    	log.error "Missing either name or value.  Cannot parse!"
    }
}

def installed() {
    updated()
}

def updated() {
    if (logEnable) runIn(1800,logsOff)
}
