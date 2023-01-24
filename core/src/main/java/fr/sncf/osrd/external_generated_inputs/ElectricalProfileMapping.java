package fr.sncf.osrd.external_generated_inputs;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import fr.sncf.osrd.infra.api.tracks.undirected.TrackInfra;
import fr.sncf.osrd.infra.api.tracks.undirected.TrackSection;
import fr.sncf.osrd.railjson.schema.RJSElectricalProfile;

import java.util.IdentityHashMap;
import java.util.List;

public class ElectricalProfileMapping {
    protected IdentityHashMap<TrackSection, RangeMap<Double, String>> mapping = new IdentityHashMap<>();

    public void clear() {
        mapping = new IdentityHashMap<>();
    }

    public void parseRJS(List<RJSElectricalProfile> rjsProfiles, TrackInfra infra) {
        assert mapping.isEmpty();

        for (var rjsProfile : rjsProfiles) {
            for (var trackRange : rjsProfile.trackRanges) {
                var trackSection = infra.getTrackSection(trackRange.track);
                var rangeMap = mapping.computeIfAbsent(trackSection, k -> TreeRangeMap.create());
                rangeMap.put(Range.closedOpen(trackRange.begin, trackRange.end), rjsProfile.value);
            }
        }
    }
}
