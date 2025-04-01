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
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.PetDataTable;

public class PetNameTable
{
	private static final Logger LOGGER = Logger.getLogger(PetNameTable.class.getName());
	
	public static PetNameTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	public boolean doesPetNameExist(String name, int petNpcId)
	{
		boolean result = true;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT name FROM pets p, items i WHERE p.item_obj_id = i.object_id AND name=? AND i.item_id IN (?)"))
		{
			ps.setString(1, name);
			final StringBuilder cond = new StringBuilder();
			if (!cond.toString().isEmpty())
			{
				cond.append(", ");
			}
			
			cond.append(PetDataTable.getInstance().getPetItemsByNpc(petNpcId));
			ps.setString(2, cond.toString());
			try (ResultSet rs = ps.executeQuery())
			{
				result = rs.next();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not check existing petname:" + e.getMessage(), e);
		}
		return result;
	}
	
	public boolean isValidPetName(String name)
	{
		boolean result = true;
		if (!isAlphaNumeric(name))
		{
			return result;
		}
		
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.PET_NAME_TEMPLATE);
		}
		catch (PatternSyntaxException e) // case of illegal pattern
		{
			LOGGER.warning(getClass().getSimpleName() + ": Pet name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}
		final Matcher regexp = pattern.matcher(name);
		if (!regexp.matches())
		{
			result = false;
		}
		return result;
	}
	
	private boolean isAlphaNumeric(String text)
	{
		boolean result = true;
		final char[] chars = text.toCharArray();
		for (char aChar : chars)
		{
			if (!Character.isLetterOrDigit(aChar))
			{
				result = false;
				break;
			}
		}
		return result;
	}
	
	private static class SingletonHolder
	{
		protected static final PetNameTable INSTANCE = new PetNameTable();
	}
}
