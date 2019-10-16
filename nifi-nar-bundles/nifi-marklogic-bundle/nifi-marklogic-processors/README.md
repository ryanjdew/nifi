In order to run all of the tests in this project, you must first deploy a test application to MarkLogic which the 
tests will run against.

To deploy the test application, first create the file "gradle-local.properties" in this directory and set the following
properties to a MarkLogic user that is able to deploy the application (the "admin" user is a reasonable choice):

    mlUsername=admin
    mlPassword=changeme
    
Then run the [ml-gradle](https://github.com/marklogic-community/ml-gradle) task to deploy the application:

    ./gradlew -i mlDeploy

This ensures that all of the integration tests - those ending in "IT" - will be able to run. 

Tests that do not end in "IT" do not depend on the test application having been deployed. 
