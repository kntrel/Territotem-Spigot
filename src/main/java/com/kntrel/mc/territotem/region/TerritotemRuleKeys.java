package com.kntrel.mc.territotem.region;

import com.kntrel.mc.territotem.Territotem;
import com.jkantrell.regionslib.regions.rules.RuleDataType;
import com.jkantrell.regionslib.regions.rules.RuleEnumDataType;
import com.jkantrell.regionslib.regions.rules.RuleKey;

public class TerritotemRuleKeys {

    //DATATYPES
    public enum TntProtection { none, all, ignitor }

    //KEYS
    public final RuleKey CREEPER_PROTECTED;
    public final RuleKey TNT_PROTECTED;
    public final RuleKey FIRE_PROTECTED;
    public final RuleKey RAID_PROTECTED;
    public final RuleKey NO_MONSTER_SPAWN;
    public final RuleKey AUTOPLANT;
    public final RuleKey FARMLAND_PROTECTED;

    //FIELDS
    private static Territotem mainInstance_ = null;

    public TerritotemRuleKeys(Territotem territotemInstance) {
        mainInstance_ = territotemInstance;
        this.CREEPER_PROTECTED = RuleKey.registerNew(mainInstance_,"creeperProtected", RuleDataType.BOOL);
        this.TNT_PROTECTED = RuleKey.registerNew(mainInstance_,"tntProtected", new RuleEnumDataType<>(TntProtection.class));
        this.FIRE_PROTECTED = RuleKey.registerNew(mainInstance_,"fireProtected",RuleDataType.BOOL);
        this.RAID_PROTECTED = RuleKey.registerNew(mainInstance_,"raidProtected",RuleDataType.BOOL);
        this.NO_MONSTER_SPAWN = RuleKey.registerNew(mainInstance_,"noMonsterSpawn",RuleDataType.BOOL);
        this.AUTOPLANT = RuleKey.registerNew(mainInstance_,"autoplant",RuleDataType.BOOL);
        this.FARMLAND_PROTECTED = RuleKey.registerNew(mainInstance_,"farmlandProtected",RuleDataType.BOOL);
    }
}
