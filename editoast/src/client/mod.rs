mod postgres_config;
mod redis_config;

use clap::{Args, Parser, Subcommand};
use derivative::Derivative;
pub use postgres_config::PostgresConfig;
pub use redis_config::RedisConfig;
use std::path::PathBuf;

#[derive(Parser, Debug)]
#[clap(author, version)]
pub struct Client {
    #[clap(flatten)]
    pub postgres_config: PostgresConfig,
    #[clap(flatten)]
    pub redis_config: RedisConfig,
    #[clap(subcommand)]
    pub command: Commands,
}

#[derive(Subcommand, Debug)]
pub enum Commands {
    Runserver(RunserverArgs),
    Generate(GenerateArgs),
    Clear(ClearArgs),
    ImportRailjson(ImportRailjsonArgs),
}

#[derive(Args, Debug, Derivative, Clone)]
#[derivative(Default)]
pub struct MapLayersConfig {
    #[derivative(Default(value = "18"))]
    #[clap(long, env, default_value_t = 18)]
    pub max_zoom: u64,
    /// Number maximum of tiles before we consider invalidating full Redis cache is required
    #[derivative(Default(value = "250_000"))]
    #[clap(long, env, default_value_t = 250_000)]
    pub max_tiles: u64,
    #[derivative(Default(value = r#""http://localhost:8090".into()"#))]
    #[clap(long, env, default_value_t = String::from("http://localhost:8090"))]
    pub root_url: String,
}

#[derive(Args, Debug, Derivative)]
#[derivative(Default)]
#[clap(about, long_about = "Launch the server")]
pub struct RunserverArgs {
    #[derivative(Default(value = "8090"))]
    #[clap(long, env = "EDITOAST_PORT", default_value_t = 8090)]
    pub port: u16,
    #[derivative(Default(value = r#""0.0.0.0".into()"#))]
    #[clap(long, env = "EDITOAST_ADDRESS", default_value_t = String::from("0.0.0.0"))]
    pub address: String,
    #[clap(flatten)]
    pub map_layers_config: MapLayersConfig,
}

#[derive(Args, Debug)]
#[clap(about, long_about = "Refresh infra generated data")]
pub struct GenerateArgs {
    /// List of infra ids
    pub infra_ids: Vec<u64>,
    #[clap(short, long)]
    /// Force the refresh of an infra (even if the generated version is up to date)
    pub force: bool,
}

#[derive(Args, Debug)]
#[clap(about, long_about = "Clear infra generated data")]
pub struct ClearArgs {
    /// List of infra ids
    pub infra_ids: Vec<u64>,
}

#[derive(Args, Debug)]
#[clap(about, long_about = "Import an infra given a railjson file")]
pub struct ImportRailjsonArgs {
    /// Infra name
    pub infra_name: String,
    /// Railjson file path
    pub railjson_path: PathBuf,
    /// Whether the import should refresh generated data
    #[clap(short = 'g', long)]
    pub generate: bool,
}
