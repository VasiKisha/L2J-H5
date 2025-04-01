/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.taskmanagers.tasks;

import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.taskmanagers.PersistentTaskManager;
import org.l2jmobius.gameserver.taskmanagers.PersistentTaskManager.ExecutedTask;

/**
 * Updates all data of Olympiad nobles in db
 * @author godson
 */
public class TaskOlympiadSave extends PersistentTask
{
	public static final String NAME = "olympiad_save";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		if (Olympiad.getInstance().inCompPeriod())
		{
			Olympiad.getInstance().saveOlympiadStatus();
			LOGGER.info("Olympiad System: Data updated.");
		}
	}
	
	@Override
	public void initializate()
	{
		super.initializate();
		PersistentTaskManager.addUniqueTask(NAME, PersistentTaskType.TYPE_FIXED_SHEDULED, "900000", "1800000", "");
	}
}
