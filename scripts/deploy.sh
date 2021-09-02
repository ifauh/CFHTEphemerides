#!/bin/bash

VERSION=`jq -r .neo.project.version neo/project.json`

rm -rf tmp && mkdir -p tmp

echo $VERSION > VERSION
git log > tmp/CHANGELOG
echo "" > tmp/README
echo "java -jar CFHTEphemerides-$VERSION.jar -h" >> tmp/README
echo "" >> tmp/README
java -jar build/libs/CFHTEphemerides-$VERSION.jar -h >> tmp/README
echo "" >> tmp/README

if [ "$1" != "-doIt" ]; then
  echo "Check tmp/CHANGELOG and tmp/README"
  echo "Then execute $0 -doIt"
  exit 0
fi

scp VERSION mops@nmops15:/var/www/html/users/cfht/VERSION
scp tmp/CHANGELOG mops@nmops15:/var/www/html/users/cfht/CHANGELOG
scp tmp/README mops@nmops15:/var/www/html/users/cfht/README
scp build/libs/CFHTEphemerides-$VERSION.jar mops@nmops15:/var/www/html/users/cfht/
scp build/libs/CFHTEphemerides-$VERSION.jar mops@nmops15:/var/www/html/users/cfht/CFHTEphemerides-latest.jar

