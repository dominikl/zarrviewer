# zarrviewer
Simple Java Zarrviewer

## Usage

You have to build [zarr-java](https://github.com/zarr-developers/zarr-java/tree/main) first:

```
git clone https://github.com/zarr-developers/zarr-java.git
cd zarr-java
mvn package
```

...and install it into the local maven repository:

```
mvn install:install-file \
   -Dfile=target/zarr-java-0.0.5-SNAPSHOT.jar \
   -DgroupId=dev.zarr \
   -DartifactId=zarr-java \
   -Dversion=0.0.5-SNAPSHOT \
   -Dpackaging=jar \
   -DgeneratePom=true
```

Then you can build/run zarrviewer:

```
git clone https://github.com/dlindner/zarrviewer.git
cd zarrviewer
gradle run
```
