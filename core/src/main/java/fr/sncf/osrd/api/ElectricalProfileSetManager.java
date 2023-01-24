package fr.sncf.osrd.api;

import fr.sncf.osrd.infra.api.tracks.undirected.TrackInfra;
import fr.sncf.osrd.external_generated_inputs.ElectricalProfileMapping;
import fr.sncf.osrd.railjson.schema.RJSElectricalProfile;
import okhttp3.OkHttpClient;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ElectricalProfileSetManager extends MiddleWareInteraction {
    private final ConcurrentHashMap<String, ElectricalProfileMapping> cache =
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

    /**
     * Get a mapping from track sections to electrical profile value, given an electrical profile set id
     */
    public ElectricalProfileMapping getProfileMap(String profileSetId,
                                                  TrackInfra infra) {
        if (profileSetId == null) {
            return new ElectricalProfileMapping();
        } else if (cache.containsKey(profileSetId)) {
            logger.info("Electrical profile set {} is already cached", profileSetId);
            return cache.get(profileSetId);
        }

        cache.put(profileSetId, new ElectricalProfileMapping());
        var cacheEntry = cache.get(profileSetId);

        synchronized (cacheEntry) {
            try {
                logger.info("Electrical profile set {} is not cached, fetching it", profileSetId);
                var rjsProfiles = RJSElectricalProfile.listAdapter.fromJson(fetchProfileSet(profileSetId));
                logger.info("Electrical profile set {} fetched, parsing it", profileSetId);
                cacheEntry.parseRJS(rjsProfiles, infra);
                logger.info("Electrical profile set {} parsed", profileSetId);
            } catch (IOException | UnexpectedHttpResponse e) {
                logger.error("failed to fetch electrical profile set", e);
                cacheEntry.clear();
            }
        }
        return cacheEntry;
    }
}
