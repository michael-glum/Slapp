# Slapp!
An android app that catches users mindlessly scrolling on social media and sends them a notification (or slap) that playfully reminds them to find a better use for their time.

Slapp accomplishes this by creating a periodic background worker which persists after the app is killed and tracks which app is in use in the foreground at all times (roughly). If the foreground app has been selected within the settings page by the user, it is considered restricted. (The settings page contains an automatically populated list of potential "problem apps"--usually social media--which are installed on the user's device.) After a waiting period, the background worker queues a notification containing a comical message reminding the user to spend less time scrolling.



https://github.com/michael-glum/Slapp/assets/61207272/f1c5a38b-60bb-490e-b7bc-4d6da869268b

