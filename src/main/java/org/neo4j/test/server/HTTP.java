/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.test.server;

import org.codehaus.jackson.JsonNode;

import java.io.*;
import java.net.*;
import java.util.*;

import org.codehaus.jackson.JsonParseException;
import org.neo4j.server.rest.domain.JsonHelper;

import static java.util.Collections.unmodifiableMap;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.neo4j.server.rest.domain.JsonHelper.createJsonFrom;

/**
 * A tool for performing REST HTTP requests
 */
public class HTTP
{

    private static final Builder BUILDER = new Builder().withHeaders( "Accept", "application/json" );

    public static Builder withHeaders( Map<String, String> headers )
    {
        return BUILDER.withHeaders( headers );
    }

    public static Builder withHeaders( String... kvPairs )
    {
        return BUILDER.withHeaders( kvPairs );
    }

    public static Builder withBaseUri( String baseUri )
    {
        return BUILDER.withBaseUri( baseUri );
    }

    public static Response POST( String uri )
    {
        return BUILDER.POST( uri );
    }

    public static Response POST( String uri, Object payload )
    {
        return BUILDER.POST( uri, payload );
    }

    public static Response POST( String uri, RawPayload payload )
    {
        return BUILDER.POST( uri, payload );
    }

    public static Response PUT( String uri )
    {
        return BUILDER.PUT( uri );
    }

    public static Response PUT( String uri, Object payload )
    {
        return BUILDER.PUT( uri, payload );
    }

    public static Response PUT( String uri, RawPayload payload )
    {
        return BUILDER.PUT( uri, payload );
    }

    public static Response DELETE( String uri )
    {
        return BUILDER.DELETE( uri );
    }

    public static Response GET( String uri )
    {
        return BUILDER.GET( uri );
    }

    public static Response request( String method, String uri )
    {
        return BUILDER.request( method, uri );
    }

    public static Response request( String method, String uri, Object payload )
    {
        return BUILDER.request( method, uri, payload );
    }

    public static class Builder
    {
        private final Map<String, String> headers;
        private final String baseUri;

        private Builder()
        {
            this( Collections.<String, String>emptyMap(), "" );
        }

        private Builder( Map<String, String> headers, String baseUri )
        {
            this.baseUri = baseUri;
            this.headers = unmodifiableMap( headers );
        }

        public Builder withHeaders( String... kvPairs )
        {
            Map<String,String> map = new LinkedHashMap<>(kvPairs.length / 2);
            for (int i = 0; i < kvPairs.length; i+=2) map.put(kvPairs[i],kvPairs[i+1]);
            return withHeaders( map );
        }

        public Builder withHeaders( Map<String, String> newHeaders )
        {
            Map<String, String> combined = new LinkedHashMap<>();
            combined.putAll( headers );
            combined.putAll( newHeaders );
            return new Builder( combined, baseUri );
        }

        public Builder withBaseUri( String baseUri )
        {
            return new Builder( headers, baseUri );
        }

        public Response POST( String uri )
        {
            return request( "POST", uri );
        }

        public Response POST( String uri, Object payload )
        {
            return request( "POST", uri, payload );
        }

        public Response POST( String uri, RawPayload payload )
        {
            return request( "POST", uri, payload );
        }

        public Response PUT( String uri )
        {
            return request( "PUT", uri );
        }

        public Response PUT( String uri, Object payload )
        {
            return request( "PUT", uri, payload );
        }

        public Response PUT( String uri, RawPayload payload )
        {
            return request( "PUT", uri, payload );
        }

        public Response DELETE( String uri )
        {
            return request( "DELETE", uri );
        }

        public Response GET( String uri )
        {
            return request( "GET", uri );
        }

        public Response request( String method, String uri )
        {
            try {
                HttpURLConnection conn = connect(method, uri, null);
                return createResponse(conn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private HttpURLConnection connect(String method, String uri, String payload) throws IOException {
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            handleAuth(url, conn);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                conn.setRequestProperty(header.getKey(),header.getValue());
            }
            if (payload == null) conn.setDoOutput(false);

            else {
                conn.setRequestProperty("content-type", "application/json");
                conn.setRequestProperty("content-length", String.valueOf(payload.length()));
                conn.setDoOutput(true);
            }
            conn.connect();
            return conn;
        }

        private void handleAuth(URL url, HttpURLConnection conn) {
            String auth = url.getUserInfo();
            if (auth == null || auth.trim().isEmpty()) return;
            String authHeader = "Basic " + new String(Base64.getEncoder().encode((auth).getBytes()));
            conn.setRequestProperty("Authorization", authHeader);
        }

        public Response request( String method, String uri, Object payload )
        {
            if(payload == null)
            {
                return request(method, uri);
            }

            try {
                String jsonPayload = payload instanceof RawPayload ? ((RawPayload) payload).get() : createJsonFrom(payload);
                HttpURLConnection conn = connect(method, uri, jsonPayload);
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
                writer.write(jsonPayload);
                writer.close();
                return createResponse(conn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private Response createResponse(HttpURLConnection conn) throws IOException {
            int length = conn.getContentLength();
            int status = conn.getResponseCode();
            String location = conn.getHeaderField("location");
            String encoding = conn.getHeaderField("content-encoding");
            InputStream is = new BufferedInputStream(conn.getInputStream());
            Scanner scanner = new Scanner(is).useDelimiter("\\Z");
            Object content = scanner.next();
            boolean more = scanner.hasNext();
            return new Response( status, content, location );
        }

        private URI buildUri( String uri )
        {
            URI unprefixedUri = URI.create( uri );
            if ( unprefixedUri.isAbsolute() )
            {
                return unprefixedUri;
            }
            else
            {
                return URI.create( baseUri + uri );
            }
        }

    }

    /**
     * Check some general validations that all REST responses should always pass.
     */
//    public static ClientResponse sanityCheck( ClientResponse response )
//    {
//        List<String> contentEncodings = response.getHeaders().get( "Content-Encoding" );
//        String contentEncoding;
//        if ( contentEncodings != null && (contentEncoding = singleOrNull( contentEncodings )) != null )
//        {
//            // Specifically, this is never used for character encoding.
//            contentEncoding = contentEncoding.toLowerCase();
//            assertThat( contentEncoding, anyOf(
//                    containsString( "gzip" ),
//                    containsString( "deflate" ) ) );
//            assertThat( contentEncoding, allOf(
//                    not( containsString( "utf-8" ) ) ) );
//        }
//        return response;
//    }

    public static class Response
    {
//        private final ClientResponse response;
        private final String entity;
        private int status;
        private String location;
        private Map<String,String> headers = new HashMap<>();

        public Response( int status, Object content, String location )
        {
            this.status = status;
            this.location = location;
            this.entity = content.toString();
//            this.response = sanityCheck( response );
        }

        public int status()
        {
            return status;
        }

        public String location()
        {
            if ( location != null )
            {
                return location;
            }
            throw new RuntimeException( "The request did not contain a location header, " +
                    "unable to provide location. Status code was: " + status() );
        }

        @SuppressWarnings("unchecked")
        public <T> T content()
        {
            try
            {
                return (T) JsonHelper.readJson( entity );
            }
            catch ( RuntimeException e )
            {
                throw new RuntimeException( "Unable to deserialize: " + entity, e );
            }
        }

        public String rawContent()
        {
            return entity;
        }

        public String stringFromContent( String key ) throws JsonParseException
        {
            return get(key).asText();
        }

        public JsonNode get(String fieldName) throws JsonParseException
        {
            return JsonHelper.jsonNode( entity ).get( fieldName );
        }

        public String header( String name )
        {
            return headers.get( name );
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "HTTP " ).append( status ).append( "\n" );
            for ( Map.Entry<String, String> header : headers.entrySet() )
            {
                sb.append(header.getKey()).append(": ").append( header.getValue() ).append( "\n" );
            }
            sb.append( "\n" );
            sb.append( entity ).append( "\n" );

            return sb.toString();
        }
    }

    public static class RawPayload
    {
        private final String payload;

        public static RawPayload rawPayload( String payload )
        {
            return new RawPayload( payload );
        }

        public static RawPayload quotedJson( String json )
        {
            return new RawPayload( json.replaceAll( "'", "\"" ) );
        }

        private RawPayload( String payload )
        {
            this.payload = payload;
        }

        public String get()
        {
            return payload;
        }
    }
}
