/*
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  10/19/2021  scottmil  Adapted from ioBroker code developed by eibyer
 *
 "states": {
      "0": "Away",
      "1": "Home",
      "2": "Schedule",
      "3": "Custom 1",
      "4": "Custom 2",
      "5": "Custom 3",
      "47": "Geofencing",
      "63": "Disarmed"
 */
metadata {
	definition (name: "Eufy Homebase", namespace: "scottmil", author: "scottmil") {
		capability "Switch"
		capability "Refresh"
        
        command "poll"
        command "refresh"
        
        // HomeBase commands
        command "away"
        command "home"
        command "schedule"
        command "custom1"
        command "custom2"
        command "custom3"
        command "geofencing"
        command "disarmed"

		  attribute "mode", "string"
		  attribute "lastUpdate", "string"
	}
   
    preferences {
	    section ("Settings") {
            input name: "deviceIP", type:"text", title:"ioBroker IP Address", required: true
            input name: "devicePort", type:"text", title:"ioBroker Port", required: true
            input name: "deviceSerialNumber", type: "text", title: "Eufy Homebase Serial Number", required: true      
        }
	}

}

private getApiPath() { 
	"/get/eufy-security.0." 
}

private setApiPath() { 
	"/set/eufy-security.0." 
}

def installed() {
	log.debug "Installed with settings: ${settings}"
}

def uninstalled() {
	log.debug "uninstalled()"
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	initialize()
}

def initialize() {
	
    // Do the initial poll
	poll()
	// Schedule it to run every minute
	runEvery5Minutes("poll")

}
def refresh() {
	poll()
}

def poll() {

	if (deviceIP == null) {
    	log.debug "ioBroker IP address missing in preferences"
        return
    }
    if (devicePort == null) {
    	log.debug "ioBroker Port missing in preferences"
        return
    }
    if (deviceSerialNumber == null) {
    	log.debug "HomeBase SN missing in preferences"
        return
    }
    
    def path = getApiPath() + deviceSerialNumber + ".station.guard_mode"
    device.deviceNetworkId = "$deviceSerialNumber" 
  	def hostAddress = "$deviceIP:$devicePort"
    def headers = [:] 
    headers.put("HOST", hostAddress)

    def hubAction = new hubitat.device.HubAction(
        method: "GET",
        path: path,
        headers: headers,
        null,
        [callback : parse] 
    )
    sendHubCommand(hubAction)
}

def parse(response) {
	
   log.debug "Parsing '${response}'"
   def json = response.json
	log.debug "Received '${json}'"
	switch (json.val) {
      case 0:
    	   sendEvent(name: "mode", value: "away")
           sendEvent(name: "switch", value: "on")
           break
    	case 1:
    	   sendEvent(name: "mode", value: "home")
           sendEvent(name: "switch", value: "on")
           break
    	case 2:
    	   sendEvent(name: "mode", value: "schedule")
           sendEvent(name: "switch", value: "on")
           break
    	case 3:
    	   sendEvent(name: "mode", value: "custom1")
           sendEvent(name: "switch", value: "on")
           break
    	case 4:
    	   sendEvent(name: "mode", value: "custom2")
           sendEvent(name: "switch", value: "on")
           break
        case 5:
    	   sendEvent(name: "mode", value: "custom3")
           sendEvent(name: "switch", value: "on")
           break
        case 47:
    	   sendEvent(name: "mode", value: "geofencing")
           sendEvent(name: "switch", value: "on")
           break
        case 63:
           sendEvent(name: "mode", value: "disarmed")
           sendEvent(name: "switch", value: "off")
           break
        default:
           log.warn "Unknown JSON response"
    }
    
    sendEvent(name: 'lastUpdate', value: lastUpdated(now()), unit: "")
   
}

// on defaults to home mode
def on() {
	doSwitch(1)
}

// off defaults to disarmed mode
def off() {
	doSwitch(63)
}

def away() {
   doSwitch(0)
}

def home() {
   doSwitch(1)
}

def schedule() {
   doSwitch(2)
}

def custom1() {
   doSwitch(3)
}

def custom2() {
   doSwitch(4)
}

def custom3() {
   doSwitch(5)
}

def geofencing() {
   doSwitch(47)
}

def disarmed() {
   doSwitch(63)
}


def doSwitch(mode) {
	
    if (deviceIP == null) {
    	log.debug "ioBroker IP address missing in preferences"
        return
    }
    if (deviceIP == null) {
        log.debug "ioBroker Port missing in preferences"
        return
    }
    if (deviceSerialNumber == null) {
        log.debug "HomeBase SN missing in preferences"
        return
    }
   
 	def path = setApiPath() + deviceSerialNumber + ".station.guard_mode?value=" + mode + "&ack=false"

    device.deviceNetworkId = "$deviceSerialNumber" 
  	def hostAddress = "$deviceIP:$devicePort"
    def headers = [:] 
    headers.put("HOST", hostAddress)

    log.debug path
    
    def hubAction = new hubitat.device.HubAction(
        method: "GET",
        path: path,
        headers: headers,
        null,
        [callback : parse] 
    )
    sendHubCommand(hubAction)
}

def lastUpdated(time) {
	def timeNow = now()
	def lastUpdate = ""
	if(location.timeZone == null) {
    	log.debug "Cannot set update time : location not defined in app"
    }
    else {
   		lastUpdate = new Date(timeNow).format("MMM dd yyyy HH:mm", location.timeZone)
    }
    return lastUpdate
}
