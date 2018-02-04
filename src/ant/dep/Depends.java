package ant.dep;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.ArrayList;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.SocketException;
import java.net.MalformedURLException;

import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

public class Depends extends Task {
    String _dir = null;
    ArrayList<Maven> _mavs = new ArrayList<Maven>();
    ArrayList<Download> _dlds = new ArrayList<Download>();

    public void setDir(String d) {
        _dir = d;
    }

    public Maven createMaven() {
        Maven d = new Maven();
        _mavs.add(d);
        return d;
    }

    public Download createDownload() {
        Download d = new Download();
        _dlds.add(d);
        return d;
    }

    public void execute() {
        if (_dir == null) {
            throw new BuildException("No destination directory specified");
        }
        File d = new File(_dir);
        if (d.exists() && !d.isDirectory()) {
            throw new BuildException("Destination directory exists as a regular file");
        }

        if (!d.exists()) {
            if (!d.mkdirs()) {
                throw new BuildException("Unable to create destination directory");
            }
        }

        for (Maven mav : _mavs) {
            mav.doSetDir(_dir);
            mav.execute();
        }
        
        for (Download dld : _dlds) {
            dld.doSetDir(_dir);
            dld.execute();
        }
        
    }

    public class Maven extends Task {

        String _dir;
        String _group;
        String _artifact;
        String _version;

        ArrayList<Native> _native = new ArrayList<Native>();

        public void doSetDir(String d) { _dir = d; }
        public void setGroup(String g) { _group = g; }
        public void setArtifact(String a) { _artifact = a; }
        public void setVersion(String v) { _version = v; }

        public Native createNative() {
            Native n = new Native();
            _native.add(n);
            return n;
        }

        public void execute() {
            String destName = _artifact + ".jar";
            File destDir = new File(_dir);
            File destFile = new File(destDir, destName);
            if (!destFile.exists()) {
                log(String.format("Getting %s:%s:%s ...", _group, _artifact, _version), Project.MSG_INFO);
                try {

                    String p = _group.replaceAll("\\.", "/");

                    // https://search.maven.org/remotecontent?filepath=${grp}/${artifact}/${version}/${artifact}-${version}.jar
                    downloadFile(String.format("https://search.maven.org/remotecontent?filepath=%s/%s/%s/%s-%s.jar",
                        p, _artifact, _version, _artifact, _version), destFile);
                } catch (Exception ex) {
                    throw new BuildException(ex.toString());
                }
            }

            for (Native n : _native) {
                n.doSetDir(_dir);
                n.doSetGroup(_group);
                n.doSetArtifact(_artifact);
                n.doSetVersion(_version);
                n.execute();
            }
        }

        public class Native extends Task {
            String _dir;
            String _group;
            String _artifact;
            String _version;
            String _arch = null;

            public void setArch(String a) { _arch = a; }
            public void doSetDir(String d) { _dir = d; }
            public void doSetGroup(String g) { _group = g; }
            public void doSetArtifact(String a) { _artifact = a; }
            public void doSetVersion(String v) { _version = v; }

            public void execute() {
                if (_arch == null) {
                    throw new BuildException("Architecture not set");
                }
                String destName = _artifact + "-" + _arch + ".jar";
                File destDir = new File(_dir);
                File destFile = new File(destDir, destName);
                if (!destFile.exists()) {
                    log(String.format("Getting %s:%s:%s (%s) ...", _group, _artifact, _version, _arch), Project.MSG_INFO);
                    try {

                        String p = _group.replaceAll("\\.", "/");

                        // https://search.maven.org/remotecontent?filepath=${grp}/${artifact}/${version}/${artifact}-${version}-${arch}.jar
                        downloadFile(String.format("https://search.maven.org/remotecontent?filepath=%s/%s/%s/%s-%s-%s.jar",
                            p, _artifact, _version, _artifact, _version, _arch), destFile);
                    } catch (Exception ex) {
                        throw new BuildException(ex.toString());
                    }
                }
            }
        }

    }

    public class Download extends Task {

        String _dir;
        String _url;
        String _filename;

        public void doSetDir(String d) { _dir = d; }
        public void setUrl(String u) { _url = u; }
        public void setFilename(String f) { _filename = f; }

        public void execute() {
            File destDir = new File(_dir);
            File destFile = new File(destDir, _filename);
            if (!destFile.exists()) {
                log(String.format("Getting %s from %s ...", _filename, _url), Project.MSG_INFO);
                try {
                    downloadFile(_url, destFile);
                } catch (Exception ex) {
                    if (destFile.exists()) {
                        destFile.delete();
                    }
                    throw new BuildException(ex.toString());
                }
            }
        }
    }

    void downloadFile(String url, File to) throws
            MalformedURLException,
            IOException {


        URL resourceUrl, base, next;
        HttpURLConnection conn;
        String location;

        while (true) {
            resourceUrl = new URL(url);
            conn        = (HttpURLConnection) resourceUrl.openConnection();

            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(false);   // Make the logic below easier to detect redirections
            conn.setRequestProperty("User-Agent", "Mozilla/5.0...");

            switch (conn.getResponseCode()) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    location = conn.getHeaderField("Location");
                    location = URLDecoder.decode(location, "UTF-8");
                    base     = new URL(url);               
                    next     = new URL(base, location);  // Deal with relative URLs
                    url      = next.toExternalForm();
                    continue;
            }

            break;
        }

        int size = conn.getContentLength();
        if (size == -1) {
            throw new SocketException("Unable to get content length");
        }

        InputStream stream = conn.getInputStream();

        ReadableByteChannel rbc = Channels.newChannel(stream);
        FileOutputStream fos = new FileOutputStream(to);

        long toRead = size;
        long pos = 0;
        String otn = getTaskName();
        while (toRead > 0) {
            long nread = fos.getChannel().transferFrom(rbc, pos, toRead > (10240*64) ? (10240*64) : toRead);
            toRead -= nread;
            pos += nread;
            StringBuilder b = new StringBuilder();
            for (long i = 0; i < nread / 10240L; i++) {
                b.append("#");
            }
            String ntn = String.format("%3d%%", (pos * 100L) / size);
            setTaskName(ntn);
            log(b.toString(), Project.MSG_INFO);
        }

        fos.close();
    }

}

