package org.neo4j.starter;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.test.server.CypherExecutor;
import org.neo4j.test.server.HTTP;
import org.neo4j.test.server.ManagedServerBuilders;
import org.neo4j.test.server.ServerControls;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 30.11.15
 */
public class Neo4jStarterTest2 {
    @Test
    @Ignore
    public void testMyExtension() throws Exception {
        // Given
        try (ServerControls server = ManagedServerBuilders.newManagedBuilder()
                .withExtension("/myExtension", MyUnmanagedExtension.class)
                .newServer("2.2.5")) {
            // When
            HTTP.Response response = HTTP.GET(server.httpURI().resolve("myExtension").toString());

            // Then
            assertEquals(200, response.status());
        }
    }

    @Test
    public void testServerWithFunctionFixture() throws Exception {
        // Given
        try (ServerControls server = ManagedServerBuilders.newManagedBuilder()
                .withFixture("CREATE (:User)")
                .newServer("2.2.5")) {
            // When
            Iterable result = server.execute("MATCH (n:User) return n");

            // Then
            assertEquals(true, result.iterator().hasNext());
        }
    }

    @Test
    public void testServerWithHttps() throws Exception {
        // Given
        try (ServerControls server = ManagedServerBuilders.newManagedBuilder()
                .withFixture("CREATE (:User)")
                .withHttps()
                .newServer("2.2.5")) {
            // When
            CypherExecutor cypherExecutor = new CypherExecutor(server.httpsURI());
            Iterable<Map<String, Object>> result = cypherExecutor.execute("MATCH (n:User) return n", null);

            // Then
            assertEquals(true, result.iterator().hasNext());
        }
    }

    @Test
    public void testServerWithAuth() throws Exception {
        // Given
        try (ServerControls server = ManagedServerBuilders.newManagedBuilder()
                .withAuth()
                .withFixture("CREATE (:User)")
                .newServer("2.2.5")) {
            // When
            CypherExecutor cypherExecutor = new CypherExecutor(server.httpURI());
            Iterable<Map<String, Object>> result = cypherExecutor.execute("MATCH (n:User) return n", null);
            // Then
            assertEquals(true, result.iterator().hasNext());
        }
    }

    static class MyUnmanagedExtension {
    }
}
