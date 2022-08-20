# propertypartner-test
This is a test task for Property Partner.
This small console application simulates toy social network.
Following third party libraries are used:
* Cats
* Cats Effect 3
* fs2
* ScalaTest
* cats-effect-testing-scalatest

## Building the project
Before building the project please ensure that JDK and SBT are installed. 
JDK version should be 8 or above.
To build the application run
```
$ sbt clean compile test assembly
```
Build artifact (fat jar) will be stored at
```
$ ./target/scala-2.13/socialnetwork-assembly-0.1.0.jar
```

## Launching the application
Run following command from within the repository folder
```
$ java -jar target/scala-2.13/socialnetwork-assembly-0.1.0.jar  
```
or 
```
$ java -cp target/scala-2.13/socialnetwork-assembly-0.1.0.jar socialnetwork.Application
```




