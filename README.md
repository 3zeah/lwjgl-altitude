# lwjgl-altitude

Compatibility layer between LWJGL2 (legacy) and LWJGL3, tuned for the video game [Altitude](https://altitudegame.com/) (also legacy, technologically). In particular, at least on modern drivers, the display management of vanilla Altitude is horrible. Hopefully GLFW (used by LWJGL3) is better...

## Issues

* Nobody except me can build this
* Nobody except me can install this
* Window icon is not initially set in taskbar when running Altitude with console, but is displayed in title bar, and works if set after creation (on my machine, Windows 11)
  * Is this related to https://github.com/glfw/glfw/issues/1163, for which we already have a workaround?

## Laziness limitations

No plans to support. But probably could be supported.

* Gamma slider does nothing
* Map editor not supported
