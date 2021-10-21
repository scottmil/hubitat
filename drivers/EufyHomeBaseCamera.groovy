/*
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  10/19/2021  scottmil  Adapted from ioBroker code developed by eibyer
 *
 */
 
metadata {
	definition (name: "Eufy HomeBase Camera", namespace: "scottmil", author: "scottmil") {
		capability "Switch"
		capability "Refresh"
        
        command "poll"
        command "refresh"
        
        // HomeBase commands
        command "enableMotion"
        command "disableMotion"
        command "enableAudio"
        command "disableAudio"
        command "enableLed"
        command "disableLed"
       
        attribute "motionDetection", "string"
		  attribute "audioRecording", "string"
		  attribute "statusLed", "string"
		  attribute "battery", "string"
        attribute "lastUpdate", "string"
	}
   
    preferences {
	    section ("Settings") {
            input name: "deviceIP", type:"text", title:"ioBroker IP Address", required: true
            input name: "devicePort", type:"text", title:"ioBroker Port", required: true
            input name: "deviceHomeBaseSerialNumber", type: "text", title: "Eufy HomeBase Serial Number", required: true
            input name: "deviceCameraSerialNumber", type: "text", title: "Eufy Camera Serial Number", required: true         
        }
	}

}

private getApiPath() { 
	"/get/eufy-security.0." + "$deviceHomeBaseSerialNumber" + ".cameras."
}

private setApiPath() { 
	"/set/eufy-security.0." + "$deviceHomeBaseSerialNumber" + ".cameras."
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
	// Schedule it to run every 5 minutes
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
    if (deviceHomeBaseSerialNumber == null) {
    	log.debug "HomeBase SN missing in preferences"
        return
    }
    if (deviceCameraSerialNumber == null) {
    	log.debug "Camera SN missing in preferences"
        return
    }
    
    device.deviceNetworkId = "$deviceCameraSerialNumber"
    
    lst = ["enabled","motion_detection", "audio_recording", "battery", "status_led"]
    lst.each { doPoll(it) }
    
}
     
private def doPoll(param) { 
      
    log.debug "Entering doPoll() with param = " + "$param"
     
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
	
   log.debug "Parsing '${response}'"
   def json = response.json
   log.debug "Received '${json}'"
    
    if (json.toString().contains("$deviceCameraSerialNumber" + ".motion_detection")) {
        sendEvent(name: 'motionDetection', value: "${json.val}")
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
     
    log.debug "Exiting parse(response)"
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
    
    log.debug "Entering doSwitch with command=" + "$command"+ " and value=" + "$value" 
	
    if (deviceIP == null) {
    	log.debug "ioBroker IP address missing in preferences"
        return
    }
    if (devicePort == null) {
    	log.debug "ioBroker Port missing in preferences"
        return
    }
    if (deviceHomeBaseSerialNumber == null) {
    	log.debug "HomeBase SN missing in preferences"
        return
    }
    if (deviceCameraSerialNumber == null) {
    	log.debug "Camera SN missing in preferences"
        return
    }
        
 	def path = setApiPath() + deviceCameraSerialNumber + "." + command +"?value=" + value + "&ack=false"
    device.deviceNetworkId = "$deviceCameraSerialNumber" 
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
