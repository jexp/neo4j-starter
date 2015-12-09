package org.neo4j.test.server;

import java.net.URI;
import java.util.Map;

/**
 * @author mh
 * @since 30.11.15
 */
public interface ServerControls extends AutoCloseable
{
    /** Returns the URI to the root resource of the instance. For example, http://localhost:7474/ */
    URI httpURI();

    /**
     * Returns ths URI to the root resource of the instance using the https protocol.
     * For example, https://localhost:7475/.
     */
    URI httpsURI();

    /** Stop the test instance and delete all files related to it on disk. */
    @Override
    void close();

    Iterable<Map<String,Object>> execute(String statement);
    Iterable<Map<String,Object>> execute(String statement, Map<String,Object> params);
}
