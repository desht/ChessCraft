package me.desht.chesscraft.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import me.desht.chesscraft.blocks.BlockUtils;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.chesscraft.util.jnbt.ByteArrayTag;
import me.desht.chesscraft.util.jnbt.CompoundTag;
import me.desht.chesscraft.util.jnbt.NBTInputStream;
import me.desht.chesscraft.util.jnbt.ShortTag;
import me.desht.chesscraft.util.jnbt.StringTag;
import me.desht.chesscraft.util.jnbt.Tag;

import org.bukkit.Location;
import org.bukkit.World;

/*
 * Copyright (C) 2012 p000ison
 *
 * This work is licensed under the Creative Commons
 * Attribution-NonCommercial-NoDerivs 3.0 Unported License. To view a copy of
 * this license, visit http://creativecommons.org/licenses/by-nc-nd/3.0/ or send
 * a letter to Creative Commons, 171 Second Street, Suite 300, San Francisco,
 * California, 94105, USA.
 *
 */

/**
 * @author Max
 * 
 * With modifications by desht to use fast NMS block-modification calls.
 * 
 */
public class Schematic
{

	private byte[] blocks;
	private byte[] data;
	private short width;
	private short length;
	private short height;

	public Schematic(byte[] blocks, byte[] data, short width, short length, short height)
	{
		this.blocks = blocks;
		this.data = data;
		this.width = width;
		this.length = length;
		this.height = height;
	}

	/**
	 * @return the blocks
	 */
	 public byte[] getBlocks()
	 {
		 return blocks;
	 }

	 /**
	  * @return the data
	  */
	 public byte[] getData()
	 {
		 return data;
	 }

	 /**
	  * @return the width
	  */
	 public short getWidth()
	 {
		 return width;
	 }

	 /**
	  * @return the lenght
	  */
	 public short getLength()
	 {
		 return length;
	 }

	 /**
	  * @return the height
	  */
	 public short getHeight()
	 {
		 return height;
	 }

	 
	 public void paste(World world, Location loc)
	 {
		 byte[] blocks = getBlocks();
		 byte[] blockData = getData();

		 int x0 = loc.getBlockX();
		 int y0 = loc.getBlockY();
		 int z0 = loc.getBlockZ();
		 
		 for (int x = 0; x < width; ++x) {
			 for (int y = 0; y < height; ++y) {
				 for (int z = 0; z < length; ++z) {
					 int index = y * width * length + z * width + x;
					 // desht
					 BlockUtils.setBlockFast(world.getBlockAt(x + x0, y + y0, z + z0), blocks[index], blockData[index]);
//					 Block block = new Location(world, x + loc.getX(), y + loc.getY(), z + loc.getZ()).getBlock();
//					 block.setTypeIdAndData(blocks[index], blockData[index], true);
				 }
			 }
		 }
		 
		 Cuboid c = new Cuboid(world, x0, y0, z0, x0 + width, y0 + height, z0 + length);
		 c.initLighting();
		 c.sendClientChanges();
	 }

	 public static Schematic loadSchematic(File file) throws IOException
	 {
		 FileInputStream stream = new FileInputStream(file);
		 NBTInputStream nbtStream = new NBTInputStream(new GZIPInputStream(stream));

		 CompoundTag schematicTag = (CompoundTag) nbtStream.readTag();
		 if (!schematicTag.getName().equals("Schematic")) {
			 throw new IllegalArgumentException("Tag \"Schematic\" does not exist or is not first");
		 }

		 Map<String, Tag> schematic = schematicTag.getValue();
		 if (!schematic.containsKey("Blocks")) {
			 throw new IllegalArgumentException("Schematic file is missing a \"Blocks\" tag");
		 }

		 short width = getChildTag(schematic, "Width", ShortTag.class).getValue();
		 short length = getChildTag(schematic, "Length", ShortTag.class).getValue();
		 short height = getChildTag(schematic, "Height", ShortTag.class).getValue();

		 String materials = getChildTag(schematic, "Materials", StringTag.class).getValue();
		 if (!materials.equals("Alpha")) {
			 throw new IllegalArgumentException("Schematic file is not an Alpha schematic");
		 }

		 byte[] blocks = getChildTag(schematic, "Blocks", ByteArrayTag.class).getValue();
		 byte[] blockData = getChildTag(schematic, "Data", ByteArrayTag.class).getValue();
		 return new Schematic(blocks, blockData, width, length, height);
	 }

	 /**
	  * Get child tag of a NBT structure.
	  *
	  * @param items The parent tag map
	  * @param key The name of the tag to get
	  * @param expected The expected type of the tag
	  * @return child tag casted to the expected type
	  * @throws DataException if the tag does not exist or the tag is not of the
	  * expected type
	  */
	 private static <T extends Tag> T getChildTag(Map<String, Tag> items, String key, Class<T> expected) throws IllegalArgumentException
	 {
		 if (!items.containsKey(key)) {
			 throw new IllegalArgumentException("Schematic file is missing a \"" + key + "\" tag");
		 }
		 Tag tag = items.get(key);
		 if (!expected.isInstance(tag)) {
			 throw new IllegalArgumentException(key + " tag is not of tag type " + expected.getName());
		 }
		 return expected.cast(tag);
	 }
}