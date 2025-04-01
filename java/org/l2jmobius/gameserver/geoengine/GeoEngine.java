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
package org.l2jmobius.gameserver.geoengine;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.FenceData;
import org.l2jmobius.gameserver.geoengine.geodata.Cell;
import org.l2jmobius.gameserver.geoengine.geodata.IRegion;
import org.l2jmobius.gameserver.geoengine.geodata.regions.NullRegion;
import org.l2jmobius.gameserver.geoengine.geodata.regions.Region;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator2D;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator3D;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.util.GeoUtils;

/**
 * The {@code GeoEngine} class is responsible for managing geospatial data used in the game.<br>
 * It handles geodata loading, movement validation, line-of-sight (LOS) checks and other spatial calculations related to the game's 3D world.
 * <p>
 * Key functionalities include:
 * <ul>
 * <li>Loading geodata files for specific regions.</li>
 * <li>Converting between world and geodata coordinates.</li>
 * <li>Validating movement paths and detecting obstacles.</li>
 * <li>Checking line-of-sight (LOS) between two positions.</li>
 * <li>Retrieving terrain height and handling elevation-related calculations.</li>
 * </ul>
 * @author -Nemesiss-, HorridoJoho, Mobius
 */
public class GeoEngine
{
	private static final Logger LOGGER = Logger.getLogger(GeoEngine.class.getName());
	
	public static final String FILE_NAME_FORMAT = "%d_%d.l2j";
	
	private static final int ELEVATED_SEE_OVER_DISTANCE = 2;
	private static final int MAX_SEE_OVER_HEIGHT = 48;
	private static final int SPAWN_Z_DELTA_LIMIT = 100;
	
	private static final int WORLD_MIN_X = -655360;
	private static final int WORLD_MIN_Y = -589824;
	private static final int GEO_REGIONS_X = 32;
	private static final int GEO_REGIONS_Y = 32;
	private static final int GEO_REGIONS = GEO_REGIONS_X * GEO_REGIONS_Y;
	private static final AtomicReferenceArray<IRegion> REGIONS = new AtomicReferenceArray<>(GEO_REGIONS);
	
	protected GeoEngine()
	{
		// Initially set all regions to NullRegion.
		for (int i = 0; i < GEO_REGIONS; i++)
		{
			REGIONS.set(i, NullRegion.INSTANCE);
		}
		
		int loadedRegions = 0;
		try
		{
			for (int regionX = World.TILE_X_MIN; regionX <= World.TILE_X_MAX; regionX++)
			{
				for (int regionY = World.TILE_Y_MIN; regionY <= World.TILE_Y_MAX; regionY++)
				{
					final Path geoFilePath = Config.GEODATA_PATH.resolve(String.format(FILE_NAME_FORMAT, regionX, regionY));
					if (Files.exists(geoFilePath))
					{
						try
						{
							// LOGGER.info(getClass().getSimpleName() + ": Loading " + geoFilePath.getFileName() + "...");
							loadRegion(geoFilePath, regionX, regionY);
							loadedRegions++;
						}
						catch (Exception e)
						{
							LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to load " + geoFilePath.getFileName() + "!", e);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Failed to load geodata!", e);
			System.exit(1);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + loadedRegions + " regions.");
		
		// Avoid wrong configuration when no files are loaded.
		if ((loadedRegions == 0) && (Config.PATHFINDING > 0))
		{
			Config.PATHFINDING = 0;
			LOGGER.info(getClass().getSimpleName() + ": Pathfinding is disabled.");
		}
	}
	
	/**
	 * Loads geodata for a specific region from the provided file path.
	 * @param filePath the path to the geodata file.
	 * @param regionX the X coordinate of the region.
	 * @param regionY the Y coordinate of the region.
	 * @throws IOException if an error occurs while reading the file.
	 */
	private void loadRegion(Path filePath, int regionX, int regionY) throws IOException
	{
		final int regionOffset = (regionX * GEO_REGIONS_Y) + regionY;
		try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r"))
		{
			REGIONS.set(regionOffset, new Region(raf.getChannel().map(MapMode.READ_ONLY, 0, raf.length()).order(ByteOrder.LITTLE_ENDIAN)));
		}
	}
	
	/**
	 * Sets a geodata region at the specified coordinates.
	 * @param regionX the X coordinate of the region.
	 * @param regionY the Y coordinate of the region.
	 * @param region the geodata region to set.
	 */
	public void setRegion(int regionX, int regionY, Region region)
	{
		final int regionOffset = (regionX * GEO_REGIONS_Y) + regionY;
		REGIONS.set(regionOffset, region);
	}
	
	/**
	 * Retrieves the geodata region for the specified coordinates.
	 * @param geoX the geodata X coordinate.
	 * @param geoY the geodata Y coordinate.
	 * @return the geodata region.
	 */
	public IRegion getRegion(int geoX, int geoY)
	{
		return REGIONS.get(((geoX / IRegion.REGION_CELLS_X) * GEO_REGIONS_Y) + (geoY / IRegion.REGION_CELLS_Y));
	}
	
	/**
	 * Checks if geodata is available at the specified coordinates.
	 * @param geoX the geodata X coordinate.
	 * @param geoY the geodata Y coordinate.
	 * @return {@code true} if geodata is available, {@code false} otherwise.
	 */
	public boolean hasGeoPos(int geoX, int geoY)
	{
		return getRegion(geoX, geoY).hasGeo();
	}
	
	/**
	 * Checks if movement is possible in the specified direction (NSWE) from the given geodata coordinates and height.
	 * @param geoX the geodata X coordinate.
	 * @param geoY the geodata Y coordinate.
	 * @param worldZ the world Z coordinate.
	 * @param nswe the direction to check.
	 * @return {@code true} if movement is possible, {@code false} otherwise.
	 */
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return getRegion(geoX, geoY).checkNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	/**
	 * Checks if movement is possible in the specified direction (NSWE) from the given geodata coordinates and height, considering anti-corner-cut logic.
	 * @param geoX the geodata X coordinate.
	 * @param geoY the geodata Y coordinate.
	 * @param worldZ the world Z coordinate.
	 * @param nswe the direction to check.
	 * @return {@code true} if movement is possible, {@code false} otherwise.
	 */
	public boolean checkNearestNsweAntiCornerCut(int geoX, int geoY, int worldZ, int nswe)
	{
		boolean can = true;
		
		final IRegion region = getRegion(geoX, geoY);
		if ((nswe & Cell.NSWE_NORTH_EAST) == Cell.NSWE_NORTH_EAST)
		{
			can = region.checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_EAST) && region.checkNearestNswe(geoX + 1, geoY, worldZ, Cell.NSWE_NORTH);
		}
		
		if (can && ((nswe & Cell.NSWE_NORTH_WEST) == Cell.NSWE_NORTH_WEST))
		{
			can = region.checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_WEST) && region.checkNearestNswe(geoX - 1, geoY, worldZ, Cell.NSWE_NORTH);
		}
		
		if (can && ((nswe & Cell.NSWE_SOUTH_EAST) == Cell.NSWE_SOUTH_EAST))
		{
			can = region.checkNearestNswe(geoX, geoY + 1, worldZ, Cell.NSWE_EAST) && region.checkNearestNswe(geoX + 1, geoY, worldZ, Cell.NSWE_SOUTH);
		}
		
		if (can && ((nswe & Cell.NSWE_SOUTH_WEST) == Cell.NSWE_SOUTH_WEST))
		{
			can = region.checkNearestNswe(geoX, geoY + 1, worldZ, Cell.NSWE_WEST) && region.checkNearestNswe(geoX - 1, geoY, worldZ, Cell.NSWE_SOUTH);
		}
		
		return can && region.checkNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	/**
	 * Sets the nearest NSWE data at the specified coordinates and height.
	 * @param geoX the geodata X coordinate.
	 * @param geoY the geodata Y coordinate.
	 * @param worldZ the world Z coordinate.
	 * @param nswe the direction data to set.
	 */
	public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		getRegion(geoX, geoY).setNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	/**
	 * Removes the nearest NSWE data at the specified coordinates and height.
	 * @param geoX the geodata X coordinate.
	 * @param geoY the geodata Y coordinate.
	 * @param worldZ the world Z coordinate.
	 * @param nswe the direction data to remove.
	 */
	public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		getRegion(geoX, geoY).unsetNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	/**
	 * Retrieves the nearest Z coordinate at the specified geodata coordinates and height.
	 * @param geoX the geodata X coordinate.
	 * @param geoY the geodata Y coordinate.
	 * @param worldZ the world Z coordinate.
	 * @return the nearest Z coordinate.
	 */
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return getRegion(geoX, geoY).getNearestZ(geoX, geoY, worldZ);
	}
	
	/**
	 * Retrieves the next lower Z coordinate at the specified geodata coordinates and height.
	 * @param geoX the geodata X coordinate.
	 * @param geoY the geodata Y coordinate.
	 * @param worldZ the world Z coordinate.
	 * @return the next lower Z coordinate.
	 */
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		return getRegion(geoX, geoY).getNextLowerZ(geoX, geoY, worldZ);
	}
	
	/**
	 * Retrieves the next higher Z coordinate at the specified geodata coordinates and height.
	 * @param geoX the geodata X coordinate.
	 * @param geoY the geodata Y coordinate.
	 * @param worldZ the world Z coordinate.
	 * @return the next higher Z coordinate.
	 */
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		return getRegion(geoX, geoY).getNextHigherZ(geoX, geoY, worldZ);
	}
	
	/**
	 * Converts a world X coordinate to a geodata X coordinate.
	 * @param worldX the world X coordinate.
	 * @return the corresponding geodata X coordinate.
	 */
	public int getGeoX(int worldX)
	{
		return (worldX - WORLD_MIN_X) / 16;
	}
	
	/**
	 * Converts a world Y coordinate to a geodata Y coordinate.
	 * @param worldY the world Y coordinate.
	 * @return the corresponding geodata Y coordinate.
	 */
	public int getGeoY(int worldY)
	{
		return (worldY - WORLD_MIN_Y) / 16;
	}
	
	/**
	 * Converts a geodata X coordinate to a world X coordinate.
	 * @param geoX the geodata X coordinate.
	 * @return the corresponding world X coordinate.
	 */
	public int getWorldX(int geoX)
	{
		return (geoX * 16) + WORLD_MIN_X + 8;
	}
	
	/**
	 * Converts a geodata Y coordinate to a world Y coordinate.
	 * @param geoY the geodata Y coordinate.
	 * @return the corresponding world Y coordinate.
	 */
	public int getWorldY(int geoY)
	{
		return (geoY * 16) + WORLD_MIN_Y + 8;
	}
	
	/**
	 * Gets the height.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return the height
	 */
	public int getHeight(int x, int y, int z)
	{
		return getNearestZ(getGeoX(x), getGeoY(y), z);
	}
	
	/**
	 * Gets the spawn height.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the the z coordinate
	 * @return the spawn height
	 */
	public int getSpawnHeight(int x, int y, int z)
	{
		final int geoX = getGeoX(x);
		final int geoY = getGeoY(y);
		
		if (!hasGeoPos(geoX, geoY))
		{
			return z;
		}
		
		final int nextLowerZ = getNextLowerZ(geoX, geoY, z + 20);
		return Math.abs(nextLowerZ - z) <= SPAWN_Z_DELTA_LIMIT ? nextLowerZ : z;
	}
	
	/**
	 * Gets the spawn height.
	 * @param location the location
	 * @return the spawn height
	 */
	public int getSpawnHeight(Location location)
	{
		return getSpawnHeight(location.getX(), location.getY(), location.getZ());
	}
	
	/**
	 * Can see target. Doors as target always return true. Checks doors between.
	 * @param cha the character
	 * @param target the target
	 * @return {@code true} if the character can see the target (LOS), {@code false} otherwise
	 */
	public boolean canSeeTarget(WorldObject cha, WorldObject target)
	{
		return (target != null) && (target.isDoor() || canSeeTarget(cha.getX(), cha.getY(), cha.getZ(), cha.getInstanceId(), target.getX(), target.getY(), target.getZ(), target.getInstanceId()));
	}
	
	/**
	 * Can see target. Checks doors between.
	 * @param cha the character
	 * @param worldPosition the world position
	 * @return {@code true} if the character can see the target at the given world position, {@code false} otherwise
	 */
	public boolean canSeeTarget(WorldObject cha, ILocational worldPosition)
	{
		return canSeeTarget(cha.getX(), cha.getY(), cha.getZ(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), cha.getInstanceId());
	}
	
	/**
	 * Can see target. Checks doors between.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @param instanceId
	 * @param tx the target's x coordinate
	 * @param ty the target's y coordinate
	 * @param tz the target's z coordinate
	 * @param tInstanceId the target's instance id
	 * @return
	 */
	public boolean canSeeTarget(int x, int y, int z, int instanceId, int tx, int ty, int tz, int tInstanceId)
	{
		return (instanceId == tInstanceId) && canSeeTarget(x, y, z, tx, ty, tz, instanceId);
	}
	
	/**
	 * Can see target. Checks doors between.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @param tx the target's x coordinate
	 * @param ty the target's y coordinate
	 * @param tz the target's z coordinate
	 * @param instanceId
	 * @return {@code true} if there is line of sight between the given coordinate sets, {@code false} otherwise
	 */
	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz, int instanceId)
	{
		// Door checks.
		if (DoorData.getInstance().checkIfDoorsBetween(x, y, z, tx, ty, tz, instanceId, true))
		{
			return false;
		}
		
		// Fence checks.
		if (FenceData.getInstance().checkIfFenceBetween(x, y, z, tx, ty, tz, instanceId))
		{
			return false;
		}
		
		return canSeeTarget(x, y, z, tx, ty, tz);
	}
	
	private int getLosGeoZ(int prevX, int prevY, int prevGeoZ, int curX, int curY, int nswe)
	{
		if ((((nswe & Cell.NSWE_NORTH) != 0) && ((nswe & Cell.NSWE_SOUTH) != 0)) || (((nswe & Cell.NSWE_WEST) != 0) && ((nswe & Cell.NSWE_EAST) != 0)))
		{
			throw new RuntimeException("Multiple directions!");
		}
		return checkNearestNsweAntiCornerCut(prevX, prevY, prevGeoZ, nswe) ? getNearestZ(curX, curY, prevGeoZ) : getNextHigherZ(curX, curY, prevGeoZ);
	}
	
	/**
	 * Can see target. Does not check doors between.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @param tx the target's x coordinate
	 * @param ty the target's y coordinate
	 * @param tz the target's z coordinate
	 * @return {@code true} if there is line of sight between the given coordinate sets, {@code false} otherwise
	 */
	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz)
	{
		int geoX = getGeoX(x);
		int geoY = getGeoY(y);
		int tGeoX = getGeoX(tx);
		int tGeoY = getGeoY(ty);
		
		int nearestFromZ = getNearestZ(geoX, geoY, z);
		int nearestToZ = getNearestZ(tGeoX, tGeoY, tz);
		
		// Fastpath.
		if ((geoX == tGeoX) && (geoY == tGeoY))
		{
			return !hasGeoPos(tGeoX, tGeoY) || (nearestFromZ == nearestToZ);
		}
		
		int fromX = tx;
		int fromY = ty;
		int toX = tx;
		int toY = ty;
		if (nearestToZ > nearestFromZ)
		{
			int tmp = toX;
			toX = fromX;
			fromX = tmp;
			
			tmp = toY;
			toY = fromY;
			fromY = tmp;
			
			tmp = nearestToZ;
			nearestToZ = nearestFromZ;
			nearestFromZ = tmp;
			
			tmp = tGeoX;
			tGeoX = geoX;
			geoX = tmp;
			
			tmp = tGeoY;
			tGeoY = geoY;
			geoY = tmp;
		}
		
		final GridLineIterator3D pointIter = new GridLineIterator3D(geoX, geoY, nearestFromZ, tGeoX, tGeoY, nearestToZ);
		// First point is guaranteed to be available, skip it, we can always see our own position.
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		final int prevZ = pointIter.z();
		int prevGeoZ = prevZ;
		int ptIndex = 0;
		while (pointIter.next())
		{
			final int curX = pointIter.x();
			final int curY = pointIter.y();
			
			if ((curX == prevX) && (curY == prevY))
			{
				continue;
			}
			
			final int beeCurZ = pointIter.z();
			int curGeoZ = prevGeoZ;
			
			// Check if the position has geodata.
			if (hasGeoPos(curX, curY))
			{
				final int nswe = GeoUtils.computeNswe(prevX, prevY, curX, curY);
				curGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, curX, curY, nswe);
				final int maxHeight = ptIndex < ELEVATED_SEE_OVER_DISTANCE ? nearestFromZ + MAX_SEE_OVER_HEIGHT : beeCurZ + MAX_SEE_OVER_HEIGHT;
				boolean canSeeThrough = false;
				if (curGeoZ <= maxHeight)
				{
					if ((nswe & Cell.NSWE_NORTH_EAST) == Cell.NSWE_NORTH_EAST)
					{
						final int northGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX, prevY - 1, Cell.NSWE_EAST);
						final int eastGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX + 1, prevY, Cell.NSWE_NORTH);
						canSeeThrough = (northGeoZ <= maxHeight) && (eastGeoZ <= maxHeight) && (northGeoZ <= getNearestZ(prevX, prevY - 1, beeCurZ)) && (eastGeoZ <= getNearestZ(prevX + 1, prevY, beeCurZ));
					}
					else if ((nswe & Cell.NSWE_NORTH_WEST) == Cell.NSWE_NORTH_WEST)
					{
						final int northGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX, prevY - 1, Cell.NSWE_WEST);
						final int westGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX - 1, prevY, Cell.NSWE_NORTH);
						canSeeThrough = (northGeoZ <= maxHeight) && (westGeoZ <= maxHeight) && (northGeoZ <= getNearestZ(prevX, prevY - 1, beeCurZ)) && (westGeoZ <= getNearestZ(prevX - 1, prevY, beeCurZ));
					}
					else if ((nswe & Cell.NSWE_SOUTH_EAST) == Cell.NSWE_SOUTH_EAST)
					{
						final int southGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX, prevY + 1, Cell.NSWE_EAST);
						final int eastGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX + 1, prevY, Cell.NSWE_SOUTH);
						canSeeThrough = (southGeoZ <= maxHeight) && (eastGeoZ <= maxHeight) && (southGeoZ <= getNearestZ(prevX, prevY + 1, beeCurZ)) && (eastGeoZ <= getNearestZ(prevX + 1, prevY, beeCurZ));
					}
					else if ((nswe & Cell.NSWE_SOUTH_WEST) == Cell.NSWE_SOUTH_WEST)
					{
						final int southGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX, prevY + 1, Cell.NSWE_WEST);
						final int westGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX - 1, prevY, Cell.NSWE_SOUTH);
						canSeeThrough = (southGeoZ <= maxHeight) && (westGeoZ <= maxHeight) && (southGeoZ <= getNearestZ(prevX, prevY + 1, beeCurZ)) && (westGeoZ <= getNearestZ(prevX - 1, prevY, beeCurZ));
					}
					else
					{
						canSeeThrough = true;
					}
				}
				
				if (!canSeeThrough)
				{
					return false;
				}
			}
			
			prevX = curX;
			prevY = curY;
			prevGeoZ = curGeoZ;
			++ptIndex;
		}
		
		return true;
	}
	
	/**
	 * Verifies if the is a path between origin's location and destination, if not returns the closest location.
	 * @param origin the origin
	 * @param destination the destination
	 * @return the destination if there is a path or the closes location
	 */
	public Location getValidLocation(ILocational origin, ILocational destination)
	{
		return getValidLocation(origin.getX(), origin.getY(), origin.getZ(), destination.getX(), destination.getY(), destination.getZ(), 0);
	}
	
	/**
	 * Move check.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @param tx the target's x coordinate
	 * @param ty the target's y coordinate
	 * @param tz the target's z coordinate
	 * @param instanceId the instance id
	 * @return the last Location (x,y,z) where player can walk - just before wall
	 */
	public Location getValidLocation(int x, int y, int z, int tx, int ty, int tz, int instanceId)
	{
		final int geoX = getGeoX(x);
		final int geoY = getGeoY(y);
		final int nearestFromZ = getNearestZ(geoX, geoY, z);
		final int tGeoX = getGeoX(tx);
		final int tGeoY = getGeoY(ty);
		final int nearestToZ = getNearestZ(tGeoX, tGeoY, tz);
		
		// Door checks.
		if (DoorData.getInstance().checkIfDoorsBetween(x, y, nearestFromZ, tx, ty, nearestToZ, instanceId, false))
		{
			return new Location(x, y, getHeight(x, y, nearestFromZ));
		}
		
		// Fence checks.
		if (FenceData.getInstance().checkIfFenceBetween(x, y, nearestFromZ, tx, ty, nearestToZ, instanceId))
		{
			return new Location(x, y, getHeight(x, y, nearestFromZ));
		}
		
		final GridLineIterator2D pointIter = new GridLineIterator2D(geoX, geoY, tGeoX, tGeoY);
		// first point is guaranteed to be available
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		int prevZ = nearestFromZ;
		
		while (pointIter.next())
		{
			final int curX = pointIter.x();
			final int curY = pointIter.y();
			final int curZ = getNearestZ(curX, curY, prevZ);
			if ((curZ - prevZ) > 40) // Check for sudden height increase.
			{
				// Can't move, return previous location.
				return new Location(getWorldX(prevX), getWorldY(prevY), prevZ);
			}
			
			if (hasGeoPos(prevX, prevY))
			{
				if (Config.AVOID_ABSTRUCTED_PATH_NODES && !checkNearestNswe(curX, curY, curZ, Cell.NSWE_ALL))
				{
					// Can't move, return previous location.
					return new Location(getWorldX(prevX), getWorldY(prevY), prevZ);
				}
				
				if (!checkNearestNsweAntiCornerCut(prevX, prevY, prevZ, GeoUtils.computeNswe(prevX, prevY, curX, curY)))
				{
					// Can't move, return previous location.
					return new Location(getWorldX(prevX), getWorldY(prevY), prevZ);
				}
			}
			
			prevX = curX;
			prevY = curY;
			prevZ = curZ;
		}
		return hasGeoPos(prevX, prevY) && (prevZ != nearestToZ) ? new Location(x, y, nearestFromZ) : new Location(tx, ty, nearestToZ);
	}
	
	/**
	 * Checks if it is possible to move from one location to another.
	 * @param fromX the X coordinate to start checking from
	 * @param fromY the Y coordinate to start checking from
	 * @param fromZ the Z coordinate to start checking from
	 * @param toX the X coordinate to end checking at
	 * @param toY the Y coordinate to end checking at
	 * @param toZ the Z coordinate to end checking at
	 * @param instanceId the instance id
	 * @return {@code true} if the character at start coordinates can move to end coordinates, {@code false} otherwise
	 */
	public boolean canMoveToTarget(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, int instanceId)
	{
		final int geoX = getGeoX(fromX);
		final int geoY = getGeoY(fromY);
		final int nearestFromZ = getNearestZ(geoX, geoY, fromZ);
		final int tGeoX = getGeoX(toX);
		final int tGeoY = getGeoY(toY);
		final int nearestToZ = getNearestZ(tGeoX, tGeoY, toZ);
		
		// Door checks.
		if (DoorData.getInstance().checkIfDoorsBetween(fromX, fromY, nearestFromZ, toX, toY, nearestToZ, instanceId, false))
		{
			return false;
		}
		
		// Fence checks.
		if (FenceData.getInstance().checkIfFenceBetween(fromX, fromY, nearestFromZ, toX, toY, nearestToZ, instanceId))
		{
			return false;
		}
		
		final GridLineIterator2D pointIter = new GridLineIterator2D(geoX, geoY, tGeoX, tGeoY);
		// First point is guaranteed to be available.
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		int prevZ = nearestFromZ;
		
		while (pointIter.next())
		{
			final int curX = pointIter.x();
			final int curY = pointIter.y();
			final int curZ = getNearestZ(curX, curY, prevZ);
			if ((curZ - prevZ) > 40) // Check for sudden height increase.
			{
				return false;
			}
			
			if (hasGeoPos(prevX, prevY))
			{
				if (Config.AVOID_ABSTRUCTED_PATH_NODES && !checkNearestNswe(curX, curY, curZ, Cell.NSWE_ALL))
				{
					return false;
				}
				
				if (!checkNearestNsweAntiCornerCut(prevX, prevY, prevZ, GeoUtils.computeNswe(prevX, prevY, curX, curY)))
				{
					return false;
				}
			}
			
			prevX = curX;
			prevY = curY;
			prevZ = curZ;
		}
		return !hasGeoPos(prevX, prevY) || (prevZ == nearestToZ);
	}
	
	/**
	 * Checks if it is possible to move from one location to another.
	 * @param from the {@code ILocational} to start checking from
	 * @param toX the X coordinate to end checking at
	 * @param toY the Y coordinate to end checking at
	 * @param toZ the Z coordinate to end checking at
	 * @return {@code true} if the character at start coordinates can move to end coordinates, {@code false} otherwise
	 */
	public boolean canMoveToTarget(ILocational from, int toX, int toY, int toZ)
	{
		return canMoveToTarget(from.getX(), from.getY(), from.getZ(), toX, toY, toZ, 0);
	}
	
	/**
	 * Checks if it is possible to move from one location to another.
	 * @param from the {@code ILocational} to start checking from
	 * @param to the {@code ILocational} to end checking at
	 * @return {@code true} if the character at start coordinates can move to end coordinates, {@code false} otherwise
	 */
	public boolean canMoveToTarget(ILocational from, ILocational to)
	{
		return canMoveToTarget(from, to.getX(), to.getY(), to.getZ());
	}
	
	/**
	 * Checks the specified position for available geodata.
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 * @return {@code true} if there is geodata for the given coordinates, {@code false} otherwise
	 */
	public boolean hasGeo(int x, int y)
	{
		return hasGeoPos(getGeoX(x), getGeoY(y));
	}
	
	public static GeoEngine getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GeoEngine INSTANCE = new GeoEngine();
	}
}
