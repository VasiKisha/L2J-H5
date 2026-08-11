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
package ai.others.SoulTrader;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Asyatei AI.<br>
 * Carrying the Mark of Keucereus - Stage 2 opens the discounted list instead of the standard one.
 * @author Altur
 */
public class SoulTrader extends Script
{
	// NPC
	private static final int ASYATEI = 32546;
	// Item
	private static final int MARK_OF_KEUCEREUS_STAGE_2 = 13692;
	
	private SoulTrader()
	{
		addFirstTalkId(ASYATEI);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return player.getInventory().getItemByItemId(MARK_OF_KEUCEREUS_STAGE_2) == null ? "32546-01.html" : "32546-02.html";
	}
	
	public static void main(String[] args)
	{
		new SoulTrader();
	}
}
