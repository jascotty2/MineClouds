/**
 * Copyright (C) 2012 Jacob Scott <jascottytechie@gmail.com>
 *
 * Description: Cloud Generator for Bukkit
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package me.jascotty2.clouds;

//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.OutputStreamWriter;
//import java.io.Writer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.stream.Collectors;
import me.jascotty2.libv2.io.CheckInput;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.noise.SimplexOctaveGenerator;

public class Clouds extends JavaPlugin implements Listener {

	int minCloudHeight = 6;
	int maxCloudDepth = 3;
	int cloudFloor = 80;
	int cloudCeiling = 250;
	final Set<Material> groundTypes = new HashSet();
	Map<World, SimplexOctaveGenerator> worldNoiseGenerators = new HashMap<>();
	double noiseScale = 1 / 19.0;
	int noiseOctaves = 6;
	double noisefreq = 0.4, noiseamp = .3, yScale = 2.1;
	double noisethreshold = .6;
	int min_cloud_size = 5, max_cloud_size = -1;
	//int block_id = 80, block_data = 0;
	Material block_material = Material.POWDER_SNOW;
	boolean global_enabled = true, softClouds = false;
	String[] enabled_worlds = new String[0];

	@Override
	public void onEnable() {
		//int ground[] = new int[]{1, 2, 3, 4, 8, 9, 10, 11, 12, 13, 14, 15, 16, 48, 49, 79, 82, 110};
		groundTypes.clear();
		groundTypes.addAll(Arrays.asList(
				Material.STONE,
				Material.SHORT_GRASS,
				Material.DIRT,
				Material.DIRT_PATH,
				Material.FARMLAND,
				Material.COARSE_DIRT,
				Material.PODZOL,
				Material.COBBLESTONE,
				Material.WATER,
				Material.LAVA,
				Material.SAND,
				Material.GRAVEL,
				Material.RED_SAND,
				Material.GOLD_ORE,
				Material.IRON_ORE,
				Material.COAL_BLOCK,
				Material.MOSSY_COBBLESTONE,
				Material.OBSIDIAN,
				Material.SNOW,
				Material.SNOW_BLOCK,
				Material.ICE,
				Material.FROSTED_ICE,
				Material.MAGMA_BLOCK,
				Material.CLAY,
				Material.MYCELIUM,
				Material.BEDROCK,
				Material.TERRACOTTA,
				Material.WHITE_TERRACOTTA,
				Material.ORANGE_TERRACOTTA,
				Material.MAGENTA_TERRACOTTA,
				Material.LIGHT_BLUE_TERRACOTTA,
				Material.YELLOW_TERRACOTTA,
				Material.LIME_TERRACOTTA,
				Material.PINK_TERRACOTTA,
				Material.GRAY_TERRACOTTA,
				Material.LIGHT_GRAY_TERRACOTTA,
				Material.CYAN_TERRACOTTA,
				Material.PURPLE_TERRACOTTA,
				Material.BLUE_TERRACOTTA,
				Material.BROWN_TERRACOTTA,
				Material.GREEN_TERRACOTTA,
				Material.RED_TERRACOTTA,
				Material.BLACK_TERRACOTTA,
				Material.GRANITE,
				Material.DIORITE,
				Material.ANDESITE
		));
		load();
		getServer().getPluginManager().registerEvents(this, this);
		try {
			// Metrics :)
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			// Failed to submit the stats :-(
		}
	}

	void load() {
		saveDefaultConfig();
		reloadConfig();

		global_enabled = getConfig().getBoolean("globalEnabled", global_enabled);
		enabled_worlds = getConfig().getString("enabledWorlds", "").split(",");
		for (int i = 0; i < enabled_worlds.length; ++i) {
			enabled_worlds[i] = enabled_worlds[i].trim();
		}

		softClouds = getConfig().getBoolean("softClouds", softClouds);

		minCloudHeight = configAssertInt("minCloudHeight", 0, 200);
		maxCloudDepth = configAssertInt("maxCloudDepth", 1, 100);
		cloudFloor = configAssertInt("cloudFloor", 0, 200);
		cloudCeiling = configAssertInt("cloudCeiling", 5, 255);
		noiseScale = configAssertDouble("noiseScale", 0.0000001, 999);
		noiseOctaves = configAssertInt("noiseOctaves", 1, 50);
		noisefreq = getConfig().getDouble("noisefreq");
		noiseamp = getConfig().getDouble("noiseamp");
		yScale = getConfig().getDouble("yScale");
		noisethreshold = getConfig().getDouble("noisethreshold");
		min_cloud_size = configAssertInt("min_cloud_size", 0, 5000);
		max_cloud_size = configAssertInt("max_cloud_size", -1, -1);
		
		String mat = getConfig().getString("block_type");
		if (mat != null) {
			Material m = Material.getMaterial(mat);
			if (m != null) {
				block_material = m;
			} else {
				getLogger().warning("Invalid Material in config: " + mat);
			}
		}

		// validate data
		if (noisefreq == 0) {
			getConfig().set("noisefreq", noisefreq = 1);
		}
		if (noiseamp == 0) {
			getConfig().set("noiseamp", noiseamp = 1);
		}
		if (cloudCeiling < cloudFloor) {
			if (cloudFloor > 250) {
				getConfig().set("cloudFloor", cloudFloor = (cloudFloor - 10));
			}
			getConfig().set("cloudCeiling", cloudCeiling = cloudFloor + 10);
		}

		worldNoiseGenerators.clear();
		getServer().getWorlds().stream()
				.filter((w) -> (w.getEnvironment() == Environment.NORMAL))
				.forEachOrdered((w) -> {
					SimplexOctaveGenerator noise = new SimplexOctaveGenerator(w.getSeed(), noiseOctaves);
					noise.setScale(noiseScale);
					worldNoiseGenerators.put(w, noise);
				});

		saveConfig();
	}

	int configAssertInt(String key, int lowerBound, int upperBound) {
		int value = getConfig().getInt(key);
		if (value < lowerBound) {
			getConfig().set(key, lowerBound);
			return lowerBound;
		} else if (upperBound > lowerBound && value > upperBound) {
			getConfig().set(key, upperBound);
			return upperBound;
		}
		return value;
	}

	double configAssertDouble(String key, double lowerBound, double upperBound) {
		double value = getConfig().getDouble(key);
		if (value < lowerBound) {
			getConfig().set(key, lowerBound);
			return lowerBound;
		} else if (upperBound > lowerBound && value > upperBound) {
			getConfig().set(key, upperBound);
			return upperBound;
		}
		return value;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		boolean clear = args.length >= 1 && (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("remove"));
		if (clear || (args.length >= 1 && (args[0].equalsIgnoreCase("generate") || args[0].equalsIgnoreCase("gen")))) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Must be a player to use this command");
				return true;
			}
			int radius = 1;
			if (args.length > 2 || (args.length == 2 && (radius = CheckInput.GetInt(args[1], 0)) <= 0)) {
				sender.sendMessage(ChatColor.RED + "Invalid argument");
				return false;
			}
			if (radius == 1) {
				// clear existing clouds here
				clearClouds(((Player) sender).getLocation().getChunk());
				updateClouds(((Player) sender).getLocation().getChunk());
				if (clear) {
					sender.sendMessage(ChatColor.AQUA + "Clouds Removed!");
				} else {// regenerate chunk
					genClouds(((Player) sender).getLocation().getChunk(), true);
					sender.sendMessage(ChatColor.AQUA + "Clouds Regenerated!");
				}
			} else {
				if (runID != -1) {
					sender.sendMessage("Still Busy!");
					sender.sendMessage("On Run " + run.run + " of " + (run.area * run.area));
					return true;
				} else {
					run = new RunThread(sender, ((Player) sender).getLocation().getWorld(), radius);
					if (clear) {
						run.gen = false;
					}
					// start thread
					runID = getServer().getScheduler().scheduleSyncRepeatingTask(this, run, 1, 1);
					sender.sendMessage(ChatColor.AQUA + "Starting!");
				}
			}

		} else if (args.length == 1 && (args[0].equalsIgnoreCase("reload"))) {
			load();
			sender.sendMessage(ChatColor.AQUA + "Generator Settings Reloaded!");
		} else {
			return false;
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String commandLabel, String[] args) {
		List<String> options = null;
		if (args.length <= 1) {
			options = Arrays.asList("clear", "generate");
		} else if (args.length <= 2) {
			if (args[0].equalsIgnoreCase("generate")) {
				options = Arrays.asList("<radius>");
			}
		}
		if (options == null) {
			return Collections.EMPTY_LIST;
		}
		final String sub = args.length == 0 ? "" : args[args.length - 1];
		return options.stream()
				.filter(e -> e != null && (sub.length() == 0 || e.startsWith(sub)))
				.collect(Collectors.toList());
	}

	private static class Point3D {

		public int x, y, z;

		public Point3D() {
		}

		public Point3D(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	int runID = -1;
	RunThread run = null;

	class RunThread implements Runnable {

		int run = 0;
		private int area = 19;
		private int areaix = -(area / 2);
		private int areaiz = -(area / 2);
		boolean gen = true;
		CommandSender p;
		World w;
		//PerlinNoiseGenerator noise;

		RunThread(CommandSender sender, World world) {
			p = sender;
			w = world;
			//noise= worldNoiseGenerators.get(w);
		}

		RunThread(CommandSender sender, World world, int radius) {
			p = sender;
			w = world;
			//noise= worldNoiseGenerators.get(w);
			area = (radius * 2) + 1;
			areaix = -(area / 2);
			areaiz = -(area / 2);
			if (sender instanceof Player) {
				final Chunk il = ((Player) sender).getLocation().getChunk();
				areaix += il.getX();
				areaiz += il.getZ();
			}
		}

		@Override
		public void run() {
			final int cx = areaix + (run % area);
			final int cz = areaiz + (run / area);

			try {
				Chunk c = w.getChunkAt(cx, cz);
				// clear task
				clearClouds(c);
				updateClouds(c);
				if (gen) {
					// generate
					genClouds(c, true);
				}
			} catch (Exception ex) {
				getLogger().log(Level.SEVERE, "Error in Chunk Generator:", ex);
				p.sendMessage(ChatColor.RED + "Error in Cloud Chunk Generator (Check log for details)");
				p.getServer().getScheduler().cancelTask(runID);
				runID = -1;
			}
			if (++run >= area * area) {
				p.getServer().getScheduler().cancelTask(runID);
				runID = -1;
				p.sendMessage(ChatColor.AQUA + "Done!");
			}
		}
	}

//	public static void main(String[] args) {
//
//		SimplexOctaveGenerator noise = new SimplexOctaveGenerator(592724999, 6);
//		noise.setScale(1 / 20.0);
//		final double freq = .4, amp = .3, yMod = 2.1;
//		final double threshold = .6;
//
//		double ix = 0, iz = 0;
//		
//		final int maxSpan = 4;
//		final int start = maxSpan * 16, end = (maxSpan * 2 + 1) * 16;
//		
//		long count = 0, startT = System.currentTimeMillis();
//		double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY, total = 0, result;
//
//		double[][] array = new double[end][end];
//		for (int x = 0; x < array.length; ++x) {
//			for (int z = 0; z < array[x].length; ++z) {
//				total += result = array[x][z] = noise.noise(ix + x - start, 0, iz + z - start, freq, amp, true) - threshold;
//				if (result < min) {
//					min = result;
//				}
//				if (result > max) {
//					max = result;
//				}
//				++count;
//			}
//		}
//		printArray(array);
//		
//		long endT = System.currentTimeMillis();
//		System.out.println(count + " in " + String.format("%.2f", (endT - startT) / 1000.) + " seconds " + String.format("(%.2f/second)", count / ((double) endT - startT)));
//		System.out.println("mean: " + (total / count));
//		System.out.println("min: " + min);
//		System.out.println("max: " + max);
//	}
//
//	static void printArray(double array[][]) {
//		final boolean markChunks = false;
//		for (int x = 0; x < array.length; ++x) {
//			if (markChunks) {
//				if (x > 0 && x % 16 == 0) {
//					for (int z = 0; z < array[x].length; ++z) {
//						if (z > 0 && z % 16 == 0) {
//							System.out.print("|");
//						}
//						if (x < array[x].length / 2) {
//							// lower line
//							System.out.print("\u2581");
//						} else {
//							// upper line
//							System.out.print("\u2594");
//						}
//					}
//					System.out.println();
//				}
//			}
//			for (int z = 0; z < array[x].length; ++z) {
//				if (markChunks && z > 0 && z % 16 == 0) {
//					System.out.print("|");
//				}
//				System.out.print(array[x][z] > 0 ? "\u2588" : "\u2591");
//			}
//			System.out.println();
//		}
//	}
//
//	static void printArray(double array[][], boolean append) {
//		Writer out;
//		try {
//			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("cloud_out.txt", append), "UTF-8"));
//			try {
//				for (int x = 0; x < array.length; ++x) {
//					if (x > 0 && x % 16 == 0) {
//						for (int z = 0; z < array[x].length; ++z) {
//							if (z > 0 && z % 16 == 0) {
//								out.write("|");
//							}
//							if (x < array[x].length / 2) {
//								// lower line
//								out.write("\u2581");
//							} else {
//								// upper line
//								out.write("\u2594");
//							}
//						}
//						out.write("\n");
//					}
//					for (int z = 0; z < array[x].length; ++z) {
//						if (z > 0 && z % 16 == 0) {
//							out.write("|");
//						}
//						out.write(array[x][z] > 0 ? "\u2588" : "\u2591");
//					}
//					out.write("\n");
//				}
//			} finally {
//				out.close();
//			}
//		} catch (Exception ex) {
////			Logger.getLogger(Clouds.class.getName()).log(Level.SEVERE, null, ex);
//		}
//	}
	void clearClouds(Chunk c) {
		for (int x = 0; x < 16; ++x) {
			for (int z = 0; z < 16; ++z) {
				for (int y = cloudFloor; y <= cloudCeiling; ++y) {
					final Block b = c.getBlock(x, y, z);
					if (b.getType() == block_material) {
						b.setType(Material.AIR);
					}
				}
			}
		}
	}

	final int cloudScanVersion = 3;

	void updateClouds(Chunk c) {
		for (int x = 0; x < 16; ++x) {
			for (int z = 0; z < 16; ++z) {
				// step one: find a cloud block that is: 1) the bottom of a cloud, and 2) a good distance from the ground
				int lastGroundHeight = 0;
				for (int y = cloudFloor; y <= cloudCeiling; ++y) {
					final Block b = c.getBlock(x, y, z);
					if (b.getType() == Material.SNOW_BLOCK && (y - lastGroundHeight) > 15) {
						// only update if not updated already this reboot
						List<MetadataValue> mv = b.getMetadata("clouds");
						if (mv.isEmpty() || mv.get(0).asInt() != cloudScanVersion) {
							//System.out.printf("Found cloud to update at (%d, %d, %d)\n", (c.getX() << 4) | x, y, (c.getZ() << 4) | z);
							updateCloud(c.getWorld(), (c.getX() << 4) | x, y, (c.getZ() << 4) | z);
						}// else {
						//	System.out.printf("Skipping known cloud at (%d, %d, %d)\n", (c.getX() << 4) | x, y, (c.getZ() << 4) | z);
						//}
						y = 9999; // only check for one cloud per column
					}// else if (b.getType() == Material.SNOW_BLOCK) {
					//	System.out.printf("Skipping low cloud at (%d, %d, %d) at %d\n", (c.getX() << 4) | x, y, (c.getZ() << 4) | z, (y - lastGroundHeight));
					//} 
					else if (groundTypes.contains(b.getType())) {
						lastGroundHeight = y;
					}
				}
			}
		}
	}

	void updateCloud(World w, int x, int y, int z) {
		// find the full extent of the height/width (cuboid) of non-air blocks, and return if any are not snow blocks
		// to be lazy, let's start with a reasonably-sized block:
		Point3D min = new Point3D(x - 10, y - 5, z - 10);
		Point3D max = new Point3D(x + 10, y + 30, z + 10);
		// now let's check if our edges are actually edges:
		boolean north = false, south = false, east = false, west = false, up = false, down = false, rerun = true;
		while (rerun) {
			rerun = false;
			while (!north) {
				// check north side
				north = true;
				int z2 = min.z;
				for (int x2 = min.x; north && x2 < max.x; ++x2) {
					for (int y2 = min.y; north && y2 < max.y; ++y2) {
						Block b = w.getBlockAt(x2, y2, z2);
						if (b.getType() == Material.SNOW_BLOCK) {
							north = false;
						}
					}
				}
				if (!north) {
					// need more north!
					min.z -= 5;
				}
			}
			while (!south) {
				south = true;
				int z2 = max.z;
				for (int x2 = min.x; south && x2 < max.x; ++x2) {
					for (int y2 = min.y; south && y2 < max.y; ++y2) {
						Block b = w.getBlockAt(x2, y2, z2);
						if (b.getType() == Material.SNOW_BLOCK) {
							south = false;
						}
					}
				}
				if (!south) {
					// need more south!
					max.z += 5;
				}
			}
			// now do the same thing, but east and west
			while (!east) {
				// check north side
				east = true;
				int x2 = min.x;
				for (int z2 = min.z; east && z2 < max.z; ++z2) {
					for (int y2 = min.y; east && y2 < max.y; ++y2) {
						Block b = w.getBlockAt(x2, y2, z2);
						if (b.getType() == Material.SNOW_BLOCK) {
							east = false;
						}
					}
				}
				if (!east) {
					// need more east!
					min.x -= 5;
				}
			}
			while (!west) {
				west = true;
				int x2 = max.x;
				for (int z2 = min.z; west && z2 < max.z; ++z2) {
					for (int y2 = min.y; west && y2 < max.y; ++y2) {
						Block b = w.getBlockAt(x2, y2, z2);
						if (b.getType() == Material.SNOW_BLOCK) {
							west = false;
						}
					}
				}
				if (!west) {
					// need more west!
					max.x += 5;
				}
			}
			// now for the tricky bits - if we move up or down, re-scan N/E/W/S

			while (!up) {
				up = true;
				int y2 = max.y;
				for (int x2 = min.x; up && x2 < max.x; ++x2) {
					for (int z2 = min.z; up && z2 < max.z; ++z2) {
						Block b = w.getBlockAt(x2, y2, z2);
						if (b.getType() == Material.SNOW_BLOCK) {
							rerun = true;
							up = false;
						}
					}
				}
				if (!up) {
					// need more west!
					max.y += 5;
				}
			}

			while (!down) {
				down = true;
				int y2 = min.y;
				for (int x2 = min.x; down && x2 < max.x; ++x2) {
					for (int z2 = min.z; down && z2 < max.z; ++z2) {
						Block b = w.getBlockAt(x2, y2, z2);
						if (b.getType() == Material.SNOW_BLOCK) {
							rerun = true;
							down = false;
						}
					}
				}
				if (!down) {
					// need more west!
					min.y -= 5;
				}
			}
		}
		// at this point we should have a good box around our cloud
		// all we need to know is if there are non-snow blocks in this box
		boolean isCloud = true;
		int checked = 0;
		for (int z2 = min.z; isCloud && z2 < max.z; ++z2) {
			for (int x2 = min.x; isCloud && x2 < max.x; ++x2) {
				for (int y2 = min.y; isCloud && y2 < max.y; ++y2) {
					Block b = w.getBlockAt(x2, y2, z2);
					++checked;
					switch (b.getType()) {
						case AIR:
						case CAVE_AIR:
						case SNOW:
						case SNOW_BLOCK:
						case POWDER_SNOW:
							break;
						default:
							//System.out.printf("Found non-cloud block at (%d, %d, %d)\n", x2, y2, z2);
							isCloud = false;
					}
				}
			}
		}
		if (isCloud) {
			//System.out.printf("Cloud updating! (%d, %d, %d) -> (%d, %d, %d) = %d\n", min.x, min.y, min.z, max.x, max.y, max.z, checked);
			// replace snow with powder!
			for (int z2 = min.z; z2 < max.z; ++z2) {
				for (int x2 = min.x; x2 < max.x; ++x2) {
					for (int y2 = min.y; y2 < max.y; ++y2) {
						Block b = w.getBlockAt(x2, y2, z2);
						if (b.getType() == Material.SNOW_BLOCK) {
							b.setType(block_material, false);
						}
					}
				}
			}
		} else {
			//System.out.printf("Cloud Structure found - skipping! (%d, %d, %d) -> (%d, %d, %d) = %d\n", min.x, min.y, min.z, max.x, max.y, max.z, checked);
			// mark as found and move on.
			for (int z2 = min.z; z2 < max.z; ++z2) {
				for (int x2 = min.x; x2 < max.x; ++x2) {
					for (int y2 = min.y; y2 < max.y; ++y2) {
						Block b = w.getBlockAt(x2, y2, z2);
						switch (b.getType()) {
							case AIR:
							case CAVE_AIR:
							case SNOW:
								break;
							case SNOW_BLOCK:
								b.setMetadata("clouds", new FixedMetadataValue(this, cloudScanVersion));
								break;
							default:
								y2 = 999;
						}
					}
				}
			}
		}
	}

	void genClouds(Chunk c, boolean force) {
		if (c.getWorld().getEnvironment() != Environment.NORMAL) {
			return;
		}
		//TODO: use a database for this!
		if (!force) {
			List<MetadataValue> meta = c.getBlock(0, 0, 0).getMetadata("clouds");
			if (!meta.isEmpty()) {
				// just in case, check the data
				boolean ignore = false;
				for (MetadataValue v : meta) {
					if (v.value() instanceof Boolean && (Boolean) v.value()) {
						ignore = true;
						break;
					}
				}
				if (ignore) {
					return;
				}
			}
			// check if allowed on this world
			if (!global_enabled) {
				boolean allow = false;
				final String name = c.getWorld().getName();
				for (String w : enabled_worlds) {
					if (w != null && name.equalsIgnoreCase(w)) {
						allow = true;
						break;
					}
				}
				if (!allow) {
					return;
				}
			}
		}
		SimplexOctaveGenerator noise = worldNoiseGenerators.get(c.getWorld());
		if (noise == null) {
			worldNoiseGenerators.put(c.getWorld(), noise = new SimplexOctaveGenerator(c.getWorld().getSeed(), noiseOctaves));
			noise.setScale(noiseScale);
		}

		double ix = (c.getX() * 16),
				iz = (c.getZ() * 16); //(Math.abs(c.getZ() + 10000) * 16) + 0.5;

		// generate a double-array of the values returned for this layer (subtract threshold from each cell)
		// for any values > 0, check that all neighbors are set
		// once all set, return to the chunk section - for each defined area, calculate the cloud base height
		// then add top 2 layers (adding next layer if prior exists)
		// additionally - calculate all 3 layers before determining base height
		double[][][] cloudchunk = new double[16][maxCloudDepth][16];
		boolean cloud = false;
		for (int x = 0; x < 16; ++x) {
			for (int y = 0; y < maxCloudDepth; ++y) {
				for (int z = 0; z < 16; ++z) {
					cloudchunk[x][y][z] = (noise.noise(ix + x, y * yScale, iz + z, noisefreq, noiseamp, true)) - noisethreshold;
					if (cloudchunk[x][y][z] > 0) {
						cloud = true;
					}
				}
			}
		}
		if (!cloud) {
			return;
		}

		// quick height map of the chunk
		int landHeight[][] = new int[16][16];
		for (int x = 0; x < 16; ++x) {
			for (int z = 0; z < 16; ++z) {
				for (int y = 255; y > 0; --y) {
					final Block b = c.getBlock(x, y, z);
					if (!b.isEmpty() && !(b.getType() == block_material)
							&& groundTypes.contains(b.getType())) {
						landHeight[x][z] = y;
						break;
					}
				}
			}
		}

		// ready space for 5 chunks on each side
		final int maxSpan = 5;
		final int start = maxSpan * 16, end = (maxSpan * 2 + 1) * 16;
		double[][][] cloudCache = new double[end][maxCloudDepth][end];

		// find & seperate the clouds
		Stack<Point3D> path = new Stack();
		double[][][] tempCloud = new double[16][maxCloudDepth][16];
		double total;
		int count;
		for (int x = 0; x < 16; ++x) {
			int y = 0;
			//for (; y < maxCloudDepth; ++y) {
			for (int z = 0; z < 16; ++z) {
				if (cloudchunk[x][y][z] > 0) {
					for (int x2 = 0; x2 < 16; ++x2) {
						for (int y2 = 0; y2 < maxCloudDepth; ++y2) {
							for (int z2 = 0; z2 < 16; ++z2) {
								tempCloud[x2][y2][z2] = 0;
							}
						}
					}
					count = 1;
					tempCloud[x][y][z] = total = cloudchunk[x][y][z];
					cloudchunk[x][y][z] = 0;

					Point3D current = new Point3D(x, y, z);
					while (current != null) {
						int x2, y2, z2;
						double v;
						/*
						if(current.x + 1 < start && current.y >= 0 && current.z >= -start) {
							
						}
						
						cloudCache[start + x2][y2][start + z2] = noise.noise(x2 + ix, y2 * yScale, z2 + iz, noisefreq, noiseamp, true) - noisethreshold;
						
						if () {*/
						//*
						if (false // spacer, for formatting, lol
								|| ((x2 = current.x + 1) < start && (y2 = current.y) >= 0 && (z2 = current.z) >= -start
								&& (v = ((x2 >= 0 && x2 < 16 && z2 >= 0 && z2 < 16) ? cloudchunk[x2][y2][z2]
										: (cloudCache[start + x2][y2][start + z2] == 0 ? (cloudCache[start + x2][y2][start + z2] = (noise.noise(x2 + ix, y2 * yScale, z2 + iz, noisefreq, noiseamp, true) - noisethreshold)) : cloudCache[start + x2][y2][start + z2]))) > 0) //
								|| ((x2 = current.x) >= -start && (y2 = current.y) >= 0 && (z2 = current.z + 1) < start
								&& (v = ((x2 >= 0 && x2 < 16 && z2 >= 0 && z2 < 16) ? cloudchunk[x2][y2][z2]
										: (cloudCache[start + x2][y2][start + z2] == 0 ? (cloudCache[start + x2][y2][start + z2] = (noise.noise(x2 + ix, y2 * yScale, z2 + iz, noisefreq, noiseamp, true) - noisethreshold)) : cloudCache[start + x2][y2][start + z2]))) > 0) //
								|| ((x2 = current.x) >= -start && (y2 = current.y + 1) < maxCloudDepth && (z2 = current.z) >= -start
								&& (v = ((x2 >= 0 && x2 < 16 && z2 >= 0 && z2 < 16) ? cloudchunk[x2][y2][z2]
										: (cloudCache[start + x2][y2][start + z2] == 0 ? (cloudCache[start + x2][y2][start + z2] = (noise.noise(x2 + ix, y2 * yScale, z2 + iz, noisefreq, noiseamp, true) - noisethreshold)) : cloudCache[start + x2][y2][start + z2]))) > 0) //
								|| ((x2 = current.x - 1) >= -start && (y2 = current.y) >= 0 && (z2 = current.z) >= -start
								&& (v = ((x2 >= 0 && x2 < 16 && z2 >= 0 && z2 < 16) ? cloudchunk[x2][y2][z2]
										: (cloudCache[start + x2][y2][start + z2] == 0 ? (cloudCache[start + x2][y2][start + z2] = (noise.noise(x2 + ix, y2 * yScale, z2 + iz, noisefreq, noiseamp, true) - noisethreshold)) : cloudCache[start + x2][y2][start + z2]))) > 0) //
								|| ((x2 = current.x) >= -start && (y2 = current.y) >= 0 && (z2 = current.z - 1) >= -start
								&& (v = ((x2 >= 0 && x2 < 16 && z2 >= 0 && z2 < 16) ? cloudchunk[x2][y2][z2]
										: (cloudCache[start + x2][y2][start + z2] == 0 ? (cloudCache[start + x2][y2][start + z2] = (noise.noise(x2 + ix, y2 * yScale, z2 + iz, noisefreq, noiseamp, true) - noisethreshold)) : cloudCache[start + x2][y2][start + z2]))) > 0) //
								|| ((x2 = current.x) >= -start && (y2 = current.y - 1) >= 0 && (z2 = current.z) >= -start
								&& (v = ((x2 >= 0 && x2 < 16 && z2 >= 0 && z2 < 16) ? cloudchunk[x2][y2][z2]
										: (cloudCache[start + x2][y2][start + z2] == 0 ? (cloudCache[start + x2][y2][start + z2] = (noise.noise(x2 + ix, y2 * yScale, z2 + iz, noisefreq, noiseamp, true) - noisethreshold)) : cloudCache[start + x2][y2][start + z2]))) > 0) //
								) {
							total += v;
							++count;
							if (x2 >= 0 && x2 < 16 && z2 >= 0 && z2 < 16) {
								tempCloud[x2][y2][z2] = v;
								cloudchunk[x2][y2][z2] = 0;
							} else {
								cloudCache[start + x2][y2][start + z2] = -1;
							}
							path.push(current);
							current = new Point3D(x2, y2, z2);
						} else if (!path.isEmpty()) {
							current = path.pop();
						} else {
							current = null;
						}
					}
					if (count >= min_cloud_size && (max_cloud_size <= 0 || count <= max_cloud_size)) {
						// now have the cloud
						total /= count;
						// total is now a value 0 < x < freq - threshold
						int height = cloudFloor + (int) Math.round((cloudCeiling - cloudFloor - (maxCloudDepth - 1)) * ((total % .05) / .05));
						int groundHeight = 0;

						// don't generate the cloud if it collides with something
						boolean collision = false;
						for (int x2 = 0; x2 < 16 && !collision; ++x2) {
							for (int y2 = 0; y2 < maxCloudDepth && !collision; ++y2) {
								for (int z2 = 0; z2 < 16 && !collision; ++z2) {
									if (tempCloud[x2][y2][z2] > 0) {
										if (landHeight[x2][z2] - y > groundHeight) {
											groundHeight = landHeight[x2][z2] - y;
										}
										if (!c.getBlock(x2, height + y2, z2).isEmpty()) {
											collision = true;
										}
									}
								}
							}
						}
						//minCloudHeight
						if (!collision && height - groundHeight >= minCloudHeight) {
							for (int x2 = 0; x2 < 16; ++x2) {
								for (int y2 = 0; y2 < maxCloudDepth; ++y2) {
									for (int z2 = 0; z2 < 16; ++z2) {
										if (tempCloud[x2][y2][z2] > 0) {
											c.getBlock(x2, height + y2, z2).setType(block_material, false);
											//c.getBlock(x2, height + y2, z2).setTypeIdAndData(block_id, (byte) block_data, false);
										}
									}
								}
							}
						}
					}
				}
			}
			// double-checking...
			path.clear();
			//}
		}
		// mark this chunk so don't mistakenly re-generate clouds
		//c.getBlock(0, 0, 0).setTypeId(7);
		//c.getBlock(0, 0, 0).setMetadata("clouds", new FixedMetadataValue(this, true));
	}

	@EventHandler
	void onChunkGen(ChunkLoadEvent event) {
		if(event.isNewChunk()) genClouds(event.getChunk(), true);
		//if(!event.isNewChunk()) updateClouds(event.getChunk());
	}

//	@EventHandler
//	void onSpawn(CreatureSpawnEvent event) {
//		// cancel spawning on 'clouds'
//		Block b = event.getLocation().getBlock();
//		while (b != null && b.isEmpty()) {
//			b = b.getRelative(BlockFace.DOWN);
//		}
//		if (b != null
//				&& b.getTypeId() == block_id
//				&& b.getY() >= cloudFloor
//				&& event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
//			event.setCancelled(true);
//		}
//	}
	@EventHandler(priority = EventPriority.LOW)
	void onCloudFall(EntityDamageEvent event) {
		if (softClouds && event.getCause() == DamageCause.FALL) {
			Block landing = getBlockBelow(event.getEntity().getLocation());
			if (landing.getY() > cloudFloor && landing.getY() < cloudCeiling
					&& landing.getType() == block_material) {
				event.setCancelled(true);
			}
		}
	}

	Block getBlockBelow(Location l) {
		Block b = l.getBlock();
		while (b.getY() > 0 && b.isEmpty()) {
			b = b.getRelative(BlockFace.DOWN);
		}
		return b;
	}

}

class Point3D {

	public int x, y, z;

	public Point3D(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

}
