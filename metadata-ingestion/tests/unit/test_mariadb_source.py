from orrery.ingestion.api.common import PipelineContext
from orrery.ingestion.source.sql.mariadb import MariaDBSource
from orrery.ingestion.source.sql.mysql import MySQLConfig, MySQLSource


def test_platform_correctly_set_mariadb():
    source = MariaDBSource(
        ctx=PipelineContext(run_id="mariadb-source-test"),
        config=MySQLConfig(),
    )
    assert source.platform == "mariadb"


def test_platform_correctly_set_mysql():
    source = MySQLSource(
        ctx=PipelineContext(run_id="mysql-source-test"),
        config=MySQLConfig(),
    )
    assert source.platform == "mysql"
