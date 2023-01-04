use clap::Args;
use derivative::Derivative;

#[derive(Args, Debug, Derivative, Clone)]
#[derivative(Default)]
pub struct RedisConfig {
    #[derivative(Default(value = r#""redis://localhost:6379/".into()"#))]
    #[clap(long, env, default_value = "redis://localhost:6379")]
    redis_url: String,
}

impl RedisConfig {
    pub fn url(&self) -> String {
        if self.redis_url.ends_with('/') {
            self.redis_url.clone()
        } else {
            format!("{}/", self.redis_url)
        }
    }
}
