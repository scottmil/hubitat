/*  Eufy HomeBase
 *  Version 2.2
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  10/19/2021  scottmil  1.0   Adapted from ioBroker code developed by eibyer
 *  10/23/2021  scottmil  1.1   Added debug preference, removed redundant code for checking required preferences
 *  02/05/2022  scottmil  1.2   Changed "schedule" to "scheduled" to avoid naming conflict with Hubitat
 *  02/05/2022  scottmil  1.2.1 Handled json.val parsing error 
 *  02/06/2022  scottmil  1.2.2 Added refresh() if initial parse callback yields no response
 *  02/06/2022  scottmil  1.2.3 Added attribute switch and default port
 *  02/09/2022  scottmil  2.0   Added ability to configure ioBroker.euSec instance  See: https://github.com/bropat/ioBroker.eusec
 *  06/22/2022  scottmil  2.1   Updated when debug logging occurs
 *  12/02/2022  scottmil  2.1.1 Fixed minor parse() bug where debug msg "No JSON response received..." logged when debug off
 *  04/06/2024  scottmil  2.2   Added optional labels for custom modes
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
        
        //command "poll"
        command "refresh"
        
        // HomeBase commands
        command "away"
        command "home"
        command "scheduled"
        command "custom1"
        command "custom2"
        command "custom3"
        command "geofencing"
        command "disarmed"

	attribute "mode", "string"
        attribute "switch", "string"
	attribute "lastUpdate", "string"
    }
   
    preferences {
	    section ("Settings") {
            input name: "deviceIP", type:"text", title:"ioBroker IP Address", required: true
            input name: "devicePort", type:"text", title:"ioBroker Port", required: true, defaultValue: "8087"
            input name: "euSecInstance", type:"text", title:"ioBroker.euSec Instance", required: true, defaultValue: "eusec.0"
            input name: "deviceSerialNumber", type: "text", title: "Eufy Homebase Serial Number", required: true 
            input name: "custom1Label", type: "text", title: "Custom1 Label", required: false
            input name: "custom2Label", type: "text", title: "Custom2 Label", required: false
            input name: "custom3Label", type: "text", title: "Custom3 Label", required: false
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
	"/get/" + "$euSecInstance" + "."
}

private setApiPath() { 
	"/set/" + "$euSecInstance" + "."
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
   if (json) {
      if (logEnable)log.debug "Received '${json}'"
      switch (json.val) {
         case 0:
    	      sendEvent(name: "mode", value: "Away")
              sendEvent(name: "switch", value: "on")
              break
    	 case 1:
    	      sendEvent(name: "mode", value: "Home")
              sendEvent(name: "switch", value: "on")
              break
    	 case 2:
    	      sendEvent(name: "mode", value: "Scheduled")
              sendEvent(name: "switch", value: "on")
              break
    	 case 3:
              if(custom1Label) {
                  sendEvent(name: "mode", value: custom1Label)
              } else {
    	          sendEvent(name: "mode", value: "Custom1")
              }
              sendEvent(name: "switch", value: "on")
              break
    	 case 4:
              if(custom2Label) {
                  sendEvent(name: "mode", value: custom2Label)
              } else {
    	          sendEvent(name: "mode", value: "Custom2")
              }
              sendEvent(name: "switch", value: "on")
              break
         case 5:
    	      if(custom3Label) {
                  sendEvent(name: "mode", value: custom3Label)
              } else {
    	          sendEvent(name: "mode", value: "Custom3")
              }
              sendEvent(name: "switch", value: "on")
              break
         case 47:
    	      sendEvent(name: "mode", value: "Geofencing")
              sendEvent(name: "switch", value: "on")
              break
         case 63:
              sendEvent(name: "mode", value: "Disarmed")
              sendEvent(name: "switch", value: "off")
              break
         default:
              log.warn "Unknown JSON response"
        }
        sendEvent(name: 'lastUpdate', value: lastUpdated(now()), unit: "")
   } else {
       if (logEnable)log.debug "No JSON response received, refreshing..."
       //Arbitrary delay
       def count = 1
       while(count <= 50) {
          count++
       }
       refresh()
   }   
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

def scheduled() {
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
   
    def path = setApiPath() + deviceSerialNumber + ".station.guard_mode?value=" + mode + "&prettyPrint&ack=false"
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
