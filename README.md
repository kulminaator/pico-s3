# Tiny s3 client library for java: pico-s3
* ultra minimalistic client library for accessing aws s3 service
* goal is zero addition dependencies to Java SE itself
* uses Java Standard Edition's http client, xml parser and cryptography support
* will do my best to keep this tiny, right now the jar file is less than 30kb, i will try to keep it under 100kb feature complete

## This is still work in progress, don't use yet
* will let you know once it's properly running and good enough for even beta use (you're welcome to read the source
and check it out, parts of it already work).

* stuff that should work right now
  * authentication against amazon's systems
  * s3 list
  * s3 get


## Goals
* to support no-auth, auth with credentials, auth from instance profile
* to support list, get and put commands
* anything else will be optional (but not off limits)



## Motivation
My main motivation to create this is the absurd size of amazon's s3 library if you include it's dependencies. If you 
have a vanilla project and you pull it in you're punished with 4-5 megabytes of dependencies. If your project is an aws 
lambda then the lambda is **unusable** due to the loading time of aws's original s3 sdk (in case the first thing you 
do is load config from s3 or alike, even the largest lambdas will have a cold start time counted in **seconds**, 
whereas your vanilla lambda was starting in milliseconds before you added the aws s3 sdk on top of it). So that is that. 

I understand that aws's sdk does not shine out in a java behemoth application, but on a clean project it's just too big 
and loads too long.

_Legal stuff: all the trademarks mentioned in the text above belong to their according owners._
