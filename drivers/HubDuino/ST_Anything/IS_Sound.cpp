//******************************************************************************************
//  File: IS_Sound.cpp
//  Authors: Scott Miller (modifier), Dan G Ogorchock & Daniel J Ogorchock (Father and Son, creators)
//
//  Summary:  IS_Sound is a class which implements the SmartThings "Sound Sensor" device capability.
//			  It inherits from the st::InterruptSensor class.
//
//			  Create an instance of this class in your sketch's global variable section
//			  For Example:  static st::IS_Sound sensor6(F("sound1"), PIN_SOUND, HIGH, false, 500);
//
//			  st::IS_Sound() constructor requires the following arguments
//				- String &name - REQUIRED - the name of the object - must match the Groovy ST_Anything DeviceType tile name
//				- byte pin - REQUIRED - the Arduino Pin to be used as a digital input
//				- bool iState - REQUIRED - LOW or HIGH - determines which value indicates the interrupt is true
//				- bool internalPullup - OPTIONAL - true == INTERNAL_PULLUP
//				- long numReqCounts - OPTIONAL - number of counts before changing state of input (prevent false alarms)
//
//  Change History:
//
//    Date        Who            What
//    ----        ---            ----
//    2015-01-03  Dan & Daniel   Original Creation
//    2015-03-17  Dan Ogorchock  Added optional "numReqCounts" constructor argument/capability
//    2018-08-30  Dan Ogorchock  Modified comment section above to comply with new Parent/Child Device Handler requirements
//    2019-11-04  Dan Ogorchock  Updated Comments
//    2022-04-20  Scott Miller         Created and modified for Sound Sensor
//
//
//******************************************************************************************

#include "IS_Sound.h"

#include "Constants.h"
#include "Everything.h"

namespace st
{
//private

//public
	//constructor
	IS_Sound::IS_Sound(const __FlashStringHelper *name, byte pin, bool iState, bool pullup, long numReqCounts) :
		InterruptSensor(name, pin, iState, pullup, numReqCounts)  //use parent class' constructor
		{
		}
	
	//destructor
	IS_Sound::~IS_Sound()
	{
	}
	
	void IS_Sound::init()
	{
		//get current status of motion sensor by calling parent class's init() routine - no need to duplicate it here!
		InterruptSensor::init();
	}

	//called periodically by Everything class to ensure ST Cloud is kept consistent with the state of the contact sensor
	void IS_Sound::refresh()
	{
		Everything::sendSmartString(getName() + (getStatus() ? F(" notdetected") : F(" detected")));
	}

	void IS_Sound::runInterrupt()
	{
		//add the "closed" event to the buffer to be queued for transfer to the ST Shield
		Everything::sendSmartString(getName() + F(" notdetected"));
	}
	
	void IS_Sound::runInterruptEnded()
	{
		//add the "open" event to the buffer to be queued for transfer to the ST Shield
		Everything::sendSmartString(getName() + F(" detected"));
	}

}
