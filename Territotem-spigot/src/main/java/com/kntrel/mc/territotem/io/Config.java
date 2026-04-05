package com.kntrel.mc.territotem.io;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.potion.PotionType;
import java.util.List;
import java.util.logging.Level;

public class Config {
    
    //ENUMS
    public enum GroupLevelReach {
        noOne, all, responsible, members, lvl;
        private int level_ = -1;

        public void setLevel(int level) {
            this.level_ = level;
        }
        public int getLevel() {
            return this.level_;
        }
    }

    //RECORDS
    public record TotemInteractionData(Material item, int count, boolean consume) {}
    public record TitleData(int fadeIn, int stay, int fadeOut) {}
    public record ParticleData(Particle particle, int count, Double[] delta) {
        public void spawn(Location location, double speed) {
            if (location.getWorld() == null) {
                throw new IllegalArgumentException("The location must contain a World");
            }
            this.spawn(location.getWorld(),location,speed);
        }
        public void spawn(World world, Location location, double speed) {
            Double[] delta = this.delta();
            world.spawnParticle(this.particle(),location,this.count,delta[0].doubleValue(),delta[1].doubleValue(),delta[2].doubleValue(),speed);
        }
    }

    //FIELDS
    public String configPath = "plugins/Landlords";

    public Level loggingLevel = Level.INFO;

    public String defaultLanguageCode = "en";

    public int totemDefaultGroupLevel = 1;

    public TotemInteractionData totemUpgradeItem = new TotemInteractionData(Material.DIAMOND,1,true);

    public Material totemDirectionalItem = Material.BLAZE_ROD;

    public int totemInteractCoolDown = 400;

    public double totemDropBackRate = 0.5;

    public Config.ParticleData totemEnableParticleData = new ParticleData(Particle.REVERSE_PORTAL,400,new Double[] {0.0,0.0,0.0});

    public Config.ParticleData totemHurtParticleData = new ParticleData(Particle.DRAGON_BREATH,120,new Double[] {.3,.3,.3});

    public Config.ParticleData totemFeedParticleData = new ParticleData(Particle.PORTAL,250,new Double[] {.3,.3,.3});

    public Config.ParticleData totemDisableParticleData = new ParticleData(Particle.CRIT,300,new Double[] {.6,.6,.6});

    public List<PotionType> totemDestroyArrowEffects = List.of(PotionType.POISON);

    public boolean totemAttackAlerting = true;

    public Material deedsExchangeItem = Material.WRITABLE_BOOK;

    public int deedsPlayersPerPage = 13;

    public int deedsPlayersForNewPage = 10;

    public boolean deedsUnsignable = true;

    public int regionsMinimumNameLength = 8;

    public int regionsMaximumNameLength = 32;

    public int regionsBorderPersistence = 420;

    public List<Material> regionsEnforcedButtons = List.of(Material.STONE_BUTTON, Material.POLISHED_BLACKSTONE_BUTTON);

    public List<Material> regionsLeverLockerBlocks = List.of(Material.COPPER_BLOCK, Material.IRON_BLOCK);

    public Config.TitleData regionsNameTitleData = new TitleData(10,30,8);

    public boolean regionsNameTitleEnabled = true;

    public double regionFireExtinguishChance = 0.4;

    public Config.GroupLevelReach msgReachRegionResize = GroupLevelReach.all;
}
