package org.neo4j.starter;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author mh
 * @since 11.10.15
 */
public class ManageNeo4j {

    private static final int MB = 1024 * 1024;
    private static final String BASE_URL = "http://dist.neo4j.org/";
    public static final String TMPDIR = System.getProperty("java.io.tmpdir");
//    private static final String BASE_URL = "http://localhost:8000/";

    public static void main(String[] args) throws Exception {
//        if (args[0].equals("download")) downloadNeo4j(args[1]);
        String version = "2.2.5";
        // todo create an installation object
        downloadNeo4j(version);
        int port = setupNeo4j(version);
        neo4jCommand(version,port,"start");
        neo4jCommand(version,port,"stop");
        removeSetup(version,port);
    }

    public static void removeSetup(String version, int port) {
        File installLocation = installLocation(version, port);
        if (installLocation.exists() && installLocation.isDirectory()) {
            neo4jCommand(version, port, "stop");
            deleteRecursively(installLocation);
        }
    }

    static class Version {
        enum Edition { community, enterprise }
        enum OS { unix, windows; public String compressed(boolean compressed) { return compressed ? (this == unix ? ".gz" : ".zip"):"";} }
        Edition edition;
        OS os;
        String version;

        public String toString() {
            return toString(edition,version,os,false);
        }

        public String toString(Edition edition, String version, OS os, boolean compressed) {
            return String.format("neo4j-%s-%s-%s%s",edition,version,os,os.compressed(compressed));
        }
        public static String toString(Object...parts) {
            StringBuilder result = new StringBuilder("neo4j");
            for (int i = 0; i < parts.length; i++) {
                Object part = parts[i];
                result.append(part);
                if (i<parts.length-1) result.append("-");
            }
            return result.toString();
        }
    }

    static void writeFile(File path, String file, String contents) {
        File target = new File(path, String.join(File.separator, file.split("/")));
        File parent = target.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs()) throw new RuntimeException("Error creating directory: "+parent);
        }
        try {
            FileWriter writer = new FileWriter(target);
            writer.write(contents);
            writer.close();
        } catch (IOException ioe) {
            throw new RuntimeException("Error writing to file "+target,ioe);
        }
    }

    static void extractFiles(File downloadFile, File installLocation) {
        String downloadFilePath = downloadFile.getAbsolutePath();
        String[] cmd = {"tar", "xzf", downloadFilePath, "-C", installLocation.getAbsolutePath(),"--strip-components","1"};
        execCommand(cmd);
    }

    public static boolean neo4jCommand(String version, int port, String operation) {
        return execCommand(new File(installLocation(version,port),"bin/neo4j").getAbsolutePath(), operation) == 0;
    }
    public static int setupNeo4j(String version) {
        int port = findFreePort();
        File installLocation = installLocation(version, port);
        output("install location:"+installLocation+" port "+port);
        assertEmptyDirectory(installLocation);
        String downloadFile = downloadFileName(version).getAbsolutePath();
        String[] cmd = {"tar", "xzf", downloadFile, "-C", installLocation.getAbsolutePath(),"--strip-components","1"};
        execCommand(cmd);

        patchConfig(installLocation,"neo4j-server.properties",
                "org.neo4j.server.webserver.port","org.neo4j.server.webserver.port="+port,
                "dbms.security.auth_enabled","dbms.security.auth_enabled=false",
                "org.neo4j.server.webserver.https.enabled","org.neo4j.server.webserver.https.enabled=false");
        patchConfig(installLocation,"neo4j.properties",
                "remote_shell_enabled","remote_shell_enabled=false",
                "dbms.pagecache.memory","dbms.pagecache.memory=128m",
                "keep_logical_logs","keep_logical_logs=false");
        return port;
    }

    static File installLocation(String version, int port) {
        File installLocation = new File(TMPDIR, neo4jFileName(version, false) + "-" + port);
        installLocation.deleteOnExit();
        return installLocation;
    }

    static File assertEmptyDirectory(File file) {
        if (!file.exists()) {
            if (!file.mkdirs()) throw new RuntimeException("Error creating directories "+file);
            return file;
        }
        if (!file.isDirectory()) throw new RuntimeException("No directory "+file);
        if (!file.canWrite()) throw new RuntimeException("Not Writable "+file);
        if ( file.list().length>0) throw new RuntimeException("Not Empty "+file);

        return file;
    }

    static void patchConfig(File path, String file, String... replacements) {
        assert replacements.length % 2 == 0;
        File configFile = new File(new File(path, "conf"), file);
        output("Patching config file: %s%n",configFile);
        List<String> lines = readFile(configFile);
        try (Writer writer = new FileWriter(configFile)) {
            for (String line : lines) {
                for (int i = 0; i < replacements.length; i += 2) {
                    if (line.contains(replacements[i])) {
                        System.err.println(line + " -> " + replacements[i + 1]);
                        line = replacements[i + 1];
                        break;
                    }
                }
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error patching file "+configFile,e);
        }
    }

    private static List<String> readFile(File file) {
        try (Scanner scanner = new Scanner(file)) {
            List<String> lines = new ArrayList<>(50);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lines.add(line);
            }
            return lines;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error reading from "+file,e);
        }
    }

    private static int getFreePort() {
        int port;
        do {
            port = ThreadLocalRandom.current().nextInt(10000, 65000);
        } while(!testSocket(port));
        return port;
    }

    private static int execCommand(String... cmd) {
        output("Running commands: %s%n",Arrays.toString(cmd));
        try {
            long time = System.currentTimeMillis();
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();
            int exitValue = process.exitValue();
            time = (System.currentTimeMillis() - time) / 1000;
            System.err.printf("Process finished after %d seconds, exit code %d%n", time, exitValue);
            System.err.println(grabOutput(process.getInputStream()));
            System.err.println(grabOutput(process.getErrorStream()));
            return exitValue;
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException("Error running commands: "+ Arrays.toString(cmd));
        }
    }

    private static String grabOutput(InputStream err) {
        Scanner scanner = new Scanner(err).useDelimiter("\\Z");
        return scanner.hasNext() ? scanner.next() : "";
    }

    static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Could not find a free port",e);
        }
    }
    private static boolean testSocket(int port) {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(100);
            socket.connect(new InetSocketAddress(port));
            return !socket.isConnected();
        } catch (IOException e) {
//            System.err.println("Error connecting to port " + port + " " + e.getMessage());
            return true;
        }
    }

    public static File downloadNeo4j(String version) {
        File target = downloadFileName(version);
        long time = System.currentTimeMillis();
        if (!target.exists()) {
            String url = BASE_URL + neo4jFileName(version, true);
            try {
                downloadFile(new URL(url), target);
            } catch (IOException e) {
                throw new RuntimeException("Error downloading URL "+url,e);
            }
        }
        time = (System.currentTimeMillis() - time ) / 1000;
        output("Downloaded version %s to %s took %d seconds size %d%n",version,target.getAbsolutePath(),time, target.length());
        return target;
    }

    private static File downloadFileName(String version) {
        String tmpDir = TMPDIR;
        return new File(tmpDir, neo4jFileName(version,true));
    }

    private static void output(String msg, Object...args) {
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof String) {
                args[i] = sanitize((String)args[i]);
            }
            if (arg instanceof File) {
                args[i] = sanitize(((File)args[i]).getAbsolutePath());
            }
        }
        System.err.printf(sanitize(msg),args);
    }

    private static String sanitize(String msg) {
        return msg.replace(TMPDIR,"$TMPDIR/");
    }

    private static String neo4jFileName(String version, boolean compressed) {
        return String.format("neo4j-community-%s-unix%s",version, compressed ? ".tar.gz" : "");
    }

    public static String neo4jFileName(Object...parts) {
        StringBuilder result = new StringBuilder("neo4j-");
        for (int i = 0; i < parts.length; i++) {
            Object part = parts[i];
            result.append(part);
            if (i<parts.length-1) result.append("-");
        }
        return result.toString();
    }

    private static long downloadFile(URL source, File target) throws IOException {
        output("Downloading %s to %s%n", source, target);
        byte[] buffer = new byte[10*MB];
//        InputStream is = new GZIPInputStream(new BufferedInputStream(source.openStream(),MB));
        InputStream is = new BufferedInputStream(source.openStream(),MB);
        OutputStream os = new BufferedOutputStream(new FileOutputStream(target),MB);
        long total = 0;
        int read = -1;
        long start = System.currentTimeMillis();
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer,0,read);
            total += read;
        }
        os.close();
        is.close();
        output("Download complete. %d in %d seconds.%n", total, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start));
        return total;
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files!=null) {
                for (File child : files) deleteRecursively(child);
            }
        }
        file.delete();
    }
}
