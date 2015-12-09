package org.neo4j.starter;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author mh
 * @since 08.12.15
 */
public class Neo4jInstallation {
    String edition = "community";
    String version;
    File downloadFile;
    File installLocation;
    String download;
    String directory;
    String host = "localhost";
    int port = -1;
    boolean useHttps = false;
    int httpsPort = -1;
    int pid;

    boolean useAuth = false;
    String password = "test";
    String authFileContents = "neo4j:SHA-256,3A8E8612A55E6466FA19A4A8F7FEC249467C03647C5F923A234EEE65CC4F2F72,7A9DA58DB8B7C95030B2E3982A37F399:";
    String basicAuth = "Basic bmVvNGo6dGVzdA==";

    int heapInMB = 512, pageCacheInMB = 128;
    private Map<String, String> config = new LinkedHashMap<>();
    private boolean started;
    // pagecache,heapInMB,extension

    public Neo4jInstallation(String version) {
        this.version = version;
    }

    public Neo4jInstallation download() {
        this.downloadFile = ManageNeo4j.downloadNeo4j(version);
        return this;
    }

    public Neo4jInstallation extract() {
        this.port = port == -1 ? ManageNeo4j.findFreePort() : port;
        this.installLocation = ManageNeo4j.installLocation(version, port);
        System.err.println("install location:" + installLocation + " port " + port);
        ManageNeo4j.assertEmptyDirectory(installLocation);
        ManageNeo4j.extractFiles(downloadFile, installLocation);
        return this;
    }

    public URI httpURI() {
        try {
            return new URI("http", (useAuth ? "neo4j:" + password : null), host, port, null, null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error creating URI", e);
        }
    }

    public URI httpsURI() {
        if (!useHttps) throw new RuntimeException("HTTPs not configured, use withHttps()");
        try {
            return new URI("https", (useAuth ? "neo4j:" + password : null), host, httpsPort, null, null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error creating URI", e);
        }
    }

    public Neo4jInstallation configure() {
        ManageNeo4j.patchConfig(installLocation, "neo4j.properties",
                "remote_shell_enabled", "remote_shell_enabled=false",
                "keep_logical_logs", "keep_logical_logs=false");
        configurePort();
        configureHttps();
        configureAuth();
        configurePageCache();
        configureHeap();
        configureConfig();
        return this;
    }

    private void configureConfig() {
        if (!config.isEmpty()) {
            String[] configStrings = new String[config.size()];
            int i=0;
            for (Map.Entry<String, String> entry : config.entrySet()) {
                configStrings[i++]=entry.getKey();
                configStrings[i++]=entry.getKey()+"="+entry.getValue();
            }
            ManageNeo4j.patchConfig(installLocation, "neo4j.properties",configStrings);
            ManageNeo4j.patchConfig(installLocation, "neo4j-server.properties",configStrings);
        }
    }

    private void configureHeap() {
        ManageNeo4j.patchConfig(installLocation, "neo4j-wrapper.conf",
                "wrapper.java.initmemory", "wrapper.java.initmemory=" + heapInMB,
                "wrapper.java.maxmemory", "wrapper.java.maxmemory=" + heapInMB);
    }

    private void configurePageCache() {
        ManageNeo4j.patchConfig(installLocation, "neo4j.properties",
                "dbms.pagecache.memory", "dbms.pagecache.memory=" + pageCacheInMB + "M");
    }

    private void configurePort() {
        ManageNeo4j.patchConfig(installLocation, "neo4j-server.properties",
                "org.neo4j.server.webserver.port", "org.neo4j.server.webserver.port=" + port);
    }

    private void configureAuth() {
        ManageNeo4j.patchConfig(installLocation, "neo4j-server.properties",
                "dbms.security.auth_enabled", "dbms.security.auth_enabled=" + (useAuth ? "true" : "false"));
        if (useAuth) {
            ManageNeo4j.writeFile(installLocation, "data/dbms/auth", authFileContents);
        }
    }

    private void configureHttps() {
        ManageNeo4j.patchConfig(installLocation, "neo4j-server.properties",
                "org.neo4j.server.webserver.https.enabled", "org.neo4j.server.webserver.https.enabled=" + (useHttps ? "true" : "false"));
        if (useHttps) {
            httpsPort = httpsPort == -1 ? ManageNeo4j.findFreePort() : httpsPort;
            ManageNeo4j.patchConfig(installLocation, "neo4j-server.properties",
                    "org.neo4j.server.webserver.https.port=", "org.neo4j.server.webserver.https.port=" + this.httpsPort);
        }
    }


    public boolean stop() {
        return ManageNeo4j.neo4jCommand(version, port, "stop");
    }

    public Neo4jInstallation start() {
        started = ManageNeo4j.neo4jCommand(version, port, "start");
        return this;
    }

    public Neo4jInstallation withHttps() {
        this.useHttps = true;
        return this;
    }

    public Neo4jInstallation withAuth() {
        this.useAuth = true;
        return this;
    }

    public Neo4jInstallation withPort(int port) {
        this.port = port;
        return this;
    }

    public Neo4jInstallation remove() {
        ManageNeo4j.removeSetup(version,port);
        return this;
    }

    public Neo4jInstallation withConfig(Map<String, String> config) {
        this.config.putAll(config);
        return this;
    }
}
