use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use diesel::expression::AsExpression;
use diesel::sql_types::Jsonb;

use crate::schema::TrackRange;


#[derive(Debug, Serialize, Deserialize, PartialEq, AsExpression)]
#[diesel(sql_type = Jsonb)]
pub struct ElectricalProfile {
    pub value: String,
    pub power_class: String,
    pub track_ranges: Vec<TrackRange>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, AsExpression)]
#[diesel(sql_type = Jsonb)]
pub struct ElectricalProfileSetData {
    pub levels: Vec<ElectricalProfile>,
    pub level_order: HashMap<String, Vec<String>>,
}
