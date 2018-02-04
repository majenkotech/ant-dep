Ant Dep
=======

This is a small task for Apache Ant to manage downloading of dependencies from various locations:

* Maven central repository
* Arbitrary URL
* (More to come)


Usage
-----

Add the task to your build.xml file thus:

```xml
<taskdef name="depends" classname="ant.dep.Depends" classpath="ant-dep/ant-dep.jar"/>
```

Obviously adjust the classpath to suit.

Create (within a target) a `<depends dir="deps">` task specifying the directory to download denendencies to.  Within that place a number of subtasks (detailed below)
for the various dependencies.

Maven
-----

Use the `maven` sub-task to download from the Maven Central Repository.

It not only handles getting the normal `.jar` files, but also JNA native jar files as well.
```xml
<depends dir="deps">
    <maven group="org.apache.commons" artifact="commons-compress" version="1.15" />
    <maven group="org.apache.commons" artifact="commons-io"       version="1.3.2" />
    <maven group="org.apache.commons" artifact="commons-lang3"    version="3.7" />
<maven>
```

If there are extra native jar files (which should be named according to the format `{artifact}-{version}-{arch}.jar`) you can add
a collection of `<native>` tags within a dependency:

```xml
<depends dir="deps">
    <maven group="org.usb4java" artifact="libusb4java" version="1.2.0" >
        <native arch="linux-arm" />
        <native arch="linux-x86" />
        <native arch="linux-x86_64" />
        <native arch="osx-x86" />
        <native arch="osx-x86_64" />
        <native arch="windows-x86" />
        <native arch="windows-x86_64" />
    </maven>
</depends>
```

All the shown values are required, and there are no other values supported.

Files will be downloaded into the folder specified in the maven tag's `dir` value. They are named according to the format `{artifact}.jar` or, if a
native jar file, `{artifact}-{arch}.jar`. If the file already exists it will be silently skipped.

Arbitrary URL
-------------

You can also download files from arbitrary URLs (much like the standard Apache Ant task `get`) - this just keeps it all integrated.

Use the `download` tag to download arbitrary files:

```xml
<depends dir="deps">
    <download url="http://example.com/file.zip" filename="file.zip"/>
</depends>
```

The file will be downloaded from the URL (following redirects across HTTP and HTTPS) and stored as the specified filename within
the dependencies directory.

Again, if the file exists, it will be silently skipped.

Mixing
------

Different repository types can be mixed in one dependency set. For example, using all the above exaples:


```xml
<depends dir="deps">
    <maven group="org.apache.commons" artifact="commons-compress" version="1.15" />
    <maven group="org.apache.commons" artifact="commons-io"       version="1.3.2" />
    <maven group="org.apache.commons" artifact="commons-lang3"    version="3.7" />
    <maven group="org.usb4java" artifact="libusb4java"            version="1.2.0" >
        <native arch="linux-arm" />
        <native arch="linux-x86" />
        <native arch="linux-x86_64" />
        <native arch="osx-x86" />
        <native arch="osx-x86_64" />
        <native arch="windows-x86" />
        <native arch="windows-x86_64" />
    </maven>
    <download url="http://example.com/file.zip" filename="file.zip"/>
<maven>
```
