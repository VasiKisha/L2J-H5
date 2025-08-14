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
package org.l2jmobius.gameserver.taskmanagers.tasks;

import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.managers.RaidBossPointsManager;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.taskmanagers.PersistentTaskManager;
import org.l2jmobius.gameserver.taskmanagers.PersistentTaskManager.ExecutedTask;

public class TaskRaidPointsReset extends PersistentTask
{
	public static final String NAME = "raid_points_reset";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		final Calendar cal = Calendar.getInstance();
		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
		{
			// reward clan reputation points
			final Map<Integer, Integer> rankList = RaidBossPointsManager.getInstance().getRankList();
			for (Clan c : ClanTable.getInstance().getClans())
			{
				for (Entry<Integer, Integer> entry : rankList.entrySet())
				{
					if ((entry.getValue() <= 100) && c.isMember(entry.getKey()))
					{
						int reputation = 0;
						switch (entry.getValue())
						{
							case 1:
							{
								reputation = Config.RAID_RANKING_1ST;
								break;
							}
							case 2:
							{
								reputation = Config.RAID_RANKING_2ND;
								break;
							}
							case 3:
							{
								reputation = Config.RAID_RANKING_3RD;
								break;
							}
							case 4:
							{
								reputation = Config.RAID_RANKING_4TH;
								break;
							}
							case 5:
							{
								reputation = Config.RAID_RANKING_5TH;
								break;
							}
							case 6:
							{
								reputation = Config.RAID_RANKING_6TH;
								break;
							}
							case 7:
							{
								reputation = Config.RAID_RANKING_7TH;
								break;
							}
							case 8:
							{
								reputation = Config.RAID_RANKING_8TH;
								break;
							}
							case 9:
							{
								reputation = Config.RAID_RANKING_9TH;
								break;
							}
							case 10:
							{
								reputation = Config.RAID_RANKING_10TH;
								break;
							}
							default:
							{
								if (entry.getValue() <= 50)
								{
									reputation = Config.RAID_RANKING_UP_TO_50TH;
								}
								else
								{
									reputation = Config.RAID_RANKING_UP_TO_100TH;
								}
								break;
							}
						}
						
						c.addReputationScore(reputation);
					}
				}
			}
			
			RaidBossPointsManager.getInstance().cleanUp();
			LOGGER.info("Raid Points Reset Global Task: launched.");
		}
	}
	
	@Override
	public void initializate()
	{
		super.initializate();
		PersistentTaskManager.addUniqueTask(NAME, PersistentTaskType.TYPE_GLOBAL_TASK, "1", "00:10:00", "");
	}
}
