Maven Ant
=========

This is a small task for Apache Ant to manage downloading of dependencies from the Maven
central repository.

It not only handles getting the normal `.jar` files, but also JNA native jar files as well.

Usage
-----

Add the task to your build.xml file thus:

```xml
<taskdef name="maven" classname="uk.co.majenko.maven.Maven" classpath="maven-ant/maven-ant.jar"/>
```

Obviously adjust the classpath to suit.

Create (within a target) a `<maven>` task specifying the directory to download denendencies to.  Within that place a number of `<dependency>` tasks.

```xml
<maven dir="deps">
    <dependency group="org.apache.commons" artifact="commons-compress" version="1.15" />
    <dependency group="org.apache.commons" artifact="commons-io"       version="1.3.2" />
    <dependency group="org.apache.commons" artifact="commons-lang3"    version="3.7" />
<maven>
```

If there are extra native jar files (which should be named according to the format `{artifact}-{version}-{arch}.jar`) you can add
a collection of `<native>` tags within a dependency:

```xml
<maven dir="deps">
    <dependency group="org.usb4java" artifact="libusb4java" version="1.2.0" >
        <native arch="linux-arm" />
        <native arch="linux-x86" />
        <native arch="linux-x86_64" />
        <native arch="osx-x86" />
        <native arch="osx-x86_64" />
        <native arch="windows-x86" />
        <native arch="windows-x86_64" />
    </dependency>
</maven>
```

All the shown values are required, and there are no other values supported.

Files will be downloaded into the folder specified in the maven tag's `dir` value. They are named according to the format `{artifact}.jar` or, if a
native jar file, `{artifact}-{arch}.jar`. If the file already exists it will be silently skipped.
