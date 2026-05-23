## run this in the terminal
#### Example path - find your actual 'jbr' folder path
to build the app, set JDK path available through android studio
my case it was below On a linux machine.

`export JAVA_HOME="/opt/android-studio/jbr"`

command to trigger the build
`./gradlew :app:assembleFossDebug`
