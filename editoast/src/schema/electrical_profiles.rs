use diesel::PgConnection;
use serde::{Deserialize, Serialize};
use thiserror::Error;

use crate::tables::osrd_infra_electricalprofilesset;
use diesel::result::Error as DieselError;
use crate::diesel::{QueryDsl, RunQueryDsl};
use crate::api_error::ApiError;
use crate::tables::osrd_infra_electricalprofilesset::dsl;
use rocket::http::Status;
use serde_json::{json, Map, Value};

#[derive(Debug, PartialEq, Queryable, Identifiable, Serialize, Deserialize)]
#[diesel(table_name = osrd_infra_electricalprofilesset)]
pub struct ElectricalProfileSet {
    pub id: i32,
    pub name: String,
    pub data: Value,
    pub level_order: Value,
}

#[derive(Debug, PartialEq, Queryable, Serialize)]
pub struct LightElectricalProfileSet {
    pub id: i32,
    pub name: String
}

#[derive(Debug, Error)]
pub enum ElectricalProfilesApiError {
    /// Couldn't found the infra with the given id
    #[error("Electrical Profile Set '{0}', could not be found")]
    NotFound(i32),
    #[error("An internal diesel error occurred: '{}'", .0.to_string())]
    DieselError(DieselError),
}

impl ApiError for ElectricalProfilesApiError {
    fn get_status(&self) -> Status {
        match self {
            ElectricalProfilesApiError::NotFound(_) => Status::NotFound,
            ElectricalProfilesApiError::DieselError(_) => Status::InternalServerError,
        }
    }

    fn get_type(&self) -> &'static str {
        match self {
            ElectricalProfilesApiError::NotFound(_) => "editoast:electrical_profiles:NotFound",
            ElectricalProfilesApiError::DieselError(_) => "editoast:electrical_profiles:DieselError",
        }
    }

    fn extra(&self) -> Option<Map<String, Value>> {
        match self {
            ElectricalProfilesApiError::NotFound(electrical_profile_set_id) => json!({
                "electrical_profile_set_id": electrical_profile_set_id,
            })
            .as_object()
            .cloned(),
            _ => None,
        }
    }
}

impl ElectricalProfileSet {
    pub fn retrieve(conn: &mut PgConnection, ep_set_id: i32) -> Result<ElectricalProfileSet, Box<dyn ApiError>> {
        match dsl::osrd_infra_electricalprofilesset.find(ep_set_id).first(conn) {
            Ok(ep_set) => Ok(ep_set),
            Err(DieselError::NotFound) => Err(Box::new(ElectricalProfilesApiError::NotFound(ep_set_id))),
            Err(e) => Err(Box::new(ElectricalProfilesApiError::DieselError(e))),
        }
    }

    pub fn list(conn: &mut PgConnection) -> Result<Vec<LightElectricalProfileSet>, Box<dyn ApiError>> {
        match dsl::osrd_infra_electricalprofilesset.select((dsl::id, dsl::name)).load(conn) {
            Ok(ep_sets) => Ok(ep_sets),
            Err(e) => Err(Box::new(ElectricalProfilesApiError::DieselError(e))),
        }
    }
}