mod bounding_box;
mod layer_cache;
mod layers_description;
use crate::api_error::{ApiError, ApiResult};
pub use bounding_box::{BoundingBox, InvalidationZone};
pub use layers_description::{parse_layers_description, LayersDescription};
use rocket::serde::json::{json, Error as JsonError, Json, Value as JsonValue};

use crate::client::ChartosConfig;
use crate::client::RedisConfig;
use crate::db_connection::RedisConnections;
use core::result::Result;
use deadpool_redis::{
    redis::{cmd, FromRedisValue, RedisError, RedisResult},
    Config, Runtime,
};
use reqwest::Client;
use rocket::{routes, Route, State};
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
    HashMap::from([("/chartos", routes![health, info])])
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

#[cfg(test)]
mod tests {
    use crate::chartos::layers_description::Layer;
    use crate::chartos::parse_layers_description;
    use crate::client::PostgresConfig;
    use crate::create_server;
    use rocket::http::Status;
    use rocket::local::blocking::Client;
    use std::fs;
    use std::path::Path;

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
}
