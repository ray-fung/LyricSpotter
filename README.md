# LyricSpotter
An Android application for searching the lyrics of a song!

Connecting through Spotify allows the application to search for the song that is currently being played through the application. However, the app can also be used without connecting Spotify.

There's also an option to open up an overlay to the application, which when pressed automatically opens up the app for use.

This app was a project for fun and to practice my skills with Android development and working with APIs. I'll hopefully be continuing to update this project throughout the future, so keep an eye out for updates!

Additionally in the future I might upload this to the Google Play Store!

If there's any questions or issues, please feel free to create an issue or send me an email: rayf1013@cs.washington.edu.

# Developer Notes
If you wish to use this code, you must first create your own Spotify Developer Account and create a project through their portal. You'll be given a Client ID key which you should use and replace with the one in `SplashActivity.java`. Additionally, the redirect URI must be **exactly** the same as the one entered in the Spotify Developer Dashboard.

Currently, the Spotify Android SDK does not support token refreshing which means that the only way is the either use the Web API or to request another authentication token. My app does the latter when the original token expires.

This app uses both the Spotify Android SDK and JSoup in order to determine what lyrics to search. I extract the song information using the Spotify API and then I use JSoup to parse the HTML of a Genius.com page for lyrics.

Note that all the pages of Genius.com are conveniently written in this format:

>  http://genius.com/artist-name-song-name-lyrics

or an example

> http://genius.com/taylor-swift-love-story-lyrics

The app was built and supports up to Android Pie (API 28), the minimum specs it was tested on is Android Nougat (API 24).

# Difficulties and How I Overcame Them
These are just a list of issues that I ran into and how I fixed them

Problem: Couldn't connect to Spotify on actual device with INVALID_APP_ID but works properly on Android Emulator

Solution: You need to go to the Spotify dashboard and edit the settings to include your Android Package Name and SHA1 Fingerprint. In order to find that, you must go to the root of the project in the console and type
`./gradlew signingReport`
Then find the debug variant. Copy it and paste it into the Spotify settings and it should work.

---

Problem: It's impossible to add anything to the ActionBar of default activities, as they're not accessible

Solution: I created my own menu type within my resources. You need to create a `menu` folder and then within it I have a `mymenu.xml` file you can look at.

---

Problem: My overlay fails with an Permission denied for window type error

Solution: I learned that starting with Android SDK Oreo you can no longer use `WindowManager.LayoutParams.TYPE_PHONE`, instead replace it with `TYPE_APPLICATION_OVERLAY`. Otherwise you should check you have
```
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
```
within your `AndroidManifest.xml`.


