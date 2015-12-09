package org.neo4j.test.server;

/**
 * @author mh
 * @since 30.11.15
 */
public class ManagedServerBuilders {
    public static ServerBuilder newManagedBuilder() {
        return new TestServerBuilder();
    }
}
