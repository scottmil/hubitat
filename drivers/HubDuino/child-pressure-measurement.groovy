/**
 *  Child Pressure Measurement
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
 *    2018-07-01  Dan Ogorchock  Original Creation - Unique for SmartThings as there is no standard Pressure Measurement Capability
 *    2020-09-28  Dan Ogorchock  Removed lastUpdated attribute
 *    2023-03-21  Scott Miller   Added logEnable preference
 *    2023-03-22  Scott Miller   Modified logsOff unscheduling
 *
 * 
 */
metadata {
	definition (name: "Child Pressure Measurement", namespace: "ogiewon", author: "Daniel Ogorchock") {
		capability "Sensor"

        attribute "pressure", "Number"   //ST does not have a standard Capability for Pressure Measurement
	}
        
	preferences {
        section("Prefs") {
            input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        }
    }
}

def logsOff(){
    log.warn "Debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def parse(String description) {
    if (logEnable) log.debug "parse(${description}) called"
	def parts = description.split(" ")
    def name  = parts.length>0?parts[0].trim():null
    def value = parts.length>1?parts[1].trim():null
    if (name && value) {
        // Update device
        sendEvent(name: name, value: value) 
    } else {
    	log.error "Missing either name or value.  Cannot parse!"
    }
}

def installed() {
    updated()
}

def updated() {
    if (logEnable) {
        log.info "Enabling debug logging for 30 minutes" 
        runIn(1800,logsOff)
    } else {
        unschedule(logsOff)
    }
}
