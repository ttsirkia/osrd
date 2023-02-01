use serde::{Deserialize, Serialize};
use std::collections::HashMap;

use crate::schema::TrackRange;


#[derive(Debug, PartialEq, Serialize, Deserialize)]
pub struct ElectricalProfile {
    pub value: String,
    pub power_class: String,
    pub track_ranges: Vec<TrackRange>,
}

#[derive(Debug, PartialEq, Serialize, Deserialize)]
pub struct ElectricalProfileSet {
    pub levels: Vec<ElectricalProfile>,
    #[serde(skip_serializing)]
    pub level_order: HashMap<String, Vec<String>>,
}
