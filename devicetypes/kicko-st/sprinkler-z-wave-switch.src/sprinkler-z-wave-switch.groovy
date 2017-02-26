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
	definition (name: "Sprinkler Z-Wave Switch", namespace: "kicko-st", author: "kicko") {
		capability "Actuator"
		capability "Indicator"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
        command "OnWithZoneTimes"
        command "rainDelayed"
        command "update"         
        command "noEffect"
        command "skip"
        command "expedite"
        command "onHold"
        command "warning"
	    attribute "effect", "string"

		fingerprint inClusters: "0x25"
	}

	// simulator metadata
	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"

		// reply messages
		reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
		reply "200100,delay 100,2502": "command: 2503, payload: 00"
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
		standardTile("indicator", "device.indicatorStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
			state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
			state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
		}
		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "switch"
		details(["switch","refresh","scheduleEffect","indicator"])
	}
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x20: 1, 0x70: 1])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	if (result?.name == 'hail' && hubFirmwareLessThan("000.011.00602")) {
		result = [result, response(zwave.basicV1.basicGet())]
		log.debug "Was hailed: requesting state update"
	} else {
		log.debug "Parse returned ${result?.descriptionText}"
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	def value = "when off"
	if (cmd.configurationValue[0] == 1) {value = "when on"}
	if (cmd.configurationValue[0] == 2) {value = "never"}
	[name: "indicatorStatus", value: value, display: false]
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
	[name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	log.debug "manufacturerId:   ${cmd.manufacturerId}"
	log.debug "manufacturerName: ${cmd.manufacturerName}"
	log.debug "productId:        ${cmd.productId}"
	log.debug "productTypeId:    ${cmd.productTypeId}"
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)
	updateDataValue("manufacturer", cmd.manufacturerName)
	createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}


def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def on() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	])
}

def OnWithZoneTimes(value) {
    log.info "Executing 'allOn' with zone times [$value]"
    def evt = createEvent(name: "switch", value: "starting", displayed: true)
    sendEvent(evt)
    
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	])
}

def off() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	])
}

def poll() {
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	])
}

def refresh() {
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	])
}

def update() {
    delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	])
}

def indicatorWhenOn() {
	sendEvent(name: "indicatorStatus", value: "when on", display: false)
	zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()
}

def indicatorWhenOff() {
	sendEvent(name: "indicatorStatus", value: "when off", display: false)
	zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()
}

def indicatorNever() {
	sendEvent(name: "indicatorStatus", value: "never", display: false)
	zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1).format()
}

def invertSwitch(invert=true) {
	if (invert) {
		zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1).format()
	}
	else {
		zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1).format()
	}
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