/**
 *  Copyright 2015 SmartThings
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
 */
metadata {
	definition (name: "Quirky Outlink as a Sprinkler Switch", namespace: "kicko-st", author: "kicko") {
		capability "Actuator"	// has commands
		capability "Sensor" 	// has attributes
        capability "Switch"
		capability "Refresh"		
		command "OnWithZoneTimes"
		command "rainDelayed"
		command "update"         
		command "noEffect"
		command "skip"
		command "expedite"
		command "onHold"
		command "warning"
		attribute "effect", "string"

		fingerprint endpointId: "01", profileId: "0104", inClusters: "0000, 0003, 0006, 0005, 0004, FC20, 0702", outClusters: "0019", manufacturer: "Quirky", model: "ZHA Smart Plug"
	}

	// tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: 'Running', action: "switch.off", icon: "st.Health & Wellness.health7", backgroundColor: "#53a7c0", nextState: "stopping"
				attributeState "off", label: 'Start', action: "switch.on", icon: "st.Outdoor.outdoor12", backgroundColor: "#ffffff", nextState: "starting"
                attributeState "starting", label: 'Starting...', action: "switch.off", icon: "st.Health & Wellness.health7", backgroundColor: "#53a7c0"
            	attributeState "stopping", label: 'Stopping...', action: "switch.off", icon: "st.Health & Wellness.health7", backgroundColor: "#53a7c0"
            	attributeState "rainDelayed", label: 'Rain Delay', action: "switch.off", icon: "st.Weather.weather10", backgroundColor: "#fff000", nextState: "off"
        		attributeState "warning", label: 'Issue', action: "switch.off", icon: "st.Health & Wellness.health7", backgroundColor: "#ff000f", nextState: "off"
			}
            tileAttribute ("device.effect", key: "SECONDARY_CONTROL") {
            	attributeState "noEffect", label:'Normal', icon: "st.Office.office7", backgroundColor: "#ffffff"
                attributeState "skip", label:'Skip 1X', icon: "st.Office.office7", backgroundColor: "#c0a353"
                attributeState "expedite", label:'Expedite', icon: "st.Office.office7", backgroundColor: "#53a7c0"
                attributeState "onHold", label:'Pause', icon: "st.Office.office7", backgroundColor: "#bc2323"
            }                       
		}
        standardTile("scheduleEffect", "device.effect", width: 2, height: 2) {
            state("noEffect", label: "Normal", action: "skip", icon: "st.Office.office7", backgroundColor: "#ffffff")
            state("skip", label: "Skip 1X", action: "expedite", icon: "st.Office.office7", backgroundColor: "#c0a353")
            state("expedite", label: "Expedite", action: "onHold", icon: "st.Office.office7", backgroundColor: "#53a7c0")
            state("onHold", label: "Pause", action: "noEffect", icon: "st.Office.office7", backgroundColor: "#bc2323")
        }
		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "switch"
		details(["switch","refresh","scheduleEffect"])
	}
}

def parse(String description) {
	log.debug "description is $description"
    def resultMap = zigbee.getKnownDescription(description)
    
    if (resultMap) {
        if (resultMap.type == "update") {
            log.info "$device updates: ${resultMap.value}"
        }
        else {
            createEvent(name: resultMap.type, value: resultMap.value)
        }
    }
    else {
        def descMap = zigbee.parseDescriptionAsMap(description)
        log.debug descMap        
    }
}

def off() {
    zigbee.off()
}

def on() {
    zigbee.on()
}

def refresh() {
	zigbee.onOffRefresh()
}

def configure() {    
    zigbee.onOffConfig() + 
    refresh() 
}

def OnWithZoneTimes(value) {
    log.info "Executing 'allOn' with zone times [$value]"
    def evt = createEvent(name: "switch", value: "starting", displayed: true)
    sendEvent(evt)
    
	zigbee.on()
}

def update() {
    zigbee.onOffRefresh()
}

def rainDelayed() {
    log.info "rain delayed"
    if(device.currentValue("switch") != "on") {
        sendEvent (name:"switch", value:"rainDelayed", displayed: true)
    }
}

def warning() {
    log.info "Warning: Programmed Irrigation Did Not Start"
    if(device.currentValue("switch") != "on") {
        sendEvent (name:"switch", value:"warning", displayed: true)
    }
}

// commands that over-ride the SmartApp

// skip one scheduled watering
def	skip() {
    def evt = createEvent(name: "effect", value: "skip", displayed: true)
    log.info("Sending: $evt")
    sendEvent(evt)
}
// over-ride rain delay and water even if it rains
def	expedite() {
    def evt = createEvent(name: "effect", value: "expedite", displayed: true)
    log.info("Sending: $evt")
    sendEvent(evt)
}

// schedule operates normally
def	noEffect() {
    def evt = createEvent(name: "effect", value: "noEffect", displayed: true)
    log.info("Sending: $evt")
    sendEvent(evt)
}

// turn schedule off indefinitely
def	onHold() {
    def evt = createEvent(name: "effect", value: "onHold", displayed: true)
    log.info("Sending: $evt")
    sendEvent(evt)
}