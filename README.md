# Tiny s3 client library for java: pico-s3
* ultra minimalistic client library for accessing aws s3 service
* goal is zero addition dependencies to Java SE itself
* uses Java Standard Edition's http client, xml parser and cryptography support
* will do my best to keep this tiny, right now the jar file is less than 30kb, i will try to keep it under 100kb feature complete

## This is still work in progress, right now in beta quality levels, use with caution and lots of testing.
* will let you know once it's properly running and good enough for stable use (you're welcome to read the source
and check it out, parts of it already work).

* stuff that should work right now
  * authentication against amazon's systems
  * s3 list (including listing huge folders with concatenation)
  * s3 get object
  * s3 put object
  * unicode filenames
  * unicode file contents

## Goals
* to support no-auth, auth with credentials, auth from instance profile
* to support list, get and put commands
* anything else will be optional (but not off limits)

## Usage example
Listing two example cases below with different auth types:
```java
import com.github.kulminaator.s3.Client;
import com.github.kulminaator.s3.PicoClient;
import com.github.kulminaator.s3.S3Object;
import com.github.kulminaator.s3.auth.EnvironmentCredentialsProvider;
import com.github.kulminaator.s3.auth.SimpleCredentialsProvider;

import java.util.List;

class Foo {
    /* List objects by using aws credentials from environment parameters */
    public List<S3Object> listObjectsWithEnvCredentials(String bucketName) {
        final Client pClient = new PicoClient.Builder()
                .withRegion("eu-west-1")
                .withCredentialsProvider(new EnvironmentCredentialsProvider())
                .build();
        return pClient.listObjects(bucketName);
    }

    /* Get an object data as just a string by using aws credentials from method parameters */
    public String getObject(String bucketName, String objectName, String accessKey, String secretKey) {
        SimpleCredentialsProvider simpleCredentialsProvider = new SimpleCredentialsProvider();
        simpleCredentialsProvider.setAccessKeyId(accessKey);
        simpleCredentialsProvider.setSecretAccessKey(secretKey);
        final Client pClient = new PicoClient.Builder()
                .withRegion("eu-west-1")
                .withCredentialsProvider(simpleCredentialsProvider)
                .build();
        return pClient.getObjectDataAsString(bucketName, objectName);
    }
}
```

## Including into your project
After adding jcenter / bintray to your repositories list you can just include it like this in gradle
```gradle
compile 'com.github.kulminaator:pico-s3:1.1'
```
Or like this in maven
```xml
<dependency>
  <groupId>com.github.kulminaator</groupId>
  <artifactId>pico-s3</artifactId>
  <version>1.1</version>
  <type>pom</type>
</dependency>
```
Check for the latest version at https://bintray.com/kulminaator/kulminaator/pico-s3

## Motivation
My main motivation to create this is the absurd size of amazon's s3 library if you include it's dependencies. If you 
have a vanilla project and you pull it in you're punished with 4-5 megabytes of dependencies. If your project is an aws 
lambda then the lambda is **unusable** due to the loading time of aws's original s3 sdk (in case the first thing you 
do is load config from s3 or alike, even the largest lambdas will have a cold start time counted in **seconds**, 
whereas your vanilla lambda was starting in milliseconds before you added the aws s3 sdk on top of it). So that is that. 

I understand that aws's sdk does not shine out in a java behemoth application, but on a clean project it's just too big 
and loads too long.

## Comparison with aws s3 sdk
Creating a simple lambda with environment credentials and measuring the cold starts (first executions) of the lambdas:

### Shaded jar file sizes ###

| Comparison           | Pico S3 (1.0-SNAPSHOT-RC2) | Aws s3 sdk (1.11.433)    |
| ---------------------|----------------------------|--------------------------|
|Shaded Lambda jar size|             36.19 kilobytes|         6279.73 kilobytes|
				
### Test, cold lambda read 1 hello world file from s3, milliseconds	###

|Lambda Size in MB |Time1 Pico|Time2 Pico|Time1 AWS S3 SDK|Time2 AWS S3 SDK|
|------------------|--------|--------|--------|--------|
|128|11639|11218|OutOfMemoryError|OutOfMemoryError|
|192|8710|8317|18860|18441|
|256|7410|7040|16879|16501|
|512|3734|3397|8538|8201|
|1024|2560|2098|4262|3924|
|1536|1555|1201|3156|2826|
|3008|1019|766|2538|2196|

* *Time 1 - the time from loading the lambda class until the response read from s3. True coldboot.*
* *Time 2 - the time spent in the s3 client build and reading block itself.*

The sourcefiles for these tests are found compressed under the "misc" folder.

_Legal stuff: all the trademarks mentioned in the text above belong to their according owners._
