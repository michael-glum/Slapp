# Slapp!
An android app which catches users mindlessly scrolling on social media and sends them a notification (or slap) that playfully reminds them to find a better use for their time.

Slapp accomplishes this by creating a periodic background worker which persists after the app is killed and tracks which app is in use in the foreground at all times. If the foreground app has been selected within the settings page by the user, it is considered restriced. (The settings page contains an automatically populated list of potential "problem apps"--usually social media--which are installed on the user's device.) After a waiting period, the background worker queues a notification containing a comical message reminding the user to spend less time scrolling.
