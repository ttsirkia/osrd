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
    private static final Logger logger = LoggerFactory.getLogger(ElectricalProfileSetManager.class);

    public ElectricalProfileSetManager(String baseUrl, String authorizationToken, OkHttpClient client) {
        super(baseUrl, authorizationToken, client);
    }


    private BufferedSource fetchProfileSet(String profileSetId) throws IOException, UnexpectedHttpResponse {
        var endpointPath = String.format("/electrical_profile_set/%s/", profileSetId);
        var request = buildRequest(endpointPath);
        try {
            var response = httpClient.newCall(request).execute();
            if (!response.isSuccessful())
                throw new UnexpectedHttpResponse(response);
            return response.body().source();
        } catch (IOException e) {
            logger.error("Failed to fetch profile set {}", profileSetId, e);
            throw e;
        }
    }

    protected void parseRJS(List<RJSElectricalProfile> rjsProfiles,
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
        if (profileSetId == null) {
            return new IdentityHashMap<>();
        } else if (cache.containsKey(profileSetId)) {
            logger.info("Electrical profile set {} is already cached", profileSetId);
            return cache.get(profileSetId);
        }

        cache.put(profileSetId, new IdentityHashMap<>());
        var cacheEntry = cache.get(profileSetId);

        synchronized (cacheEntry) {
            try {
                logger.info("Electrical profile set {} is not cached, fetching it", profileSetId);
                var rjsProfiles = RJSElectricalProfile.listAdapter.fromJson(fetchProfileSet(profileSetId));
                logger.info("Electrical profile set {} fetched, parsing it", profileSetId);
                parseRJS(rjsProfiles, infra, cacheEntry);
                logger.info("Electrical profile set {} parsed", profileSetId);
            } catch (IOException | UnexpectedHttpResponse e) {
                logger.error("failed to fetch electrical profile set", e);
                cacheEntry.clear();
            }
        }
        return cacheEntry;
    }
}
