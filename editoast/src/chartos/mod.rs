mod bounding_box;
mod layer_cache;
mod layers_description;
use crate::api_error::{ApiError, ApiResult};
use crate::api_error::{ChartosError, EditoastError};
pub use bounding_box::{BoundingBox, InvalidationZone};
pub use layers_description::{
    parse_layers_description, Layer, LayersDescription, Named, SelfConfig,
};
use rocket::serde::json::{json, Error as JsonError, Json, Value as JsonValue};

use crate::client::ChartosConfig;
use crate::client::RedisConfig;
use crate::db_connection::{DBConnection, RedisConnections};
use core::result::Result;
use deadpool_redis::{
    redis::{cmd, FromRedisValue, RedisError, RedisResult},
    Config, Runtime,
};
use reqwest::Client;
use rocket::{
    http::{ContentType, Status},
    routes, Route, State,
};
use rocket_db_pools::{deadpool_redis, Connection, Database};
use std::collections::HashMap;

const LAYERS: [&str; 12] = [
    "track_sections",
    "signals",
    "speed_sections",
    "track_section_links",
    "switches",
    "detectors",
    "buffer_stops",
    "routes",
    "operational_points",
    "catenaries",
    "lpv_panels",
    "errors",
];

/// Invalidate a zone for all chartos layers
/// If the zone is invalide nothing is done
pub async fn invalidate_zone(
    infra_id: i32,
    chartos_config: &ChartosConfig,
    zone: &InvalidationZone,
) {
    if !zone.is_valid() {
        return;
    }

    for layer in LAYERS {
        invalidate_layer_zone(infra_id, layer, zone, chartos_config).await;
    }
}

/// Invalidate all chartos layers
pub async fn invalidate_all(infra_id: i32, chartos_config: &ChartosConfig) {
    for layer in LAYERS {
        invalidate_layer(infra_id, layer, chartos_config).await;
    }
}

/// Invalidate a zone of chartos layer
/// Panic if the request failed
async fn invalidate_layer_zone(
    infra_id: i32,
    layer: &str,
    zone: &InvalidationZone,
    chartos_config: &ChartosConfig,
) {
    let resp = Client::new()
        .post(format!(
            "{}layer/{}/invalidate_bbox/?infra={}",
            chartos_config.url(),
            layer,
            infra_id
        ))
        .json(&json!([
            {
                "view": "geo",
                "bbox": zone.geo,
            },
            {
                "view": "sch",
                "bbox": zone.sch,
            }
        ]))
        .bearer_auth(&chartos_config.chartos_token)
        .send()
        .await
        .expect("Failed to send invalidate request to chartos");
    if !resp.status().is_success() {
        panic!("Failed to invalidate chartos layer: {}", resp.status());
    }
}

/// Invalidate a whole chartos layer
/// Panic if the request failed
async fn invalidate_layer(infra_id: i32, layer: &str, chartos_config: &ChartosConfig) {
    let resp = Client::new()
        .post(format!(
            "{}layer/{}/invalidate/?infra={}",
            chartos_config.url(),
            layer,
            infra_id
        ))
        .bearer_auth(&chartos_config.chartos_token)
        .send()
        .await
        .expect("Failed to send invalidate request to chartos");
    if !resp.status().is_success() {
        panic!("Failed to invalidate chartos layer: {}", resp.status());
    }
}

pub fn routes() -> HashMap<&'static str, Vec<Route>> {
    HashMap::from([("/chartos", routes![health, info, mvt_view_metadata])])
}

#[get("/health")]
pub async fn health(pool: &RedisConnections) -> () {
    let mut conn = pool.get().await.unwrap();
    cmd("PING").query_async::<_, ()>(&mut conn).await.unwrap()
}

#[get("/info")]
pub async fn info(layers_description: &State<LayersDescription>) -> ApiResult<JsonValue> {
    Ok(json!(layers_description.layers))
}

fn get_or_404<'a, T: Named>(
    elements: &'a Vec<T>,
    element_name: &str,
    array_name: &str,
) -> ApiResult<&'a T> {
    for element in elements.iter() {
        if element.name() == element_name {
            return Ok(element);
        }
    }
    Err(ChartosError {
        elements: elements,
        element_name: element_name,
        array_name: array_name,
    }
    .into())
}

#[get("/layer/<layer_slug>/mvt/<view_slug>?<infra>")]
pub async fn mvt_view_metadata(
    layer_slug: &str,
    view_slug: &str,
    infra: i64,
    layers_description: &State<LayersDescription>,
    self_config: &State<SelfConfig>,
) -> ApiResult<JsonValue> {
    let layer = get_or_404(&layers_description.layers, layer_slug, "Layer")?;
    // Check view exists
    get_or_404(&layer.views, view_slug, "Layer view")?;
    let tiles_url_pattern = format!(
        "{}/tile/{layer_slug}/{view_slug}/{{z}}/{{x}}/{{y}}/?infra={infra}",
        self_config.url
    );

    Ok(json!({
        "type": "vector",
        "name": layer.name,
        "promoteId": {layer.name.clone(): layer.id_field},
        "scheme": "xyz",
        "tiles": [tiles_url_pattern],
        "attribution": layer.attribution.clone().unwrap_or("".to_string()),
        "minzoom": 0,
        "maxzoom": self_config.max_zoom,
    }))
}
#[get("/tile/<layer_slug>/<view_slug>/<z>/<x>/<y>?<infra>")]
pub async fn mvt_view_tile(
    layer_slug: &str,
    view_slug: &str,
    z: i64,
    x: i64,
    y: i64,
    infra: i64,
    layers_description: &State<LayersDescription>,
    redis_pool: &RedisConnections,
    psql_conn: DBConnection,
) -> ApiResult<JsonValue> {
    let layer = get_or_404(&layers_description.layers, layer_slug, "Layer")?;
    let view = get_or_404(&layer.views, view_slug, "Layer view")?;

    // try to fetch the tile from the cache

    Ok(json!(""))
}

#[cfg(test)]
mod tests {
    use crate::api_error::ChartosError;
    use crate::chartos::layers_description::Layer;
    use crate::chartos::parse_layers_description;
    use crate::client::PostgresConfig;
    use crate::create_server;
    use rocket::http::Status;
    use rocket::local::blocking::Client;
    use serde_json::{Map, Value};
    use std::fs::File;
    use std::io::{BufRead, BufReader, ErrorKind};
    use std::path::Path;

    use super::get_or_404;

    /// Create a test editoast client
    /// This client create a single new connection to the database
    pub fn create_test_client() -> Client {
        let pg_config = PostgresConfig {
            pool_size: 1,
            ..Default::default()
        };
        let rocket = create_server(
            &Default::default(),
            &pg_config,
            Default::default(),
            Default::default(),
        );
        Client::tracked(rocket).expect("valid rocket instance")
    }

    fn expected_json_result(file: &Path) -> Value {
        serde_json::from_reader(BufReader::new(File::open(file).unwrap())).unwrap()
    }

    #[test]
    fn health() {
        let client = create_test_client();
        let response = client.get("/chartos/health").dispatch();
        assert_eq!(response.status(), Status::Ok);
    }

    #[test]
    fn info() {
        let client = create_test_client();
        let response = client.get("/chartos/info").dispatch();
        assert_eq!(response.status(), Status::Ok);
        let expected_result =
            parse_layers_description(Path::new("./src/chartos/layers_description.yml")).layers;
        let actual_result: Vec<Layer> =
            serde_json::from_str(&response.into_string().unwrap()).unwrap();
        assert_eq!(actual_result, expected_result)
    }

    #[test]
    fn test_get_or_404() {
        let layers_description =
            parse_layers_description(Path::new("./src/chartos/layers_description.yml"));
        let expected_result = &layers_description.layers[1];
        let actual_result = get_or_404(&layers_description.layers, "signals", "Layer");
        assert_eq!(expected_result, actual_result.unwrap());
        let not_found = get_or_404(&layers_description.layers, "does_not_exist", "Layer");
        assert_eq!(not_found.is_err(), true);
    }

    macro_rules! test_mvt_view_metadata {
        ($($name:ident: $value:expr,)*) => {
        $(
            #[test]
            fn $name() {
                let (uri, status, expected_response): (&str, Status, &str) = $value;
                let client = create_test_client();

                let response = client.get(uri).dispatch();
                assert_eq!(response.status().clone(), status);
                let expected_body = expected_json_result(Path::new(&format!("./src/chartos/test_data/{expected_response}.json")));

                let body: Value = serde_json::from_str(response.into_string().unwrap().as_str()).unwrap();
                assert_eq!(expected_body.to_string(), body.to_string())
            }
        )*
        }
    }

    test_mvt_view_metadata! {
        get_layer_404: ("/chartos/layer/track_sections/mvt/does_not_exist?infra=2",  Status::NotFound, "get_layer_404"),
        get_layer: ("/chartos/layer/track_sections/mvt/geo?infra=2",  Status::Ok, "get_layer"),
    }
}
