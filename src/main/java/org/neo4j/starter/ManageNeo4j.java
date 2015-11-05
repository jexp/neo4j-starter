package org.neo4j.starter;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * @author mh
 * @since 11.10.15
 */
public class ManageNeo4j {

    private static final int MB = 1024 * 1024;
//    private static final String BASE_URL = "http://dist.neo4j.org/";
    private static final String BASE_URL = "http://localhost:8000/";

    public static void main(String[] args) throws Exception {
//        if (args[0].equals("download")) downloadNeo4j(args[1]);
//        downloadNeo4j("2.2.5");
        int port = installNeo4j("2.2.5");
        System.out.println("Port "+port);
    }

    private static int installNeo4j(String version) throws IOException, InterruptedException {
        int port;
        do {
            port = ThreadLocalRandom.current().nextInt(10000, 65000);
        } while(!testSocket(port));
        Runtime rt = Runtime.getRuntime();
        String target = new File(tmpDir(), neo4jFileName(version, false) + "-" + port).getAbsolutePath();
        String download = downloadFileName(version).getAbsolutePath();
        String[] cmd = {"tar", "xzf", download, "-C", target};
        execCommand(cmd);
        return port;
        // Runtime.getRuntime().exec(params);
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
        System.err.printf("Download complete. %d in %d seconds.%n", total, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start));
        return total;
    }
}
