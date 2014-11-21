Muicraft Launcher
================

This project provides an open-source Minecraft launcher platform for downloading,
installing, and updating modpacks.

Introduction
------------

This launcher is maintained by Heracles421, and is a forge from sk89q's project, who writes WorldEdit, WorldGuard, and so on.

It has been primarily developed for Muicraft server (http://www.muicraft.net), but you can use it for your own modpack or
server.

~ One of Minecraft's oldest launchers -- since Minecraft Alpha
~ Requires almost no configuration files to make a modpack
~ Add a new mod by dropping in the .jar (and its configuration)
~ Remove a mod by deleting its .jar (and configuration).
~ Builds **server** modpacks with no extra configuration
~ Advanced download system: incremental, file removal detection, optional feature/mod selection, etc.
~ Very easy for users to use and install modpacks
~ Pretty well-documented with easy-to-understand, well-organized code*
~ Open source!

*Except for the Launcher frame class. That one is pretty bad.

### Previous Versions

This repository only contains code for the launcher versions 4.x and newer.

You can find [the 3.x version on GitHub](https://github.com/sk89q/skmclauncher).

Documentation
-------------

First off, be aware that the launcher in this directory has been branded for Muicraft server, so you will have to replace that with your own. There's only a few places that you need to do that, and it's all documented on sk89q's documentation page.

**You can fork the project on GitHub** and make modifications.

* [Documentation](http://wiki.sk89q.com/wiki/Launcher)

Note that documentation may be lacking in some places. If you run into problems,
**do not hesitate to ask**.

Compiling
---------

The launcher can be compiled using [Maven](http://maven.apache.org/).

    mvn clean package

If you wish to import the project into an IDE, you must add support for
Project Lombok.

Contributing
------------

Pull requests can be submitted on GitHub, but we will accept them
at our discretion. Please note that your code must follow
Oracle's Java Code Conventions.

License
-------

The launcher is licensed under the GNU General Public License, version 3.
