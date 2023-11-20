/*  Eufy HomeBase Camera
 *  Version 2.3
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  10/19/2021  1.0   scottmil  Adapted from ioBroker code developed by eibyer
 *  10/23/2021  1.1   scottmil  Added debug logging preference, removed redundant code for required preferences
 *  02/06/2022  1.1.1 scottmil  Added test for null JSON response, refreshing if no response received
 *  02/06/2022  1.1.2 scottmil  Added default port
 *  02/08/2022  1.1.3 scottmil  Added MotionSensor capability
 *  02/09/2022  2.0   scottmil  Added ability to configure ioBroker.euSec instance  See: https://github.com/bropat/ioBroker.eusec
 *  03/02/2022  2.1   scottmil  Removed Motion Sensor capablity as ioBroker and Hubitat MakerAPI required to notify when motion is detected
 *  06/22/2022  2.2   scottmil  Updated when debug logging occurs
 *  11/17/2023  2.3   scottmil  Added Battery capability so battery level can be monitored by Hubitat apps
 */
 
metadata {
	definition (name: "Eufy HomeBase Camera", namespace: "scottmil", author: "scottmil") {
	capability "Switch"
	capability "Refresh"
        capability "Sensor"
        capability "Battery"
       
        
        command "refresh"
        
        // HomeBase commands
        command "enableMotion"
        command "disableMotion"
        command "enableAudio"
        command "disableAudio"
        command "enableLed"
        command "disableLed"
       
        attribute "motion", "string"
	attribute "audioRecording", "string"
	attribute "statusLed", "string"
	attribute "battery", "string"
        attribute "lastUpdate", "string"
	}
   
    preferences {
	    section ("Settings") {
            input name: "deviceIP", type:"text", title:"ioBroker IP Address", required: true
            input name: "devicePort", type:"text", title:"ioBroker Port", required: true, defaultValue: "8087"
            input name: "euSecInstance", type:"text", title:"ioBroker.euSec Instance", required: true, defaultValue: "eusec.0"
            input name: "deviceHomeBaseSerialNumber", type: "text", title: "Eufy HomeBase Serial Number", required: true
            input name: "deviceCameraSerialNumber", type: "text", title: "Eufy Camera Serial Number", required: true 
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
    if (logEnable) log.debug "Uninstalled"
}

def updated() {
    log.info "Updated with settings: ${settings}"
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)

	initialize()
}

def initialize() {
    device.deviceNetworkId = "$deviceCameraSerialNumber"
    // Do the initial poll
	poll()
	// Schedule it to run every 5 minutes
	runEvery5Minutes("poll")
}

def refresh() {
	poll()
}

private getApiPath() { 
	"/get/" + "$euSecInstance" + "." + "$deviceHomeBaseSerialNumber" + ".cameras."
}

private setApiPath() { 
	"/set/" + "$euSecInstance" + "." + "$deviceHomeBaseSerialNumber" + ".cameras."
}

def poll() {
    //Get values of camera properties
    lst = ["enabled","motion_detection", "audio_recording", "battery", "status_led"]
    lst.each { doPoll(it) }
    
}
     
private def doPoll(param) { 
      
    if (logEnable)log.debug "Entering doPoll() with param = " + "$param"
     
    def path = getApiPath() + deviceCameraSerialNumber + "." + param
    
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
    if(json) {
       if (logEnable)log.debug "Received '${json}'"
    
       if (json.toString().contains("$deviceCameraSerialNumber" + ".motion_detection")) {
           sendEvent(name: 'motion', value: "$json.val")  
	   } else if (json.toString().contains("$deviceCameraSerialNumber" + ".battery")) {
	      sendEvent(name: 'battery', value: "$json.val")
	   } else if  (json.toString().contains("$deviceCameraSerialNumber" + ".audio_recording")) {
	      sendEvent(name: 'audioRecording', value: "$json.val") 
	   } else if (json.toString().contains("$deviceCameraSerialNumber" + ".status_led")) {
	      sendEvent(name: 'statusLed', value: "$json.val") 
	   } else if (json.toString().contains("$deviceCameraSerialNumber" + ".enabled")) {
	      if ("$json.val".equalsIgnoreCase("true")) {
	        sendEvent(name: 'switch', value: "on")
	      } else {
	        sendEvent(name: 'switch', value: "off") 
	      }
	   } else {
	     log.warn "Unknown JSON response" 
	   }
       sendEvent(name: 'lastUpdate', value: lastUpdated(now()), unit: "")
    
    } else {
       log.warn "No JSON response received, refreshing..."
       //Arbitrary delay
       def count = 1
       while(count <= 50) {
          count++
       }
       refresh()
   }   
}

// on defaults to enabled state
def on() {
	doSwitch("enabled","true")
}

// off defaults to disabled state
def off() {
	doSwitch("enabled","false")
}

def enableAudio() {
   doSwitch("audio_recording","true")
}

def disableAudio() {
   doSwitch("audio_recording","false")
}

def enableMotion() {
   doSwitch("motion_detection","true")
}

def disableMotion() {
   doSwitch("motion_detection","false")
}

def enableLed() {
   doSwitch("status_led","true")
}

def disableLed() {
   doSwitch("status_led","false")
}

def doSwitch(command, value) {
    
    if (logEnable) log.debug "Entering doSwitch with command=" + "$command"+ " and value=" + "$value" 
	   
 	def path = setApiPath() + deviceCameraSerialNumber + "." + command +"?value=" + value + "&ack=false" 
  	def hostAddress = "$deviceIP:$devicePort"
    def headers = [:] 
    headers.put("HOST", hostAddress)

    if (logEnable) log.debug path
    
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



