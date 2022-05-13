use super::{invalidate_bbox_chartos_layer, invalidate_chartos_layer, InvalidationZone};
use crate::client::ChartosConfig;
use crate::infra_cache::InfraCache;
use crate::railjson::operation::{OperationResult, RailjsonObject};
use crate::railjson::{ObjectRef, ObjectType};
use crate::schema::osrd_infra_detectorlayer;
use crate::schema::osrd_infra_detectorlayer::dsl::*;
use diesel::prelude::*;
use diesel::result::Error;
use diesel::sql_types::{Array, Integer, Text};
use diesel::{delete, sql_query};
use serde::Serialize;
use std::collections::HashSet;

#[derive(QueryableByName, Queryable, Debug, Serialize)]
#[table_name = "osrd_infra_detectorlayer"]
pub struct DetectorLayer {
    pub id: i32,
    pub infra_id: i32,
    pub obj_id: String,
}

impl DetectorLayer {
    /// Clear and regenerate fully the detector layer of a given infra id
    pub fn refresh(
        conn: &PgConnection,
        infra: i32,
        chartos_config: &ChartosConfig,
    ) -> Result<(), Error> {
        delete(osrd_infra_detectorlayer.filter(infra_id.eq(infra))).execute(conn)?;
        sql_query(include_str!("sql/generate_detector_layer.sql"))
            .bind::<Integer, _>(infra)
            .execute(conn)?;
        invalidate_chartos_layer(infra, "detectors", chartos_config);
        Ok(())
    }

    pub fn insert_update_list(
        conn: &PgConnection,
        infra: i32,
        obj_ids: HashSet<String>,
    ) -> Result<(), Error> {
        if obj_ids.is_empty() {
            return Ok(());
        }
        let obj_ids: Vec<String> = obj_ids.into_iter().collect();

        sql_query(include_str!("sql/insert_update_detector_layer.sql"))
            .bind::<Integer, _>(infra)
            .bind::<Array<Text>, _>(&obj_ids)
            .execute(conn)?;
        Ok(())
    }

    pub fn delete_list(
        conn: &PgConnection,
        infra: i32,
        obj_ids: HashSet<String>,
    ) -> Result<(), Error> {
        if obj_ids.is_empty() {
            return Ok(());
        }

        let obj_ids: Vec<String> = obj_ids.into_iter().collect();

        sql_query("DELETE FROM osrd_infra_detectorlayer WHERE infra_id = $1 AND obj_id = ANY($2)")
            .bind::<Integer, _>(infra)
            .bind::<Array<Text>, _>(&obj_ids)
            .execute(conn)?;

        Ok(())
    }

    fn fill_detector_track_refs(
        infra_cache: &InfraCache,
        track_id: &String,
        results: &mut HashSet<String>,
    ) {
        infra_cache
            .get_track_refs_type(track_id, ObjectType::Detector)
            .iter()
            .for_each(|obj_ref| {
                results.insert(obj_ref.obj_id.clone());
            });
    }

    /// Search and update all detectors that needs to be refreshed given a list of operation.
    pub fn update(
        conn: &PgConnection,
        infra: i32,
        operations: &Vec<OperationResult>,
        infra_cache: &InfraCache,
        invalid_zone: &InvalidationZone,
        chartos_config: &ChartosConfig,
    ) -> Result<(), Error> {
        let mut update_obj_ids = HashSet::new();
        let mut delete_obj_ids = HashSet::new();
        for op in operations {
            match op {
                OperationResult::Create(RailjsonObject::TrackSection { railjson })
                | OperationResult::Update(RailjsonObject::TrackSection { railjson }) => {
                    Self::fill_detector_track_refs(infra_cache, &railjson.id, &mut update_obj_ids)
                }
                OperationResult::Create(RailjsonObject::Detector { railjson })
                | OperationResult::Update(RailjsonObject::Detector { railjson }) => {
                    update_obj_ids.insert(railjson.id.clone());
                }
                OperationResult::Delete(ObjectRef {
                    obj_type: ObjectType::Detector,
                    obj_id: detector_id,
                }) => {
                    delete_obj_ids.insert(detector_id.clone());
                }
                _ => (),
            }
        }

        if update_obj_ids.is_empty() && delete_obj_ids.is_empty() {
            // No update needed
            return Ok(());
        }

        Self::delete_list(conn, infra, delete_obj_ids)?;
        Self::insert_update_list(conn, infra, update_obj_ids)?;

        invalidate_bbox_chartos_layer(infra, "detectors", invalid_zone, chartos_config);

        Ok(())
    }
}