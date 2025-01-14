use diesel::sql_types::{Jsonb, Text};
use mvt::{Feature, GeomData, GeomEncoder, MapGrid, Tile as MvtTile, TileId};
use pointy::Transform64;
use serde::{Deserialize, Serialize};
use serde_json::Value as JsonValue;

use crate::{map::View, schema::GeoJson};

#[derive(Clone, QueryableByName, Queryable, Debug, Serialize, Deserialize)]
pub struct GeoJsonAndData {
    #[diesel(sql_type = Text)]
    pub geo_json: String,
    #[diesel(sql_type = Jsonb)]
    pub data: JsonValue,
}

impl GeoJsonAndData {
    /// Converts GeoJsonAndData as mvt GeomData
    pub fn as_geom_data(&self, transform: Transform64) -> GeomData {
        let geo_json = serde_json::from_str::<GeoJson>(&self.geo_json).unwrap();
        let mut encoder = GeomEncoder::new(geo_json.get_geom_type(), transform);
        match geo_json {
            GeoJson::Point { coordinates } => {
                encoder.add_point(coordinates.0, coordinates.1);
            }
            GeoJson::MultiPoint { coordinates } => {
                for (x, y) in coordinates {
                    encoder.add_point(x, y);
                }
            }
            GeoJson::LineString { coordinates } => {
                for (x, y) in coordinates {
                    encoder.add_point(x, y);
                }
            }
            GeoJson::MultiLineString { coordinates } => {
                for line in coordinates {
                    for (x, y) in line.iter() {
                        encoder.add_point(*x, *y);
                    }
                    encoder.complete_geom().unwrap();
                }
            }
        };
        encoder.complete().unwrap().encode().unwrap()
    }
}

/// Adds tags to an MVT feature
///
/// tags must be flattened as mvt tags are only one level depth
///
/// # Arguments
///
/// * `feature` - Feature on which tags must be added
/// * `tags` - JsonValue containing tags to add
/// * `key` - Name of the tag
fn add_tags_to_feature(feature: &mut Feature, tags: JsonValue, tag_name: String) {
    match tags {
        JsonValue::Bool(bool) => feature.add_tag_bool(&tag_name, bool),
        JsonValue::Number(number) => {
            if number.is_i64() {
                feature.add_tag_int(&tag_name, number.as_i64().unwrap());
            } else if number.is_u64() {
                feature.add_tag_uint(&tag_name, number.as_u64().unwrap());
            } else {
                feature.add_tag_double(&tag_name, number.as_f64().unwrap());
            }
        }
        JsonValue::String(string) => feature.add_tag_string(&tag_name, &string),
        JsonValue::Array(json_values) => {
            // Converts array as string
            feature.add_tag_string(&tag_name, &serde_json::to_string(&json_values).unwrap())
        }
        JsonValue::Object(map_values) => {
            for (key, value) in map_values.into_iter() {
                add_tags_to_feature(
                    feature,
                    value,
                    if tag_name.is_empty() {
                        key
                    } else {
                        format!("{tag_name}_{key}")
                    },
                );
            }
        }
        JsonValue::Null => (),
    }
}

/// Creates a MVT tile and fills it with records
///
/// # Arguments
///
/// * `z` - Level of zoom
/// * `x` - X coordinate
/// * `y` - Y coordinate
/// * `layer_name` - Name of the layer
/// * `records` - Records to add as features to the MVT tile
pub fn create_and_fill_mvt_tile<T: AsRef<str>>(
    z: u64,
    x: u64,
    y: u64,
    layer_name: T,
    records: Vec<GeoJsonAndData>,
) -> MvtTile {
    let mut tile = MvtTile::new(4096);
    // Return if no records as a tile without layers can be created but is not really useful
    if records.is_empty() {
        return tile;
    }
    let mut mvt_layer = tile.create_layer(layer_name.as_ref());
    let ts = tile.extent() as f64;
    let transform = MapGrid::default()
        .tile_transform(
            TileId::new(
                x.try_into().unwrap(),
                y.try_into().unwrap(),
                z.try_into().unwrap(),
            )
            .unwrap(),
        )
        .scale(ts, ts);
    for record in records.into_iter() {
        let mut feature = mvt_layer.into_feature(record.as_geom_data(transform));
        add_tags_to_feature(&mut feature, record.data, String::new());
        mvt_layer = feature.into_layer();
    }
    tile.add_layer(mvt_layer).unwrap();
    tile
}

/// Creates an SQL query to get geo json data
///
/// # Arguments
///
/// * `table_name` - Table containing the data
/// * `view` - View containing info to get the data
pub fn get_geo_json_sql_query(table_name: &str, view: &View) -> String {
    format!(
        "
        WITH bbox AS (
            SELECT TileBBox($1, $2, $3, 3857) AS geom
        )
        SELECT ST_AsGeoJson(geographic) AS geo_json, 
            {data_expr} {exclude_fields} AS data 
        FROM {table_name} layer 
            CROSS JOIN bbox 
            {joins} 
        WHERE layer.infra_id = $4
            {where_condition}
            AND {on_field} && bbox.geom 
            AND ST_GeometryType({on_field}) != 'ST_GeometryCollection'
        ",
        on_field = view.on_field,
        data_expr = view.data_expr,
        exclude_fields = &view
            .exclude_fields
            .iter()
            .map(|field| format!("- '{field}'"))
            .collect::<Vec<_>>()
            .join(" "),
        joins = view.joins.join(" "),
        where_condition = &view
            .where_expr
            .iter()
            .map(|field| format!("AND ({field})"))
            .collect::<Vec<_>>()
            .join(" "),
    )
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use crate::map::MapLayers;

    use super::{create_and_fill_mvt_tile, get_geo_json_sql_query, GeoJsonAndData};

    #[test]
    fn test_query_creation() {
        let map_layers = MapLayers::parse();
        let expected_queries = [
        "
        WITH bbox AS (
            SELECT TileBBox($1, $2, $3, 3857) AS geom
        )
        SELECT ST_AsGeoJson(geographic) AS geo_json, 
            track_section.data - 'geo' - 'sch' AS data 
        FROM osrd_infra_tracksectionlayer layer 
            CROSS JOIN bbox 
            inner join osrd_infra_tracksectionmodel track_section on track_section.obj_id = layer.obj_id and track_section.infra_id = layer.infra_id 
        WHERE layer.infra_id = $4
            
            AND schematic && bbox.geom 
            AND ST_GeometryType(schematic) != 'ST_GeometryCollection'
        ",
        "
        WITH bbox AS (
            SELECT TileBBox($1, $2, $3, 3857) AS geom
        )
        SELECT ST_AsGeoJson(geographic) AS geo_json, 
            speed_section.data  AS data 
        FROM osrd_infra_speedsectionlayer layer 
            CROSS JOIN bbox 
            inner join osrd_infra_speedsectionmodel speed_section on speed_section.obj_id = layer.obj_id and speed_section.infra_id = layer.infra_id 
        WHERE layer.infra_id = $4
            AND (not (speed_section.data @? '$.extensions.lpv_sncf.z'))
            AND schematic && bbox.geom 
            AND ST_GeometryType(schematic) != 'ST_GeometryCollection'
        "
        ];
        for (i, layer_name) in ["track_sections", "speed_sections"].iter().enumerate() {
            let track_sections = map_layers.layers.get(*layer_name).unwrap();
            let query = get_geo_json_sql_query(
                &track_sections.table_name,
                track_sections.views.get("sch").unwrap(),
            );
            assert_eq!(expected_queries[i], query);
        }
    }

    #[test]
    fn test_create_and_fill_tile() {
        let records = vec![GeoJsonAndData {
          geo_json: "{\"type\":\"MultiLineString\",\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:3857\"}},\"coordinates\":[[[252941.293121198,6258992.543559584],[252858.794084681,6258960.207464033],[252853.844467147,6258958.257191242]]]}".to_string(),
          data: json!({
            "id": "a",
            "extensions": {
              "lpv_sncf": null
            },
            "speed_limit": null,
            "track_ranges": [
              {
                "end": 77,
                "begin": 0,
              }
            ],
            "speed_limit_by_tag": {
              "train": 19.2,
            }
          })
        },GeoJsonAndData {
            geo_json: "{\"type\":\"MultiLineString\",\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:3857\"}},\"coordinates\":[[[258640.840874342,6254230.029006824],[258640.02733391,6254232.868657458],[258621.907591885,6254296.420095009]]]}".to_string(),
            data: json!({
              "id": "b",
              "extensions": {
                "lpv_sncf": null
              },
              "speed_limit": null,
              "track_ranges": [
                {
                  "end": 211,
                  "begin": 0,
                }
              ],
              "speed_limit_by_tag": {
                "new train": 15.2
              }
            })
          }];
        let tile = create_and_fill_mvt_tile(7, 64, 44, "signal_layers", records);
        assert_eq!(tile.num_layers(), 1);
        assert_eq!(
            tile.to_bytes().unwrap(),
            vec![
                26, 226, 1, 120, 2, 10, 13, 115, 105, 103, 110, 97, 108, 95, 108, 97, 121, 101,
                114, 115, 18, 21, 18, 6, 0, 0, 1, 1, 2, 2, 24, 2, 34, 9, 9, 218, 51, 70, 18, 1, 2,
                0, 0, 18, 22, 18, 6, 0, 3, 3, 4, 2, 5, 24, 2, 34, 10, 9, 238, 52, 196, 1, 18, 0, 1,
                0, 0, 26, 2, 105, 100, 26, 24, 115, 112, 101, 101, 100, 95, 108, 105, 109, 105,
                116, 95, 98, 121, 95, 116, 97, 103, 95, 116, 114, 97, 105, 110, 26, 12, 116, 114,
                97, 99, 107, 95, 114, 97, 110, 103, 101, 115, 26, 28, 115, 112, 101, 101, 100, 95,
                108, 105, 109, 105, 116, 95, 98, 121, 95, 116, 97, 103, 95, 110, 101, 119, 32, 116,
                114, 97, 105, 110, 34, 3, 10, 1, 97, 34, 9, 25, 51, 51, 51, 51, 51, 51, 51, 64, 34,
                24, 10, 22, 91, 123, 34, 98, 101, 103, 105, 110, 34, 58, 48, 44, 34, 101, 110, 100,
                34, 58, 55, 55, 125, 93, 34, 3, 10, 1, 98, 34, 9, 25, 102, 102, 102, 102, 102, 102,
                46, 64, 34, 25, 10, 23, 91, 123, 34, 98, 101, 103, 105, 110, 34, 58, 48, 44, 34,
                101, 110, 100, 34, 58, 50, 49, 49, 125, 93, 40, 128, 32
            ]
        );
    }

    #[test]
    fn test_empty_tile() {
        let empty_tile = create_and_fill_mvt_tile(7, 63, 23, "track_sections", vec![]);
        assert!(empty_tile.to_bytes().unwrap().is_empty());
    }
}
