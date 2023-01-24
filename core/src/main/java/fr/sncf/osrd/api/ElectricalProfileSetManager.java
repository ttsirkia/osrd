package fr.sncf.osrd.api;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import fr.sncf.osrd.infra.api.tracks.undirected.TrackInfra;
import fr.sncf.osrd.infra.api.tracks.undirected.TrackSection;
import fr.sncf.osrd.railjson.schema.RJSElectricalProfile;
import okhttp3.OkHttpClient;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ElectricalProfileSetManager extends MiddleWareInteraction {
    private final ConcurrentHashMap<String, IdentityHashMap<TrackSection, RangeMap<Double, String>>> cache =
            new ConcurrentHashMap<>();
    static final Logger logger = LoggerFactory.getLogger(ElectricalProfileSetManager.class);

    public ElectricalProfileSetManager(String baseUrl, String authorizationToken, OkHttpClient client) {
        super(baseUrl, authorizationToken, client);
    }


    private BufferedSource fetchProfileSet(String profileSetId) throws IOException, UnexpectedHttpResponse {
        var endpointPath = String.format("/electrical_profile_set/%s/", profileSetId);
        var request = buildRequest(endpointPath);
        var response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) throw new UnexpectedHttpResponse(response);
        return response.body().source();
    }

    private void parseRJS(List<RJSElectricalProfile> rjsProfiles,
                          TrackInfra infra,
                          IdentityHashMap<TrackSection, RangeMap<Double, String>> cache) {
        for (var rjsProfile : rjsProfiles) {
            for (var trackRange : rjsProfile.trackRanges) {
                var trackSection = infra.getTrackSection(trackRange.track);
                var rangeMap = cache.computeIfAbsent(trackSection, k -> TreeRangeMap.create());
                rangeMap.put(Range.closedOpen(trackRange.begin, trackRange.end), rjsProfile.value);
            }
        }
    }

    /**
     * Get a mapping from track sections to electrical profile value, given an electrical profile set id
     */
    public IdentityHashMap<TrackSection, RangeMap<Double, String>> getProfileMap(String profileSetId,
                                                                                 TrackInfra infra) {
        if (profileSetId == null)
            return new IdentityHashMap<>();
        if (cache.containsKey(profileSetId))
            return cache.get(profileSetId);

        cache.put(profileSetId, new IdentityHashMap<>());
        var cacheEntry = cache.get(profileSetId);

        synchronized (cacheEntry) {
            try {
                var rjsProfiles = RJSElectricalProfile.listAdapter.fromJson(fetchProfileSet(profileSetId));
                parseRJS(rjsProfiles, infra, cacheEntry);
            } catch (IOException | UnexpectedHttpResponse e) {
                logger.error("failed to fetch electrical profile set", e);
                cacheEntry.clear();
            }
        }
        return cacheEntry;
    }
}
