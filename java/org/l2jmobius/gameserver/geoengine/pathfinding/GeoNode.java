/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.geoengine.pathfinding;

/**
 * @author Mobius
 */
public class GeoNode
{
	private GeoLocation _location;
	private GeoNode _parent;
	private GeoNode _next = null;
	private boolean _isInUse = true;
	private float _cost = -1000;
	
	public GeoNode(GeoLocation location)
	{
		_location = location;
	}
	
	public void setParent(GeoNode parent)
	{
		_parent = parent;
	}
	
	public GeoNode getParent()
	{
		return _parent;
	}
	
	public GeoLocation getLocation()
	{
		return _location;
	}
	
	public void setLoc(GeoLocation location)
	{
		_location = location;
	}
	
	public boolean isInUse()
	{
		return _isInUse;
	}
	
	public void setInUse()
	{
		_isInUse = true;
	}
	
	public GeoNode getNext()
	{
		return _next;
	}
	
	public void setNext(GeoNode next)
	{
		_next = next;
	}
	
	public float getCost()
	{
		return _cost;
	}
	
	public void setCost(double cost)
	{
		_cost = (float) cost;
	}
	
	public void free()
	{
		setParent(null);
		_cost = -1000;
		_isInUse = false;
		_next = null;
	}
	
	@Override
	public int hashCode()
	{
		return (31 * 1) + ((_location == null) ? 0 : _location.hashCode());
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (!(obj instanceof GeoNode))
		{
			return false;
		}
		final GeoNode other = (GeoNode) obj;
		if (_location == null)
		{
			if (other._location != null)
			{
				return false;
			}
		}
		else if (!_location.equals(other._location))
		{
			return false;
		}
		return true;
	}
}
