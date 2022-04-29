//******************************************************************************************
//  File: IS_Sound.h
//  Authors: Scott Miller (modifier) Dan G Ogorchock & Daniel J Ogorchock (Father and Son, creators)
//
//  Summary:  IS_Sound is a class which implements the SmartThings "Sound Detector" device capability.
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
//    2022-04-20  Scott Miller   Created and modified for Sound Sensor
//
//
//******************************************************************************************

#ifndef ST_IS_SOUND_H
#define ST_IS_SOUND_H

#include "InterruptSensor.h"

namespace st
{
	class IS_Sound: public InterruptSensor
	{
		private:
			//inherits everything necessary from parent InterruptSensor Class
			
		public:
			//constructor - called in your sketch's global variable declaration section
			IS_Sound(const __FlashStringHelper *name, byte pin, bool iState, bool internalPullup = false, long numReqCounts = 0); //(defaults to NOT using internal pullup resistors, and required counts = 0)
			
			//destructor
			virtual ~IS_Sound();
			
			//initialization function
			virtual void init();

			//called periodically by Everything class to ensure ST Cloud is kept consistent with the state of the sensor
			virtual void refresh();

			//handles what to do when interrupt is triggered 
			virtual void runInterrupt();

			//handles what to do when interrupt is ended 
			virtual void runInterruptEnded();
	
	};
}


#endif
