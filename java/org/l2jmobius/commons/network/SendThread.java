package org.l2jmobius.commons.network;

import java.io.OutputStream;
import java.util.Queue;
import java.util.Set;

/**
 * @author Pantelis Andrianakis
 * @since June 22nd 2023
 * @param <E> extends NetClient
 */
public class SendThread<E extends NetClient> implements Runnable
{
	// Throttle packets sent per cycle to limit flooding from waiting one client.
	private static final int MAX_PACKETS_SENT_PER_CYCLE = 2000;
	
	private final Set<E> _pool;
	private boolean _idle;
	
	public SendThread(Set<E> pool)
	{
		_pool = pool;
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			// No need to iterate when pool is empty.
			if (!_pool.isEmpty())
			{
				_idle = true;
				
				// Iterate client pool.
				ITERATE: for (E client : _pool)
				{
					if (!client.isConnected())
					{
						_pool.remove(client);
						continue ITERATE;
					}
					
					try
					{
						final Queue<WritablePacket> packetQueue = client.getSendPacketQueue();
						if (packetQueue.isEmpty())
						{
							continue ITERATE;
						}
						
						final OutputStream outputStream = client.getOutputStream();
						SEND_CYCLE: for (int count = 0; count < MAX_PACKETS_SENT_PER_CYCLE; count++)
						{
							final WritablePacket writablePacket = packetQueue.poll();
							if (writablePacket == null)
							{
								continue ITERATE;
							}
							
							final byte[] sendableBytes = writablePacket.getSendableBytes(client.getEncryption());
							if (sendableBytes == null)
							{
								continue SEND_CYCLE;
							}
							
							// Send the packet data.
							outputStream.write(sendableBytes);
							outputStream.flush();
							
							// Run packet implementation.
							writablePacket.run();
							
							_idle = false;
						}
					}
					catch (Exception ignored)
					{
						_pool.remove(client);
						client.onDisconnection();
					}
				}
				
				// Prevent high CPU caused by repeatedly looping.
				try
				{
					Thread.sleep(_idle ? 10 : 1);
				}
				catch (Exception ignored)
				{
				}
			}
			else // Remain idle for 1 second.
			{
				// Prevent high CPU caused by repeatedly looping.
				try
				{
					Thread.sleep(1000);
				}
				catch (Exception ignored)
				{
				}
			}
		}
	}
}
