package org.neo4j.starter;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.test.server.HTTP;
import org.neo4j.test.server.Neo4jRule;

import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 30.11.15
 */
public class Neo4jStarterTest {
    @Rule
    public Neo4jRule neo4j = new Neo4jRule()
            .withFixture( "CREATE (admin:Admin)" );

    @Test
    public void shouldWorkWithServer() throws Exception
    {
        // Given
        URI serverURI = neo4j.httpURI();

        // When I access the server
        HTTP.Response response = HTTP.GET( serverURI.toString() );

        // Then it should reply
        assertEquals(200, response.status());

        assertEquals( 1, neo4j.execute("MATCH (n:Admin) RETURN count(*) as c").iterator().next().get("c"));
    }
}
