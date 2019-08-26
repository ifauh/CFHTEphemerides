#!/bin/bash

VERSION=`cat /home/schastel/workspace/CFHTEphemerides/build.gradle | grep ^version | sed 's/.*= //g' | sed 's/"//g' | sed 's/\\s\\s//g'`

echo $VERSION > VERSION
hg log > tmp/CHANGELOG
echo "" > tmp/README
echo "java -jar CFHTEphemerides-$VERSION.jar -h" >> tmp/README
echo "" >> tmp/README
java -jar build/libs/CFHTEphemerides-$VERSION.jar -h >> tmp/README
echo "" >> tmp/README

scp VERSION mops@nmops15:/var/www/html/users/cfht/VERSION
scp tmp/CHANGELOG mops@nmops15:/var/www/html/users/cfht/CHANGELOG
scp tmp/README mops@nmops15:/var/www/html/users/cfht/README
scp build/libs/CFHTEphemerides-$VERSION.jar mops@nmops15:/var/www/html/users/cfht/
scp build/libs/CFHTEphemerides-$VERSION.jar mops@nmops15:/var/www/html/users/cfht/CFHTEphemerides-last.jar

