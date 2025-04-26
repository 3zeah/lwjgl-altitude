# lwjgl-altitude

Compatibility layer between LWJGL2 (legacy) and LWJGL3, tuned for the video game [Altitude](https://altitudegame.com/) (also legacy, technologically). In particular, at least on modern drivers, the display management of vanilla Altitude is horrible. Hopefully GLFW (used by LWJGL3) is better...

## Quickstart

1. Ensure compatible version of Altitude, by verifying that the game directory contains the _app_ subdirectory. At the time of writing, the current version of Altitude was 1.1.6
2. Download _lwjgl-altitude-\<version\>.zip_ from the latest release, or build it yourself according to the [Build section](#build)
3. Extract the contents of the distribution artifact (_lwjgl-altitude-\<version\>.zip_) to _\<altitude game path\>/app_
4. Modify _\<altitude game path\>/app/Altitude.cfg_
   * Replace the following lines
     * `app.classpath=$APPDIR\lib\lwjgl-2.9.3.jar`
     * `app.classpath=$APPDIR\lib\lwjgl-platform-2.9.3-natives-linux.jar`
     * `app.classpath=$APPDIR\lib\lwjgl-platform-2.9.3-natives-osx.jar`
     * `app.classpath=$APPDIR\lib\lwjgl-platform-2.9.3-natives-windows.jar`
     * `app.classpath=$APPDIR\lib\lwjgl_util-2.9.3.jar`
   * with
     * `app.classpath=$APPDIR\lib\lwjgl-altitude.jar`
5. Run Altitude

### Disable

Undo the modification of _\<altitude game path\>/app/Altitude.cfg_ in step 4.

## Configuration

After installation, _\<altitude game path\>/app/lib/lwjgl-altitude_ will contain the file _lwjgl-altitude.properties_. This file is the total configuration of the installation.

| Property                     | Allowed values    | Effect                                                                                                                                                                                                                               |
|------------------------------|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `prefer_windowed_fullscreen` | `true` or `false` | When true, exclusive fullscreen and windowed fullscreen are swapped. Normally, windowed fullscreen has to be toggled with a console command (`testWindowedFullscreen`), so this makes windowed fullscreen more convenient to access. |

## Build

`mvn package` packages a release, _lwjgl-altitude-\<version\>.zip_, to _/core/target_

## Issues

* Window icon is not initially set in taskbar when running Altitude with console, but is displayed in title bar, and works if set after creation (on my machine, Windows 11)
  * Is this related to https://github.com/glfw/glfw/issues/1163, for which we already have a workaround?

## Laziness limitations

No plans to support. But probably could be supported.

* Gamma slider does nothing (requires understanding gamma and how safely to change it app-specifically)
* Map editor not supported (requires implementing AWT rendering for LWJGL3)

## Details

The primary artifact, _lwjgl-altitude.jar_, is a compatibility layer between LWJGL2 and LWJGL3. It both introduces adapter classes for API:s removed in LWJGL3, and extends extant LWJGL3 classes that had breaking changes from LWJGL2. In either case, only API:s used by Altitude are adapted.

For convenience, releases bundle the project artifact along with the required LWJGL3 jars and LWJGL3 natives, to produce a drop-in install. Thus, to enable this project, only _lwjgl-altitude.jar_ needs to be linked to Altitude, replacing its link to LWJGL2. Because the application classpath of Altitude is in plain text, this is trivial to accomplish, and just as easily reverted. Presently, in 1.1.6, Altitude stores the classpath of each executable _\<name\>.exe_ in _/app/\<name\>.cfg_. In particular, the classpath of the regular game is defined in _/app/Altitude.cfg_.

### Other dependencies

These require no action to install, but are noted for posterity. The following are dependencies on which the project relies, both to run and to compile, but are not redistributed here, because they are found within Altitude itself.

* jinput: The controller support of this project is ripped and retained straight from LWJGL2. This support wraps jinput
* slf4j/log4j: To simplify debugging, this project logs to the regular Altitude log. Accordingly, this project relies on log4j and its slf4j bindings, which Altitude uses for logging
