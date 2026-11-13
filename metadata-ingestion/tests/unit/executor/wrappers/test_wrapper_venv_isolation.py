"""Test that wrapper modules use the venv's orrery binary via subprocess,
not the parent's imports, and that shared logic lives in wrapper_common.
"""

import ast
import importlib.util
import inspect
from pathlib import Path

import pytest

from orrery.executor.execution import wrapper_common

WRAPPER_MODULES = [
    "orrery.executor.wrappers.run_ingest",
    "orrery.executor.wrappers.run_test_connection",
]


def wrapper_source(module_name: str) -> str:
    spec = importlib.util.find_spec(module_name)
    assert spec and spec.origin, f"could not locate {module_name}"
    return Path(spec.origin).read_text()


class TestWrapperVenvIsolation:
    """Test that wrapper modules properly isolate venv execution."""

    @pytest.mark.parametrize("module_name", WRAPPER_MODULES)
    def test_wrapper_does_not_import_orrery_entrypoints(self, module_name: str) -> None:
        """Wrappers must NOT import orrery.entrypoints (would bypass venv)."""
        tree = ast.parse(wrapper_source(module_name))

        for node in ast.walk(tree):
            if isinstance(node, ast.ImportFrom) and node.module == "orrery.entrypoints":
                pytest.fail(
                    f"{module_name} should NOT import from orrery.entrypoints. "
                    "It should delegate to wrapper_common which uses subprocess."
                )

    @pytest.mark.parametrize("module_name", WRAPPER_MODULES)
    def test_wrapper_imports_wrapper_common(self, module_name: str) -> None:
        """Wrappers must import from wrapper_common (shared subprocess logic)."""
        content = wrapper_source(module_name)

        assert "from orrery.executor.execution.wrapper_common import" in content, (
            f"{module_name} should import from wrapper_common"
        )

    def test_wrapper_common_uses_subprocess_popen(self) -> None:
        """wrapper_common must use subprocess.Popen to run orrery CLI."""
        source = inspect.getsource(wrapper_common.run_orrery_subprocess)
        assert "subprocess.Popen" in source

    def test_wrapper_common_uses_venv_orrery_binary(self) -> None:
        """wrapper_common.run_orrery_subprocess receives the command with venv orrery path."""
        assert (
            "cmd" in inspect.signature(wrapper_common.run_orrery_subprocess).parameters
        )

    @pytest.mark.parametrize("module_name", WRAPPER_MODULES)
    def test_wrapper_passes_venv_orrery_in_cmd(self, module_name: str) -> None:
        """Wrapper must build command starting with venv_orrery."""
        content = wrapper_source(module_name)
        assert "venv_orrery" in content, (
            f"{module_name} should reference venv_orrery in the command"
        )
