package org.neo4j.test.server;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.net.URI;
import java.util.Map;

/**
 * @author mh
 * @since 30.11.15
 */
public class Neo4jRule implements TestRule, ServerBuilder
{
    private static final String DEFAULT_VERSION = "2.2.5";
    private final String version;
    private ServerBuilder builder;
    private ServerControls controls;

    Neo4jRule( ServerBuilder builder, String version )
    {
        this.builder = builder;
        this.version = version;
    }

    public Neo4jRule()
    {
        this( DEFAULT_VERSION );
    }
    public Neo4jRule(String version)
    {
        this( ManagedServerBuilders.newManagedBuilder(), version);
    }

/*
    public Neo4jRule( File workingDirectory )
    {
        this( TestServerBuilders.newInProcessBuilder( workingDirectory ) );
    }
*/

    @Override
    public Statement apply(final Statement base, Description description )
    {
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                try ( ServerControls sc = controls = builder.newServer(version) )
                {
                    base.evaluate();
                }
            }
        };
    }

    @Override
    public ServerControls newServer(String version)
    {
        throw new UnsupportedOperationException( "The server cannot be manually started via this class, it must be used as a JUnit rule." );
    }

    @Override
    public Neo4jRule withConfig( String key, String value )
    {
        builder = builder.withConfig( key, value );
        return this;
    }

    @Override
    public Neo4jRule withExtension( String mountPath, Class<?> extension )
    {
        builder = builder.withExtension( mountPath, extension );
        return this;
    }

    @Override
    public Neo4jRule withExtension( String mountPath, String packageName )
    {
        builder = builder.withExtension( mountPath, packageName );
        return this;
    }

    @Override
    public Neo4jRule withFixture( File cypherFileOrDirectory )
    {
        builder = builder.withFixture( cypherFileOrDirectory );
        return this;
    }

    @Override
    public Neo4jRule withFixture( String fixtureStatement )
    {
        builder = builder.withFixture( fixtureStatement );
        return this;
    }

    @Override
    public Neo4jRule copyFrom( File sourceDirectory )
    {
        builder = builder.copyFrom( sourceDirectory );
        return this;
    }

    public URI httpURI()
    {
        if(controls == null)
        {
            throw new IllegalStateException( "Cannot access instance URI before or after the test runs." );
        }
        return controls.httpURI();
    }

    public URI httpsURI()
    {
        if(controls == null)
        {
            throw new IllegalStateException( "Cannot access instance URI before or after the test runs." );
        }
        return controls.httpsURI();
    }

    public Iterable<Map<String,Object>> execute(String statement) {
        if(controls == null)
        {
            throw new IllegalStateException( "Cannot access instance URI before or after the test runs." );
        }
        return controls.execute(statement);

    }

    Iterable<Map<String,Object>> execute(String statement, Map<String,Object> params) {
        if(controls == null)
        {
            throw new IllegalStateException( "Cannot access instance URI before or after the test runs." );
        }
        return controls.execute(statement,params);
    }

    @Override
    public ServerBuilder withAuth() {
        builder = builder.withAuth();
        return this;
    }

    @Override
    public ServerBuilder withHttps() {
        builder = builder.withHttps();
        return this;
    }
}
