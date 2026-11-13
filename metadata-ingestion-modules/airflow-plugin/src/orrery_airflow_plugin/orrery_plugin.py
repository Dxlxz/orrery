import contextlib
import os

with contextlib.suppress(Exception):
    if os.getenv("ORRERY_AIRFLOW_PLUGIN_SKIP_FORK_PATCH", "false").lower() not in (
        "true",
        "1",
    ):
        # From https://github.com/apache/airflow/discussions/24463#discussioncomment-4404542
        # I'm not exactly sure why this fixes it, but I suspect it's that this
        # forces the proxy settings to get cached before the fork happens.
        #
        # For more details, see https://github.com/python/cpython/issues/58037
        # and https://wefearchange.org/2018/11/forkmacos.rst.html
        # and https://bugs.python.org/issue30385#msg293958
        # An alternative fix is to set NO_PROXY='*'

        from _scproxy import _get_proxy_settings

        _get_proxy_settings()

from airflow.plugins_manager import AirflowPlugin

from orrery_airflow_plugin.orrery_listener import get_airflow_plugin_listener

if os.getenv("ORRERY_AIRFLOW_PLUGIN_USE_V1_PLUGIN", "false").lower() in ("true", "1"):
    raise RuntimeError("The Orrery Airflow v1 plugin was removed in v1.1.0.5.")


class OrreryPlugin(AirflowPlugin):
    name = "orrery_plugin"

    listeners: list = list(filter(None, [get_airflow_plugin_listener()]))
