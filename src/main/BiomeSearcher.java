// @formatter:off
package main;

import Util.Util;
import amidst.logging.AmidstLogger;
import amidst.mojangapi.file.LauncherProfile;
import amidst.mojangapi.file.MinecraftInstallation;
import amidst.mojangapi.minecraftinterface.MinecraftInterface;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaceCreationException;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaceException;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaces;
import amidst.mojangapi.world.*;
import amidst.mojangapi.world.biome.Biome;
import amidst.mojangapi.world.biome.UnknownBiomeIndexException;
import amidst.mojangapi.world.coordinates.CoordinatesInWorld;
import amidst.mojangapi.world.coordinates.Resolution;
import amidst.parsing.FormatException;
import gui.GUI;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A service that searches for worlds that match specific criteria.
 *
 * @author scudobuio, Zodsmar, YourCoalAlt
 */
public class BiomeSearcher implements Runnable {

	private WorldBuilder mWorldBuilder;

	private MinecraftInterface mMinecraftInterface;

	/**
	 * The width of each quadrant of the search area.
	 * <p>
	 * The value of this field is greater than {@code 0}.
	 */
	private int mSearchQuadrantWidth;

	/**
	 * The height of each quadrant of the search area.
	 * <p>
	 * The value of this field is greater than {@code 0}.
	 */
	private int mSearchQuadrantHeight;

	/**
	 * The number of matching worlds to discover.
	 * <p>
	 * The value of this field is greater than or equal to {@code 0}.
	 */
	private int mMaximumMatchingWorldsCount;

	public BiomeSearcher(String minecraftVersion,
			int searchQuadrantWidth, int searchQuadrantHeight, int maximumMatchingWorldsCount)
			throws IOException, FormatException, MinecraftInterfaceCreationException {
		this.mWorldBuilder = WorldBuilder.createSilentPlayerless();
		MinecraftInstallation minecraftInstallation;

		String pathToDirectory = GUI.textBoxMinecraftDir.getText();
		if (pathToDirectory == null ||
			pathToDirectory.trim().equals(""))
		{
			minecraftInstallation = MinecraftInstallation.newLocalMinecraftInstallation();
		} else {
			minecraftInstallation = MinecraftInstallation.newLocalMinecraftInstallation(pathToDirectory);
		}
		LauncherProfile launcherProfile = null;
		try{
			launcherProfile = minecraftInstallation.newLauncherProfile(minecraftVersion);
		} catch (FileNotFoundException e) {
			Util.console("No install directory found for Minecraft version " + minecraftVersion + "!");
			throw e;
		}
		this.mMinecraftInterface = MinecraftInterfaces.fromLocalProfile(launcherProfile);
		this.mSearchQuadrantWidth = searchQuadrantWidth;
		this.mSearchQuadrantHeight = searchQuadrantHeight;
		this.mMaximumMatchingWorldsCount = maximumMatchingWorldsCount;

	}

	/**
	 * Creates a random, default world using the default (empty) generator
	 * options.
	 */
	World createWorld() throws MinecraftInterfaceException {
		Consumer<World> onDispose = world -> {};
		WorldOptions worldOptions = new WorldOptions(WorldSeed.random(), WorldType.DEFAULT); // TODO allow players to choose?
		return this.mWorldBuilder.from(this.mMinecraftInterface, onDispose, worldOptions);
	}
	
	int[] getBiomeCodes(long nwCornerX, long nwCornerY, int width, int height) throws MinecraftInterfaceException {
		return this.mMinecraftInterface.getBiomeData(
				(int) (Resolution.QUARTER.convertFromWorldToThis(nwCornerX)),
				(int) (Resolution.QUARTER.convertFromWorldToThis(nwCornerY)),
				width / 4,
				height / 4,
				true // useQuarterResolution
		);
	}
	
	/**
	 * Determines whether to accept a world.
	 *
	 * @throws MinecraftInterfaceCreationException
	 * @throws FormatException
	 * @throws IOException
	 * @throws InterruptedException
	 */

	Biome[] biomes = {}; boolean searchBiomes = true;
	Biome[] rejectedBiomes = {}; boolean searchRejectedBiomes = true;
	HashMap<Biome, String> biomeSets = new HashMap<Biome, String>(); boolean searchBiomeSets = true;
	HashMap<Biome, String> rejectedBiomeSets = new HashMap<Biome, String>(); boolean searchRejectedBiomesSets = true;
	StructureSearcher.Type[] structures = {};
	//, Biome.forest, Biome.desert, Biome.birchForest, Biome.plains

	boolean accept(World world) throws MinecraftInterfaceException, UnknownBiomeIndexException, InterruptedException,
			IOException, FormatException, MinecraftInterfaceCreationException {
		//! This returns the actual spawnpoint or should... but it doesn't it is off. Double checking
		//! in amidst and it is incorrect I created the world to see if this was correct and amidst was off
		//! this is incorrect and amidst is... no idea why...
		// TODO: Look into this (probably will fix the structures being off too...)
		//CoordinatesInWorld searchCenter = world.getSpawnWorldIcon().getCoordinates();

		// ! This returns [0, 0] everytime
		CoordinatesInWorld searchCenter = CoordinatesInWorld.origin();

		if (searchCenter == null) {
			// The world spawn could not be determined.
			return false;
		}
		long searchCenterX = searchCenter.getX();
		long searchCenterY = searchCenter.getY();
		int[] biomeCodes = getBiomeCodes(
				searchCenterX - this.mSearchQuadrantWidth,
				searchCenterY - this.mSearchQuadrantHeight,
				2 * this.mSearchQuadrantWidth,
				2 * this.mSearchQuadrantHeight);
		int biomeCodesCount = biomeCodes.length;
		//System.out.println(biomeCodesCount);
		
		if (biomes.length == 0 && rejectedBiomes.length == 0 && biomeSets.size() == 0 && rejectedBiomeSets.size() == 0) {
			Util.console("Creating Selected Biomes from list...");
			Util.console("Creating Rejected Biomes from list...");
		}
		
		if (biomes.length == 0 && searchBiomes) {
			biomes = GUI.manageCheckedCheckboxes();
			if (biomes.length == 0 && searchBiomes) {
				searchBiomes = false;
				System.out.println(searchBiomes);
			}
		}
		
		if (rejectedBiomes.length == 0 && searchRejectedBiomes) {
			rejectedBiomes = GUI.manageCheckedCheckboxesRejected();
			if (rejectedBiomes.length == 0 && searchRejectedBiomes) {
				searchRejectedBiomes = false;
				System.out.println(searchRejectedBiomes);
			}
		}

		if (biomeSets.size() == 0 && searchBiomeSets) {
			biomeSets = GUI.manageCheckedCheckboxesSets();
			if (biomeSets.size() == 0 && searchBiomeSets) {
				searchBiomeSets = false;
				System.out.println(searchBiomeSets);
			}
		}

		if (rejectedBiomeSets.size() == 0 && searchRejectedBiomesSets) {
			rejectedBiomeSets = GUI.manageCheckedCheckboxesSetsRejected();
			if (rejectedBiomeSets.size() == 0 && searchRejectedBiomesSets) {
				searchRejectedBiomesSets = false;
				System.out.println(rejectedBiomeSets);
			}
		}
		
		if (!searchBiomes && !searchRejectedBiomes && !searchBiomeSets && !searchRejectedBiomesSets) {
			Util.console("\nNo biomes are selected or rejected!\nPlease select Biomes!\nSearch has cancelled.\nRecommend you clear console!\n");
			GUI.stop();
			return false;
		}
		
		boolean hasRequiredStructures = false;
		if (Main.DEV_MODE) {
			if (structures.length == 0 && GUI.findStructures.isSelected()) {
				Util.console("Creating Structures from list...");
				structures = GUI.manageCheckedCheckboxesFindStructures();
			}
		}
		
		if (structures.length > 0) {
			hasRequiredStructures = StructureSearcher.hasStructures(
					structures,
					world,
					searchCenterX,
					searchCenterY
					);
			
			// Could meet structure requirements, move to next seed.
			if (!hasRequiredStructures) return false;
		}
		
		
		// Start with a set of all biomes to find.
		Set<Biome> undiscoveredBiomes = new HashSet<>(Arrays.asList(biomes));
		Set<Biome> undiscoveredRejectedBiomes = new HashSet<>(Arrays.asList(rejectedBiomes));
		HashMap<Biome, String> undiscoveredBiomeSets = new HashMap<>(biomeSets);
		HashMap<Biome, String> undiscoveredRejectedBiomeSets = new HashMap<>(rejectedBiomeSets);

		for (int biomeCodeIndex = 0; biomeCodeIndex < biomeCodesCount; biomeCodeIndex++) {
			if (undiscoveredBiomes.remove(Biome.getByIndex(biomeCodes[biomeCodeIndex]))) {
				// A new biome has been found.
				// Determine whether this was the last biome to find.
			}
			
			// In theory this should return false if the world contains a specific biome
			if(undiscoveredRejectedBiomes.remove(Biome.getByIndex(biomeCodes[biomeCodeIndex]))) {
				//Works except for ocean. No idea why
				return false; // Adding this makes excluded biomes not be resulted anymore. DO NOT REMOVE UNLESS YOU HAVE A FIX FOR THIS
			}

			if (undiscoveredBiomeSets.containsKey(Biome.getByIndex(biomeCodes[biomeCodeIndex]))) {
				String setValue = undiscoveredBiomeSets.get(Biome.getByIndex(biomeCodes[biomeCodeIndex]));
				// Get the iterator over the HashMap
				undiscoveredBiomeSets.entrySet()
				.removeIf(
					entry -> (setValue.equals(entry.getValue())));
			}

			if (undiscoveredRejectedBiomeSets.containsKey(Biome.getByIndex(biomeCodes[biomeCodeIndex]))) {
				return false;
			}
		}

		if (undiscoveredBiomes.isEmpty() && undiscoveredBiomeSets.isEmpty()) {
			System.out.println("Valid Seed: "+world.getWorldSeed().getLong());
	//			&& (GUI.findStructures.isSelected() && hasStructures)) {
			return true;
		}
		System.out.println(undiscoveredBiomes);
		System.out.println(undiscoveredBiomeSets);
		return false;
	}

	/**
	 * Updates the progress output for a world that has been rejected.
	 *
	 * @param rejectedWorldsCount the number of worlds that have been rejected
	 *            since the last world was accepted
	 */
	
	

	static void updateRejectedWorldsProgress(int rejectedWorldsCount) {
		GUI.seedCount.setText("Current Rejected Seed Count: " + rejectedWorldsCount);
		GUI.totalSeedCount.setText("Total Rejected Seed Count: " + totalRejectedSeedCount);
	}

	/**
	 * Updates the progress output for a world that has been accepted.
	 *
	 * @param rejectedWorldsCount the number of worlds that have been rejected
	 *            since the last world was accepted
	 * @param acceptedWorldsCount the number of worlds that have been accepted,
	 *            including the given world
	 * @param acceptedWorld the world that has been accepted
	 */
	static void updateAcceptedWorldsProgress(
			int rejectedWorldsCount,
			int acceptedWorldsCount,
			World acceptedWorld) {
		if (rejectedWorldsCount / (1 << 4) > 0) {
			// An incomplete line of dots was printed.
			Util.console("");
		}
		Util.console(
				acceptedWorldsCount + ": " + acceptedWorld.getWorldSeed().getLong() + " (rejected "
						+ rejectedWorldsCount + ")");
	}
	
	/**
	 * Searches for matching worlds, and prints the seed of each matching world
	 * to the given output stream.
	 *
	 * @throws MinecraftInterfaceCreationException
	 * @throws FormatException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static int totalRejectedSeedCount = 0;
	void search() throws InterruptedException,IOException, FormatException, MinecraftInterfaceCreationException {
		int rejectedWorldsCount = 0;
		int acceptedWorldsCount = 0;
		
		while (acceptedWorldsCount < this.mMaximumMatchingWorldsCount && GUI.running) {
			if (!GUI.paused) {
				World world;
				try {
					world = createWorld();
				} catch (MinecraftInterfaceException e) {
					// TODO log
					rejectedWorldsCount++;
					totalRejectedSeedCount++;
					updateRejectedWorldsProgress(rejectedWorldsCount);
					continue;
				}
				boolean isWorldAccepted;
				try {
					isWorldAccepted = accept(world);
				} catch (MinecraftInterfaceException | UnknownBiomeIndexException e) {
					// Biome data for the world could not be obtained.
					// Biome data included an unknown biome code.
					// TODO log
					rejectedWorldsCount++;
					totalRejectedSeedCount++;
					updateRejectedWorldsProgress(rejectedWorldsCount);
					continue;
				}
				if (!isWorldAccepted) {
					rejectedWorldsCount++;
					totalRejectedSeedCount++;
					updateRejectedWorldsProgress(rejectedWorldsCount);
					continue;
				}
				acceptedWorldsCount++;
				updateAcceptedWorldsProgress(rejectedWorldsCount, acceptedWorldsCount, world);
				rejectedWorldsCount = 0;
			}
			
			//Literally without pause doesn't work....
			System.out.print("");
		}
		
//		Util.console("Finished Search!");
		GUI.stop();
	}

	/**
	 * Searches for matching worlds, and prints the seed of each matching world
	 * to the standard output stream.
	 */
	@Override
	public void run() {
		//Util.printingSetup();
		try {
			search();
		} catch (InterruptedException | IOException | FormatException | MinecraftInterfaceCreationException e) {
			e.printStackTrace();
		}
	}
	
	static {
		// By default, AMIDST logs to the standard output stream and to an
		// in-memory buffer.
		// Turn off logging to the standard output stream.
		// Otherwise, the desired output could be overwhelmed by noise.
		AmidstLogger.removeListener("console");
		// Turn off logging to the in-memory buffer.
		// Otherwise, the JVM could run out of heap space when checking many
		// seeds.
		AmidstLogger.removeListener("master");
		// TODO add file logging?
	}
	/*
	 * public static void main(String ... args) throws IOException,
	 * FormatException, MinecraftInterfaceCreationException { // Execution
	 * options. // Hard-coded for now, but they could be specified as
	 * command-line arguments. MainGUI gui = new MainGUI(); String
	 * minecraftVersionId = "1.13"; SearchCenterKind searchCenterKind =
	 * SearchCenterKind.ORIGIN; int searchQuadrantWidth = 2048; int
	 * searchQuadrantHeight = 2048; int maximumMatchingWorldsCount = 10; //
	 * Execute. new BiomeSearcher(minecraftVersionId, searchCenterKind,
	 * searchQuadrantWidth, searchQuadrantHeight,
	 * maximumMatchingWorldsCount).run();
	 *
	 * }
	 */
}
