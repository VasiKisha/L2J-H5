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
package org.l2jmobius.gameserver.ai;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExMoveToLocationAirShip;
import org.l2jmobius.gameserver.network.serverpackets.ExStopMoveAirShip;

/**
 * @author DS, Mobius
 */
public class AirShipAI extends CreatureAI
{
	public AirShipAI(AirShip airShip)
	{
		super(airShip);
	}
	
	@Override
	protected void moveTo(int x, int y, int z)
	{
		if (_actor.isMovementDisabled())
		{
			return;
		}
		
		_actor.moveToLocation(x, y, z, 0);
		_actor.broadcastPacket(new ExMoveToLocationAirShip(getActor()));
	}
	
	@Override
	public void clientStopMoving(Location loc)
	{
		if (_actor.isMoving())
		{
			_actor.stopMove(loc);
			_actor.broadcastPacket(new ExStopMoveAirShip(getActor()));
			return;
		}
		
		if (loc != null)
		{
			_actor.broadcastPacket(new ExStopMoveAirShip(getActor()));
		}
	}
	
	@Override
	public void describeStateToPlayer(Player player)
	{
		if (!_actor.isMoving())
		{
			return;
		}
		
		player.sendPacket(new ExMoveToLocationAirShip(getActor()));
	}
	
	@Override
	public void setIntentionAttack(WorldObject target)
	{
	}
	
	@Override
	public void setIntentionCast(Skill skill, WorldObject target)
	{
	}
	
	@Override
	public void setIntentionFollow(WorldObject target)
	{
	}
	
	@Override
	public void setIntentionPickUp(WorldObject item)
	{
	}
	
	@Override
	public void setIntentionInteract(WorldObject object)
	{
	}
	
	@Override
	public void notifyActionAttacked(WorldObject attacker)
	{
	}
	
	@Override
	public void notifyActionAggression(WorldObject target, int aggro)
	{
	}
	
	@Override
	public void notifyActionStunned()
	{
	}
	
	@Override
	public void notifyActionSleeping()
	{
	}
	
	@Override
	public void notifyActionRooted()
	{
	}
	
	@Override
	public void notifyActionForgetObject(WorldObject object)
	{
	}
	
	@Override
	public void notifyActionCancel()
	{
	}
	
	@Override
	public void notifyActionDeath()
	{
	}
	
	@Override
	public void notifyActionFakeDeath()
	{
	}
	
	@Override
	public void notifyActionFinishCasting()
	{
	}
	
	@Override
	protected void clientActionFailed()
	{
	}
	
	@Override
	public void moveToPawn(WorldObject pawn, int offset)
	{
	}
	
	@Override
	protected void clientStoppedMoving()
	{
	}
	
	@Override
	public AirShip getActor()
	{
		return (AirShip) _actor;
	}
}
