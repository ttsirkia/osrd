WITH track_ranges AS (
    SELECT obj_id as speed_id,
        (
            jsonb_array_elements(data->'track_ranges')->'begin'
        )::numeric as slice_begin,
        (
            jsonb_array_elements(data->'track_ranges')->'end'
        )::numeric as slice_end,
        jsonb_array_elements(data->'track_ranges')->'track'->>'id' as track_id
    FROM osrd_infra_speedsectionmodel
    WHERE infra_id = $1 and obj_id in ($2)
),
sliced_tracks AS (
    SELECT track_ranges.speed_id,
        ST_Transform(
            ST_LineSubstring(
                ST_GeomFromGeoJSON(tracks.data->'geo'),
                GREATEST(
                    track_ranges.slice_begin / (tracks.data->'length')::numeric,
                    0.
                ),
                LEAST(
                    track_ranges.slice_end / (tracks.data->'length')::numeric,
                    1.
                )
            ),
            3857
        ) as geo,
        ST_Transform(
            ST_LineSubstring(
                ST_GeomFromGeoJSON(tracks.data->'sch'),
                GREATEST(
                    track_ranges.slice_begin / (tracks.data->'length')::numeric,
                    0.
                ),
                LEAST(
                    track_ranges.slice_end / (tracks.data->'length')::numeric,
                    1.
                )
            ),
            3857
        ) as sch
    FROM track_ranges
        INNER JOIN osrd_infra_tracksectionmodel AS tracks ON tracks.obj_id = track_ranges.track_id
        AND tracks.infra_id = $1
)
INSERT INTO osrd_infra_speedsectionlayer (obj_id, infra_id, geographic, schematic)
SELECT speed_id,
    $1,
    St_Collect(geo),
    St_Collect(sch)
FROM sliced_tracks
GROUP BY speed_id