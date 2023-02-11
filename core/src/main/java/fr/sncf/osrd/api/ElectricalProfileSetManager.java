package fr.sncf.osrd.api;

import com.squareup.moshi.JsonDataException;
import fr.sncf.osrd.external_generated_inputs.ElectricalProfileMapping;
import fr.sncf.osrd.railjson.schema.external_generated_inputs.RJSElectricalProfileSet;
import okhttp3.OkHttpClient;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Manager that fetches and stores the different electrical profile sets used. */
public class ElectricalProfileSetManager extends APIClient {
    private final ConcurrentHashMap<String, CacheEntry> cache =
            new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ElectricalProfileSetManager.class);

    public ElectricalProfileSetManager(String baseUrl, String authorizationToken, OkHttpClient client) {
        super(baseUrl, authorizationToken, client);
    }

    /**
     * Get the electrical profile set with the given ID and store it in the cacheEntry.
     */
    public void downloadSet(CacheEntry cacheEntry, String profileSetId) {
        try {
            logger.info("Electrical profile set {} is not cached", profileSetId);
            var endpointPath = String.format("/electrical_profile_set/%s/", profileSetId);
            var request = buildRequest(endpointPath);

            logger.info("Fetching from {}", request.url());
            cacheEntry.setStatus(CacheStatus.DOWNLOADING);
            RJSElectricalProfileSet rjsProfileSet;
            try (var response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful())
                    throw new UnexpectedHttpResponse(response);
                var body = response.body();
                if (body == null)
                    throw new JsonDataException("empty response body");
                cacheEntry.setStatus(CacheStatus.PARSING_JSON);
                rjsProfileSet = RJSElectricalProfileSet.adapter.fromJson(body.source());
            }
            if (rjsProfileSet == null)
                throw new JsonDataException("Empty electrical profile set JSON");

            logger.info("Electrical profile set {} fetched, parsing it", profileSetId);
            cacheEntry.setStatus(CacheStatus.PARSING_MODEL);
            var mapping = new ElectricalProfileMapping();
            mapping.parseRJS(rjsProfileSet);

            logger.info("Electrical profile set {} parsed", profileSetId);
            cacheEntry.mapping = mapping;
            cacheEntry.setStatus(CacheStatus.CACHED);
        } catch (IOException | UnexpectedHttpResponse e) {
            logger.error("Soft error while loading electrical profile set", e);
            cacheEntry.setStatus(CacheStatus.TRANSIENT_ERROR);
        } catch (Exception e) {
            logger.error("Hard error while loading electrical profile set", e);
            cacheEntry.setStatus(CacheStatus.ERROR);
        }
    }

    /**
     * Return the electrical profile set corresponding to the given id, in a ready-to-use format.
     */
    public Optional<ElectricalProfileMapping> getProfileMap(String profileSetId) {
        if (profileSetId == null) {
            return Optional.empty();
        }

        cache.putIfAbsent(profileSetId, new CacheEntry(null));
        var cacheEntry = cache.get(profileSetId);

        if (!cacheEntry.status.isStable) {
            synchronized (cacheEntry) {
                downloadSet(cacheEntry, profileSetId);
            }
        }

        if (cacheEntry.status == CacheStatus.CACHED)
            return Optional.of(cacheEntry.mapping);
        return Optional.empty();
    }

    private static class CacheEntry {
        private CacheStatus status;
        private ElectricalProfileMapping mapping;

        public CacheEntry(ElectricalProfileMapping mapping) {
            this.mapping = mapping;
            this.status = CacheStatus.INITIALIZING;
        }

        public void setStatus(CacheStatus newStatus) {
            assert status.canTransitionTo(newStatus);
            status = newStatus;
        }
    }
}
