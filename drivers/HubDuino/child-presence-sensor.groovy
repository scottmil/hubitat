/**
 *  Child Presence Sensor
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
 *    2018-02-24  Dan Ogorchock  Original Creation
 *    2018-06-02  Dan Ogorchock  Revised/Simplified for Hubitat Composite Driver Model
 *    2020-09-28  Dan Ogorchock  Tweaked metadata section for new ST App, removed lastUpdated attribute 
 *    2023-03-21  Scott Miller   Added logEnable preference
 *    2023-03-22  Scott Miller   Modified logsOff unscheduling
 * 
 */
metadata {
	definition (name: "Child Presence Sensor", namespace: "ogiewon", author: "Daniel Ogorchock", vid: "generic-arrival-4") {
		capability "Sensor"
		capability "Presence Sensor"

	}
    
	preferences {
		section("Prefs") {
			input "presenceTriggerValue", "number", title: "(Optional) Presence Trigger Value\nAt what value is presence triggered?", required: false, displayDuringSetup: false
            input "invertTriggerLogic", "bool", title: "(Optional) Invert Logic", description: "False = Present > Trigger Value\nTrue = Present < Trigger Value", default: false, required: false, displayDuringSetup: false
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
        if (value.isNumber()) {
            if (presenceTriggerValue) {
                if(logEnable) log.debug "Presence received a numeric value. Perform comparison of value: ${Float.valueOf(value.trim())} versus presenceTriggerValue: ${presenceTriggerValue}"
                if (Float.valueOf(value.trim()) >= presenceTriggerValue) {
                    value = invertTriggerLogic?"not present":"present"
                } 
                else {
                    value = invertTriggerLogic?"present":"not present"
                }
            }
            else {
                log.error "Please configure the Presence Trigger Value in device settings!"
            }
        }
        else {
            if (logEnable) log.debug "Presence received a string.  value = ${value}"
            if (value != "present") { value = "not present" }
        }
        // Update device
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
     if (logEnable) {
        log.info "Enabling debug logging for 30 minutes" 
        runIn(1800,logsOff)
     } else {
        unschedule(logsOff)
     }
}