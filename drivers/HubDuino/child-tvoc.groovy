/**
 *  Child TVOC Measurement
 *
 *  Copyright 2018 Daniel Ogorchock
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
 *    2018-07-01  Dan Ogorchock  Original Creation 
 *    2021-05-09  Scott Miller   Adapted for TVOC Measurement
 *    2022-04-29  Scott Miller   Removed SmartThings tiles metadata
 *    2023-03-18  Scott Miller   Removed custom attribute "lastUpdated" and restored "ogiewon" namespace
 * 
 */
metadata {
	definition (
        name: "Child TVOC Measurement",
        namespace: "ogiewon",
        importUrl: "https://github.com/scottmil/hubitat/tree/main/drivers/HubDuino/child-tvoc.groovy",
        author: "Scott Miller"
    ) {
        capability "AirQuality"
		capability "Sensor"
        
        attribute "tvoc", "string"
        
	}
	
	preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
	}
        
}

def parse(String description) {
    if (logEnable) log.debug "parse(${description}) called"
	def parts = description.split(" ")
    def name  = parts.length>0?parts[0].trim():null
    def value = parts.length>1?parts[1].trim():null
    if (name && value) {
        // Update device
        sendEvent(name: name, value: value, unit:" ")
    }
    else {
    	log.error "Missing either name or value.  Cannot parse!"
    }
}

def installed() {
    updated()
}

def updated() {
    if (logEnable) runIn(1800, parse,logsOff)
}
