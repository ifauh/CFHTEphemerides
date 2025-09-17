# CFHTEphemerides

Build ephemerides for the CFHT by pulling objects from the MPC and/or JPL.

<code>java -jar CFHTEphemerides-latest.jar help</code> for its documentation

The jar can be downloaded from https://neo.ifa.hawaii.edu/users/cfht/CFHTEphemerides-latest.jar. Have no worries if it gets updated, you'll get a message looking like:

```
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
!
! You are not using the latest version (i.e 'current value') of this software (you're using 'other value')
!
! You can:
! - Either: Download the latest version at https://neo.ifa.hawaii.edu/users/cfht/CFHTEphemerides-latest.jar
! - Or: Append the -bypassVersionCheck option to the command line if you cannot or don't want to update it 
!    (Be aware that there is no guarantee that the software will work properly though)
!
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
```

test write with mopspipe@nmops02

## SSL Certificate Issue

Previously, folk have had issues with using the scout API. Java decides to freak-out at the certifcates and you get a message like:

	sun.security.provider.certpath.SunCertPathBuilderException: unable to
	find valid certification path to requested target

Here's a work around.

First, get the certificate from the api page: https://ssd-api.jpl.nasa.gov/scout.api
It became active on Feb 5th 2025, which is around when these issues started.

Add the certificate to your java version using keytool. Likely, you have $JAVA_HOME set and it should look something like this:
	keytool -importcert -file ~/Downloads/ssd-api-jpl-nasa-gov.pem -keystore "$JAVA_HOME"/lib/security/cacerts -cacerts

The default password to update the keystore is 'changeit' if you have not used it before.
