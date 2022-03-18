use super::{ObjectType, OperationError};
use crate::response::ApiError;
use diesel::{sql_query, PgConnection, RunQueryDsl};
use rocket::serde::Deserialize;
use std::collections::{HashMap, HashSet};

#[derive(Clone, Deserialize)]
#[serde(crate = "rocket::serde")]
pub struct DeleteOperation {
    pub obj_type: ObjectType,
    pub obj_id: String,
}

impl DeleteOperation {
    pub fn apply(&self, infra_id: i32, conn: &PgConnection) -> Result<(), Box<dyn ApiError>> {
        match sql_query(format!(
            "DELETE FROM {} WHERE obj_id = '{}' AND infra_id = {}",
            self.obj_type.get_table(),
            self.obj_id,
            infra_id
        ))
        .execute(conn)
        {
            Ok(1) => Ok(()),
            Ok(_) => Err(Box::new(OperationError::NotFound(self.obj_id.clone()))),
            Err(err) => Err(Box::new(OperationError::Other(err))),
        }
    }

    pub fn get_updated_objects(&self, update_lists: &mut HashMap<ObjectType, HashSet<String>>) {
        update_lists
            .entry(self.obj_type.clone())
            .or_insert(Default::default())
            .insert(self.obj_id.clone());
    }
}

#[cfg(test)]
mod test {
    use super::DeleteOperation;
    use crate::client::PostgresConfig;
    use crate::models::Infra;
    use crate::railjson::operation::CreateOperation;
    use crate::railjson::ObjectType;
    use crate::railjson::TrackSection;
    use diesel::result::Error;
    use diesel::sql_types::BigInt;
    use diesel::{sql_query, Connection, PgConnection, RunQueryDsl};

    #[derive(QueryableByName)]
    struct Count {
        #[sql_type = "BigInt"]
        nb: i64,
    }

    #[test]
    fn delete_track() {
        let conn = PgConnection::establish(&PostgresConfig::default().url()).unwrap();
        conn.test_transaction::<_, Error, _>(|| {
            let track_creation = CreateOperation::TrackSection {
                railjson: TrackSection {
                    id: "my_track".to_string(),
                    length: 100.,
                    line_name: "line_test".to_string(),
                    track_name: "track_test".to_string(),
                    ..Default::default()
                },
            };

            let infra = Infra::create(&"test".to_string(), &conn).unwrap();
            assert!(track_creation.apply(infra.id, &conn).is_ok());

            let track_deletion = DeleteOperation {
                obj_type: ObjectType::TrackSection,
                obj_id: "my_track".to_string(),
            };

            assert!(track_deletion.apply(infra.id, &conn).is_ok());

            let res_del = sql_query(format!(
                "SELECT COUNT (*) AS nb FROM osrd_infra_tracksectionmodel WHERE obj_id = 'my_track' AND infra_id = {}",
                infra.id
            ))
            .load::<Count>(&conn).unwrap();

            assert_eq!(res_del[0].nb, 0);

            Ok(())
        });
    }
}