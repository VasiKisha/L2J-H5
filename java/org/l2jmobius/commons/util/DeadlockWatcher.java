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
package org.l2jmobius.commons.util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A dedicated thread to periodically monitor for deadlocks and provide detailed reports when detected.<br>
 * Allows for custom action when a deadlock is found by executing a user-defined callback.
 * @author Mobius
 */
public class DeadlockWatcher extends Thread
{
	private static final Logger LOGGER = Logger.getLogger(DeadlockWatcher.class.getName());
	
	private final Duration _checkInterval;
	private final Runnable _deadlockCallback;
	private final ThreadMXBean _threadMXBean;
	
	public DeadlockWatcher(Duration checkInterval, Runnable deadlockCallback)
	{
		super("DeadlockWatcher");
		_checkInterval = checkInterval;
		_deadlockCallback = deadlockCallback;
		_threadMXBean = ManagementFactory.getThreadMXBean();
	}
	
	@Override
	public void run()
	{
		LOGGER.info("DeadlockWatcher: Thread started.");
		
		while (!isInterrupted())
		{
			try
			{
				// Detect deadlocks and handle them if found.
				final long[] deadlockedThreadIds = _threadMXBean.findDeadlockedThreads();
				if (deadlockedThreadIds != null)
				{
					LOGGER.warning("DeadlockWatcher: Deadlock detected!");
					
					// Build detailed deadlock report.
					final ThreadInfo[] deadlockedThreadsInfo = _threadMXBean.getThreadInfo(deadlockedThreadIds, true, true);
					final StringBuilder report = new StringBuilder("DeadlockWatcher: Deadlock detected among the following threads:").append(System.lineSeparator());
					for (ThreadInfo threadInfo : deadlockedThreadsInfo)
					{
						// Append basic details of the thread.
						report.append("Thread Name: ").append(threadInfo.getThreadName()).append(System.lineSeparator()).append("Thread State: ").append(threadInfo.getThreadState()).append(System.lineSeparator()).append("Locked Resource: ").append(threadInfo.getLockName()).append(System.lineSeparator());
						
						// Append locked resources.
						if (threadInfo.getLockedSynchronizers().length > 0)
						{
							report.append("Locked Synchronizers:").append(System.lineSeparator());
							for (LockInfo lock : threadInfo.getLockedSynchronizers())
							{
								report.append("\t- ").append(lock).append(System.lineSeparator());
							}
						}
						
						if (threadInfo.getLockedMonitors().length > 0)
						{
							report.append("Locked Monitors:").append(System.lineSeparator());
							for (MonitorInfo monitor : threadInfo.getLockedMonitors())
							{
								report.append("\t- Locked on monitor: ").append(monitor.getClassName()).append(" at ").append(monitor.getLockedStackFrame()).append(System.lineSeparator());
							}
						}
						
						// Append the chain of deadlocked threads.
						ThreadInfo current = threadInfo;
						report.append("Deadlock Chain:").append(System.lineSeparator());
						while (current != null)
						{
							report.append("\t- ").append(current.getThreadName()).append(" is waiting to lock ").append(current.getLockInfo()).append(" held by ").append(current.getLockOwnerName()).append(System.lineSeparator());
							
							if (current.getLockOwnerId() == -1)
							{
								break; // End of deadlock chain.
							}
							
							current = _threadMXBean.getThreadInfo(current.getLockOwnerId());
						}
						
						report.append(System.lineSeparator());
					}
					
					LOGGER.warning(report.toString());
					
					// Invoke callback if set.
					if (_deadlockCallback != null)
					{
						try
						{
							_deadlockCallback.run();
						}
						catch (Exception e)
						{
							LOGGER.log(Level.SEVERE, "DeadlockWatcher: Exception in deadlock callback: ", e);
						}
					}
				}
				
				Thread.sleep(_checkInterval.toMillis());
			}
			catch (InterruptedException e)
			{
				LOGGER.info("DeadlockWatcher: Thread interrupted and will exit.");
				Thread.currentThread().interrupt(); // Set the interrupt flag.
				break;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "DeadlockWatcher: Exception during deadlock check: ", e);
			}
		}
		
		LOGGER.info("DeadlockWatcher: Thread terminated.");
	}
}
