/*  Eufy HomeBase
 *  Version 1.1
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  10/19/2021  scottmil  Adapted from ioBroker code developed by eibyer
 *  10/23/2021  scottmil  Added debug preference, removed redundant code for checking required preferences
 *
 *    states:
 *    "0": "Away",
 *    "1": "Home",
 *    "2": "Schedule",
 *    "3": "Custom 1",
 *    "4": "Custom 2",
 *    "5": "Custom 3",
 *    "47": "Geofencing",
 *    "63": "Disarmed"
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
            input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        }
	}

}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}


def installed() {
	log.info "Installed with settings: ${settings}"
}

def uninstalled() {
	if (logEnable)log.debug "Uninstalled"
}

def updated() {
	log.info "Updated with settings: ${settings}"
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)

	initialize()
}

def initialize() {
    device.deviceNetworkId = "$deviceSerialNumber" 
    // Do the initial poll
	poll()
	// Schedule it to run every 5 minutes
	runEvery5Minutes("poll")
}

def refresh() {
	poll()
}

private getApiPath() { 
	"/get/eufy-security.0." 
}

private setApiPath() { 
	"/set/eufy-security.0." 
}

def poll() {
    
    def path = getApiPath() + deviceSerialNumber + ".station.guard_mode"
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
	
   if (logEnable)log.debug "Parsing '${response}'"
   def json = response.json
   if (logEnable)log.debug "Received '${json}'"
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
   
 	def path = setApiPath() + deviceSerialNumber + ".station.guard_mode?value=" + mode + "&ack=false" 
  	def hostAddress = "$deviceIP:$devicePort"
    def headers = [:] 
    headers.put("HOST", hostAddress)

    if (logEnable)log.debug path
    
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
    	log.warn "Cannot set update time : location not defined in app"
    }
    else {
   		lastUpdate = new Date(timeNow).format("MMM dd yyyy HH:mm", location.timeZone)
    }
    return lastUpdate
}
