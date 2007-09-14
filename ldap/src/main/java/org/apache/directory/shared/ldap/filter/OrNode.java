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


import java.util.List;

/**
 * Node representing an OR connector in a filter operation
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 517453 $
 */
public class OrNode extends BranchNode
{
    /**
     * Creates a OrNode using a logical operator and a list of children.
     * 
     * @param childList the child nodes under this branch node.
     */
    public OrNode( List<ExprNode> children)
    {
        super( children );
    }


    /**
     * Creates an empty OrNode
     */
    public OrNode()
    {
        this( null );
    }




    /**
     * Gets the operator for this branch node.
     * 
     * @return the operator constant.
     */
    public AssertionEnum getOperator()
    {
        return AssertionEnum.OR;
    }


    /**
     * Tests whether or not this node is a disjunction (a OR'ed branch).
     * 
     * @return true if the operation is a OR, false otherwise.
     */
    public boolean isDisjunction()
    {
        return true;
    }


    /**
     * Tests whether or not this node is a conjunction (a AND'ed branch).
     * 
     * @return true if the operation is a AND, false otherwise.
     */
    public boolean isConjunction()
    {
        return false;
    }


    /**
     * Tests whether or not this node is a negation (a NOT'ed branch).
     * 
     * @return true if the operation is a NOT, false otherwise.
     */
    public boolean isNegation()
    {
        return false;
    }


    /**
     * Recursively prints the String representation of this node and all its
     * descendents to a buffer.
     * 
     * @see org.apache.directory.shared.ldap.filter.ExprNode#printToBuffer(java.lang.StringBuffer)
     */
    public StringBuilder printToBuffer( StringBuilder buf )
    {
        buf.append( "(|" );

        for ( ExprNode node:children )
        {
        	node.printToBuffer( buf );
        }
        
        buf.append( ')' );
        
        if ( ( null != getAnnotations() ) && getAnnotations().containsKey( "count" ) )
        {
            buf.append( '[' );
            buf.append( ( ( Long ) getAnnotations().get( "count" ) ).toString() );
            buf.append( "] " );
        }
        else
        {
            buf.append( ' ' );
        }

        return buf;
    }

    
    /**
     * @see ExprNode#printRefinementToBuffer(StringBuffer)
     */
    public StringBuilder printRefinementToBuffer( StringBuilder buf ) throws UnsupportedOperationException
    {
        buf.append( "or: {" );
        boolean isFirst = true;
        
        for ( ExprNode node:children )
        {
        	if ( isFirst )
        	{
        		isFirst = false;
        	}
        	else
        	{
        		buf.append( ", " );
        	}
        	
            node.printRefinementToBuffer( buf );
        }
        
        buf.append( '}' );
        
        return buf;
    }

    /**
     * Gets the recursive prefix string represent of the filter from this node
     * down.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "OR" );
        
        if ( ( null != getAnnotations() ) && getAnnotations().containsKey( "count" ) )
        {
            buf.append( '[' );
            buf.append( ( ( Long ) getAnnotations().get( "count" ) ) );
            buf.append( "] " );
        }
        else
        {
            buf.append( ' ' );
        }

        return buf.toString();
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#accept(
     *      org.apache.directory.shared.ldap.filter.FilterVisitor)
     */
    public void accept( FilterVisitor visitor )
    {
        if ( visitor.isPrefix() )
        {
            List<ExprNode> children = visitor.getOrder( this, this.children );

            if ( visitor.canVisit( this ) )
            {
                visitor.visit( this );
            }

            for ( ExprNode node:children )
            {
                node.accept( visitor );
            }
        }
        else
        {
            List<ExprNode> children = visitor.getOrder( this, this.children );

            for ( ExprNode node:children )
            {
                node.accept( visitor );
            }

            if ( visitor.canVisit( this ) )
            {
                visitor.visit( this );
            }
        }
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        int hash = 7;
        hash = hash*31 + AssertionEnum.OR.hashCode();
        hash = hash*31 + ( annotations == null ? 0 : annotations.hashCode() );
        return hash;
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

        if ( !( other instanceof OrNode ) )
        {
            return false;
        }

        OrNode otherExprNode = ( OrNode ) other;

        List<ExprNode> otherChildren = otherExprNode.getChildren();

        if ( otherChildren == children )
        {
            return true;
        }

        if ( children.size() != otherChildren.size() )
        {
        	return false;
        }
        
        for ( int i = 0; i < children.size(); i++ )
        {
        	ExprNode child = children.get( i );
        	ExprNode otherChild = children.get( i );
        	
        	if ( !child.equals( otherChild ) )
        	{
        		return false;
        	}
        }
        
        return true;
    }
}
