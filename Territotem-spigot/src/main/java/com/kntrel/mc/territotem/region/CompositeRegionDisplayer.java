package com.kntrel.mc.territotem.region;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.display.DisplayToken;
import com.kntrel.mc.regionLib.region.display.RegionDisplayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CompositeRegionDisplayer implements RegionDisplayer {

    //FIELDS
    private final CopyOnWriteArrayList<RegionDisplayer> delegates_ = new CopyOnWriteArrayList<>();
    private final Map<Long, List<DisplayToken>> displayTokens_ = new HashMap<>();
    private long nextId_ = Long.MIN_VALUE;

    //UTILITY
    public boolean addDelegate(RegionDisplayer delegate) {
        Objects.requireNonNull(delegate, "delegate cannot be null");
        if (this.hasDelegate(delegate)) {
            return false;
        }
        this.delegates_.add(delegate);
        return true;
    }

    public boolean removeDelegate(RegionDisplayer delegate) {
        Objects.requireNonNull(delegate, "delegate cannot be null");
        boolean removed = false;
        for (RegionDisplayer current : this.delegates_) {
            if (current == delegate) {
                removed = this.delegates_.remove(current);
                break;
            }
        }
        if (removed) {
            this.stopDelegateTokens(delegate);
        }
        return removed;
    }

    public List<RegionDisplayer> delegates() {
        return List.copyOf(this.delegates_);
    }


    //IMPLEMENTATION
    @Override
    public DisplayToken display(Region region) {
        return this.display(region, null);
    }

    @Override
    public DisplayToken display(Region region, Player player) {
        List<DisplayToken> delegateTokens = new ArrayList<>();
        try {
            for (RegionDisplayer delegate : this.delegates_) {
                delegateTokens.add(delegate.display(region, player));
            }
        } catch (RuntimeException e) {
            stopTokens(delegateTokens);
            throw e;
        }

        long id;
        synchronized (this.displayTokens_) {
            id = this.nextId_++;
            this.displayTokens_.put(id, List.copyOf(delegateTokens));
        }
        return new DisplayToken(this, id);
    }

    @Override
    public void stop(DisplayToken token) {
        if (token == null || token.displayer() != this) {
            return;
        }

        List<DisplayToken> delegateTokens;
        synchronized (this.displayTokens_) {
            delegateTokens = this.displayTokens_.remove(token.id());
        }
        if (delegateTokens != null) {
            stopTokens(delegateTokens);
        }
    }

    private boolean hasDelegate(RegionDisplayer delegate) {
        for (RegionDisplayer current : this.delegates_) {
            if (current == delegate) {
                return true;
            }
        }
        return false;
    }

    private void stopDelegateTokens(RegionDisplayer delegate) {
        List<DisplayToken> removedTokens = new ArrayList<>();
        synchronized (this.displayTokens_) {
            for (Map.Entry<Long, List<DisplayToken>> entry : List.copyOf(this.displayTokens_.entrySet())) {
                List<DisplayToken> keptTokens = new ArrayList<>();
                for (DisplayToken token : entry.getValue()) {
                    if (token.displayer() == delegate) {
                        removedTokens.add(token);
                    } else {
                        keptTokens.add(token);
                    }
                }

                if (keptTokens.isEmpty()) {
                    this.displayTokens_.remove(entry.getKey());
                } else if (keptTokens.size() != entry.getValue().size()) {
                    this.displayTokens_.put(entry.getKey(), List.copyOf(keptTokens));
                }
            }
        }
        stopTokens(removedTokens);
    }

    private static void stopTokens(List<DisplayToken> tokens) {
        for (DisplayToken token : tokens) {
            token.displayer().stop(token);
        }
    }
}
