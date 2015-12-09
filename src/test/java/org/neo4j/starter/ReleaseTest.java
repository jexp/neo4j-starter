package org.neo4j.starter;

import org.junit.Test;
import org.neo4j.test.server.HTTP;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 09.12.15
 */
public class ReleaseTest {
    @Test
    public void testLatestRelease() throws Exception {
        HTTP.Response response = HTTP.GET("http://github.com/neo4j/neo4j/releases/latest");
        assertEquals(200,response.status());
        assertEquals("2.2.7",response.get("tag_name").getTextValue());
    }
}
