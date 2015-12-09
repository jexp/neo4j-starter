package org.neo4j.test.server;

import java.io.File;

/**
 * @author mh
 * @since 01.12.15
 */
public interface ServerBuilder {
    /**
     * Start a new server. By default, the server will listen to a random free port, and you can determine where to
     * connect using the {@link ServerControls#httpURI()} method. You could also specify explicit ports using the
     * {@link #withConfig(String, String)} method. Please refer to the Neo4j Manual for
     * details on available configuration options.
     * <p/>
     * When the returned controls are {@link ServerControls#close() closed}, the temporary directory the server used
     * will be removed as well.
     * @param version
     */
    public ServerControls newServer(String version);

    /**
     * Configure the Neo4j instance. Configuration here can be both configuration aimed at the server as well as the
     * database tuning options. Please refer to the Neo4j Manual for details on available configuration options.
     *
     * @param key   the config key
     * @param value the config value
     * @return this builder instance
     */
    public ServerBuilder withConfig(String key, String value);

    /**
     * Shortcut for configuring the server to use an unmanaged extension. Please refer to the Neo4j Manual on how to
     * write unmanaged extensions.
     *
     * @param mountPath the http path, relative to the server base URI, that this extension should be mounted at.
     * @param extension the extension class.
     * @return this builder instance
     */
    public ServerBuilder withExtension(String mountPath, Class<?> extension);

    /**
     * Shortcut for configuring the server to find and mount all unmanaged extensions in the given package.
     *
     * @param mountPath   the http path, relative to the server base URI, that this extension should be mounted at.
     * @param packageName a java package with extension classes.
     * @return this builder instance
     * @see #withExtension(String, Class)
     */
    public ServerBuilder withExtension(String mountPath, String packageName);

    /**
     * Data fixtures to inject upon server start. This can be either a file with a plain-text cypher query
     * (for example, myFixture.cyp), or a directory containing such files with the suffix ".cyp".
     *
     * @param cypherFileOrDirectory file with cypher statement, or directory containing ".cyp"-suffixed files.
     * @return this builder instance
     */
    public ServerBuilder withFixture(File cypherFileOrDirectory);

    /**
     * Data fixture to inject upon server start. This should be a valid Cypher statement.
     *
     * @param fixtureStatement a cypher statement
     * @return this builder instance
     */
    public ServerBuilder withFixture(String fixtureStatement);

    /**
     * Pre-populate the server with a database copied from the specified directory
     *
     * @param sourceDirectory
     * @return this builder instance
     */
    public ServerBuilder copyFrom(File sourceDirectory);

    public ServerBuilder withAuth();

    public ServerBuilder withHttps();

}

