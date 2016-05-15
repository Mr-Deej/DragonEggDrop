package com.ninjaguild.dragoneggdrop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class DragonDeathRunnable implements Runnable {

	private final DragonEggDrop plugin;
	
	private final World world;
	private final boolean placeEgg;
	
	private int particleAmount = 0;
	private double particleLength = 0D;
	private double particleExtra = 0D;
	private long particleInterval = 0L;
	private double oX = 0D;
	private double oY = 0D;
	private double oZ = 0D;
	private Particle particleType = null;

	private boolean respawnDragon = false;

	public DragonDeathRunnable(final DragonEggDrop plugin, final World world, boolean prevKilled) {
		this.plugin = plugin;
		this.world = world;
		this.placeEgg = prevKilled;

		particleAmount = plugin.getConfig().getInt("particle-amount", 4);
		particleLength = plugin.getConfig().getDouble("particle-length", 6.0D);
		particleExtra = plugin.getConfig().getDouble("particle-extra", 0.0D);
		particleInterval = plugin.getConfig().getLong("particle-interval", 1L);
		oX = plugin.getConfig().getDouble("particle-offset-x", 0.25D);
		oY = plugin.getConfig().getDouble("particle-offset-y", 0D);
		oZ = plugin.getConfig().getDouble("particle-offset-z", 0.25D);
		particleType = Particle.valueOf(plugin.getConfig().getString("particle-type", "FLAME").toUpperCase());

		respawnDragon = plugin.getConfig().getBoolean("respawn", false);
	}

	@Override
	public void run() {
		double startY = plugin.getConfig().getDouble("egg-start-y", 180D);

		new BukkitRunnable()
		{
			double currentY = startY;
			Location pLoc = new Location(world, 0.5D, currentY, 0.5D, 0f, 90f);

			@Override
			public void run() {
				currentY -= 1D;
				pLoc.setY(currentY);

				for (double d = 0; d < particleLength; d+=0.1D) {
					world.spawnParticle(particleType, pLoc.clone().add(pLoc.getDirection().normalize().multiply(d * -1)),
							particleAmount, oX, oY, oZ, particleExtra, null);
				}

				if (world.getBlockAt(pLoc).getType() == Material.BEDROCK) {
					cancel();

					new BukkitRunnable()
					{
						@Override
						public void run() {
							Location prevLoc = pLoc.clone().add(new Vector(0D, 1D, 0D));

							int lightningAmount = plugin.getConfig().getInt("lightning-amount", 4);
							for (int i = 0; i < lightningAmount; i++) {
								world.strikeLightningEffect(prevLoc);
							}

							if (placeEgg) {
								String rewardType = plugin.getConfig().getString("drop-type", "egg");
								if (rewardType.equalsIgnoreCase("chest")) {
									//spawn a loot chest
									plugin.getDEDManager().getLootManager().placeChest(prevLoc);
								}
								else if (rewardType.equalsIgnoreCase("chance")) {
									double chance = plugin.getConfig().getInt("chest-spawn-chance", 20);
									chance = chance / 100D;
									if (Math.random() <= chance) {
										plugin.getDEDManager().getLootManager().placeChest(prevLoc);
									}
									else {
										world.getBlockAt(prevLoc).setType(Material.DRAGON_EGG);
									}
								}
								else if (rewardType.equalsIgnoreCase("all")) {
									plugin.getDEDManager().getLootManager().placeChestAll(prevLoc);
								}
								else {
									world.getBlockAt(prevLoc).setType(Material.DRAGON_EGG);
								}
							}

							//landing particles
							for (int i = 0; i <= 360; i++) {
								double x = Math.cos(i);
								double y = Math.random();
								double z = Math.sin(i);
								Vector vec = new Vector(x, y, z);
								prevLoc.add(vec);
								world.spawnParticle(Particle.BLOCK_DUST, prevLoc,
										2, 0D, 0D, 0D, 0.5D, new MaterialData(Material.BEDROCK));
								prevLoc.subtract(vec);
							}

							if (respawnDragon) {
								if (prevLoc.getWorld().getPlayers().size() > 0) {
									plugin.getDEDManager().startRespawn(prevLoc);
								}
//								new BukkitRunnable() {
//									@Override
//									public void run() {
//										boolean dragonExists = !prevLoc.getWorld().getEntitiesByClasses(EnderDragon.class).isEmpty();
//										if (dragonExists) {
//											return;
//										}
//										//start respawn process
//										Location[] crystalLocs = new Location[] {
//												prevLoc.clone().add(3, -3, 0),
//												prevLoc.clone().add(0, -3, 3),
//												prevLoc.clone().add(-3, -3, 0),
//												prevLoc.clone().add(0, -3, -3)
//										};
//										
//										EnderDragonBattle dragonBattle = plugin.getEnderDragonBattleFromWorld(world);
//										
//										for (int i = 0; i < crystalLocs.length; i++) {
//											Location cLoc = crystalLocs[i];
//											new BukkitRunnable() {
//												@Override
//												public void run() {
//													Chunk crystalChunk = world.getChunkAt(cLoc);
//													if (!crystalChunk.isLoaded()) {
//														crystalChunk.load();
//													}
//													EnderCrystal crystal = (EnderCrystal)world.spawnEntity(cLoc, EntityType.ENDER_CRYSTAL);
//													crystal.setShowingBottom(false);
//													
//													cLoc.getWorld().createExplosion(cLoc.getX(), cLoc.getY(), cLoc.getZ(), 0F, false, false);
//													cLoc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, cLoc, 0);
//
//													dragonBattle.e();
//												}
//
//											}.runTaskLater(plugin, (i + 1) * 22);
//										}
//									}
//
//								}.runTaskLater(plugin, respawnDelay * 20);
							}
						}

					}.runTask(plugin);
				}
			}

		}.runTaskTimerAsynchronously(plugin, 0L, particleInterval);
	}

}
