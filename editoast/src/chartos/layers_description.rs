use derivative::Derivative;
use serde::{Deserialize, Serialize};
use serde_yaml::{self};
use std::fs;
use std::path::Path;

// select C.stuff from A inner join B C on C.id = C.id;
//                       \___________________________/
//                             a join expression
//                            C is an alias for B
type JoinExpr = String;

fn empty_vec() -> Vec<String> {
    vec![]
}

pub trait Named {
    fn name(&self) -> &str;
}

#[derive(Serialize, Deserialize, PartialEq, Debug)]
pub struct View {
    pub name: String,
    pub on_field: String,
    pub data_expr: String,
    #[serde(default = "empty_vec")]
    pub exclude_fields: Vec<String>,
    #[serde(default = "empty_vec")]
    pub joins: Vec<JoinExpr>,
    pub cache_duration: u32,
    #[serde(default = "empty_vec")]
    pub where_expr: Vec<String>,
}

impl Named for View {
    fn name(&self) -> &str {
        &self.name
    }
}

#[derive(Serialize, Deserialize, PartialEq, Debug)]
pub struct Layer {
    pub name: String,
    pub table_name: String,
    pub views: Vec<View>,
    #[serde(default)]
    pub id_field: Option<String>,
    #[serde(default)]
    pub attribution: Option<String>,
}

impl Named for Layer {
    fn name(&self) -> &str {
        &self.name
    }
}

#[derive(Serialize, Deserialize, PartialEq, Debug)]
pub struct LayersDescription {
    pub layers: Vec<Layer>,
}

// TODO move it where more relevant
#[derive(Debug, Derivative, Clone)]
#[derivative(Default)]
pub struct SelfConfig {
    pub url: String,
    #[derivative(Default(value = "18"))]
    pub max_zoom: u32,
}

pub fn parse_layers_description(file: &Path) -> LayersDescription {
    let layers_description: LayersDescription =
        serde_yaml::from_str(&fs::read_to_string(file).unwrap()).unwrap();
    layers_description
}

#[cfg(test)]
mod tests {
    use super::parse_layers_description;
    use std::path::Path;

    #[test]
    pub fn parse() {
        let layers_description =
            parse_layers_description(Path::new("./src/chartos/layers_description.yml"));
        assert_eq!(
            layers_description.layers[0].table_name,
            "osrd_infra_tracksectionlayer"
        );
    }
}
