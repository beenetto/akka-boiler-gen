package utils

import com.typesafe.config.{Config, ConfigFactory}

object ConfigFactorySingleton {

  private val MergedConfig: Config = {
    val config            = ConfigFactory.load()
    val envConfigPath     = s"${Option(System.getenv("APP_ENV")).getOrElse("default")}.conf"
    val envConfig: Config = ConfigFactory.parseResources(envConfigPath).withFallback(ConfigFactory.empty())
    envConfig.withFallback(config)
  }

  def loadEnvironmentConfig(): Config = MergedConfig
}
