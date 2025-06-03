from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field

class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file    = ".env",
        env_file_encoding = "utf-8",
        extra       = "ignore",
    )

    # PostgreSQL
    postgres_user     : str = Field(..., env="POSTGRES_USER")
    postgres_password : str = Field(..., env="POSTGRES_PASSWORD")
    postgres_host     : str = Field(..., env="POSTGRES_HOST")
    postgres_port     : int = Field(..., env="POSTGRES_PORT")
    postgres_db       : str = Field(..., env="POSTGRES_DB")

    # WebHDFS NameNode
    hdfs_host         : str = Field(..., env="HDFS_HOST")
    hdfs_port         : int = Field(..., env="HDFS_PORT")
    hdfs_dir          : str = Field(..., env="HDFS_DIR")
    hdfs_user         : str = Field(..., env="HDFS_USER")

    hdfs_namenode_container : str = Field(
        "hadoop-namenode", env="HDFS_NAMENODE_CONTAINER"
    )
    hdfs_tmp_dir           : str = Field(
        "/tmp/bank_accounts", env="HDFS_TMP_DIR"
    )

    upload_dir       : str = Field("uploaded_files", env="UPLOAD_DIR")

    # JWT / Seguridad
    jwt_secret_key             : str = Field(..., env="JWT_SECRET_KEY")
    jwt_algorithm              : str = Field("HS256",    env="JWT_ALGORITHM")
    access_token_expire_minutes: int = Field( 60,          env="ACCESS_TOKEN_EXPIRE_MINUTES")

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+asyncpg://"
            f"{self.postgres_user}:{self.postgres_password}"
            f"@{self.postgres_host}:{self.postgres_port}"
            f"/{self.postgres_db}"
        )

settings = Settings()
