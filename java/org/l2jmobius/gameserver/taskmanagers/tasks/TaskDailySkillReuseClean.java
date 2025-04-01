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

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.taskmanagers.PersistentTaskManager;
import org.l2jmobius.gameserver.taskmanagers.PersistentTaskManager.ExecutedTask;

public class TaskDailySkillReuseClean extends PersistentTask
{
	private static final String NAME = "daily_skill_clean";
	
	private static final int[] _daily_skills =
	{
		2510,
		22180
	};
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			for (int skill_id : _daily_skills)
			{
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_skills_save WHERE skill_id=?;"))
				{
					ps.setInt(1, skill_id);
					ps.execute();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.severe(getClass().getSimpleName() + ": Could not reset daily skill reuse: " + e);
		}
		LOGGER.info("Daily skill reuse cleaned.");
	}
	
	@Override
	public void initializate()
	{
		super.initializate();
		PersistentTaskManager.addUniqueTask(NAME, PersistentTaskType.TYPE_GLOBAL_TASK, "1", "06:30:00", "");
	}
}
