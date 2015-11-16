package org.neo4j.starter;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author mh
 * @since 11.10.15
 */
public class ManageNeo4j {

    private static final int MB = 1024 * 1024;
    private static final String BASE_URL = "http://dist.neo4j.org/";
//    private static final String BASE_URL = "http://localhost:8000/";

    public static void main(String[] args) throws Exception {
//        if (args[0].equals("download")) downloadNeo4j(args[1]);
        downloadNeo4j("2.2.5");
        int port = installNeo4j("2.2.5");
        System.out.println("Port "+port);
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
    }
    static class Installation {
        String edition;
        String version;
        String download;
        String directory;
        int port;
        int pid;
        // auth, pagecache,heap,ssl,extension
    }

    private static int installNeo4j(String version) throws IOException, InterruptedException {
        int port = getFreePort();
        String target = new File(tmpDir(), neo4jFileName(version, false) + "-" + port).getAbsolutePath();
        System.err.println("target:"+target);
        assertEmptyDirectory(target);
        String download = downloadFileName(version).getAbsolutePath();
        String[] cmd = {"tar", "xzf", download, "-C", target,neo4jFileName(version,false)+"/*"};
        execCommand(cmd);
        patchConfig(target,"neo4j-server.properties","org.neo4j.server.webserver.https.port","org.neo4j.server.webserver.https.port="+port);
        return port;
    }

    private static File assertEmptyDirectory(String target) throws IOException {
        File file = new File(target);
        if (!file.exists()) {
            if (!file.mkdirs()) throw new IOException("Error creating directories "+target);
            return file;
        }
        if (!file.isDirectory()) throw new IOException("No directory "+target);
        if (!file.canWrite()) throw new IOException("Not Writable "+target);
        if ( file.list().length>0) throw new IOException("Not Empty "+target);

        return file;
    }

    private static void patchConfig(String path, String file, String pattern, String newLine) throws IOException {
        File configFile = new File(new File(path, "conf"), file);
        Scanner scanner = new Scanner(configFile);
        List<String> lines = readFile(scanner);
        FileWriter writer = new FileWriter(configFile);
        for (String line : lines) {
            if (line.contains(pattern)) line = newLine;
            writer.write(line);
        }
        writer.close();
    }

    private static List<String> readFile(Scanner scanner) {
        List<String> lines = new ArrayList<>(50);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            lines.add(line);
        }
        scanner.close();
        return lines;
    }

    private static int getFreePort() {
        int port;
        do {
            port = ThreadLocalRandom.current().nextInt(10000, 65000);
        } while(!testSocket(port));
        return port;
    }

    private static void execCommand(String... cmd) throws IOException, InterruptedException {
        System.out.println(Arrays.toString(cmd));
        Process process = Runtime.getRuntime().exec(cmd);
        InputStream err = process.getErrorStream();
        process.waitFor();
        int i = process.exitValue();
        String errorOutput = new Scanner(err).useDelimiter("\\Z").next();
        System.err.println(errorOutput);
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

    private static void downloadNeo4j(String version) throws IOException {
        downloadFile(new URL(BASE_URL + neo4jFileName(version,true)), downloadFileName(version));
    }

    private static File downloadFileName(String version) {
        String tmpDir = tmpDir();
//        String tmpDir = System.getenv("TMP");
        return new File(tmpDir, neo4jFileName(version,true));
    }

    private static String tmpDir() {
        return System.getProperty("java.io.tmpdir");
    }

    private static String neo4jFileName(String version, boolean compressed) {
        return String.format("neo4j-community-%s-unix%s",version, compressed ? ".tar.gz" : "");
    }

    private static long downloadFile(URL source, File target) throws IOException {
        System.err.printf("Downloading %s to %s%n", source, target);
        byte[] buffer = new byte[MB];
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
        System.err.printf("Download complete. %d in %d seconds.%n", total, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start));
        return total;
    }
}
