package org.neo4j.test.server;

import org.neo4j.starter.ManageNeo4j;
import org.neo4j.starter.Neo4jInstallation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.*;

/**
 * @author mh
 * @since 01.12.15
 */
public class TestServerBuilder implements ServerBuilder {
    public static final String JAX_RS_CLASSES = "org.neo4j.server.thirdparty_jaxrs_classes";
    private Map<String, String> config = new LinkedHashMap<>();
    private List<String> statements = new ArrayList<>(100);
    private boolean useAuth;
    private boolean useHttps;

    @Override
    public ServerControls newServer(final String version) {
        try {
            Neo4jInstallation installing = new Neo4jInstallation(version).download().extract();
            if (useAuth) installing = installing.withAuth();
            if (useHttps) installing = installing.withHttps();
            final Neo4jInstallation installation = installing.withConfig(config).configure().start();
            final URI uri = installing.httpURI();
            final CypherExecutor executor = new CypherExecutor(uri);
            for (String statement : statements) {
                for (Map row : executor.execute(statement,null));
            }
            return new ServerControls() {
                @Override
                public URI httpURI() {
                    return uri;
                }

                @Override
                public URI httpsURI() {
                    return installation.httpsURI();
                }

                @Override
                public void close() {
                    installation.remove();
                }

                @Override
                public Iterable<Map<String, Object>> execute(String statement) {
                    return executor.execute(statement,null);
                }

                @Override
                public Iterable<Map<String, Object>> execute(String statement, Map<String, Object> params) {
                    return executor.execute(statement,params);
                }
            };
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServerBuilder withConfig(String key, String value) {
        config.put(key,value);
        return this;
    }

    @Override
    public ServerBuilder withExtension(String mountPath, Class<?> extension) {
        return withExtension(mountPath,extension.getPackage().getName());
    }

    @Override
    public ServerBuilder withExtension(String mountPath, String packageName) {
        String extension = packageName + "=" + mountPath;
        String extensions = config.get(JAX_RS_CLASSES);
        if (extensions==null) extensions = extension;
        else extensions += "," + extension;
        return withConfig(JAX_RS_CLASSES,extensions);
    }

    @Override
    public ServerBuilder withFixture(File cypherFileOrDirectory) {
        ServerBuilder builder = this;
        if (cypherFileOrDirectory.isDirectory()) {
            File[] files = cypherFileOrDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.matches("(?i)\\.(cyp|cypher|cql)$");
                }
            });
            if (files!=null) {
                for (File file : files) {
                    builder = builder.withFixture(file);
                }
            }
        } else {
            try (Scanner scanner = new Scanner(cypherFileOrDirectory).useDelimiter(";\\s*$")) {
                while (scanner.hasNext()) {
                    statements.add(scanner.next());
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Error reading file "+cypherFileOrDirectory, e);
            }
        }
        return builder;
    }

    @Override
    public ServerBuilder withFixture(String fixtureStatement) {
        statements.add(fixtureStatement);
        return this;
    }

    @Override
    public ServerBuilder copyFrom(File sourceDirectory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServerBuilder withAuth() {
        this.useAuth = true;
        return this;
    }

    @Override
    public ServerBuilder withHttps() {
        this.useHttps = true;
        return this;
    }
}
