# cameralowrez
 Low resolution camera app for Android

This is intended as a PoC. Use at your own risk.

It should be modified to use the latest Android Camera API's and remove deprecated methods.


Intent and extras:

--es width 800
--es height 800                this value will be used if ascpetratio is set to false
--es quality 0-100             JPEG quality format
--es rotate true|false         rotate image
--es aspectratio true|false    keep aspect ratio based on width value

Sample:
adb shell am start -n net.znordic.cameralowrez/.MainActivity -a net.znordic.cameralowrez.REMOTE_CONFIG --es width 800 --es height 400 --es rotate true --es aspectratio false
