package org.aliceGrimoire.alicegrimoire.entity.doll.combat;

import net.minecraft.world.entity.LivingEntity;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.DollType;

import java.util.HashMap;
import java.util.Map;

public class DollCombatManager {
    private final DollEntity doll;
    private final Map<DollType, ICombatStrategy> strategyMap = new HashMap<>();

    public DollCombatManager(DollEntity doll) {
        this.doll = doll;
    }

    public void tick() {
        LivingEntity target = doll.getTarget();
        if (target == null || !target.isAlive()) return;

        DollType type = doll.getDollType();
        ICombatStrategy strategy = strategyMap.computeIfAbsent(type, DollType::createStrategy);
        if (strategy != null) {
            strategy.tick(doll, target, doll.getOwner());
        }
    }
}