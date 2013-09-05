PowerMonitor
============

# Introduction
This is an Android app to display data from Visible Energy smart outlets. VisibleEnergy has an iOS app, and I wanted similar functions on Android. Their
[web portal](http://service.visiblenergy.com/login) has never been reliable or functional for me, so let's DIY.

Free and open source, Apache 2 license.

## Current Status

Project started 9/3/2013, app can poll wattage data from the device and display current/max/difference plus a progress bar indicator. Hardwired IP, not ready to
test elsewhere.

## Hardware

I've got and am testing with the [Monostrip](http://www.visiblenergy.com/products/monostrip.html) ($49, a good deal IMHO) but the
[UFO Power Center](http://www.visiblenergy.com/products/ufo.html) should work too.

## API

The HTTP API [is well documented here](http://portal.visiblenergy.com/page/articles.html/_/developers/local-http-api/). JSON, HTTP, REST, pretty simple.

## Plan

Add preferences, [2D charts](https://github.com/limccn/Android-Charts), ZeroConf discovery, publish to Google Play.