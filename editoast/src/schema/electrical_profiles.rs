use diesel::PgConnection;
use serde::{Deserialize, Serialize};
use thiserror::Error;

use crate::tables::osrd_infra_electricalprofileset;
use diesel::result::Error as DieselError;
use crate::diesel::{QueryDsl, RunQueryDsl};
use crate::api_error::ApiError;
use crate::tables::osrd_infra_electricalprofileset::dsl;
use rocket::http::Status;
use serde_json::{json, Map, Value};

#[derive(Debug, PartialEq, Queryable, Identifiable, Serialize, Deserialize)]
#[diesel(table_name = osrd_infra_electricalprofileset)]
pub struct ElectricalProfileSet {
    pub id: i64,
    pub name: String,
    pub data: Value
}

#[derive(Debug, Serialize, Deserialize, PartialEq)]
pub struct ElectricalProfileSetSchema {
    pub levels: Value,
    pub level_order: Value,
}


#[derive(Debug, PartialEq, Queryable, Serialize)]
pub struct LightElectricalProfileSet {
    pub id: i64,
    pub name: String
}

#[derive(Debug, Error)]
pub enum ElectricalProfilesApiError {
    /// Couldn't found the infra with the given id
    #[error("Electrical Profile Set '{0}', could not be found")]
    NotFound(i64),
    #[error("An internal diesel error occurred: '{}'", .0.to_string())]
    DieselError(DieselError),
    #[error("An internal error occurred: '{}'", .0.to_string())]
    InternalError(String),
}

impl ApiError for ElectricalProfilesApiError {
    fn get_status(&self) -> Status {
        match self {
            Self::NotFound(_) => Status::NotFound,
            Self::DieselError(_) => Status::InternalServerError,
            Self::InternalError(_) => Status::InternalServerError,
        }
    }

    fn get_type(&self) -> &'static str {
        match self {
            Self::NotFound(_) => "editoast:electrical_profiles:NotFound",
            Self::DieselError(_) => "editoast:electrical_profiles:DieselError",
            Self::InternalError(_) => "editoast:electrical_profiles:InternalError",
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
    pub fn retrieve(conn: &mut PgConnection, ep_set_id: i64) -> Result<ElectricalProfileSet, Box<dyn ApiError>> {
        match dsl::osrd_infra_electricalprofileset.find(ep_set_id).first(conn) {
            Ok(ep_set) => Ok(ep_set),
            Err(DieselError::NotFound) => Err(Box::new(ElectricalProfilesApiError::NotFound(ep_set_id))),
            Err(e) => Err(Box::new(ElectricalProfilesApiError::DieselError(e))),
        }
    }

    pub fn retrieve_levels(conn: &mut PgConnection, ep_set_id: i64) -> Result<Value, Box<dyn ApiError>> {
        let ep_set = Self::retrieve(conn, ep_set_id);
        match ep_set {
            Ok(ep_set) => match serde_json::from_value::<ElectricalProfileSetSchema>(ep_set.data) {
                Ok(ep_set_schema) => Ok(ep_set_schema.levels),
                Err(e) => Err(Box::new(ElectricalProfilesApiError::InternalError(e.to_string()))),
            },
            Err(e) => Err(e),
        }
    }

    pub fn retrieve_level_order(conn: &mut PgConnection, ep_set_id: i64) -> Result<Value, Box<dyn ApiError>> {
        let ep_set = Self::retrieve(conn, ep_set_id);
        match ep_set {
            Ok(ep_set) => match serde_json::from_value::<ElectricalProfileSetSchema>(ep_set.data) {
                Ok(ep_set_schema) => Ok(ep_set_schema.level_order),
                Err(e) => Err(Box::new(ElectricalProfilesApiError::InternalError(e.to_string()))),
            },
            Err(e) => Err(e),
        }
    }

    pub fn list(conn: &mut PgConnection) -> Result<Vec<LightElectricalProfileSet>, Box<dyn ApiError>> {
        match dsl::osrd_infra_electricalprofileset.select((dsl::id, dsl::name)).load(conn) {
            Ok(ep_sets) => Ok(ep_sets),
            Err(e) => Err(Box::new(ElectricalProfilesApiError::DieselError(e))),
        }
    }
}