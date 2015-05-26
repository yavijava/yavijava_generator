[![Build Status](https://travis-ci.org/yavijava/yavijava_generator.svg?branch=master)](https://travis-ci.org/yavijava/yavijava_generator)
[![Join the chat at https://gitter.im/yavijava/yavijava](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/yavijava/yavijava?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# yavijava_generator

A code generator written in Groovy using [jsoup](http://jsoup.org/) to 
parse the HTML documentation provided by VMWare that is used to generate 
code for [yavjava](http://yavijava.com).

## Why
Much of the code in yavijava is a wrapper around a pretty well documented 
SOAP API. I got sick of making the DataObject classes because it was a major 
exercise in copy and paste with getters and setters. Very boring!

## How
Using jsoup we parse the HTML file to find the embedded WSDL snippet for 
a given DataObject. Next using the XMLSlurper in Groovy we parse the WSDL 
and generate a Java class with the info found.

## Tests
Tests can be found in the src/test package. If you submit a pull request 
your pull request should have a test where applicable or the pull request 
will not be merged. Please make sure to run the tests before you submit a 
pull request to make sure your change did not break the tests.

To run the tests execute:

    ./gradlew test

## Current Status
The application is currently Beta use with caution. It has been tested with vSphere 6.0

## Usage
    ./gradelw fatJar
    java -jar build/libs/yavijava_generator-1.0.jar --dest /Users/errr/temp/ --source /Users/errr/programs/java/yavijava.github.io/docs/new-do-types-landing.html --type dataobj --all

This would build a jar containing all deps needed to run the app.

    --dest is the output directory where generated code will be placed
    --source is the path to the dataobjects file
    --type is the type of file to generate. Valid values are one of dataobj, fault, enum
    --all sets a flag to generate all data objects found on the source html page. That means new and existing with new properties

## License
This application is released under the terms of the Apache 2.0 license. 
A license file is included.

## Build System
This application uses Gradle for the build system. You do not need to 
install Gradle to use it. All you need is a JDK. I include the Gradle 
Wrapper so everything is included by downloading the repo. To build 
the source code on Linux or Mac OSX:

    ./gradlew fatJar

On Windows please use the ```gradlew.bat``` file.

## Bugs
Report them on the GitHub issue tracker.
