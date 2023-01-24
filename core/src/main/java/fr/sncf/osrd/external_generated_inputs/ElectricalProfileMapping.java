package fr.sncf.osrd.external_generated_inputs;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import fr.sncf.osrd.infra_state.api.TrainPath;
import fr.sncf.osrd.railjson.schema.RJSElectricalProfile;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * A mapping from track sections to electrical profiles
 */
public class ElectricalProfileMapping {
    /**
     * Internal representation: {"power class": {"track section": {"range": "electrical profile value"}}}
     */
    protected HashMap<String, HashMap<String, RangeMap<Double, String>>> mapping = new HashMap<>();

    /**
     * Parse the rjs profiles and store them in the internal mapping.
     */
    public void parseRJS(List<RJSElectricalProfile> rjsProfiles) {
        assert mapping.isEmpty();

        for (var rjsProfile : rjsProfiles) {
            var trackMapping = mapping.computeIfAbsent(rjsProfile.powerClass, k -> new HashMap<>());
            for (var trackRange : rjsProfile.trackRanges) {
                var rangeMapping = trackMapping.computeIfAbsent(trackRange.track, k -> TreeRangeMap.create());
                rangeMapping.put(Range.closedOpen(trackRange.begin, trackRange.end), rjsProfile.value);
            }
        }
    }

    /**
     * Returns the electrical profile values encountered along the train path.
     */
    public HashMap<String, RangeMap<Double, String>> getProfilesOnPath(TrainPath trainPath, Set<String> powerClasses) {
        var res = new HashMap<String, RangeMap<Double, String>>();
        for (var powerClass : powerClasses) {
            if (!mapping.containsKey(powerClass))
                continue;
            var byTrackMapping = mapping.get(powerClass);
            var rangeMap = new ImmutableRangeMap.Builder<Double, String>();
            double offset = 0;
            for (var trackRange : TrainPath.removeLocation(trainPath.trackRangePath())) {
                var rangeMapping = trackRange.convertMap(byTrackMapping.get(trackRange.track.getEdge().getID()));
                for (var entry : rangeMapping.asMapOfRanges().entrySet())
                    rangeMap.put(Range.closedOpen(entry.getKey().lowerEndpoint() + offset,
                            entry.getKey().upperEndpoint() + offset), entry.getValue());
                offset += trackRange.getLength();
            }
            res.put(powerClass, rangeMap.build());
        }

        return res;
    }
}
