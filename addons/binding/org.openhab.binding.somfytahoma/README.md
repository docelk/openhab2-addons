# Somfy Tahoma Binding
Somfy Tahoma binding for OpenHAB v2.x

It should be working also for Connexoom device since it should be using the same API
## Supported Things

Currently supports these things
- bridge (Somfy Tahoma bridge, which can discover roller shutters and action)
- roller shutters (UP, DOWN, STOP control of a roller shutter)
- actiongroups (can execute predefined Tahoma action - groups of steps, e.g. send to all roller shutters down command, one by one)

Currently only Somfy Tahoma device has been tested.

## Discovery

First install this binding by copying .jar file to addon directory of OpenHAB 2.x installation.
To start a discovery, just 
- open Paper UI
- add a new thing in menu Configuration/Things
- choose SomfyTahoma Binding and select Somfy Tahoma Bridge
- enter your email (login) and password to the TahomaLink cloud portal
 
If the supplied TahomaLink credentials are correct the automatic discovery starts immediately and detected roller shutters and action groups appear in Paper UI inbox. 

## Thing Configuration

To manually configure the thing you have to specify bridge and things in *.things file in conf/addons directory of your OpenHAB 2.x installation.
To manually link the thing channels to items just use the *.items file in conf/items directory of your OpenHAB 2.x installation. 
To retrieve thing configuration and url parameter, just add the automatically discovered device from you inbox and copy its values from thing edit page. (the url parameter is only at edit page visible)
Please see the example below.

## Channels

A bridge exposes this channel:
- version (this is a firmware version of your Tahoma device)

A roller shutter thing exposes these channels:
- position (a percentual position of the roller shutter, it can have value 0-100)
- control (a rollershutter controller which reacts to commands UP/DOWN/STOP)

When STOP command received two possible behaviours are possible
- when the roller shutter is idle then MY command is interpreted (the roller shutter goes to your favourite position)
- when the roller shutter is moving then STOP command is interpreted (the roller shutter stops)

An action group thing has this channel:
- starter (a switch which reacts to ON command and executes the predefined Tahoma action)

## Full Example

.things file
```
Bridge somfytahoma:bridge:237dbae7 "Somfy Tahoma Bridge" [ email="my@email.com", password="MyPassword", refresh=30 ] {
    Thing somfytahoma:rollershutter:31da8dac-8e09-455a-bc7a-6ed70f740001 "Bedroom" [ url="io://0204-1234-8041/6825356" ]
    Thing somfytahoma:rollershutter:87bf0403-a45d-4037-b874-28f4ece30004 "Living room" [ url="io://0204-1234-8041/3832644" ]
    Thing somfytahoma:rollershutter:68bee082-63ab-421d-9830-3ea561601234 "Hall" [ url="io://0204-1234-8041/4873641" ]
    Thing somfytahoma:actiongroup:2104c46f-478d-6543-956a-10bd93b5dc54 "1st floor up" [ url="2104c46f-478d-6543-956a-10bd93b5dc54" ]
    Thing somfytahoma:actiongroup:0b5f195a-5223-5432-b1af-f5fa1d59074f "1st floor down" [ url="0b5f195a-5223-5432-b1af-f5fa1d59074f" ]
    Thing somfytahoma:actiongroup:712c0019-b422-1234-b4da-208e249c571b "2nd floor up" [ url="712c0019-b422-1234-b4da-208e249c571b" ]
    Thing somfytahoma:actiongroup:e201637b-de3b-1234-b7af-5693811a953b "2nd floor down" [ url="e201637b-de3b-1234-b7af-5693811a953b" ]
}
```
.items file
```
Rollershutter RollerShutterBedroom "Roller shutter [%d %%]"  {channel="somfytahoma:rollershutter:31da8dac-8e09-455a-bc7a-6ed70f740001:control"}
Dimmer RollerShutterBedroomD "Roller shutter dimmer [%.1f]"  {channel="somfytahoma:rollershutter:31da8dac-8e09-455a-bc7a-6ed70f740001:position"}
Rollershutter RollerShutterLiving "Roller shutter [%d %%]"  {channel="somfytahoma:rollershutter:87bf0403-a45d-4037-b874-28f4ece30004:control" }
Dimmer RollerShutterLivingD "Roller shutter dimmer [%.1f]"  {channel="somfytahoma:rollershutter:87bf0403-a45d-4037-b874-28f4ece30004:position"}
Rollershutter RollerShutterHall "Roller shutter [%d %%]"  {channel="somfytahoma:rollershutter:68bee082-63ab-421d-9830-3ea561601234:control"}
Dimmer RollerShutterHallD "Roller shutter dimmer [%.1f]"  {channel="somfytahoma:rollershutter:68bee082-63ab-421d-9830-3ea561601234:position"}

Switch Rollers1UP "Rollers 1st floor UP" {channel="somfytahoma:actiongroup:2104c46f-478d-6543-956a-10bd93b5dc54:starter", autoupdate="false"}
Switch Rollers1DOWN "Rollers 1st floor DOWN" {channel="somfytahoma:actiongroup:0b5f195a-5223-5432-b1af-f5fa1d59074f:starter", autoupdate="false"}
Switch Rollers2UP "Rollers 2nd floor UP" {channel="somfytahoma:actiongroup:712c0019-b422-1234-b4da-208e249c571b:starter", autoupdate="false"}
Switch Rollers2DOWN "Rollers 2nd floor DOWN" {channel="somfytahoma:actiongroup:e201637b-de3b-1234-b7af-5693811a953b:starter", autoupdate="false"}

```