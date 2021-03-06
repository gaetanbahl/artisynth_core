# Carapas README

**ORIGINAL ARTISYNTH README BELOW **

## Introduction

This is Carapas, an Orthopedic Surgery Simulator based on Artisynth.

## Installation

```
git clone http://github.com/gaetanbahl/carapas 
cd carapas
source setup.bash
./bin/updateArtisynthLibs
make
``` 

## Usage

To launch Carapas:

`./carapas`

To launch the muscle mesh generator:

`./musclefitting`


# Original Artisynth README

This is the general distribution of ArtiSynth.

For installation instructions, go to www.artisynth.org, and navigate
to Documentation -> Installation

Other files in this directory:

.artisynthInit
    Default ArtiSynth initialization file

.demoModels 
    List of "banner" demo models available when ArtiSynth starts up

.mainModels 
    List of "banner" anatomical models in the ArtiSynth Models package
    (which must be installed).

LICENSE
    Licensing and terms of use

Makefile
    Makefile for compiling and doing certain maintenance operations in
    a shell environment.

Makefile.base
    Base definitions for Makefile and Makefile in subdirectories.

VERSION
    Current version

bin
    Stand-alone programs, mostly implemented as scripts. 
    The program 'artisynth' starts up the ArtiSynth system.

classes
    Root directory for compiled classes

demoMenu.xml
    Default format for the Models menu

doc
    System documenation

eclipseSettings.zip
    Default project settings for the Eclipse IDE

lib
    Java libraries, plus architecture-specific libraries for native code
    support, mostly involving linear solvers, Java OpenGL (JOGL), and
    collision detection

matlab
    Matlab scripts for running ArtiSynth from matlab

scripts
    Jython scripts for basic testing, etc. Some of these may assume
    the installation of additional projects.

setup.bash
    Example ArtiSynth setup script for bash

setup.csh
    Example ArtiSynth setup script for chs/tcsh

src
    ArtiSynth source code

support
    Configuration information for external IDEs and support software,
    including default settings for the Eclipse IDE.

tmp
    Temp directory for operations like storing files when making
    movies.

