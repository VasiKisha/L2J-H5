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
package org.l2jmobius.gameserver.model.actor.holders.player;

import java.util.regex.Matcher;

import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;

/**
 * This class will hold the information of the player classes.
 * @author Zoey76
 */
public class ClassInfoHolder
{
	private final PlayerClass _playerClass;
	private final String _className;
	private final PlayerClass _parentClass;
	
	/**
	 * Constructor for ClassInfo.
	 * @param playerClass the PlayerClass.
	 * @param className the in game class name.
	 * @param parentClass the parent PlayerClass for the given {@code playerClass}.
	 */
	public ClassInfoHolder(PlayerClass playerClass, String className, PlayerClass parentClass)
	{
		_playerClass = playerClass;
		_className = className;
		_parentClass = parentClass;
	}
	
	/**
	 * @return the PlayerClass.
	 */
	public PlayerClass getPlayerClass()
	{
		return _playerClass;
	}
	
	/**
	 * @return the hardcoded in-game class name.
	 */
	public String getClassName()
	{
		return _className;
	}
	
	/**
	 * @return the class client Id.
	 */
	private int getClassClientId()
	{
		int classClientId = _playerClass.getId();
		if ((classClientId >= 0) && (classClientId <= 57))
		{
			classClientId += 247;
		}
		else if ((classClientId >= 88) && (classClientId <= 118))
		{
			classClientId += 1071;
		}
		else if ((classClientId >= 123) && (classClientId <= 136))
		{
			classClientId += 1438;
		}
		return classClientId;
	}
	
	/**
	 * @return the class client Id formatted to be displayed on a HTML.
	 */
	public String getClientCode()
	{
		return "&$" + getClassClientId() + ";";
	}
	
	/**
	 * @return the escaped class client Id formatted to be displayed on a HTML.
	 */
	public String getEscapedClientCode()
	{
		return Matcher.quoteReplacement(getClientCode());
	}
	
	/**
	 * @return the parent PlayerClass.
	 */
	public PlayerClass getParentClass()
	{
		return _parentClass;
	}
}