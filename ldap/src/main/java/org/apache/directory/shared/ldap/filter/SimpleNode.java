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

import org.apache.directory.shared.ldap.constants.SchemaConstants;


/**
 * A simple assertion value node.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class SimpleNode extends LeafNode
{
	/** the value */
    protected Object value;

    /** Constants for comparisons */
    public final static boolean EVAL_GREATER = true;
    public final static boolean EVAL_LESSER = false;

    /**
     * Creates a new SimpleNode object.
     * 
     * @param attribute the attribute name
     * @param value the value to test for
     */
    public SimpleNode( String attribute, byte[] value )
    {
        super( attribute );
        this.value = value;
    }


    /**
     * Creates a new SimpleNode object.
     * 
     * @param attribute the attribute name
     * @param value the value to test for
     */
    public SimpleNode( String attribute, String value )
    {
        super( attribute );
        this.value = value;
    }


    /**
     * Gets the value.
     * 
     * @return the value
     */
    public final Object getValue()
    {
        return value;
    }


    /**
     * Sets the value of this node.
     * 
     * @param value the value for this node
     */
    public void setValue( Object value )
    {
        this.value = value;
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#printToBuffer(
     *      java.lang.StringBuilder)
     */
    public StringBuilder printToBuffer( StringBuilder buf )
    {
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
        if ( getAttribute() == null || !SchemaConstants.OBJECT_CLASS_AT.equalsIgnoreCase( getAttribute() ) )
        {
            throw new UnsupportedOperationException( "Invalid attribute " + getAttribute() + " for a refinement" );
        }

        buf.append( "item: " ).append( value );

        return buf;
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


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof SimpleNode ) )
        {
            return false;
        }

        SimpleNode otherNode = (SimpleNode)other;

        return ( value == null ? otherNode.value == null : value.equals( otherNode.value ) );
    }
}
