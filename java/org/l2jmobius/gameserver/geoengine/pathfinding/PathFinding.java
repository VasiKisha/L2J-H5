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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.World;

/**
 * @author Mobius
 */
public class PathFinding
{
	private static final Logger LOGGER = Logger.getLogger(PathFinding.class.getName());
	
	private BufferInfo[] _allBuffers;
	
	protected PathFinding()
	{
		try
		{
			final String[] array = Config.PATHFIND_BUFFERS.split(";");
			
			_allBuffers = new BufferInfo[array.length];
			
			String buffer;
			String[] args;
			for (int i = 0; i < array.length; i++)
			{
				buffer = array[i];
				args = buffer.split("x");
				if (args.length != 2)
				{
					throw new Exception("Invalid buffer definition: " + buffer);
				}
				
				_allBuffers[i] = new BufferInfo(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "CellPathFinding: Problem during buffer init: " + e.getMessage(), e);
			throw new Error("CellPathFinding: load aborted");
		}
	}
	
	/**
	 * Converts a geodata position to a pathnode position.
	 * @param geoPos the geodata position
	 * @return the corresponding pathnode position
	 */
	public short getNodePos(int geoPos)
	{
		return (short) (geoPos >> 3); // OK?
	}
	
	/**
	 * Converts a node position to a pathnode block position.
	 * @param nodePos the node position
	 * @return the pathnode block position (range: 0 to 255)
	 */
	public short getNodeBlock(int nodePos)
	{
		return (short) (nodePos % 256);
	}
	
	/**
	 * Retrieves the X region of a node position.
	 * @param nodePos the node position
	 * @return the X region
	 */
	public byte getRegionX(int nodePos)
	{
		return (byte) ((nodePos >> 8) + World.TILE_X_MIN);
	}
	
	/**
	 * Retrieves the Y region of a node position.
	 * @param nodePos the node position
	 * @return the Y region
	 */
	public byte getRegionY(int nodePos)
	{
		return (byte) ((nodePos >> 8) + World.TILE_Y_MIN);
	}
	
	/**
	 * Calculates the region offset based on region X and Y.
	 * @param rx the X region
	 * @param ry the Y region
	 * @return the region offset
	 */
	public short getRegionOffset(byte rx, byte ry)
	{
		return (short) ((rx << 5) + ry);
	}
	
	/**
	 * Converts a pathnode X coordinate to a world X coordinate.
	 * @param nodeX the pathnode X coordinate
	 * @return the corresponding world X coordinate
	 */
	public int calculateWorldX(short nodeX)
	{
		return World.WORLD_X_MIN + (nodeX * 128) + 48;
	}
	
	/**
	 * Converts a pathnode Y coordinate to a world Y coordinate.
	 * @param nodeY the pathnode Y coordinate
	 * @return the corresponding world Y coordinate
	 */
	public int calculateWorldY(short nodeY)
	{
		return World.WORLD_Y_MIN + (nodeY * 128) + 48;
	}
	
	/**
	 * Finds a path between two world positions considering geodata and an instance.
	 * @param x the starting X coordinate
	 * @param y the starting Y coordinate
	 * @param z the starting Z coordinate
	 * @param tx the target X coordinate
	 * @param ty the target Y coordinate
	 * @param tz the target Z coordinate
	 * @param instanceId the instance ID to consider for pathfinding
	 * @param playable whether the pathfinding is for a playable character
	 * @return a list of pathnodes forming the path, or {@code null} if no path is found
	 */
	public List<GeoLocation> findPath(int x, int y, int z, int tx, int ty, int tz, int instanceId, boolean playable)
	{
		final GeoEngine geoEngine = GeoEngine.getInstance();
		final int gx = geoEngine.getGeoX(x);
		final int gy = geoEngine.getGeoY(y);
		if (!geoEngine.hasGeo(x, y))
		{
			return null;
		}
		
		final int gz = geoEngine.getHeight(x, y, z);
		final int gtx = geoEngine.getGeoX(tx);
		final int gty = geoEngine.getGeoY(ty);
		if (!geoEngine.hasGeo(tx, ty))
		{
			return null;
		}
		
		final int gtz = geoEngine.getHeight(tx, ty, tz);
		final NodeBuffer buffer = alloc(64 + (2 * Math.max(Math.abs(gx - gtx), Math.abs(gy - gty))));
		if (buffer == null)
		{
			return null;
		}
		
		List<GeoLocation> path = null;
		try
		{
			final GeoNode result = buffer.findPath(gx, gy, gz, gtx, gty, gtz);
			if (result == null)
			{
				return null;
			}
			
			path = constructPath(result);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "CellPathFinding: Problem finding path: " + e.getMessage(), e);
			return null;
		}
		finally
		{
			buffer.free();
		}
		
		if ((path.size() < 3) || (Config.MAX_POSTFILTER_PASSES <= 0))
		{
			return path;
		}
		
		int pass = 0;
		boolean remove;
		do
		{
			pass++;
			remove = false;
			int currentX = x;
			int currentY = y;
			int currentZ = z;
			final int size = path.size();
			final List<GeoLocation> newPath = new ArrayList<>(size);
			for (int i = 0; i < (size - 1); i++)
			{
				final GeoLocation locMiddle = path.get(i);
				final GeoLocation locEnd = path.get(i + 1);
				if (geoEngine.canMoveToTarget(currentX, currentY, currentZ, locEnd.getX(), locEnd.getY(), locEnd.getZ(), instanceId))
				{
					remove = true;
				}
				else
				{
					newPath.add(locMiddle);
					currentX = locMiddle.getX();
					currentY = locMiddle.getY();
					currentZ = locMiddle.getZ();
				}
			}
			
			// Add the last node
			newPath.add(path.get(size - 1));
			path = newPath;
		}
		while (playable && remove && (path.size() > 2) && (pass < Config.MAX_POSTFILTER_PASSES));
		
		return path;
	}
	
	/**
	 * Constructs a path from a given node by traversing its parent nodes.
	 * @param node the starting node
	 * @return the constructed path as a list of node locations
	 */
	private List<GeoLocation> constructPath(GeoNode node)
	{
		final List<GeoLocation> path = new ArrayList<>();
		int previousDirectionX = Integer.MIN_VALUE;
		int previousDirectionY = Integer.MIN_VALUE;
		int directionX;
		int directionY;
		
		GeoNode tempNode = node;
		while (tempNode.getParent() != null)
		{
			if (!Config.ADVANCED_DIAGONAL_STRATEGY && (tempNode.getParent().getParent() != null))
			{
				final int tmpX = tempNode.getLocation().getNodeX() - tempNode.getParent().getParent().getLocation().getNodeX();
				final int tmpY = tempNode.getLocation().getNodeY() - tempNode.getParent().getParent().getLocation().getNodeY();
				if (Math.abs(tmpX) == Math.abs(tmpY))
				{
					directionX = tmpX;
					directionY = tmpY;
				}
				else
				{
					directionX = tempNode.getLocation().getNodeX() - tempNode.getParent().getLocation().getNodeX();
					directionY = tempNode.getLocation().getNodeY() - tempNode.getParent().getLocation().getNodeY();
				}
			}
			else
			{
				directionX = tempNode.getLocation().getNodeX() - tempNode.getParent().getLocation().getNodeX();
				directionY = tempNode.getLocation().getNodeY() - tempNode.getParent().getLocation().getNodeY();
			}
			
			// Only add a new route point if moving direction changes.
			if ((directionX != previousDirectionX) || (directionY != previousDirectionY))
			{
				previousDirectionX = directionX;
				previousDirectionY = directionY;
				
				path.addFirst(tempNode.getLocation());
				tempNode.setLoc(null);
			}
			
			tempNode = tempNode.getParent();
		}
		
		return path;
	}
	
	/**
	 * Allocates a buffer for pathfinding based on the specified size.
	 * @param size the required size of the buffer
	 * @return a locked buffer, or {@code null} if no suitable buffer is available
	 */
	private NodeBuffer alloc(int size)
	{
		NodeBuffer current = null;
		
		for (BufferInfo info : _allBuffers)
		{
			if (info.mapSize >= size)
			{
				for (NodeBuffer buffer : info.buffers)
				{
					if (buffer.lock())
					{
						current = buffer;
						break;
					}
				}
				if (current != null)
				{
					break;
				}
				
				// Not found, allocate temporary buffer.
				current = new NodeBuffer(info.mapSize);
				current.lock();
				if (info.buffers.size() < info.count)
				{
					info.buffers.add(current);
					break;
				}
			}
		}
		
		return current;
	}
	
	/**
	 * Represents buffer information for managing pathfinding buffers.
	 */
	private static class BufferInfo
	{
		final int mapSize;
		final int count;
		final List<NodeBuffer> buffers;
		
		public BufferInfo(int size, int cnt)
		{
			mapSize = size;
			count = cnt;
			buffers = Collections.synchronizedList(new ArrayList<>(count));
		}
	}
	
	public static PathFinding getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PathFinding INSTANCE = new PathFinding();
	}
}
