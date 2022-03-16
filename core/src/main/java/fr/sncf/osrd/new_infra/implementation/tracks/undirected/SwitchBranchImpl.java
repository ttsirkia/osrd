package fr.sncf.osrd.new_infra.implementation.tracks.undirected;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import fr.sncf.osrd.new_infra.api.Direction;
import fr.sncf.osrd.new_infra.api.tracks.undirected.Switch;
import fr.sncf.osrd.new_infra.api.tracks.undirected.SwitchBranch;
import fr.sncf.osrd.new_infra.api.tracks.undirected.TrackObject;
import fr.sncf.osrd.utils.DoubleRangeMap;
import fr.sncf.osrd.utils.jacoco.ExcludeFromGeneratedCodeCoverage;
import java.util.EnumMap;
import java.util.Map;

public class SwitchBranchImpl implements SwitchBranch {

    public Switch switchRef;
    int index;

    /** static mapping from direction to empty map. Avoids unnecessary object instantiations */
    private static final EnumMap<Direction, DoubleRangeMap> emptyMap = new EnumMap<>(Map.of(
            Direction.FORWARD, new DoubleRangeMap(),
            Direction.BACKWARD, new DoubleRangeMap()
    ));

    @Override
    @ExcludeFromGeneratedCodeCoverage
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("switchRef", switchRef.getID())
                .toString();
    }

    @Override
    public Switch getSwitch() {
        return switchRef;
    }

    @Override
    public ImmutableList<TrackObject> getTrackObjects() {
        return ImmutableList.of();
    }

    @Override
    public EnumMap<Direction, DoubleRangeMap> getGradients() {
        return emptyMap;
    }

    @Override
    public EnumMap<Direction, DoubleRangeMap> getSpeedSections() {
        return emptyMap;
    }

    @Override
    public int getIndex() {
        return index;
    }
}
