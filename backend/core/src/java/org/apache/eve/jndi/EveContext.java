/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.jndi;


import java.util.Hashtable ;

import javax.naming.Name ;
import javax.naming.Context ;
import javax.naming.NameParser ;
import javax.naming.ldap.Control ;
import javax.naming.NamingException ;
import javax.naming.NamingEnumeration ;
import javax.naming.directory.Attributes ;
import javax.naming.InvalidNameException ;
import javax.naming.directory.SearchControls ;

import org.apache.ldap.common.name.LdapName ;
import org.apache.ldap.common.filter.PresenceNode ;
import org.apache.ldap.common.util.NamespaceTools ;
import org.apache.ldap.common.message.LockableAttributesImpl ;

import org.apache.eve.PartitionNexus;


/**
 * A non-federated abstract Context implementation.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class EveContext implements Context
{
    /** */
    public static final String DELETE_OLD_RDN_PROP = "java.naming.ldap.deleteRDN" ;

    /** The interceptor proxy to the backend nexus */
    private final PartitionNexus nexusProxy ;
    /** The cloned environment used by this Context */
    private final Hashtable env ;
    /** The distinguished name of this Context */
    private final LdapName dn ;
    

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Must be called by all subclasses to initialize the nexus proxy and the
     * environment settings to be used by this Context implementation.
     * 
     * @param nexusProxy the intercepting proxy to the nexus.
     * @param env the environment properties used by this context.
     * @throws NamingException if the environment parameters are not set 
     * correctly.
     */
    protected EveContext( PartitionNexus nexusProxy, Hashtable env ) throws NamingException
    {
        this.nexusProxy = nexusProxy ;
        this.env = ( Hashtable ) env.clone() ;
        
        if ( null == env.get( PROVIDER_URL ) )
        {
            throw new NamingException( PROVIDER_URL + " property not found in environment." ) ;
        }
        

        /*
         * TODO Make sure we can handle URLs here as well as simple DNs
         * The PROVIDER_URL is interpreted as just a entry Dn since we are 
         * within the server.  However this may change in the future if we 
         * want to convey the listener from which the protocol originating
         * requests are comming from.
         */
        dn = new LdapName( ( String ) env.get( PROVIDER_URL ) ) ;
    }


    /**
     * Must be called by all subclasses to initialize the nexus proxy and the
     * environment settings to be used by this Context implementation.
     * 
     * @param nexusProxy the intercepting proxy to the nexus
     * @param env the environment properties used by this context
     * @param dn the distinguished name of this context
     */
    protected EveContext( PartitionNexus nexusProxy, Hashtable env, LdapName dn )
    {
        this.dn = ( LdapName ) dn.clone() ;
        this.env = ( Hashtable ) env.clone() ;
        this.env.put( PROVIDER_URL, dn.toString() ) ;
        this.nexusProxy = nexusProxy ;
    }


    // ------------------------------------------------------------------------
    // Protected Accessor Methods
    // ------------------------------------------------------------------------


    /**
     * Gets the RootNexus proxy.
     * 
     * @return the proxy to the backend nexus.
     */
    protected PartitionNexus getNexusProxy()
    {
       return nexusProxy  ;
    }
    
    
    /**
     * Gets the distinguished name of the entry associated with this Context.
     * 
     * @return the distinguished name of this Context's entry.
     */
    protected Name getDn()
    {
        return dn ;
    }


    // ------------------------------------------------------------------------
    // JNDI Context Interface Methods
    // ------------------------------------------------------------------------


    /**
     * @see javax.naming.Context#close()
     */
    public void close() throws NamingException
    {
        // Does nothing yet?
    }


    /**
     * @see javax.naming.Context#getNameInNamespace()
     */
    public String getNameInNamespace() throws NamingException
    {
        return dn.toString() ;
    }


    /**
     * @see javax.naming.Context#getEnvironment()
     */
    public Hashtable getEnvironment() throws NamingException
    {
        return env ;
    }


    /**
     * @see javax.naming.Context#addToEnvironment(java.lang.String, 
     * java.lang.Object)
     */
    public Object addToEnvironment( String propName, Object propVal ) throws NamingException
    {
        return env.put( propName, propVal ) ;
    }


    /**
     * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
     */
    public Object removeFromEnvironment( String propName ) throws NamingException
    {
        return env.remove( propName ) ;
    }


    /**
     * @see javax.naming.Context#createSubcontext(java.lang.String)
     */
    public Context createSubcontext( String name ) throws NamingException
    {
        return createSubcontext( new LdapName( name ) ) ;
    }


    /**
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext( Name name ) throws NamingException
    {
        /* 
         * Start building the server side attributes to be added directly to
         * the backend.
         * 
         * The RDN from name can be a multivalued RDN based on more than one
         * attribute using the '+' AVA concatenator in a name component.  Right
         * now this code will bomb out because we presume single valued RDNs.
         * 
         * TODO Add multivalued RDN handling code 
         */
        Attributes attributes = new LockableAttributesImpl() ;
        LdapName target = buildTarget( name ) ;
        String rdn = name.get( name.size() - 1 ) ;
        String rdnAttribute = NamespaceTools.getRdnAttribute( rdn ) ;
        String rdnValue = NamespaceTools.getRdnValue( rdn ) ;

        /* 
         * TODO Add code within the interceptor service managing operational
         * attributes the ability to add the target user provided DN to the 
         * attributes before normalization.  The result should have ths same
         * affect as the following line within the interceptor.
         * 
         * attributes.put( BootstrapSchema.DN_ATTR, target.toString() ) ;
         */
        attributes.put( rdnAttribute, rdnValue ) ;
        attributes.put( JavaLdapSupport.OBJECTCLASS_ATTR, JavaLdapSupport.JCONTAINER_ATTR ) ;
        attributes.put( JavaLdapSupport.OBJECTCLASS_ATTR, JavaLdapSupport.TOP_ATTR ) ;
        
        /*
         * Add the new context to the server which as a side effect adds 
         * operational attributes to the attributes refering instance which
         * can them be used to initialize a new EveLdapContext.  Remember
         * we need to copy over the controls as well to propagate the complete 
         * environment besides whats in the hashtable for env.
         */
        nexusProxy.add( target.toString(), target, attributes ) ;
        
        EveLdapContext ctx = new EveLdapContext( nexusProxy, env, target ) ;
        Control [] controls = ( Control [] ) ( ( EveLdapContext ) this ).getRequestControls().clone() ;
        ctx.setRequestControls( controls ) ;
        return ctx ;
    }


    /**
     * @see javax.naming.Context#destroySubcontext(java.lang.String)
     */
    public void destroySubcontext( String name ) throws NamingException
    {
        destroySubcontext( new LdapName( name ) ) ;
    }


    /**
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext( Name name ) throws NamingException
    {
        Name target = buildTarget( name ) ;
        nexusProxy.delete( target ) ;
    }


    /**
     * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
     */
    public void bind( String name, Object obj ) throws NamingException
    {
        bind( new LdapName( name ), obj ) ;
    }
    

    /**
     * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
     */
    public void bind( Name name, Object obj ) throws NamingException
    {
        if ( obj instanceof EveLdapContext )
        {
            throw new IllegalArgumentException( "Cannot bind a directory context object!" ) ;
        }

        /* 
         * Start building the server side attributes to be added directly to
         * the backend.
         * 
         * The RDN from name can be a multivalued RDN based on more than one
         * attribute using the '+' AVA concatenator in a name component.  Right
         * now this code will bomb out because we presume single valued RDNs.
         * 
         * TODO Add multivalued RDN handling code 
         */
        Attributes attributes = new LockableAttributesImpl() ;
        Name target = buildTarget( name ) ;

        // Serialize object into entry attributes and add it.
        JavaLdapSupport.serialize( attributes, obj ) ;
        nexusProxy.add( target.toString(), target, attributes ) ;
    }


    /**
     * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
     */
    public void rename( String oldName, String newName )
        throws NamingException
    {
        rename( new LdapName( oldName ), new LdapName( newName ) ) ;
    }


    /**
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     */
    public void rename( Name oldName, Name newName ) throws NamingException
    {
        Name oldDn = buildTarget( oldName ) ;
        Name newDn = buildTarget( newName ) ;
        Name oldBase = oldName.getSuffix( 1 ) ;
        Name newBase = newName.getSuffix( 1 ) ;

        String newRdn = newName.get( newName.size() - 1 ) ;
        String oldRdn = oldName.get( oldName.size() - 1 ) ;
                
        boolean delOldRdn = true ;
            
        /*
         * Attempt to use the java.naming.ldap.deleteRDN environment property
         * to get an override for the deleteOldRdn option to modifyRdn.  
         */
        if ( null != env.get( DELETE_OLD_RDN_PROP ) )
        {
            String delOldRdnStr = ( String ) env.get( DELETE_OLD_RDN_PROP ) ;
            delOldRdn = ! ( delOldRdnStr.equals( "false" ) ||
                delOldRdnStr.equals( "no" ) ||
                delOldRdnStr.equals( "0" ) ) ;
        }

        /*
         * We need to determine if this rename operation corresponds to a simple
         * RDN name change or a move operation.  If the two names are the same
         * except for the RDN then it is a simple modifyRdn operation.  If the
         * names differ in size or have a different baseDN then the operation is
         * a move operation.  Furthermore if the RDN in the move operation 
         * changes it is both an RDN change and a move operation.
         */
        if ( oldName.size() == newName.size() && oldBase.equals( newBase ) )
        {
            nexusProxy.modifyRn( oldDn, newRdn, delOldRdn ) ;
        }
        else
        {
            Name parent = newDn.getSuffix( 1 ) ;
            
            if ( newRdn.equalsIgnoreCase( oldRdn ) )
            {
                nexusProxy.move( oldDn, parent ) ;
            }
            else
            {
                nexusProxy.move( oldDn, parent, newRdn, delOldRdn ) ;
            }
        }
    }


    /**
     * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
     */
    public void rebind( String name, Object obj ) throws NamingException
    {
        rebind( new LdapName( name ), obj ) ;
    }


    /**
     * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
     */
    public void rebind( Name name, Object obj ) throws NamingException
    {
        Name target = buildTarget( name ) ;

        if ( nexusProxy.hasEntry( target ) )
        {
            nexusProxy.delete( target ) ;
        }

        bind( name, obj ) ;
    }


    /**
     * @see javax.naming.Context#unbind(java.lang.String)
     */
    public void unbind( String name ) throws NamingException
    {
        unbind( new LdapName( name ) ) ;
    }


    /**
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    public void unbind( Name name ) throws NamingException
    {
        nexusProxy.delete( buildTarget( name ) ) ;
    }


    /**
     * @see javax.naming.Context#lookup(java.lang.String)
     */
    public Object lookup( String name ) throws NamingException
    {
        return lookup( new LdapName( name ) ) ;
    }


    /**
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    public Object lookup( Name name ) throws NamingException
    {
        LdapName target = buildTarget( name ) ;
        Attributes attributes = nexusProxy.lookup( target ) ;
        
        // First lets test and see if the entry is a serialized java object
        if ( attributes.get( JavaLdapSupport.JCLASSNAME_ATTR ) != null )
        {
            // Give back serialized object and not a context
            return JavaLdapSupport.deserialize( attributes ) ;
        }
        
        // Initialize and return a context since the entry is not a java object
        EveLdapContext ctx = new EveLdapContext( nexusProxy, env, target ) ;
            
        // Need to add controls to propagate extended ldap operational env
        Control [] controls = ( ( EveLdapContext ) this ).getRequestControls() ;
        if ( null != controls )
        {    
            ctx.setRequestControls( ( Control [] ) controls.clone() ) ;
        }
        
        return ctx ;
    }


    /**
     * @see javax.naming.Context#lookupLink(java.lang.String)
     */
    public Object lookupLink( String name ) throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */
    public Object lookupLink( Name name ) throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Non-federated implementation presuming the name argument is not a 
     * composite name spanning multiple namespaces but a compound name in 
     * the same LDAP namespace.  Hence the parser returned is always the
     * same as calling this method with the empty String.
     * 
     * @see javax.naming.Context#getNameParser(java.lang.String)
     */
    public NameParser getNameParser( String name ) throws NamingException
    {
        return LdapName.getNameParser() ;
    }


    /**
     * Non-federated implementation presuming the name argument is not a 
     * composite name spanning multiple namespaces but a compound name in 
     * the same LDAP namespace.  Hence the parser returned is always the
     * same as calling this method with the empty String Name.
     * 
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser( Name name ) throws NamingException
    {
        return LdapName.getNameParser() ;
    }


    /**
     * @see javax.naming.Context#list(java.lang.String)
     */
    public NamingEnumeration list( String name ) throws NamingException
    {
        return list( new LdapName( name ) ) ;
    }


    /**
     * @see javax.naming.Context#list(javax.naming.Name)
     */
    public NamingEnumeration list( Name name ) throws NamingException
    {
        return nexusProxy.list( buildTarget( name ) ) ;
    }


    /**
     * @see javax.naming.Context#listBindings(java.lang.String)
     */
    public NamingEnumeration listBindings( String name ) throws NamingException
    {
        return listBindings( new LdapName( name ) ) ;
    }


    /**
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    public NamingEnumeration listBindings( Name name ) throws NamingException
    {
        // Conduct a special one level search at base for all objects
        Name base = buildTarget( name ) ;
        PresenceNode filter = new PresenceNode( "objectClass" ) ;
        SearchControls ctls = new SearchControls() ;
        ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE ) ;

        return nexusProxy.search( base , getEnvironment(), filter, ctls ) ;
    }


    /**
     * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
     */
    public String composeName( String name, String prefix ) throws NamingException
    {
        return composeName( new LdapName( name ), new LdapName( prefix ) ).toString() ;
    }


    /**
     * TODO Needs some serious testing here!
     * @see javax.naming.Context#composeName(javax.naming.Name, 
     * javax.naming.Name)
     */
    public Name composeName( Name name, Name prefix ) throws NamingException
    {
        // No prefix reduces to name, or the name relative to this context
        if ( prefix == null || prefix.size() == 0 )
        {
            return name ;
        }

        /*
         * Example: This context is ou=people and say name is the relative
         * name of uid=jwalker and the prefix is dc=domain.  Then we must
         * compose the name relative to prefix which would be:
         * 
         * uid=jwalker,ou=people,dc=domain.
         * 
         * The following general algorithm generates the right name:
         *      1). Find the Dn for name and walk it from the head to tail
         *          trying to match for the head of prefix.
         *      2). Remove name components from the Dn until a match for the 
         *          head of the prefix is found.
         *      3). Return the remainder of the fqn or Dn after chewing off some
         */
         
        // 1). Find the Dn for name and walk it from the head to tail
        Name fqn = buildTarget( name ) ;
        String head = prefix.get( 0 ) ;
        
        // 2). Walk the fqn trying to match for the head of the prefix
        while ( fqn.size() > 0 )
        {
            // match found end loop
            if ( fqn.get( 0 ).equalsIgnoreCase( head ) )
            {
                return fqn ;
            }
            else // 2). Remove name components from the Dn until a match 
            {
                fqn.remove( 0 ) ;
            }
        }
        
        throw new NamingException( "The prefix '" + prefix
                + "' is not an ancestor of this "  + "entry '" + dn + "'" ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // Utility Methods to Reduce Code
    // ------------------------------------------------------------------------
    
    
    /**
     * Clones this context's DN and adds the components of the name relative to 
     * this context to the left hand side of this context's cloned DN. 
     * 
     * @param relativeName a name relative to this context.
     * @return the name of the target
     * @throws InvalidNameException if relativeName is not a valid name in
     *      the LDAP namespace.
     */
    LdapName buildTarget( Name relativeName ) throws InvalidNameException
    {
        // Clone our DN or absolute path
        LdapName target = ( LdapName ) dn.clone() ;
        
        // Add to left hand side of cloned DN the relative name arg
        target.addAll( target.size(), relativeName ) ;
        return target ;
    }
}
