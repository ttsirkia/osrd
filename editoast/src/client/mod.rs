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
}

#[derive(Args, Debug)]
#[clap(about, long_about = "Refresh infra generated data")]
pub struct GenerateArgs {
    /// List of infra ids
    pub infra_ids: Vec<u32>,
    #[clap(short, long)]
    /// Force the refresh of an infra (even if the generated version is up to date)
    pub force: bool,
}

#[derive(Args, Debug)]
#[clap(about, long_about = "Clear infra generated data")]
pub struct ClearArgs {
    /// List of infra ids
    pub infra_ids: Vec<u32>,
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

/// Retrieve the secret key from the environment variable `SECRET_KEY`.
/// Return `None` if the environment variable is not set.
pub fn get_secret_key() -> Option<String> {
    std::env::var("SECRET_KEY").ok()
}
