package org.neo4j.test.server;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.neo4j.server.rest.domain.JsonHelper;

import java.net.URI;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

/**
 * @author mh
 * @since 04.12.15
 */
public class CypherExecutor {
    private final URI uri;

    public CypherExecutor(URI uri) {
        this.uri = uri.resolve("/db/data/transaction/commit");
    }

    public Iterable<Map<String, Object>> execute(String query, Map<String, Object> params) {
        String content = null;
        try {
            Map<String, List<Map>> statements = createPayload(query, params);
            HTTP.Response response = HTTP.POST(uri.toString(), statements);
            content = response.rawContent();
            String errors = response.stringFromContent("errors");
            if (errors != null && !errors.trim().isEmpty()) throw new RuntimeException("Error executing statement " + statements + ": " + errors);
            JsonNode result = response.get("results").get(0);
            final Map<String, Object> row = readColumns(result.get("columns").getElements());
            final Iterator<JsonNode> it = result.get("data").getElements();
            return new Iterable<Map<String, Object>>() {
                @Override
                public Iterator<Map<String, Object>> iterator() {
                    return new Iterator<Map<String, Object>>() {
                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Map<String, Object> next() {
                            JsonNode rowData = it.next().get("row");
                            int col = 0;
                            for (Map.Entry<String, Object> cell : row.entrySet()) {
                                cell.setValue(JsonHelper.readJson(rowData.get(col++)));
                            }
                            return row;
                        }
                    };
                }
            };
        } catch (JsonParseException e) {
            throw new RuntimeException("Error parsing JSON "+content,e);
        }
    }

    private Map<String, Object> readColumns(Iterator<JsonNode> result) {
        Map<String,Object> row = new LinkedHashMap<>();
        while (result.hasNext()) {
            row.put(result.next().getTextValue(),null);
        }
        return row;
    }

    private Map<String, List<Map>> createPayload(String query, Map<String, Object> params) {
        Map statement = new LinkedHashMap();
        statement.put("statement", query);
        if (params != null) statement.put("parameters", params);
        return singletonMap("statements", asList(statement));
    }
}
