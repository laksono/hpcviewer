![GitHub issues](https://img.shields.io/github/issues/hpctoolkit/hpcviewer.e4?style=for-the-badge)
![Build](https://github.com/hpctoolkit/hpcviewer.e4/actions/workflows/maven.yml/badge.svg)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=HPCToolkit_hpcviewer.e4&metric=alert_status)](https://sonarcloud.io/dashboard?id=HPCToolkit_hpcviewer.e4)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=HPCToolkit_hpcviewer.e4&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=HPCToolkit_hpcviewer.e4)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=HPCToolkit_hpcviewer.e4&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=HPCToolkit_hpcviewer.e4)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=HPCToolkit_hpcviewer.e4&metric=security_rating)](https://sonarcloud.io/dashboard?id=HPCToolkit_hpcviewer.e4)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=HPCToolkit_hpcviewer.e4&metric=coverage)](https://sonarcloud.io/dashboard?id=HPCToolkit_hpcviewer.e4)


# hpcviewer.e4

hpcviewer is the presentation layer of HPCToolkit which is a suite of tools
for measurement and analysis of program performance.
It interactively presents program performance in a top-down fashion and also 
visualizes trace data generated by hpcrun if the flag ```-t``` is specified. 
For static linked program, the variable environment ```HPCRUN_TRACE``` has to be set. 

## General Requirements

* Java 11 or newer
  Can be downloaded via Spack 
  or from Oracle https://www.oracle.com/java/technologies/javase-downloads.html 
  or AdoptJDK https://adoptopenjdk.net
* Linux: GTK+ 3.20 or newer.
To check installed GTK version on Red Hat distributions:
```
rpm -q gtk3
```
On Debian-based distributions:
```
dpkg -l  libgtk-3-0
apt-cache policy libgtk-3-0
```
If there is no GTK+ 3.20 or newer installed in the system, you may install it via `spack`:
```
spack install gtkplus
spack load gtkplus
```

## How to build and run via command line (Maven)

* Download and install Maven (if not available on the systems) at https://maven.apache.org/
  * Recommended: install via spack
  	`spack install maven; spack load maven`  
  	
* On Posix-based platform with Bash shell (Linux and MacOS), type:
    ```
    ./build.sh
    ``` 
    The script generates five `hpcviewer-<release>-<platform>.[zip|tgz]` files:
    Windows, Mac (x86_64 and Arm), and Linux (x86_64, ppcle64, and Arm).
  * `untar` or `unzip` the file based according to the platform. 
  * For Linux platform: run 
  ```./install.sh <directory>``` 
  to install the viewer. 
  
* On Windows type:
   ```
   mvn clean package
   ```
  This will compile and create hpcviewer packages for 4 platforms: Linux x86_64 and ppcle64, Windows and Mac
  with Eclipse 4.19 (the default).
  Example of the output:
```
...
[INFO] --- tycho-p2-director-plugin:1.6.0:archive-products (archive-prodcuts) @ edu.rice.cs.hpcviewer ---
[INFO] Building tar: <hpcviewer.e4>/edu.rice.cs.hpcviewer.product/target/products/edu.rice.cs.hpcviewer-linux.gtk.x86_64.tar.gz
[INFO] Building tar: <hpcviewer.e4>/edu.rice.cs.hpcviewer.product/target/products/edu.rice.cs.hpcviewer-linux.gtk.ppc64le.tar.gz
[INFO] Building zip: <hpcviewer.e4>/edu.rice.cs.hpcviewer.product/target/products/edu.rice.cs.hpcviewer-win32.win32.x86_64.zip
[INFO] Building zip: <hpcviewer.e4>/edu.rice.cs.hpcviewer.product/target/products/edu.rice.cs.hpcviewer-macosx.cocoa.x86_64.zip
[INFO] ------------------------------------------------------------------------
```
  * Unzip `edu.rice.cs.hpcviewer-win32.win32.x86_64.zip` to another folder. 
    It isn't recommended to overwrite the existing folder.

## How to build and run via Eclipse IDE

Requirements:

* Recommended: [Eclipse 2021.03](https://www.eclipse.org/downloads/packages/release/2021-03/r/eclipse-ide-rcp-and-rap-developers) or newer. 
* Warning: May not work properly with older versions of Eclipse. 

### Getting the source code into the Eclipse IDE

* Start Eclipse
* Open the Import window via the menu File > Import
* Select the import wizard General > Existing Projects into Workspace and click Next >
* In Select root directory, select the directory where you have downloaded (the Git root)
* In the Options section of the window, activate Search for nested projects
* Click Finish

### Activating the target plarform

To run hpcviewer, it requires Eclipse bundles and some external libraries such as Nebula NatTable, Eclipse Collections, JCraft and SLF4J.
The set of bundles that are available is defined by the bundles in the Eclipse workspace, and additionally the bundles in the active target platform
The first Eclipse starts after the installation, the target platform only includes the bundles that are installed in the workspace which doesn't include the external libraries.

The bundles that hpcviewer needs are defined in a custom target platform definition project, which is located in the `target.platform` directory:

* Open file `target-platform.target` in `target.platform project`.
* Click the "Set as Active Target Platform" link at the top-right panel.

### Run the app

* Open product configuration `hpcviewer.product` at `edu.rice.cs.hpcviewer.product`
* To run: Click `Launch an Eclipse application`


## Coding style

Recommended coding and formatting style:
* [Sonar source quality standards](https://www.sonarsource.com/java/)
* [Google Java style guide](https://google.github.io/styleguide/javaguide.html)
