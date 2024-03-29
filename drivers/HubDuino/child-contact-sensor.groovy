/**
 *  Child Contact Sensor
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
 *    2018-09-25  Allan (vseven) Added metadata for new SmartThings app
 *    2020-09-27  Dan Ogorchock  Tweaked metatda for new ST App, removed lastUpdated attribute
 *    2023-03-22  Scott Miller   Removed ST tiles, added logEnable preference and unscheduling
 *
 * 
 */
metadata {
	definition (name: "Child Contact Sensor", namespace: "ogiewon", author: "Dan Ogorchock", ocfDeviceType: "x.com.st.d.sensor.contact", vid: "generic-contact") {
		capability "Contact Sensor"
		capability "Sensor"

    }

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
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
