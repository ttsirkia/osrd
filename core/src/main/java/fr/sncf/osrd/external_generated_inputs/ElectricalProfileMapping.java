package fr.sncf.osrd.external_generated_inputs;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import fr.sncf.osrd.infra.api.tracks.undirected.TrackInfra;
import fr.sncf.osrd.infra.api.tracks.undirected.TrackSection;
import fr.sncf.osrd.railjson.schema.RJSElectricalProfile;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

/** A mapping from track sections to electrical profiles */
public class ElectricalProfileMapping {
    /** Internal representation: {<power class>: {<track_section>: {<range>: <electrical profile value>}}} */
    protected HashMap<String, IdentityHashMap<TrackSection, RangeMap<Double, String>>> mapping = new HashMap<>();

    public void clear() {
        mapping = new HashMap<>();
    }

    public void parseRJS(List<RJSElectricalProfile> rjsProfiles, TrackInfra infra) {
        /** Parse the rjs profiles and store them in the internal mapping */
        assert mapping.isEmpty();

        for (var rjsProfile : rjsProfiles) {
            var trackMapping = mapping.computeIfAbsent(rjsProfile.powerClass, k -> new IdentityHashMap<>());
            for (var trackRange : rjsProfile.trackRanges) {
                var trackSection = infra.getTrackSection(trackRange.track);
                var rangeMapping = trackMapping.computeIfAbsent(trackSection, k -> TreeRangeMap.create());
                rangeMapping.put(Range.closedOpen(trackRange.begin, trackRange.end), rjsProfile.value);
            }
        }
    }
}
