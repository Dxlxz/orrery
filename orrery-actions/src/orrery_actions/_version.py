# Published at https://pypi.org/project/orrery-actions/.
__package_name__ = "orrery-actions"
__version__ = "1!0.0.0.dev0"


def is_dev_mode() -> bool:
    return __version__.endswith("dev0")


def nice_version_name() -> str:
    if is_dev_mode():
        return "unavailable (installed in develop mode)"
    return __version__
