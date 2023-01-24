use crate::api_error::ApiResult;
use crate::db_connection::DBConnection;
use crate::schema::electrical_profiles::{ElectricalProfileSet, LightElectricalProfileSet};
use rocket::http::Status;
use rocket::response::status::Custom;
use rocket::serde::json::Json;
use rocket::{routes, Route};
use serde_json::Value;

/// Return a list of electrical profile sets
#[get("/")]
async fn list(conn: DBConnection) -> ApiResult<Json<Vec<LightElectricalProfileSet>>> {
    conn.run(move |conn| match ElectricalProfileSet::list(conn) {
        Ok(light_ep_sets) => Ok(Json(light_ep_sets)),
        Err(e) => Err(e.into()),
    })
    .await
}

/// Return a specific set of electrical profiles
#[get("/<electrical_profile_set>")]
async fn get(conn: DBConnection, electrical_profile_set: i32) -> ApiResult<Custom<Json<Value>>> {
    conn.run(move |conn| {
        Ok(Custom(
            Status::Ok,
            Json(ElectricalProfileSet::retrieve(conn, electrical_profile_set)?.data),
        ))
    })
    .await
}

/// Return the electrical profile value order for this set
#[get("/<electrical_profile_set>/level_order")]
async fn get_level_order(
    conn: DBConnection,
    electrical_profile_set: i32,
) -> ApiResult<Custom<Json<Value>>> {
    conn.run(move |conn| {
        Ok(Custom(
            Status::Ok,
            Json(ElectricalProfileSet::retrieve(conn, electrical_profile_set)?.level_order),
        ))
    })
    .await
}

pub fn routes() -> Vec<Route> {
    routes![list, get, get_level_order]
}
