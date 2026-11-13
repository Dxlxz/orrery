from orrery_airflow_plugin._version import __package_name__, __version__


def get_provider_info() -> dict:
    # Register our hooks with Airflow.
    return {
        "package-name": f"{__package_name__}",
        "name": f"{__package_name__}",
        "description": "Orrery metadata collector plugin",
        "connection-types": [
            {
                "hook-class-name": "orrery_airflow_plugin.hooks.orrery.OrreryRestHook",
                "connection-type": "orrery-rest",
            },
            {
                "hook-class-name": "orrery_airflow_plugin.hooks.orrery.OrreryKafkaHook",
                "connection-type": "orrery-kafka",
            },
            {
                "hook-class-name": "orrery_airflow_plugin.hooks.orrery.SynchronizedFileHook",
                "connection-type": "orrery-file",
            },
        ],
    }
