The Pushjet Android Client [![Circle CI](https://circleci.com/gh/Pushjet/Pushjet-Android.svg?style=svg)](https://circleci.com/gh/Pushjet/Pushjet-Android)
==========================
This is the pushjet android client. It currently uses google GCM to send and receive messages. This means 
that any message that is directed at the android client *will* go through google. The client is licensed 
under [BSD 2 clause][1] just like the rest of the project.

## Permissions explained
The [permissions][4] used by Pushjet might seem a bit broad but they all have a reason:

 - Read phone status and identity:
  - This is needed [to generate the device uuid ][5] that authenticates the device with the server.
 - Take pictures and videos:
  - This is needed to make sure we can scan QR codes to register new services.
 - Control flashlight/vibration and prevent phone from sleeping:
  - This makes sure we can receive notifications.

## Screenshots
![Pushjet push listing material][6] ![Pushjet push listing][2] ![Subscriptions][3]


[1]: https://tldrlegal.com/license/bsd-2-clause-license-%28freebsd%29
[2]: http://pushjet.io/images/android/screenshot_1.png
[3]: http://pushjet.io/images/android/screenshot_2.png
[6]: http://pushjet.io/images/android/screenshot_3.png
[4]: /app/src/main/AndroidManifest.xml
[5]: https://github.com/Pushjet/Pushjet-Android/blob/master/app/src/main/java/io/Pushjet/api/PushjetApi/DeviceUuidFactory.java
