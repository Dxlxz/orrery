from typing import Any, Dict, Optional, Tuple, Type, cast

import pytest
from pydantic import ValidationError

from orrery.configuration.common import ConfigModel
from orrery.ingestion.graph.client import OrreryClientConfig
from orrery.ingestion.source.state.stateful_ingestion_base import (
    DynamicTypedStateProviderConfig,
    StatefulIngestionConfig,
)
from orrery.ingestion.source.state_provider.orrery_ingestion_checkpointing_provider import (
    OrreryIngestionStateProviderConfig,
)

# 0. Common client configs.
orrery_client_full_config = {
    "server": "http://localhost:8080",
    "token": "dummy_test_tok",
    "timeout_sec": 10,
    "extra_headers": {},
    "max_threads": 10,
}


# 1. Orrery Checkpointing State Provider Config test params
checkpointing_provider_config_test_params: Dict[
    str,
    Tuple[
        Type[OrreryIngestionStateProviderConfig],
        Dict[str, Any],
        Optional[OrreryIngestionStateProviderConfig],
        bool,
    ],
] = {
    # Full custom-config
    "checkpointing_valid_full_config": (
        OrreryIngestionStateProviderConfig,
        {
            "orrery_api": orrery_client_full_config,
        },
        OrreryIngestionStateProviderConfig(
            # This test verifies that the max_threads arg is ignored.
            orrery_api=OrreryClientConfig.parse_obj_allow_extras(
                dict(
                    server="http://localhost:8080",
                    token="dummy_test_tok",
                    timeout_sec=10,
                    extra_headers={},
                    max_threads=10,
                ),
            ),
        ),
        False,
    ),
    # Default
    "checkpointing_default": (
        OrreryIngestionStateProviderConfig,
        {
            "orrery_api": None,
        },
        OrreryIngestionStateProviderConfig(
            orrery_api=None,
        ),
        False,
    ),
}


# 2. StatefulIngestion Config test params
stateful_ingestion_config_test_params: Dict[
    str,
    Tuple[
        Type[StatefulIngestionConfig],
        Dict[str, Any],
        Optional[StatefulIngestionConfig],
        bool,
    ],
] = {
    # Ful custom-config
    "stateful_ingestion_full_custom": (
        StatefulIngestionConfig,
        {
            "enabled": True,
            "max_checkpoint_state_size": 1024,
            "state_provider": {
                "type": "orrery",
                "config": orrery_client_full_config,
            },
            "ignore_old_state": True,
            "ignore_new_state": True,
        },
        StatefulIngestionConfig(
            enabled=True,
            max_checkpoint_state_size=1024,
            ignore_old_state=True,
            ignore_new_state=True,
            state_provider=DynamicTypedStateProviderConfig(
                type="orrery",
                config=orrery_client_full_config,
            ),
        ),
        False,
    ),
    # Default disabled
    "stateful_ingestion_default_disabled": (
        StatefulIngestionConfig,
        {},
        StatefulIngestionConfig(
            enabled=False,
            max_checkpoint_state_size=2**24,
            ignore_old_state=False,
            ignore_new_state=False,
            state_provider=None,
        ),
        False,
    ),
    # Default enabled
    "stateful_ingestion_default_enabled": (
        StatefulIngestionConfig,
        {"enabled": True},
        StatefulIngestionConfig(
            enabled=True,
            max_checkpoint_state_size=2**24,
            ignore_old_state=False,
            ignore_new_state=False,
            state_provider=DynamicTypedStateProviderConfig(type="orrery"),
        ),
        False,
    ),
    # Bad Config- throws ValidationError
    "stateful_ingestion_bad_config": (
        StatefulIngestionConfig,
        {"enabled": True, "state_provider": {}},
        None,
        True,
    ),
}

# 4. Combine all of the config params from 1, 2 & 3 above for the common parametrized test.
CombinedTestConfigType = Dict[
    str,
    Tuple[
        Type[ConfigModel],
        Dict[str, Any],
        Optional[ConfigModel],
        bool,
    ],
]

combined_test_configs = {
    **cast(CombinedTestConfigType, checkpointing_provider_config_test_params),
    **cast(CombinedTestConfigType, stateful_ingestion_config_test_params),
}


@pytest.mark.parametrize(
    "config_class, config_dict, expected, raises_exception",
    combined_test_configs.values(),
    ids=combined_test_configs.keys(),
)
def test_state_provider_configs(
    config_class: Type[ConfigModel],
    config_dict: Dict[str, Any],
    expected: Optional[ConfigModel],
    raises_exception: bool,
) -> None:
    if raises_exception:
        with pytest.raises(ValidationError):
            assert expected is None
            config_class.parse_obj_allow_extras(config_dict)
    else:
        config = config_class.parse_obj_allow_extras(config_dict)
        # Using model_dump() here is important. Pydantic will pass through any
        # unknown fields, but those will still impact the equality check.
        # Using model_dump() ensures we only compare the explicitly defined
        # fields.
        assert expected is not None
        assert config.model_dump() == expected.model_dump()
