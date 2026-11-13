"""
Secret masking system for Orrery ingestion.
"""

__all__ = [
    "SecretMaskingFilter",
    "StreamMaskingWrapper",
    "install_masking_filter",
    "uninstall_masking_filter",
    "SecretRegistry",
    "is_masking_enabled",
    "initialize_secret_masking",
    "get_masking_safe_logger",
]

from orrery.masking.bootstrap import initialize_secret_masking
from orrery.masking.logging_utils import get_masking_safe_logger
from orrery.masking.masking_filter import (
    SecretMaskingFilter,
    StreamMaskingWrapper,
    install_masking_filter,
    uninstall_masking_filter,
)
from orrery.masking.secret_registry import SecretRegistry, is_masking_enabled
