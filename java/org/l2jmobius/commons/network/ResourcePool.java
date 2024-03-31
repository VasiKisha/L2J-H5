/*
 * Copyright © 2019-2021 Async-mmocore
 *
 * This file is part of the Async-mmocore project.
 *
 * Async-mmocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Async-mmocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.commons.network;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.network.internal.BufferPool;

/**
 * Manages pools of ByteBuffers for efficient resource allocation and reuse.<br>
 * This class maintains a collection of buffer pools, each optimized for different buffer sizes, to reduce the overhead of buffer allocation.
 * @author JoeAlisson
 */
public class ResourcePool
{
	// private static final Logger LOGGER = Logger.getLogger(ResourcePool.class.getName());
	
	private final Map<Integer, BufferPool> _bufferPools;
	private int[] _bufferSizes;
	private int _bufferSegmentSize;
	
	public ResourcePool()
	{
		_bufferSizes = new int[]
		{
			2,
			64
		};
		_bufferPools = new HashMap<>(4);
		_bufferSegmentSize = 64;
	}
	
	public ByteBuffer getHeaderBuffer()
	{
		return getSizedBuffer(ConnectionConfig.HEADER_SIZE);
	}
	
	public ByteBuffer getSegmentBuffer()
	{
		return getSizedBuffer(_bufferSegmentSize);
	}
	
	public ByteBuffer getBuffer(int size)
	{
		return getSizedBuffer(determineBufferSize(size));
	}
	
	public ByteBuffer recycleAndGetNew(ByteBuffer buffer, int newSize)
	{
		final int bufferSize = determineBufferSize(newSize);
		if (buffer != null)
		{
			if (buffer.clear().limit() == bufferSize)
			{
				return buffer.limit(newSize);
			}
			
			recycleBuffer(buffer);
		}
		
		return getSizedBuffer(bufferSize).limit(newSize);
	}
	
	private ByteBuffer getSizedBuffer(int size)
	{
		final BufferPool pool = _bufferPools.get(size);
		ByteBuffer buffer = null;
		if (pool != null)
		{
			buffer = pool.get();
		}
		if (buffer == null)
		{
			buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN);
		}
		return buffer;
	}
	
	private int determineBufferSize(int size)
	{
		for (int bufferSize : _bufferSizes)
		{
			if (size <= bufferSize)
			{
				return bufferSize;
			}
		}
		
		// LOGGER.warning("There is no buffer pool handling buffer size " + size);
		return size;
	}
	
	public void recycleBuffer(ByteBuffer buffer)
	{
		if (buffer != null)
		{
			final BufferPool pool = _bufferPools.get(buffer.capacity());
			if ((pool == null) || !pool.recycle(buffer))
			{
				// LOGGER.warning("buffer was not recycled " + buffer + " in pool " + pool);
			}
		}
	}
	
	public int getSegmentSize()
	{
		return _bufferSegmentSize;
	}
	
	public void addBufferPool(int bufferSize, BufferPool bufferPool)
	{
		_bufferPools.putIfAbsent(bufferSize, bufferPool);
	}
	
	public int bufferPoolSize()
	{
		return _bufferPools.size();
	}
	
	public void initializeBuffers(float initBufferPoolFactor)
	{
		if (initBufferPoolFactor > 0)
		{
			_bufferPools.values().forEach(pool -> pool.initialize(initBufferPoolFactor));
		}
		_bufferSizes = _bufferPools.keySet().stream().sorted().mapToInt(Integer::intValue).toArray();
	}
	
	public void setBufferSegmentSize(int size)
	{
		_bufferSegmentSize = size;
	}
	
	public String stats()
	{
		final StringBuilder sb = new StringBuilder();
		for (BufferPool pool : _bufferPools.values())
		{
			sb.append(pool.toString()).append(System.lineSeparator());
		}
		return sb.toString();
	}
}
