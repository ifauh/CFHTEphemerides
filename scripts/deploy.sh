#!/bin/bash

VERSION=`cat /home/schastel/workspace/CFHTEphemerides/build.gradle | grep ^version | sed 's/.*= //g' | sed 's/"//g' | sed 's/\\s\\s//g'`

echo $VERSION > VERSION
scp VERSION mops@nmops15:/var/www/html/users/cfht/VERSION
scp build/libs/CFHTEphemerides-$VERSION.jar mops@nmops15:/var/www/html/users/cfht/
scp build/libs/CFHTEphemerides-$VERSION.jar mops@nmops15:/var/www/html/users/cfht/CFHTEphemerides-last.jar
