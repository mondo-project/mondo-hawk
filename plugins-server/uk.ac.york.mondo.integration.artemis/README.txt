In order to fetch these dependencies, you may:

- Using [IvyDE](https://ant.apache.org/ivy/ivyde/), right click on the
  project and select "Ivy > Retrieve 'dependencies'". You may need to
  run it twice in order for it to work, due to a problem with the
  "native-package-type" variable used by the artemis-native reference.

- Using regular Ant and Ivy, run the "retrieve" task in the
  fetch-deps.xml Ant buildfile to fetch all the .jar files required by
  Apache Artemis 1.0.0. You can do so with:

      ant -f fetch-deps.xml -Dnative-package-type=jar -lib /path/to/ivy.jar
