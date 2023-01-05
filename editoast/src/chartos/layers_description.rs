use serde::{Deserialize, Serialize};
use serde_yaml::{self};
use std::fs;
use std::path::Path;
// select C.stuff from A inner join B C on C.id = C.id;
//                       \___________________________/
//                             a join expression
//                            C is an alias for B
type JoinExpr = String;

#[derive(Serialize, Deserialize, PartialEq, Debug)]
pub struct View {
    pub name: String,
    pub on_field: String,
    pub data_expr: String,
    #[serde(default)]
    pub exclude_fields: Option<Vec<String>>,
    #[serde(default)]
    pub joins: Option<Vec<JoinExpr>>,
    pub cache_duration: u32,
    #[serde(default)]
    pub where_expr: Option<Vec<String>>,
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

#[derive(Serialize, Deserialize, PartialEq, Debug)]
pub struct LayersDescription {
    pub layers: Vec<Layer>,
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
