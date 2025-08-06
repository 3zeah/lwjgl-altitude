# lwjgl-altitude

Compatibility layer between LWJGL2 (legacy) and LWJGL3, tuned for the video game [Altitude](https://altitudegame.com/) (also legacy, technologically). In particular, at least on modern drivers, the display management of vanilla Altitude is horrible. Hopefully GLFW (used by LWJGL3) is better...

## Features

* Functional and accessible windowed fullscreen
* Alt-tabbing without second-long blackouts
* Toggling fullscreen without second-long blackouts
* Clicking parts of the screen do not randomly minimize the game
* Crosshair constrained to window while mouse-aiming
* Using any monitor, instead of just the primary one
* Misc LWJGL2 bugs fixed
  - Fixes: On Windows, mouse button 4 cannot be released while any other button is held

## Quickstart

1. Locate the _app_ directory somewhere in the game directory. If _app_ does not exist, your build is old, and incompatible with this project. (At the time of writing, the current version of Altitude was 1.1.6)
   * Windows: _Altitude/app/_
   * macOS: _Altitude.app/Contents/app/_
   * Linux: _altitude/lib/app/_
2. Download _lwjgl-altitude-\<version\>.zip_ from the latest release, or build it yourself according to the [Build section](#build)
3. Extract the contents of _lwjgl-altitude-\<version\>.zip_ to the _app_ subdirectory (BEWARE: NOT to the root directory: use the directory found in step 1)
4. Modify _Altitude.cfg_ in the _app_ directory
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

Undo the modification of _app/Altitude.cfg_ from step 4.

## Configuration

After installation, _app/lib/lwjgl-altitude_ will contain the file _lwjgl-altitude.properties_. This file is the total configuration of the installation.

| Property                     | Allowed values     | Effect                                                                                                                                                                                                                                                                                             |
|------------------------------|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `prefer_windowed_fullscreen` | `true` or `false`  | When true, the game is in windowed fullscreen whenever it is in fullscreen and the resolution matches the monitor. Normally, windowed fullscreen must be activated with a console command (`/testWindowedFullscreen true`).                                                                        |
| `monitor`                    | integer (optional) | When set, the game uses the provided monitor instead of the primary monitor. Monitor 0 is the primary monitor, and 1 the secondary, and so on. You might have to experiment to find the right one. Altitude will use the provided monitor for running fullscreen or to list available resolutions. |

## Troubleshoot

### Game does not start
If the game does not start at all, even after editing _Altitude.cfg_, ensure correct installation. If you extracted properly, the _app/lib_ directory should contain _lwjgl-altitude.jar_ and the _lwjgl-3_ and _lwjgl-altitude_ subdirectories. The _app/lib_ directory should ALSO contain a bunch of other Altitude jar files, that were there before, such as _lwjgl-2.9.3.jar_. The _app/native_ directory should contain several os-specific subdirectories, such as _windows_ and _linux_, ALONG with a bunch of other Altitude dll files, that were there before, such as _lwjgl.dll_.

### Mod does nothing

If you run the game along with the console, ie via _altitude-console.exe_, you need to edit _altitude-console.cfg_ in step 4 of [Quickstart](#quickstart), rather than _Altitude.cfg_.

## Build

`mvn package` packages a release installation, _lwjgl-altitude-\<version\>.zip_, and a naked release jar, _lwjgl-altitude.jar_, to _core/target_. The installation bundles everything required to install the mod, whereas the jar suffices when only upgrading first-party code.

## Issues

* Might not work at all on macOS... To even try, `java-options=-XstartOnFirstThread` must be added under `[JavaOptions]` in _Altitude.cfg_. I do not have access to macOS to test anything, though...
  * I think the main problem is Altitude initializing AWT, which hogs the main window or AppKit thread or whatever, but GLFW also requires hogging that thread (hence `-XstartOnFirstThread`), and there is a deadlock. GLFW not playing nice with AWT and macOS is actually relatively well documented. Some seemingly related info at https://github.com/LWJGL/lwjgl3/issues/306, which suggests running with `-Djava.awt.headless=true` as a workaround for a similar issue.
* Window icon is not initially set in taskbar when running Altitude with console, but is displayed in title bar, and works if set after creation (on my machine, Windows 11)
  * Is this related to https://github.com/glfw/glfw/issues/1163, for which we already have a workaround?

## Laziness limitations

No plans to support. But probably could be supported.

* Map editor not supported (requires implementing AWT rendering for LWJGL3)

## Details

The primary artifact, _lwjgl-altitude.jar_, is a compatibility layer between LWJGL2 and LWJGL3. It both introduces adapter classes for API:s removed in LWJGL3, and extends extant LWJGL3 classes that had breaking changes from LWJGL2. In either case, only API:s used by Altitude are adapted.

For convenience, releases bundle the project artifact along with the required LWJGL3 jars and LWJGL3 natives, to produce a drop-in install. Thus, to enable this project, only _lwjgl-altitude.jar_ needs to be linked to Altitude, replacing its link to LWJGL2. Because the application classpath of Altitude is in plain text, this is trivial to accomplish, and just as easily reverted. Presently, in 1.1.6, Altitude stores the classpath of each executable _\<name\>.exe_ in _app/\<name\>.cfg_. In particular, the classpath of the regular game is defined in _app/Altitude.cfg_.

### Development

* If property `altitudeClient.appDir` is set, maven phase `pre-integration-test` will extract the packaged release installation to that directory, simulating a fresh installation
* If property `altitudeClient.exeDir` is set, maven phase `integration-test` will run that executable

For development, then, eg, use `mvn verify -DaltitudeClient.appDir=C:/Games/Altitude_Test/app -DaltitudeClient.exePath=D:/Games/Altitude_Test/Altitude.exe` along with a sandbox copy of the Altitude client to test changes quickly. Maven even mirrors the Altitude log.

### Other dependencies

These require no action to install, but are noted for posterity. The following are dependencies on which the project relies, both to run and to compile, but are not redistributed here, because they are found within Altitude itself.

* jinput: The controller support of this project is ripped and retained straight from LWJGL2. This support wraps jinput
* slf4j/log4j: To simplify debugging, this project logs to the regular Altitude log. Accordingly, this project relies on log4j and its slf4j bindings, which Altitude uses for logging
