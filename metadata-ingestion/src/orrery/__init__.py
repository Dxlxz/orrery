# These are imported for their side effects, before any other orrery.* module
# (and thus any sqlglot import) can load:
# - _force_pure_python_sqlglot honours ORRERY_SQLGLOT_DISABLE_C by loading sqlglot
#   from .py instead of the mypyc .so extensions. A .pth installs it at interpreter
#   startup for the earliest possible activation; importing it here is the safety
#   net that guarantees it runs whenever a orrery package is imported (including
#   custom Python SDK usage), before orrery touches sqlglot.
# - _setproctitle_patch avoids a SIGSEGV on macOS when a multi-threaded process
#   forks and something calls setproctitle.
# - _pkg_resources_finder appends a sys.meta_path finder that provides a
#   pkg_resources shim when setuptools>=82 has removed it, deferring to a real
#   pkg_resources whenever one is installed.
import orrery._force_pure_python_sqlglot
import orrery._pkg_resources_finder
import orrery._setproctitle_patch
from orrery._version import __package_name__, __version__
