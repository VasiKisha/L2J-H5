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

import java.util.concurrent.locks.ReentrantLock;

import org.l2jmobius.Config;

/**
 * @author Mobius
 */
public class NodeBuffer
{
	private static final int MAX_ITERATIONS = 3500;
	
	private final ReentrantLock _lock = new ReentrantLock();
	private final int _mapSize;
	private final GeoNode[][] _buffer;
	
	private int _baseX = 0;
	private int _baseY = 0;
	
	private int _targetX = 0;
	private int _targetY = 0;
	private int _targetZ = 0;
	
	private GeoNode _current = null;
	
	public NodeBuffer(int size)
	{
		_mapSize = size;
		_buffer = new GeoNode[_mapSize][_mapSize];
	}
	
	public final boolean lock()
	{
		return _lock.tryLock();
	}
	
	public GeoNode findPath(int x, int y, int z, int tx, int ty, int tz)
	{
		_baseX = x + ((tx - x - _mapSize) / 2); // Middle of the line (x,y) - (tx,ty).
		_baseY = y + ((ty - y - _mapSize) / 2); // Will be in the center of the buffer.
		_targetX = tx;
		_targetY = ty;
		_targetZ = tz;
		_current = getNode(x, y, z);
		_current.setCost(getCost(x, y, z, Config.HIGH_WEIGHT));
		
		for (int count = 0; count < MAX_ITERATIONS; count++)
		{
			if ((_current.getLocation().getNodeX() == _targetX) && (_current.getLocation().getNodeY() == _targetY) && (Math.abs(_current.getLocation().getZ() - _targetZ) < 64))
			{
				return _current; // Found.
			}
			
			getNeighbors();
			
			final GeoNode nextCellNode = _current.getNext();
			if (nextCellNode == null)
			{
				return null; // No more ways.
			}
			
			_current = nextCellNode;
		}
		return null;
	}
	
	public void free()
	{
		_current = null;
		
		GeoNode node;
		for (int i = 0; i < _mapSize; i++)
		{
			for (int j = 0; j < _mapSize; j++)
			{
				node = _buffer[i][j];
				if (node != null)
				{
					node.free();
				}
			}
		}
		
		_lock.unlock();
	}
	
	private void getNeighbors()
	{
		if (_current.getLocation().canGoNone())
		{
			return;
		}
		
		final int x = _current.getLocation().getNodeX();
		final int y = _current.getLocation().getNodeY();
		final int z = _current.getLocation().getZ();
		
		GeoNode nodeE = null;
		GeoNode nodeS = null;
		GeoNode nodeW = null;
		GeoNode nodeN = null;
		
		// East
		if (_current.getLocation().canGoEast())
		{
			nodeE = addNode(x + 1, y, z, false);
		}
		
		// South
		if (_current.getLocation().canGoSouth())
		{
			nodeS = addNode(x, y + 1, z, false);
		}
		
		// West
		if (_current.getLocation().canGoWest())
		{
			nodeW = addNode(x - 1, y, z, false);
		}
		
		// North
		if (_current.getLocation().canGoNorth())
		{
			nodeN = addNode(x, y - 1, z, false);
		}
		
		if (!Config.ADVANCED_DIAGONAL_STRATEGY)
		{
			return;
		}
		
		// SouthEast
		if ((nodeE != null) && (nodeS != null) && nodeE.getLocation().canGoSouth() && nodeS.getLocation().canGoEast())
		{
			addNode(x + 1, y + 1, z, true);
		}
		
		// SouthWest
		if ((nodeS != null) && (nodeW != null) && nodeW.getLocation().canGoSouth() && nodeS.getLocation().canGoWest())
		{
			addNode(x - 1, y + 1, z, true);
		}
		
		// NorthEast
		if ((nodeN != null) && (nodeE != null) && nodeE.getLocation().canGoNorth() && nodeN.getLocation().canGoEast())
		{
			addNode(x + 1, y - 1, z, true);
		}
		
		// NorthWest
		if ((nodeN != null) && (nodeW != null) && nodeW.getLocation().canGoNorth() && nodeN.getLocation().canGoWest())
		{
			addNode(x - 1, y - 1, z, true);
		}
	}
	
	private GeoNode getNode(int x, int y, int z)
	{
		final int aX = x - _baseX;
		if ((aX < 0) || (aX >= _mapSize))
		{
			return null;
		}
		
		final int aY = y - _baseY;
		if ((aY < 0) || (aY >= _mapSize))
		{
			return null;
		}
		
		GeoNode result = _buffer[aX][aY];
		if (result == null)
		{
			result = new GeoNode(new GeoLocation(x, y, z));
			_buffer[aX][aY] = result;
		}
		else if (!result.isInUse())
		{
			result.setInUse();
			// Re-init node if needed.
			if (result.getLocation() != null)
			{
				result.getLocation().set(x, y, z);
			}
			else
			{
				result.setLoc(new GeoLocation(x, y, z));
			}
		}
		
		return result;
	}
	
	private GeoNode addNode(int x, int y, int z, boolean diagonal)
	{
		final GeoNode newNode = getNode(x, y, z);
		if (newNode == null)
		{
			return null;
		}
		if (newNode.getCost() >= 0)
		{
			return newNode;
		}
		
		final int geoZ = newNode.getLocation().getZ();
		
		final int stepZ = Math.abs(geoZ - _current.getLocation().getZ());
		float weight = diagonal ? Config.DIAGONAL_WEIGHT : Config.LOW_WEIGHT;
		
		if (!newNode.getLocation().canGoAll() || (stepZ > 16))
		{
			weight = Config.HIGH_WEIGHT;
		}
		else if (isHighWeight(x + 1, y, geoZ))
		{
			weight = Config.MEDIUM_WEIGHT;
		}
		else if (isHighWeight(x - 1, y, geoZ))
		{
			weight = Config.MEDIUM_WEIGHT;
		}
		else if (isHighWeight(x, y + 1, geoZ))
		{
			weight = Config.MEDIUM_WEIGHT;
		}
		else if (isHighWeight(x, y - 1, geoZ))
		{
			weight = Config.MEDIUM_WEIGHT;
		}
		
		newNode.setParent(_current);
		newNode.setCost(getCost(x, y, geoZ, weight));
		
		GeoNode node = _current;
		int count = 0;
		while ((node.getNext() != null) && (count < (MAX_ITERATIONS * 4)))
		{
			count++;
			if (node.getNext().getCost() > newNode.getCost())
			{
				// Insert node into a chain.
				newNode.setNext(node.getNext());
				break;
			}
			node = node.getNext();
		}
		if (count == (MAX_ITERATIONS * 4))
		{
			System.err.println("Pathfinding: too long loop detected, cost:" + newNode.getCost());
		}
		
		node.setNext(newNode); // Add last.
		
		return newNode;
	}
	
	private boolean isHighWeight(int x, int y, int z)
	{
		final GeoNode result = getNode(x, y, z);
		return (result == null) || !result.getLocation().canGoAll() || (Math.abs(result.getLocation().getZ() - z) > 16);
	}
	
	private double getCost(int x, int y, int z, float weight)
	{
		final int dX = x - _targetX;
		final int dY = y - _targetY;
		final int dZ = z - _targetZ;
		// Math.abs(dx) + Math.abs(dy) + Math.abs(dz) / 16
		double result = Math.sqrt((dX * dX) + (dY * dY) + ((dZ * dZ) / 256.0));
		if (result > weight)
		{
			result += weight;
		}
		
		if (result > Float.MAX_VALUE)
		{
			result = Float.MAX_VALUE;
		}
		
		return result;
	}
}
