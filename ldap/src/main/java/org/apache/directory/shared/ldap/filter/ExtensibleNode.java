/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.filter;


import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Filter expression tree node for extensible assertions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class ExtensibleNode extends LeafNode
{
    /** The value of the attribute to match for */
    private final byte[] value;

    /** The matching rules id */
    private final String matchingRuleId;

    /** The name of the dn attributes */
    private boolean dnAttributes = false;


    /**
     * Creates a new ExtensibleNode object.
     * 
     * @param attribute the attribute used for the extensible assertion
     * @param value the value to match for
     * @param matchingRuleId the OID of the matching rule
     * @param dnAttributes the dn attributes
     */
    public ExtensibleNode(String attribute, String value, String matchingRuleId, boolean dnAttributes)
    {
        this( attribute, StringTools.getBytesUtf8( value ), matchingRuleId, dnAttributes );
    }


    /**
     * Creates a new ExtensibleNode object.
     * 
     * @param attribute the attribute used for the extensible assertion
     * @param value the value to match for
     * @param matchingRuleId the OID of the matching rule
     * @param dnAttributes the dn attributes
     */
    public ExtensibleNode(String attribute, byte[] value, String matchingRuleId, boolean dnAttributes)
    {
        super( attribute );

        this.value = value;
        this.matchingRuleId = matchingRuleId;
        this.dnAttributes = dnAttributes;
    }


    /**
     * Gets the Dn attributes.
     * 
     * @return the dn attributes
     */
    public boolean dnAttributes()
    {
        return dnAttributes;
    }


    /**
     * Gets the matching rule id as an OID string.
     * 
     * @return the OID
     */
    public String getMatchingRuleId()
    {
        return matchingRuleId;
    }


    /**
     * Gets the value.
     * 
     * @return the value
     */
    public final byte[] getValue()
    {
        return value;
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#printToBuffer(
     *      java.lang.StringBuilder)
     */
    public StringBuilder printToBuffer( StringBuilder buf )
    {
        buf.append( '(' ).append( getAttribute() );
        buf.append( "-" );
        buf.append( this.dnAttributes );
        buf.append( "-EXTENSIBLE-" );
        buf.append( this.matchingRuleId );
        buf.append( "-" );
        buf.append( StringTools.utf8ToString( value ) );
        buf.append( "/" );
        buf.append( StringTools.dumpBytes( value ) );
        buf.append( ')' );

        if ( ( null != getAnnotations() ) && getAnnotations().containsKey( "count" ) )
        {
            buf.append( '[' );
            buf.append( getAnnotations().get( "count" ).toString() );
            buf.append( "] " );
        }
        else
        {
            buf.append( ' ' );
        }

        return buf;
    }

    
    /**
     * @see ExprNode#printRefinementToBuffer(StringBuilder)
     */
    public StringBuilder printRefinementToBuffer( StringBuilder buf ) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException( "ExtensibleNode can't be part of a refinement" );
    }


    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
    	StringBuilder buf = new StringBuilder();
        printToBuffer( buf );

        return ( buf.toString() );
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#accept(
     *      org.apache.directory.shared.ldap.filter.FilterVisitor)
     */
    public void accept( FilterVisitor visitor )
    {
        if ( visitor.canVisit( this ) )
        {
            visitor.visit( this );
        }
    }
}
